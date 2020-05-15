package gov.usgs.earthquake.distribution;

import javax.websocket.CloseReason;
import javax.websocket.Session;

/**
 * Allows overridden onMessage for different behavior of WebSocketClient onMessage
 */
public interface WebSocketListener {
  public void onMessage(String message);
  public void onConnectException(Exception e) throws Exception;
  public void onOpen();
  public void onClose();
}
