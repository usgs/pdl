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

  /** Logger for use in the file */
  public static final Logger LOGGER = Logger
          .getLogger(WebSocketNotificationReceiver.class.getName());

  /** Property for serverHost */
  public static final String SERVER_HOST_PROPERTY = "serverHost";
  /** Property for serverPort */
  public static final String SERVER_PORT_PROPERTY = "serverPort";
  /** Property for serverPath */
  public static final String SERVER_PATH_PROPERTY = "serverPath";
  /** Property for sequence */
  public static final String SEQUENCE_PROPERTY = "sequence";
  /** Property for timestamp */
  public static final String TIMESTAMP_PROPERTY = "timestamp";
  /** Property for trackingFileName */
  public static final String TRACKING_FILE_NAME_PROPERTY = "trackingFileName";
  /** Property for connectAttempts */
  public static final String CONNECT_ATTEMPTS_PROPERTY = "connectAttempts";
  /** Property for connectTimeout */
  public static final String CONNECT_TIMEOUT_PROPERTY = "connectTimeout";
  /** Property for retryOnClose */
  public static final String RETRY_ON_CLOSE_PROPERTY = "retryOnClose";

  /** Default server host */
  public static final String DEFAULT_SERVER_HOST = "http://www.google.com";
  /** Default server port */
  public static final String DEFAULT_SERVER_PORT = "4222";
  /** Default server path */
  public static final String DEFAULT_SERVER_PATH = "/sequence/";
  /** Default tracking file */
  public static final String DEFAULT_TRACKING_FILE_NAME = "data/WebSocketReceiverInfo";
  /** Default number of connect attempts */
  public static final String DEFAULT_CONNECT_ATTEMPTS = "5";
  /** Default timeout in ms */
  public static final String DEFAULT_CONNECT_TIMEOUT = "1000";
  /** Default condiction for retry on close */
  public static final String DEFAULT_RETRY_ON_CLOSE = "true";
  /** attribute for data */
  public static final String ATTRIBUTE_DATA = "data";

  private String serverHost;
  private String serverPort;
  private String serverPath;
  private String trackingFileName;
  private int attempts;
  private long timeout;

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
    trackingFileName = config.getProperty(TRACKING_FILE_NAME_PROPERTY, DEFAULT_TRACKING_FILE_NAME);
  }

  /**
   * Reads a sequence from a tracking file if it exists. Otherwise, starting sequence is 0.
   * Connects to web socket
   * @throws Exception if error occurs
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
   * @throws Exception if error occurs
   */
  @Override
  public void shutdown() throws Exception{
    //close socket
    client.shutdown();
    super.shutdown();
  }

  /**
   * Writes tracking file to disc, storing latest sequence
   * @throws Exception if error occurs
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
   * @throws Exception if error occurs
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
   * @param message String
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
      HeartbeatListener.sendHeartbeatMessage(getName(), "nats notification timestamp", json.getString(TIMESTAMP_PROPERTY));

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

  /** @return serverHost */
  public String getServerHost() {
    return serverHost;
  }

  /** @param serverHost to set */
  public void setServerHost(String serverHost) {
    this.serverHost = serverHost;
  }

  /** @return serverPort */
  public String getServerPort() {
    return serverPort;
  }

  /** @param serverPort to set */
  public void setServerPort(String serverPort) {
    this.serverPort = serverPort;
  }

  /** @return serverPath */
  public String getServerPath() {
    return serverPath;
  }

  /** @param serverPath to set */
  public void setServerPath(String serverPath) {
    this.serverPath = serverPath;
  }

  /** @return trackingFileName */
  public String getTrackingFileName() {
    return trackingFileName;
  }

  /** @param trackingFileName to set */
  public void setTrackingFileName(String trackingFileName) {
    this.trackingFileName = trackingFileName;
  }

  /** @return sequence */
  public String getSequence() {
    return sequence;
  }

  /** @param sequence to set */
  public void setSequence(String sequence) {
    this.sequence = sequence;
  }

  /** @return attempts */
  public int getAttempts() {
    return attempts;
  }

  /** @param attempts to set */
  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  /** @return timeout */
  public long getTimeout() {
    return timeout;
  }

  /** @param timeout to set */
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
}