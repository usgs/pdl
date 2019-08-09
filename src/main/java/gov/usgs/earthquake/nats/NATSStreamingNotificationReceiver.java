package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.distribution.DefaultNotificationReceiver;
import gov.usgs.earthquake.distribution.URLNotification;
import gov.usgs.earthquake.distribution.URLNotificationJSONConverter;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;
import io.nats.streaming.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NATSStreamingNotificationReceiver extends DefaultNotificationReceiver {

  private static final Logger LOGGER = Logger
          .getLogger(DefaultNotificationReceiver.class.getName());

  public static String CLUSTER_ID_PROPERTY = "clusterid";
  public static String CLIENT_ID_PROPERTY = "clientid";
  public static String SUBJECT_PROPERTY = "subject";

  private StreamingConnection connection;
  private Subscription subscription;
  private String clusterId;
  private String clientId;
  private String subject;

  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    clusterId = config.getProperty(CLUSTER_ID_PROPERTY);
    if (clusterId == null) {
      throw new ConfigurationException("[" + getName() + "] " + CLUSTER_ID_PROPERTY + " must be defined");
    }
    clientId = config.getProperty(CLIENT_ID_PROPERTY);
    if (clientId == null) {
      throw new ConfigurationException("[" + getName() + "] " + CLIENT_ID_PROPERTY + " must be defined");
    }
    subject = config.getProperty(SUBJECT_PROPERTY);
    if (subject == null) {
      throw new ConfigurationException("[" + getName() + "] " + SUBJECT_PROPERTY + " must be defined");
    }
  }

  /**
   *
   * @throws InterruptedException
   * @throws IOException
   */
  @Override
  public void startup() throws Exception {
    super.startup();
    connection = new StreamingConnectionFactory(clusterId,clientId).createConnection();

    //TODO: Figure out message sequence position storage
    //  - If no storage exists:
    //    - Create storage according to configuration
    //    - Deliver all available messages
    //  - If storage exists:
    //    - Cross-check with configuration; always prioritize config
    //    - Start subscription at last position
    subscription = connection.subscribe(
      subject,
      new STANMessageHandler(),
      new SubscriptionOptions.Builder().deliverAllAvailable().build());
    // currently delivering all; reference above for future behavior
  }

  /**
   *
   * @throws IOException
   * @throws InterruptedException
   * @throws TimeoutException
   */
  @Override
  public void shutdown() throws Exception {
    //TODO: Store position/other info in external storage

    subscription.unsubscribe();
    connection.close();
    super.shutdown();
  }

  /**
   * Member class handling NATS Streaming messages.
   * Can be defined/instantiated inline, but done here for clarity
   */
  private class STANMessageHandler implements MessageHandler{

    //TODO: Determine if we need more robust exception handling with message
    /**
     * Defines behavior for message receipt
     *
     * @param message
     *            The message received from the STAN server
     */
    public void onMessage(Message message) {
      // parse message
      String messageString = message.getData().toString();
      try {
        URLNotification notification = URLNotificationJSONConverter.parseJSON(StreamUtils.getInputStream(messageString));
        // send to listeners
        receiveNotification(notification);
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "[" + getName() + "]" + " exception converting message to JSON");
      }
    }

  }
}
