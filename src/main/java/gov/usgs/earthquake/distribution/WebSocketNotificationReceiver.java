package gov.usgs.earthquake.distribution;

import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;

import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receives notifications from an arbitrary web socket.
 */
public class WebSocketNotificationReceiver extends DefaultNotificationReceiver implements WebSocketListener {

  public static final Logger LOGGER = Logger
          .getLogger(WebSocketNotificationReceiver.class.getName());

  public static final String SERVER_HOST_PROPERTY = "serverHost";
  public static final String SERVER_PORT_PROPERTY = "serverPort";

  public static final String DEFAULT_SERVER_HOST = "http://www.google.com";
  public static final String DEFAULT_SERVER_PORT = "4222";

  private String serverHost;
  private String serverPort;
  private WebSocketClient client;

  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    String serverHost = config.getProperty(SERVER_HOST_PROPERTY, DEFAULT_SERVER_HOST);
    String serverPort = config.getProperty(SERVER_PORT_PROPERTY, DEFAULT_SERVER_PORT);
  }

  @Override
  public void startup() throws Exception{
    super.startup();

    //open websocket
    client = new WebSocketClient(new URI(serverHost + serverPort), this);
  }

  @Override
  public void shutdown() throws Exception{
    //close socket
    client.shutdown();
    super.shutdown();
  }

  @Override
  //TODO: message will be in different format than URLNotificationJSONConverter is built for; trim please
  public void onMessage(String message) {
    InputStream in = null;
    try {
      in = StreamUtils.getInputStream(message);
      URLNotification notification = URLNotificationJSONConverter.parseJSON(in);
      receiveNotification(notification);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] exception while processing URLNotification ", e);
    } finally {
      StreamUtils.closeStream(in);
    }

  }
}
