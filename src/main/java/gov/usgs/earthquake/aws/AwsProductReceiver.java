package gov.usgs.earthquake.aws;

import gov.usgs.earthquake.distribution.DefaultNotificationReceiver;
import gov.usgs.earthquake.distribution.HeartbeatListener;
import gov.usgs.earthquake.distribution.WebSocketClient;
import gov.usgs.earthquake.distribution.WebSocketListener;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receives notifications from an arbitrary web socket.
 */
public class AwsProductReceiver extends DefaultNotificationReceiver implements WebSocketListener {

  public static final Logger LOGGER = Logger
          .getLogger(AwsProductReceiver.class.getName());

  public static final String SERVER_HOST_PROPERTY = "serverHost";
  public static final String SERVER_PORT_PROPERTY = "serverPort";
  public static final String SERVER_PATH_PROPERTY = "serverPath";
  public static final String CREATED_AFTER_PROPERTY = "createdAfter";

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

  private WebSocketClient client;

  /* Websocket session */
  private Session session;

  /* µs timestamp of last message that has been processed */
  private Timestamp createdAfter = null;
  private JsonNotification lastBroadcast = null;
  private boolean processBroadcast = false;

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

  @Override
  public void onOpen(Session session) {
    LOGGER.info("[" + getName() + "] onOpen " + session.toString());
    // ignore broadcast until caught up
    processBroadcast = false;
    // save session
    this.session = session;
    // start catch up process
    try {
      sendProductsCreatedAfter();
    } catch (Exception e) {
      LOGGER.log(
          Level.WARNING,
          "[" + getName() + "] exception while starting catch up",
          e);
    }
  }

  /**
   * Message handler function passed to WebSocketClient
   * Parses the message as JSON, receives the contained URL notification, and writes the tracking file.
   * @param message
   */
  @Override
  synchronized public void onMessage(String message) {
    try (final JsonReader reader = Json.createReader(new StringReader(message))) {
      // parse message
      final JsonObject json = reader.readObject();
      final String action = json.getString("action");

      if ("broadcast".equals(action)) {
        onBroadcast(json);
      } else if ("products_created_after".equals(action)) {
        onProductsCreatedAfter(json);
      }
    } catch (Exception e) {
      LOGGER.log(
          Level.WARNING,
          "[" + getName() + "] exception while processing message '" + message + "'",
          e);
    }
  }

  protected void onBroadcast(final JsonObject json) throws Exception {
    final JsonNotification notification = new JsonNotification(
        json.getJsonObject("notification"));
    lastBroadcast = notification;
    if (!processBroadcast) {
      return;
    }
    onJsonNotification(notification);
  }

  protected void onJsonNotification(final JsonNotification notification) throws Exception {
    // receive and notify listeners
    receiveNotification(notification);
    // update tracking file
    this.createdAfter = notification.created;
    writeTrackingFile();
    // send heartbeat
    HeartbeatListener.sendHeartbeatMessage(getName(), "createdAfter", createdAfter.toInstant().toString());
  }

  protected void onProductsCreatedAfter(final JsonObject json) throws Exception {
    LOGGER.finer("[" + getName() + "] Received products created after " +
        json.getString("created_after"));
    // receive products
    JsonArray products = json.getJsonArray("products");
    for (JsonValue value : products) {
      onJsonNotification(new JsonNotification(value.asJsonObject()));
    }
    // check whether caught up
    if (
        // if a broadcast received during catchup,
        (lastBroadcast != null &&
            // and createdAfter is at or after last broadcast
            createdAfter.compareTo(lastBroadcast.created) >= 0)
        // or no additional products returned
        || (products.size() == 0)
    ) {
      // caught up
      processBroadcast = true;
    } else {
      // keep catching up
      sendProductsCreatedAfter();
    }
  }

  protected void sendProductsCreatedAfter() throws Exception {
    // set default for created after
    if (this.createdAfter == null) {
      this.createdAfter = new Timestamp(new Date().getTime() - 7 * 86400 * 1000);
    }
    String request = Json.createObjectBuilder()
        .add("action", "products_created_after")
        .add("created_after", this.createdAfter.toInstant().toString())
        .build()
        .toString();
    LOGGER.finer("[" + getName() + "] Sending " + request);
    // start catch up process
    this.session.getBasicRemote().sendText(request);
  }

  @Override
  public void onClose(Session session, CloseReason closeReason) {
    LOGGER.info("[" + getName() + "] onClose " + closeReason.toString());
    this.session = null;
  }

  @Override
  public void onConnectFail() {
    // do nothing
  }

  @Override
  public void onReconnectFail() {
    // do nothing
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
      createdAfter = Timestamp.valueOf(json.getString(CREATED_AFTER_PROPERTY));
    }

    //open websocket
    client = new WebSocketClient(
        new URI(serverHost + ":" + serverPort + serverPath),
        this, attempts, timeout, true);
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
   * Reads tracking file from disc
   * @return  JsonObject tracking file
   * @throws Exception
   */
  public JsonObject readTrackingFile() throws Exception {
    JsonObject json = null;

    File trackingFile = new File(trackingFileName);
    if (trackingFile.exists()) {
      InputStream contents = new ByteArrayInputStream(FileUtils.readFile(trackingFile));
      JsonReader jsonReader = Json.createReader(contents);
      json = jsonReader.readObject();
      jsonReader.close();
    }
    return json;
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
            .add(CREATED_AFTER_PROPERTY, createdAfter.toInstant().toString())
            .build();

    FileUtils.writeFileThenMove(
            new File(trackingFileName + "_tmp"),
            new File(trackingFileName),
            json.toString().getBytes());
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

  public Timestamp getCreatedAfter() {
    return createdAfter;
  }

  public void setCreatedAfter(Timestamp createdAfter) {
    this.createdAfter = createdAfter;
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