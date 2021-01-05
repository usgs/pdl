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
import java.time.temporal.ChronoUnit;
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
public class AwsProductReceiver extends DefaultNotificationReceiver implements Runnable, WebSocketListener {

  public static final Logger LOGGER = Logger
          .getLogger(AwsProductReceiver.class.getName());

  public static final String URI_PROPERTY = "url";
  public static final String CREATED_AFTER_PROPERTY = "createdAfter";
  public static final String TRACKING_INDEX_PROPERTY = "trackingIndex";
  public static final String TRACKING_FILE_NAME_PROPERTY = "trackingFileName";
  public static final String CONNECT_ATTEMPTS_PROPERTY = "connectAttempts";
  public static final String CONNECT_TIMEOUT_PROPERTY = "connectTimeout";
  public static final String INITIAL_CATCHUP_AGE_PROPERTY = "initialCatchUpAge";

  public static final String DEFAULT_TRACKING_FILE_NAME = "data/AwsReceiver.json";
  public static final String DEFAULT_CONNECT_ATTEMPTS = "5";
  public static final String DEFAULT_CONNECT_TIMEOUT = "1000";
  public static final String DEFAULT_INITIAL_CATCHUP_AGE = "7.0";

  private URI uri;
  private String trackingFileName;
  private int attempts;
  private long timeout;

  private TrackingIndex trackingIndex;
  private WebSocketClient client;

  /* Websocket session */
  private Session session;

  /* µs timestamp of last message that has been processed */
  protected Instant createdAfter = null;

  /** How far back to check when first connecting. */
  protected double initialCatchUpAge = Double.valueOf(DEFAULT_INITIAL_CATCHUP_AGE);

  /* last broadcast message that has been processed (used for catch up) */
  protected JsonNotification lastBroadcast = null;
  protected Long lastBroadcastId = null;
  /* whether to process broadcast messages (after catching up). */
  protected boolean processBroadcast = false;

  /* whether currenting catching up. */
  protected boolean catchUpRunning = false;
  /* sync object for catchUp state. */
  protected final Object catchUpSync = new Object();
  /* thread where catch up process runs. */
  protected Thread catchUpThread = null;
  /* whether thread should continue running (shutdown flag) */
  protected boolean catchUpThreadRunning = false;
  /* last catch up message sent (for response timeouts) */
  protected Instant lastCatchUpSent = null;

  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    uri = new URI(config.getProperty(URI_PROPERTY));
    attempts = Integer.parseInt(
        config.getProperty(CONNECT_ATTEMPTS_PROPERTY, DEFAULT_CONNECT_ATTEMPTS));
    timeout = Long.parseLong(
        config.getProperty(CONNECT_TIMEOUT_PROPERTY, DEFAULT_CONNECT_TIMEOUT));
    initialCatchUpAge = Double.valueOf(
        config.getProperty(INITIAL_CATCHUP_AGE_PROPERTY, DEFAULT_INITIAL_CATCHUP_AGE));

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
    // save session
    this.session = session;

