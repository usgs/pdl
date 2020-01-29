package gov.usgs.earthquake.distribution;

import javax.websocket.*;
import java.net.URI;

/**
 * Manages a simple connection to a websocket. Can also be overridden for more complex behavior.
 */
@ClientEndpoint
public class WebSocketClient {

  private Session session;
  private WebSocketListener listener;

  /**
   * Constructs the client. Also connects to the server.
   *
   * @param endpoint the URI to connect to
   * @param listener a WebSocketListener to handle incoming messages
   * @param attempts an integer number of times to try the connection
   * @param timeoutMillis a double for the time span to connect over; divided over the number of attempts
   * @throws Exception on thread interrupt or connection failure
   */
  public WebSocketClient(URI endpoint, WebSocketListener listener, int attempts, double timeoutMillis) throws Exception {
    this.listener = listener;

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
        Thread.sleep((long) timeoutMillis/attempts);
      }
    }

    // throw connect exception if all attempts fail
    if (failedAttempts == attempts) {
      throw lastExcept;
    }
  }

  @OnOpen
  public void onOpen(Session session) {
    this.session = session;
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    this.session = null;
  }

  @OnMessage
  public void onMessage(String message){
    listener.onMessage(message);
  }

  public void shutdown() throws Exception{
    this.session.close();
  }

  public void setListener(WebSocketListener listener) {
    this.listener = listener;
  }

  public boolean isConnected() {
    return this.session != null && this.session.isOpen();
  }

}
