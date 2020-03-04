package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.*;
import gov.usgs.util.Config;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends notifications directly to NATS streaming server using a NATS client
 */
public class NATSStreamingNotificationSender extends DefaultNotificationSender {

  private static final Logger LOGGER = Logger
          .getLogger(DefaultNotificationSender.class.getName());

  private NATSClient client = new NATSClient();
  private String subject;

  @Override
  public void configure(Config config) throws Exception{
    super.configure(config);
    client.configure(config);
    subject = config.getProperty(NATSClient.SUBJECT_PROPERTY, NATSClient.DEFAULT_SUBJECT);
  }

  /**
   * Publishes notification to NATS streaming server
   *
   * @param notification
   *            The notification to send
   * @throws Exception if something goes wrong with publish
   */
  @Override
  public void sendNotification(final Notification notification) throws Exception {
    String message = URLNotificationJSONConverter.toJSON((URLNotification) notification);
    try {
      client.getConnection().publish(subject, message.getBytes());
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] exception publishing NATSStreaming notification:");
      throw e;
    }
  }

  /**
   * Starts NATSStreaming connection and superclasses
   *
   * @throws Exception if there's an issue with superclasses, generating a client ID, or connecting to server
   */
  @Override
  public void startup() throws Exception {
    super.startup();
    client.startup();
  }

  /**
   * Safely closes the NATSStreaming connection and superclasses
   *
   * @throws Exception if superclasses throw exceptions
   */
  @Override
  public void shutdown() throws Exception {
    client.shutdown();
    super.shutdown();
  }

  public NATSClient getClient() {
    return client;
  }

  public void setClient(NATSClient client) {
    this.client = client;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }
}