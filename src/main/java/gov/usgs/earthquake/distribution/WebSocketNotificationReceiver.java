package gov.usgs.earthquake.distribution;

import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receives notifications from an arbitrary web socket.
 */
public class WebSocketNotificationReceiver extends DefaultNotificationReceiver implements WebSocketListener {

  public static final Logger LOGGER = Logger
          .getLogger(WebSocketNotificationReceiver.class.getName());

  public static final String SERVER_HOST_PROPERTY = "serverHost";
  public static final String SERVER_PORT_PROPERTY = "serverPort";
  public static final String SERVER_PATH_PROPERTY = "serverPath";
  public static final String SEQUENCE_PROPERTY = "sequence";
  public static final String TIMESTAMP_PROPERTY = "timestamp";
  public static final String TRACKING_FILE_NAME_PROPERTY = "trackingFileName";
  public static final String CONNECT_ATTEMPTS_PROPERTY = "connectAttempts";
  public static final String CONNECT_TIMEOUT_PROPERTY = "connectTimeout";
  public static final String RETRY_ON_CLOSE_PROPERTY = "retryOnClose";

  public static final String DEFAULT_SERVER_HOST = "http://www.google.com";
  public static final String DEFAULT_SERVER_PORT = "4222";
  public static final String DEFAULT_SERVER_PATH = "/sequence/";
  public static final String DEFAULT_TRACKING_FILE_NAME = "data/WebSocketReceiverInfo";
  public static final String DEFAULT_CONNECT_ATTEMPTS = "5";
  public static final String DEFAULT_CONNECT_TIMEOUT = "1000";
  public static final String DEFAULT_RETRY_ON_CLOSE = "true";

  public static final String ATTRIBUTE_DATA = "data";

  private String serverHost;
  private String serverPort;
  private String serverPath;
  private String trackingFileName;
  private int attempts;
  private long timeout;
  private boolean retryOnClose;

  private WebSocketClient client;
  private String sequence = "0";


  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    serverHost = config.getProperty(SERVER_HOST_PROPERTY, DEFAULT_SERVER_HOST);
    serverPort = config.getProperty(SERVER_PORT_PROPERTY, DEFAULT_SERVER_PORT);
    serverPath = config.getProperty(SERVER_PATH_PROPERTY, DEFAULT_SERVER_PATH);
    attempts = Integer.parseInt(config.getProperty(CONNECT_ATTEMPTS_PROPERTY, DEFAULT_CONNECT_ATTEMPTS));
    timeout = Long.parseLong(config.getProperty(CONNECT_TIMEOUT_PROPERTY, DEFAULT_CONNECT_TIMEOUT));
    retryOnClose = Boolean.parseBoolean(config.getProperty(RETRY_ON_CLOSE_PROPERTY, DEFAULT_RETRY_ON_CLOSE));
    trackingFileName = config.getProperty(TRACKING_FILE_NAME_PROPERTY, DEFAULT_TRACKING_FILE_NAME);
  }

  /**
   * Reads a sequence from a tracking file if it exists. Otherwise, starting sequence is 0.
   * Connects to web socket
   * @throws Exception
   */
  @Override
  public void startup() throws Exception{
    super.startup();

    //read sequence from tracking file if other parameters agree
    JsonObject json = readTrackingFile();
    if (json != null &&
            json.getString(SERVER_HOST_PROPERTY).equals(serverHost) &&
            json.getString(SERVER_PORT_PROPERTY).equals(serverPort) &&
            json.getString(SERVER_PATH_PROPERTY).equals(serverPath)) {
      sequence = json.getString(SEQUENCE_PROPERTY);
    }

    //open websocket
    client = new WebSocketClient(new URI(serverHost + ":" + serverPort + serverPath + sequence), this, attempts, timeout, true);
  }

  /**
   * Closes web socket
   * @throws Exception
   */
  @Override
  public void shutdown() throws Exception{
    //close socket
    client.shutdown();
    super.shutdown();
  }

  /**
   * Writes tracking file to disc, storing latest sequence
   * @throws Exception
   */
  public void writeTrackingFile() throws Exception {
    JsonObject json = Json.createObjectBuilder()
            .add(SERVER_HOST_PROPERTY,serverHost)
            .add(SERVER_PATH_PROPERTY,serverPath)
            .add(SERVER_PORT_PROPERTY,serverPort)
            .add(SEQUENCE_PROPERTY,sequence)
            .build();

    FileUtils.writeFileThenMove(
            new File(trackingFileName + "_tmp.json"),
            new File(trackingFileName + ".json"),
            json.toString().getBytes());
  }

  /**
   * Reads tracking file from disc
   * @return  JsonObject tracking file
   * @throws Exception
   */
  public JsonObject readTrackingFile() throws Exception {
    JsonObject json = null;

    File trackingFile = new File(trackingFileName + ".json");
    if (trackingFile.exists()) {
      InputStream contents = new ByteArrayInputStream(FileUtils.readFile(trackingFile));
      JsonReader jsonReader = Json.createReader(contents);
      json = jsonReader.readObject();
      jsonReader.close();
    }
    return json;
  }

  @Override
  public void onOpen(Session session) {
    // do nothing
  }

  /**
   * Message handler function passed to WebSocketClient
   * Parses the message as JSON, receives the contained URL notification, and writes the tracking file.
   * @param message
   */
  @Override
  public void onMessage(String message) {
    JsonObject json;
    try (InputStream in = StreamUtils.getInputStream(message); JsonReader reader = Json.createReader(in)) {
      //parse input as json
      json = reader.readObject();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] exception while receiving notification; is it encoded as JSON? ", e);
      return;
    }
    try {
      //convert to URLNotification and receive
      JsonObject dataJson = json.getJsonObject(ATTRIBUTE_DATA);
      URLNotification notification = URLNotificationJSONConverter.parseJSON(dataJson);
      receiveNotification(notification);

      //send heartbeat
      HeartbeatListener.sendHeartbeatMessage(getName(), "nats notification timestamp", json.getJsonNumber(TIMESTAMP_PROPERTY).toString());

      //write tracking file
      sequence = json.getJsonNumber(SEQUENCE_PROPERTY).toString();
      writeTrackingFile();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] exception while processing URLNotification ", e);
    }
  }

  @Override
  public void onClose(Session session, CloseReason closeReason) {
    // do nothing
  }

  @Override
  public void onConnectFail() {
    // do nothing
  }

  @Override
  public void onReconnectFail() {
    // do nothing
  }

  public String getServerHost() {
    return serverHost;
  }

  public void setServerHost(String serverHost) {
    this.serverHost = serverHost;
  }

  public String getServerPort() {
    return serverPort;
  }

  public void setServerPort(String serverPort) {
    this.serverPort = serverPort;
  }

  public String getServerPath() {
    return serverPath;
  }

  public void setServerPath(String serverPath) {
    this.serverPath = serverPath;
  }

  public String getTrackingFileName() {
    return trackingFileName;
  }

  public void setTrackingFileName(String trackingFileName) {
    this.trackingFileName = trackingFileName;
  }

  public String getSequence() {
    return sequence;
  }

  public void setSequence(String sequence) {
    this.sequence = sequence;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
}