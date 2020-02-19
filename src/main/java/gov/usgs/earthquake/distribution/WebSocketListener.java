package gov.usgs.earthquake.distribution;

import javax.websocket.CloseReason;
import javax.websocket.Session;

/**
 * Allows overridden onMessage for different behavior of WebSocketClient onMessage
 */
public interface WebSocketListener {
  public void onOpen(Session session);
  public void onMessage(String message);
  public void onClose(Session session, CloseReason closeReason);
  public void onConnectFail();
  public void onReconnectFail();
}
