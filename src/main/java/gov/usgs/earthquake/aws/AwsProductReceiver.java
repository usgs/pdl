package gov.usgs.earthquake.aws;

import gov.usgs.earthquake.distribution.DefaultNotificationReceiver;
import gov.usgs.earthquake.distribution.HeartbeatListener;
import gov.usgs.earthquake.distribution.WebSocketClient;
import gov.usgs.earthquake.distribution.WebSocketListener;
import gov.usgs.util.Config;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receives notifications from a PDL notification web socket.
 *
 * After initial connection, ignores broadcasts until catch up process is complete.
 *
 * Catch up involves sending a "products_created_after" request with the latest
 * notification "created" timestamp, and processing products until either the
 * last product matches the last broadcast or there are no more products after
 * the latest notification "created" timestamp.
 */
public class AwsProductReceiver extends DefaultNotificationReceiver implements WebSocketListener {

  public static final Logger LOGGER = Logger
          .getLogger(AwsProductReceiver.class.getName());

  public static final String URI_PROPERTY = "url";
  public static final String CREATED_AFTER_PROPERTY = "createdAfter";
  public static final String TRACKING_INDEX_PROPERTY = "trackingIndex";
  public static final String TRACKING_FILE_NAME_PROPERTY = "trackingFileName";
  public static final String CONNECT_ATTEMPTS_PROPERTY = "connectAttempts";
  public static final String CONNECT_TIMEOUT_PROPERTY = "connectTimeout";

  public static final String DEFAULT_TRACKING_FILE_NAME = "data/AwsReceiver.json";
  public static final String DEFAULT_CONNECT_ATTEMPTS = "5";
  public static final String DEFAULT_CONNECT_TIMEOUT = "1000";

  private URI uri;
  private String trackingFileName;
  private int attempts;
  private long timeout;

  private TrackingIndex trackingIndex;
  private WebSocketClient client;

  /* Websocket session */
  private Session session;

  /* µs timestamp of last message that has been processed */
  private Instant createdAfter = null;
  private JsonNotification lastBroadcast = null;
  private boolean processBroadcast = false;

  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    uri = new URI(config.getProperty(URI_PROPERTY));
    attempts = Integer.parseInt(config.getProperty(CONNECT_ATTEMPTS_PROPERTY, DEFAULT_CONNECT_ATTEMPTS));
    timeout = Long.parseLong(config.getProperty(CONNECT_TIMEOUT_PROPERTY, DEFAULT_CONNECT_TIMEOUT));

