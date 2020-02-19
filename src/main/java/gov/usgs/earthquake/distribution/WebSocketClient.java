package gov.usgs.earthquake.distribution;

import javax.websocket.*;
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

  public static final int DEFAULT_ATTEMPTS = 3;
  public static final long DEFAULT_TIMEOUT_MILLIS = 100;
  public static final boolean DEFAULT_RETRY_ON_CLOSE = true;

  /**
   * Constructs the client. Also connects to the server.
   *
   * @param endpoint the URI to connect to
   * @param listener a WebSocketListener to handle incoming messages
   * @param attempts an integer number of times to try the connection
   * @param timeoutMillis a long for the wait time between attempts
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

  public WebSocketClient(URI endpoint, WebSocketListener listener) throws Exception {
    this(endpoint, listener, DEFAULT_ATTEMPTS, DEFAULT_TIMEOUT_MILLIS, DEFAULT_RETRY_ON_CLOSE);
  }

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

  @OnOpen
  public void onOpen(Session session) {
    this.session = session;
    this.listener.onOpen(session);
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
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

  @OnMessage
  public void onMessage(String message){
    this.listener.onMessage(message);
  }

  public void shutdown() throws Exception {
    this.retryOnClose = false;
    this.session.close();
  }

  public void setListener(WebSocketListener listener) {
    this.listener = listener;
  }

  public boolean isConnected() {
    return this.session != null && this.session.isOpen();
  }

}
