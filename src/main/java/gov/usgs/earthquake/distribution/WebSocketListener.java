package gov.usgs.earthquake.distribution;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.Session;

/**
 * Allows overridden onMessage for different behavior of WebSocketClient onMessage
 */
public interface WebSocketListener {
  /** Interface method to be overriden by WebSocket files and AwsProductReceiver
   * @param session Session to open
   * @throws IOException IOException
   */
  public void onOpen(Session session) throws IOException;

  /** Interface method to be overriden by WebSocket files and AwsProductReceiver
   * @param message String message
   * @throws IOException IOException
   */
  public void onMessage(String message) throws IOException;

  /** Interface method to be overriden by WebSocket files and AwsProductReceiver
   * @param session Session to close
   * @param closeReason Reason for closing session
   * @throws IOException IOException
   */
  public void onClose(Session session, CloseReason closeReason) throws IOException;

  /** Interface method to be overriden by WebSocket files and AwsProductReceiver
   * @throws IOException IOException
   */
  public void onConnectFail() throws IOException;

  /** Interface method to be overriden by WebSocket files and AwsProductReceiver
   * @throws IOException IOException
   */
  public void onReconnectFail() throws IOException;
}
