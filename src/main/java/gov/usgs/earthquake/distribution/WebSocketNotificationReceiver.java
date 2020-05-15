package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.util.JSONTrackingFile;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
  public static final String RECONNECT_PROPERTY = "reconnect";
  public static final String RECONNECT_INTERVAL_PROPERTY = "reconnectInterval";

  //TODO: Improve defaults
  public static final String DEFAULT_SERVER_HOST = "http://www.google.com";
  public static final String DEFAULT_SERVER_PORT = "4222";
  public static final String DEFAULT_SERVER_PATH = "/sequence/";
  public static final String DEFAULT_TRACKING_FILE_NAME = "data/WebSocketReceiverInfo.json";
  public static final String DEFAULT_RECONNECT = "true";

  public static final String ATTRIBUTE_DATA = "data";

  private String serverHost;
  private String serverPort;
  private String serverPath;
  private long reconnectInterval;
  private boolean reconnect;
  private JSONTrackingFile trackingFile;

  private WebSocketClient client;
  private String sequence = "0";

  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    serverHost = config.getProperty(SERVER_HOST_PROPERTY, DEFAULT_SERVER_HOST);
    serverPort = config.getProperty(SERVER_PORT_PROPERTY, DEFAULT_SERVER_PORT);
    serverPath = config.getProperty(SERVER_PATH_PROPERTY, DEFAULT_SERVER_PATH);
    reconnect = Boolean.parseBoolean(config.getProperty(RECONNECT_PROPERTY, DEFAULT_RECONNECT));
    String reconnectIntervalString = config.getProperty(RECONNECT_INTERVAL_PROPERTY);
    if (reconnectIntervalString == null) {
      reconnectInterval = WebSocketClient.DEFAULT_RECONNECT_INTERVAL;
    } else {
      reconnectInterval = Long.parseLong(reconnectIntervalString);
    }

    String trackingFileName = config.getProperty(TRACKING_FILE_NAME_PROPERTY, DEFAULT_TRACKING_FILE_NAME);
    trackingFile = new JSONTrackingFile(trackingFileName);
  }

  /**
   * Reads a sequence from a tracking file if it exists. Otherwise, starting sequence is 0.
   * Connects to web socket
   * @throws Exception
   */
  @Override
  public void startup() throws Exception{
    super.startup();

    // try to read tracking file
    if (trackingFile == null) {
      trackingFile = new JSONTrackingFile(DEFAULT_TRACKING_FILE_NAME);
    }
    readTrackingFile();

    //open websocket
    client = new WebSocketClient(new URI(serverHost + ":" + serverPort + serverPath + sequence), this, reconnect, reconnectInterval);
    client.startup();
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

  public void readTrackingFile() throws Exception {
    JsonObject json = trackingFile.read();
    if (json != null &&
            json.getString(SERVER_HOST_PROPERTY).equals(serverHost) &&
            json.getString(SERVER_PORT_PROPERTY).equals(serverPort) &&
            json.getString(SERVER_PATH_PROPERTY).equals(serverPath)) {
      sequence = json.getString(SEQUENCE_PROPERTY);
    }
  }

  public void writeTrackingFile() throws Exception {
    trackingFile.write(
      Json.createObjectBuilder()
        .add(SERVER_HOST_PROPERTY, serverHost)
        .add(SERVER_PATH_PROPERTY,serverPath)
        .add(SERVER_PORT_PROPERTY,serverPort)
        .add(SEQUENCE_PROPERTY,sequence)
        .build()
    );
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
  public void onConnectException(Exception e) throws Exception {
    LOGGER.log(Level.WARNING, "[" + getName() + "] exception connecting to socket:", e);
  }

  @Override
  public void onOpen() {
    LOGGER.log(Level.FINE, "[" + getName() + "] connection opened");
  }

  @Override
  public void onClose() {
    LOGGER.log(Level.WARNING, "[" + getName() + "] socket closed");
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

  public URI getURI() throws URISyntaxException {
    return new URI(serverHost + ":" + serverPort + serverPath + sequence);
  }

  public JSONTrackingFile getTrackingFile() {
    return this.trackingFile;
  }

  public void setTrackingFile(JSONTrackingFile trackingFile) {
    this.trackingFile = trackingFile;
  }

  public String getSequence() {
    return sequence;
  }

  public void setSequence(String sequence) {
    this.sequence = sequence;
  }

  public long getReconnectInterval() {
    return reconnectInterval;
  }

  public void setReconnectInterval(long reconnectInterval) {
    this.reconnectInterval = reconnectInterval;
  }

  public boolean isReconnect() {
    return reconnect;
  }

  public void setReconnect(boolean reconnect) {
    this.reconnect = reconnect;
  }
}