package gov.usgs.earthquake.distribution;

/**
 * Allows overridden onMessage for different behavior of WebSocketClient onMessage
 */
public interface WebSocketListener {
  public void onMessage(String message);
}
