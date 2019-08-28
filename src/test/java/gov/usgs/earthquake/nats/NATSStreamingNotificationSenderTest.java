package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.URLNotification;
import gov.usgs.earthquake.distribution.URLNotificationJSONConverter;
import gov.usgs.earthquake.distribution.URLProductStorage;
import gov.usgs.earthquake.product.ProductId;
import io.nats.streaming.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;

@Ignore ("Requires running NATS Streaming server")
public class NATSStreamingNotificationSenderTest {

  private static final Logger LOGGER = Logger
          .getLogger(NATSStreamingNotificationSenderTest.class.getName());

  private LinkedList<URLNotification> outNotifications = new LinkedList<>();

  @Test
  public void sendNotificationTest() throws Exception {
    // NotificationSender setup
    NATSStreamingNotificationSender notificationSender = new NATSStreamingNotificationSender();
    notificationSender.setServerHost("localhost/");
    notificationSender.setServerPort("4222");
    notificationSender.setClusterId("test-cluster");
    notificationSender.setSubject("test-subject");
    notificationSender.setProductStorage(new URLProductStorage());

    // start
    notificationSender.startup();

    // generate notification
    ProductId id = new ProductId("test-source","test-type","test-code");
    URLNotification notification = new URLNotification(
      id,
      new Date(),
      new URL("http://localhost/tracker"),
      new URL("http://localhost/product"));
    LOGGER.info("Generated notification: " + URLNotificationJSONConverter.toJSON(notification));

    // preemptively subscribe
    StreamingConnectionFactory factory = new StreamingConnectionFactory("test-cluster", "test-client2");
    factory.setNatsUrl("nats://localhost:4222");
    StreamingConnection conn = factory.createConnection();

    Subscription sub = conn.subscribe("test-subject", new MessageHandler() {
      @Override
      public void onMessage(Message msg) {
        try {
          // use a list of URLNotifications in case other processes also send
          outNotifications.add(URLNotificationJSONConverter.parseJSON(new ByteArrayInputStream(msg.getData())));
        } catch (Exception e) {
          LOGGER.info("Failed to parse notification");
          Assert.fail();
        }
        LOGGER.info("Received message: " + new String(msg.getData()));
      }
    }, new SubscriptionOptions.Builder().deliverAllAvailable().build());

    // send
    notificationSender.sendNotification(notification);

    // sleep, because it's the only way to wait for all the messages
    Thread.sleep(1500);

    // iterate over all received notifications looking for the one we setn
    boolean foundSent = false;
    for (URLNotification received : outNotifications) {
      if (notification.equals(received)) {
        foundSent = true;
        break;
      }
    }
    Assert.assertTrue(foundSent);

    sub.unsubscribe();
    conn.close();
  }
}
