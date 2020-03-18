package gov.usgs.earthquake.distribution;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

/**
 * Manages a simple connection to a websocket. Can also be overridden for more complex behavior.
 */
@ClientEndpoint
public class WebSocketClient implements Runnable {

  public static final long DEFAULT_TIMEOUT_MILLIS = 30000;

  private Session session;
  private URI endpoint;
  private WebSocketListener listener;
  private Thread thread;
  private long timeoutMillis;
  private boolean stopThread;
  private boolean doReconnect;
  private Object sync = new Object();

  public WebSocketClient (URI endpoint, WebSocketListener listener) {
    this (endpoint, listener, DEFAULT_TIMEOUT_MILLIS);
  }

  public WebSocketClient(URI endpoint, WebSocketListener listener, long timeoutMillis) {
    this.listener = listener;
    this.endpoint = endpoint;
    this.timeoutMillis = timeoutMillis;
  }


  public void startup() {
    stopThread = false;
    this.thread = new Thread(this);
    this.thread.start();
  }

  public void shutdown() throws Exception {
    // bring down thread
    stopThread = true;
    thread.interrupt();
    thread.join();
  }

  public void run() {
    synchronized (sync) {
      while (!stopThread) {
        doReconnect = false;
        try {
          connect();
          if (!doReconnect) {
            sync.wait();
          }
        } catch (InterruptedException e1) {
          // likely told to stop
          stopThread = true;
        } catch (Exception e2) {
          // unresolvable problem connecting; try to resolve with listener
          try {
            listener.onConnectException(e2);
          } catch (Exception e3) {
            // unable to handle; quit
            stopThread = true;
          }
        }
      }

      // make sure we're disconnected
      if (this.session != null && this.session.isOpen()) {
        try {
          this.session.close();
        } catch (IOException e) {
          // ignore IOException; just close
        }
      }
    }
    listener.onDisconnect();
  }

  public void connect() throws Exception {
    // try to connect to server
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    try {
      container.connectToServer(this, endpoint);
    } catch (Exception e1) {
      try {
        this.onConnectException(e1);
      } catch (Exception e2) {
        throw e2;
      }
    }
  }

  private void onConnectException(Exception e) throws Exception {
    if (e instanceof IOException || e instanceof DeploymentException) {
      reconnect();
    } else {
      throw e;
    }
  }

  private void reconnect() {
    try {
      Thread.sleep(timeoutMillis);
    } catch (InterruptedException e) {
      // told to stop
      stopThread = true;
    }
    doReconnect = true;
  }

  @OnOpen
  public void onOpen(Session session) {
    this.session = session;
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    // reconnect if abnormal closure
    if (reason.getCloseCode().getCode() != 1000) {
      reconnect();
    } else {
      this.session = null;
    }
  }

  @OnMessage
  public void onMessage(String message){
    this.listener.onMessage(message);
  }

  public void setListener(WebSocketListener listener) {
    this.listener = listener;
  }

  public boolean isConnected() {
    return this.session != null && this.session.isOpen();
  }

}
