package gov.usgs.earthquake.distribution;

import javax.websocket.*;

import java.io.IOException;
import java.net.URI;

/**
 * Manages a simple connection to a websocket. Can also be overridden for more complex behavior.
 */
@ClientEndpoint
public class WebSocketClient {

  private Session session;

  private URI endpoint;
  private WebSocketListener listener;
  private int attempts;
  private long timeoutMillis;
  private boolean retryOnClose;

  /** Default number of attempts */
  public static final int DEFAULT_ATTEMPTS = 3;
  /** Default timeout in ms */
  public static final long DEFAULT_TIMEOUT_MILLIS = 100;
  /** Default for trying to retry on close */
  public static final boolean DEFAULT_RETRY_ON_CLOSE = true;

  /**
   * Constructs the client. Also connects to the server.
   *
   * @param endpoint the URI to connect to
   * @param listener a WebSocketListener to handle incoming messages
   * @param attempts an integer number of times to try the connection
   * @param timeoutMillis a long for the wait time between attempts
   * @param retryOnClose boolean for if the connection should retry when closed
   * @throws Exception on thread interrupt or connection failure
   */
  public WebSocketClient(URI endpoint, WebSocketListener listener, int attempts, long timeoutMillis, boolean retryOnClose) throws Exception {
    this.listener = listener;
    this.endpoint = endpoint;
    this.attempts = attempts;
    this.timeoutMillis = timeoutMillis;
    this.retryOnClose = retryOnClose;

    connect();
  }

  /**
   * Constructs the client
   * @param endpoint the URI to connect to
   * @param listener a WebSocketListener to handle incoming messages
   * @throws Exception thread interrupt or connection failure
   */
  public WebSocketClient(URI endpoint, WebSocketListener listener) throws Exception {
    this(endpoint, listener, DEFAULT_ATTEMPTS, DEFAULT_TIMEOUT_MILLIS, DEFAULT_RETRY_ON_CLOSE);
  }

  /**
   * Connect to server
   * @throws Exception if error occurs
   */
  public void connect() throws Exception {
    // try to connect to server
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    int failedAttempts = 0;
    Exception lastExcept = null;
    for (int i = 0; i < attempts; i++) {
      try {
        container.connectToServer(this, endpoint);
        break;
      } catch (Exception e) {
        // increment failed attempts, sleep
        failedAttempts++;
        lastExcept = e;
        Thread.sleep(timeoutMillis);
      }
    }

    // throw connect exception if all attempts fail
    if (failedAttempts == attempts) {
      this.listener.onConnectFail();
      throw lastExcept;
    }
  }

  /**
   * Sets the session and listener
   * @param session Session
   * @throws IOException if IO error occurs
   */
  @OnOpen
  public void onOpen(Session session) throws IOException {
    this.session = session;
    this.listener.onOpen(session);
  }

  /**
   * Closes the session on the lister, sets constructor session to null
   * Check if should be retryed
   * @param session Session
   * @param reason for close
   * @throws IOException if IO error occurs
   */
  @OnClose
  public void onClose(Session session, CloseReason reason) throws IOException {
    this.listener.onClose(session, reason);
    this.session = null;
    if (retryOnClose) {
      try {
        this.connect();
      } catch (Exception e) {
        // failed to reconnect
        this.listener.onReconnectFail();
      }
    }
  }

  /**
   * Gives listener the message
   * @param message String
   * @throws IOException if IO error occurs
   */
  @OnMessage
  public void onMessage(String message) throws IOException {
    this.listener.onMessage(message);
  }

  /**
   * Sets retry to false, then closes session
   * @throws Exception if error occurs
   */
  public void shutdown() throws Exception {
    this.retryOnClose = false;
    this.session.close();
  }

  /** @param listener set WebSocketListener */
  public void setListener(WebSocketListener listener) {
    this.listener = listener;
  }

  /**
   * Checks if there is an open session
   * @return boolean
   * @throws IOException if IO error occurs
   */
  public boolean isConnected() throws IOException {
    return this.session != null && this.session.isOpen();
  }

}
