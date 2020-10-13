package gov.usgs.earthquake.distribution;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.Session;

/**
 * Allows overridden onMessage for different behavior of WebSocketClient onMessage
 */
public interface WebSocketListener {
  public void onOpen(Session session) throws IOException;
  public void onMessage(String message) throws IOException;
  public void onClose(Session session, CloseReason closeReason) throws IOException;
  public void onConnectFail() throws IOException;
  public void onReconnectFail() throws IOException;
}
