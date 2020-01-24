package gov.usgs.earthquake.distribution;

import gov.usgs.util.Config;

/**
 * Receives notifications from an arbitrary web socket.
 */
public class WebSocketNotificationReceiver extends DefaultNotificationReceiver {

  public static final String SERVER_HOST_PROPERTY = "serverHost";
  public static final String SERVER_PORT_PROPERTY = "serverPort";

  public static final String DEFAULT_SERVER_HOST = "http://www.google.com";
  public static final String DEFAULT_SERVER_PORT = "4222";

  private String serverHost;
  private String serverPort;

  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    String serverHost = config.getProperty(SERVER_HOST_PROPERTY, DEFAULT_SERVER_HOST);
    String serverPort = config.getProperty(SERVER_PORT_PROPERTY, DEFAULT_SERVER_PORT);
  }

  @Override
  public void startup() {
    //open websocket
    //  - need to write a @ClientEndpoint websocketclient that does message handling
    //    - default messaging, open, close behavior
    //    - how pings work

    //add DefaultNotificationReceiver's receiveNotification (maybe wrapped) as a listener

    //done (I think)
  }

  @Override
  public void shutdown() {
    //close socket
  }
}