    // start catch up process
    LOGGER.info("[" + getName() + "] Starting catch up");
    // ignore broadcast until caught up
    processBroadcast = false;
    startCatchUp();
  }

  /**
   * Called when connection is closed, either because shutdown on this end or
   * closed by server.
   */
  @Override
  public void onClose(Session session, CloseReason closeReason) {
    LOGGER.info("[" + getName() + "] onClose " + closeReason.toString());
    this.session = null;

    // cannot catch up when not connected, restart in onOpen
    stopCatchUp();
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
    Long broadcastId = json.getJsonObject("notification").getJsonNumber("id").longValue();
    LOGGER.finer("[" + getName() + "]"
        + " onBroadcast(" + notification.getProductId() + ")"
        + " sequence=" + broadcastId + ", lastSequence=" + lastBroadcastId);

    if (processBroadcast &&
        // sanity check, broadcast ids are expected to increment
        lastBroadcastId != null
        && broadcastId != (lastBroadcastId + 1)
    ) {
      // may have missed message
      LOGGER.info("[" + getName() + "] broadcast ids out of sequence"
          + " (at " + lastBroadcastId + ", received " + broadcastId + ")"
          + ", switching to catch up mode");
      processBroadcast = false;
      startCatchUp();
    }

    // track last broadcast for catch up process (as long as newer)
    if (lastBroadcastId == null || broadcastId > lastBroadcastId) {
      lastBroadcastId = broadcastId;
      lastBroadcast = notification;
    }

    // process message if not in catch up mode
    if (processBroadcast) {
      onJsonNotification(notification);
    }
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
    LOGGER.finer("[" + getName() + "] onProduct(" + notification.getProductId() + ")");
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

    // notify background thread that a response was received,
    // as well as pausing messages until restarted below (if needed)
    stopCatchUp();

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
      startCatchUp();
    }
  }

  /**
   * Catch up process.
   *
   * Do not run directly, use {@link #startCatchUpThread()} and
   * {@link #stopCatchUpThread()} to start and stop the process.
   *
   * Process waits until {@link #startCatchUp()} is called,
   * and uses {@link #throttleQueues()} between sends.
   */
  @Override
  public void run() {
    while (catchUpThreadRunning) {
      try {
        synchronized (catchUpSync) {
          if (!catchUpRunning) {
            catchUpSync.wait();
            continue;
          }
          if (lastCatchUpSent != null) {
            // message already sent, wait for timeout
            Instant now = Instant.now();
            Instant timeout = lastCatchUpSent.plus(60, ChronoUnit.SECONDS);
            if (now.isBefore(timeout)) {
              catchUpSync.wait(now.until(timeout, ChronoUnit.MILLIS));
              continue;
            } else {
              // timed out
              LOGGER.warning("No products_created_after response"
                  + ", sent at " + lastCatchUpSent.toString());
              // fall through
            }
          }
        }

        // ready to send, but block until done throttling
        throttleQueues();

        try {
          synchronized (catchUpSync) {
            // connection may have closed while throttling
            if (!catchUpRunning) {
              continue;
            }
            sendProductsCreatedAfter();
            // track when sent
            lastCatchUpSent = Instant.now();
          }
        } catch (Exception e){
          LOGGER.log(Level.WARNING, "Exception sending products_created_after", e);
          if (catchUpThreadRunning && catchUpRunning) {
            // wait before next attempt
            Thread.sleep(1000);
          }
        }
      } catch (InterruptedException e) {
        // probably stopping
      }
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
      this.createdAfter = Instant.now().minusSeconds(
          Math.round(initialCatchUpAge * 86400));
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
   * Notify running background thread to start catch up process.
   */
  protected void startCatchUp() {
    // notify background thread to start catch up
    synchronized (catchUpSync) {
      catchUpRunning = true;
      // clear sent time
      lastCatchUpSent = null;
      catchUpSync.notify();
    }
  }

  /**
   * Start background thread for catch up process.
   */
  protected void startCatchUpThread() {
    if (catchUpThread != null) {
      throw new IllegalStateException("catchUp thread already started");
    }
    synchronized (catchUpSync) {
      catchUpThreadRunning = true;
      catchUpThread = new Thread(this);
    }
    catchUpThread.start();
  }

  /**
   * Notify running background thread to stop catch up process.
   */
  protected void stopCatchUp() {
    synchronized (catchUpSync) {
      // stop catch up
      catchUpRunning = false;
      // clear sent time
      lastCatchUpSent = null;
      catchUpSync.notify();
    }
  }

  /**
   * Stop background thread for catch up process.
   */
  protected void stopCatchUpThread() {
    if (catchUpThread == null) {
      return;
    }
    // stop catch up thread
    try {
      synchronized (catchUpSync) {
        // orderly shutdown
        catchUpThreadRunning = false;
        catchUpSync.notify();
      }
      // interrupt just in case
      catchUpThread.interrupt();
      catchUpThread.join();
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Error stopping catchUpThread", e);
    } finally {
      catchUpThread = null;
    }
  }

  @Override
  public void onConnectFail() {
    // client failed to connect
    LOGGER.info("[" + getName() + "] onConnectFail");
  }

  @Override
  public void onReconnectFail() {
    // failed to reconnect after close
    LOGGER.info("[" + getName() + "] onReconnectFail");
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

    // open websocket
    client = new WebSocketClient(uri, this, attempts, timeout, true);

    // start catch up process
    startCatchUpThread();
  }

  /**
   * Closes web socket
   * @throws Exception
   */
  @Override
  public void shutdown() throws Exception {
    // stop catch up process
    stopCatchUpThread();
    // close socket
    try {
      client.shutdown();
    } catch (Exception e) {}
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
