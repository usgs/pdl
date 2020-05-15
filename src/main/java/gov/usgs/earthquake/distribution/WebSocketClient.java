package gov.usgs.earthquake.distribution;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

/**
 * Manages a simple connection to a websocket. Can also be overridden for more complex behavior.
 */
@ClientEndpoint
public class WebSocketClient implements Runnable {

  public static final long DEFAULT_RECONNECT_INTERVAL = 30000;

  private Session session;
  private URI endpoint;
  private WebSocketListener listener;
  private Thread thread;
  private long reconnectInterval;
  private boolean stopThread;
  private boolean reconnect;
  private final Object close = new Object();

  public WebSocketClient (URI endpoint, WebSocketListener listener) {
    this (endpoint, listener, true, DEFAULT_RECONNECT_INTERVAL);
  }

  public WebSocketClient(URI endpoint, WebSocketListener listener, boolean reconnect, long reconnectInterval) {
    this.listener = listener;
    this.endpoint = endpoint;
    this.reconnect = reconnect;
    this.reconnectInterval = reconnectInterval;
  }


  public void startup() {
    stopThread = false;
    thread = new Thread(this);
    thread.start();
  }

  public void shutdown() throws Exception {
    // bring down thread
    stopThread = true;
    synchronized (close) {
      stopThread = true;
      close.notify();
      thread.interrupt();
    }
    thread.join();
    thread = null;
  }

  public void connect() throws Exception {
    if (isConnected()) {
      return;
    }
    // try to connect to server
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    container.connectToServer(this, endpoint);
  }

  public void disconnect() throws Exception {
    if (isConnected()) {
      session.close();
    }
  }

  public void run() {
    while (!stopThread) {
      // try to connect
      try {
        synchronized(close) {
          connect();
          try {
            // successful connection
            close.wait();
          } catch (InterruptedException ie) {
            // closing
            throw ie;
          }
        }
      } catch (InterruptedException ie) {
        // do nothing; closing
      } catch (Exception e1) { //exception on connect
        // let listener handle if they want
        try {
          listener.onConnectException(e1);
        } catch (Exception e2) {
          // kill thread on listener exception
          break;
        }

        // reconnect for unhandled connect exception
        if (!stopThread && reconnect) {
          try {
            Thread.sleep(reconnectInterval);
          } catch (InterruptedException ie) {
            // do nothing; closing
          }
        }
      }
    }
    try {
      disconnect(); //ensure disconnected
    } catch (Exception e) {
      // ignore; just close
    }
  }

  @OnOpen
  public void onOpen(Session session) {
    this.session = session;
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    synchronized(close) {
      this.session = null;
      close.notify();
    }
    listener.onClose();
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