    final String trackingIndexName = config.getProperty(TRACKING_INDEX_PROPERTY);
    if (trackingIndexName != null) {
      LOGGER.config("[" + getName() + "] loading tracking index "
          + trackingIndexName);
      try {
        // read object from global config
        trackingIndex = (TrackingIndex) Config.getConfig().getObject(trackingIndexName);
      } catch (Exception e) {
        LOGGER.log(
            Level.WARNING,
            "[" + getName() + "] error loading tracking index "
                + trackingIndexName,
            e);
      }
    } else {
      trackingFileName = config.getProperty(TRACKING_FILE_NAME_PROPERTY);
      if (trackingFileName != null) {
        LOGGER.config("[" + getName() + "] creating tracking index at"
            + trackingFileName);
        trackingIndex = new TrackingIndex(
            TrackingIndex.DEFAULT_DRIVER,
            "jdbc:sqlite:" + trackingFileName);
      }
    }
  }

  /**
   * Called when connection is first opened.
   *
   * Start catch up process.
   */
  @Override
  public void onOpen(Session session) throws IOException {
    LOGGER.info("[" + getName() + "] onOpen connection_id=" + session.getId());
    // ignore broadcast until caught up
    processBroadcast = false;
    // save session
    this.session = session;
    // start catch up process
    try {
      LOGGER.info("[" + getName() + "] Starting catch up");
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
   *
   * Parses the message as JSON, and checks "action" property to route message
   * for handling.
   *
   * Synchronized to process messages in order, since onProductsCreatedAfter
   * compares state of latest product to determine whether caught up and if
   * broadcasts should be processed.
   *
   * @param message
   */
  @Override
  synchronized public void onMessage(String message) throws IOException {
    try (final JsonReader reader = Json.createReader(new StringReader(message))) {
      // parse message
      final JsonObject json = reader.readObject();
      final String action = json.getString("action");

      if ("broadcast".equals(action)) {
        onBroadcast(json);
      } else if ("product".equals(action)) {
        onProduct(json);
      } else if ("products_created_after".equals(action)) {
        onProductsCreatedAfter(json);
      }
    } catch (Exception e) {
      LOGGER.log(
          Level.WARNING,
          "[" + getName() + "] exception while processing message '" + message + "'",
          e);
      throw new IOException(e);
    }
  }

  /**
   * Handle a message with "action"="broadcast".
   *
   * If caught up process notification as usual, otherwise save notification
   * to help detect when caught up.
   *
   * @param json
   * @throws Exception
   */
  protected void onBroadcast(final JsonObject json) throws Exception {
    final JsonNotification notification = new JsonNotification(
        json.getJsonObject("notification"));
    lastBroadcast = notification;
    LOGGER.info("[" + getName() + "] onBroadcast(" + notification.getProductId() + ")");
    if (!processBroadcast) {
      return;
    }
    onJsonNotification(notification);
  }

  /**
   * Process a received notification and update current "created" timestamp.
   *
   * @param notification
   * @throws Exception
   */
  protected void onJsonNotification(final JsonNotification notification) throws Exception {
    // receive and notify listeners
    receiveNotification(notification);
    // update tracking file
    this.createdAfter = notification.created;
    writeTrackingData();
    // send heartbeat
    HeartbeatListener.sendHeartbeatMessage(getName(), "createdAfter", createdAfter.toString());
  }

  /**
   * Handle a message with "action"="product", which is received during catch up.
   *
   * @param json
   * @throws Exception
   */
  protected void onProduct(final JsonObject json) throws Exception {
    final JsonNotification notification = new JsonNotification(
        json.getJsonObject("notification"));
    LOGGER.info("[" + getName() + "] onProduct(" + notification.getProductId() + ")");
    onJsonNotification(notification);
  }

  /**
   * Handle a message with "action"="products_created_after", which is received
   * during catch up.
   *
   * Indicates the end of a response from a "products_created_after" request.
   * Check whether caught up, and either switch to broadcast mode or continue
   * catch up process.
   *
   * @param json
   * @throws Exception
   */
  protected void onProductsCreatedAfter(final JsonObject json) throws Exception {
    final String after = json.getString("created_after");
    final int count = json.getInt("count");
    LOGGER.finer("[" + getName() + "] onProductsCreatedAfter(" + after
        + ", " + count + " products)");
    // check whether caught up
    if (
        // if a broadcast received during catchup,
        (lastBroadcast != null &&
            // and createdAfter is at or after last broadcast
            createdAfter.compareTo(lastBroadcast.created) >= 0)
        // or no additional products returned
        || (lastBroadcast == null && count == 0)
    ) {
      // caught up
      LOGGER.info("[" + getName() + "] Caught up, switching to broadcast");
      processBroadcast = true;
    } else {
      // keep catching up
      sendProductsCreatedAfter();
    }
  }

  /**
   * Send an "action"="products_created_after" request, which is part of the
   * catch up process.
   *
   * The server will reply with zero or more "action"="product" messages, and
   * then one "action"="products_created_after" message to indicate the request
   * is complete.
   */
  protected void sendProductsCreatedAfter() throws IOException {
    // set default for created after
    if (this.createdAfter == null) {
      this.createdAfter = Instant.now().minusSeconds(7 * 86400);
    }
    String request = Json.createObjectBuilder()
        .add("action", "products_created_after")
        .add("created_after", this.createdAfter.toString())
        .build()
        .toString();
    LOGGER.fine("[" + getName() + "] Sending " + request);
    // start catch up process
    this.session.getBasicRemote().sendText(request);
  }

  /**
   * Called when connection is closed, either because shutdown on this end or
   * closed by server.
   */
  @Override
  public void onClose(Session session, CloseReason closeReason) {
    LOGGER.info("[" + getName() + "] onClose " + closeReason.toString());
    this.session = null;
  }

  @Override
  public void onConnectFail() {
    // client failed to connect
  }

  @Override
  public void onReconnectFail() {
    // failed to reconnect after close
  }

  /**
   * Reads createdAfter from a tracking file if it exists,
   * then connects to web socket.
   *
   * @throws Exception
   */
  @Override
  public void startup() throws Exception{
    super.startup();
    if (trackingIndex == null) {
      trackingIndex = new TrackingIndex();
    }
    trackingIndex.startup();

    //read sequence from tracking file if other parameters agree
    JsonObject json = readTrackingData();
    if (json != null && json.getString(URI_PROPERTY).equals(uri.toString())) {
      createdAfter = Instant.parse(json.getString(CREATED_AFTER_PROPERTY));
    }

    //open websocket
    client = new WebSocketClient(uri, this, attempts, timeout, true);
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
   * Reads tracking file.
   *
   * @return  JsonObject tracking file
   * @throws Exception
   */
  public JsonObject readTrackingData() throws Exception {
    // use name as key
    return trackingIndex.getTrackingData(getName());
  }

  /**
   * Writes tracking file.
   *
   * @throws Exception
   */
  public void writeTrackingData() throws Exception {
    JsonObject json = Json.createObjectBuilder()
            .add(URI_PROPERTY, uri.toString())
            .add(CREATED_AFTER_PROPERTY, createdAfter.toString())
            .build();
    // use name as key
    trackingIndex.setTrackingData(getName(), json);
  }

  public URI getURI() {
    return uri;
  }

  public void setURI(final URI uri) {
    this.uri = uri;
  }

  public String getTrackingFileName() {
    return trackingFileName;
  }

  public void setTrackingFileName(final String trackingFileName) {
    this.trackingFileName = trackingFileName;
  }

  public Instant getCreatedAfter() {
    return createdAfter;
  }

  public void setCreatedAfter(final Instant createdAfter) {
    this.createdAfter = createdAfter;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(final int attempts) {
    this.attempts = attempts;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(final long timeout) {
    this.timeout = timeout;
  }
}