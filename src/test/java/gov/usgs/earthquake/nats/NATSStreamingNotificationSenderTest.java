package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.URLNotification;
import gov.usgs.earthquake.distribution.URLNotificationJSONConverter;
import gov.usgs.earthquake.distribution.URLProductStorage;
import gov.usgs.earthquake.product.ProductId;
import io.nats.streaming.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NATSStreamingNotificationSenderTest {

  private static final Logger LOGGER = Logger
          .getLogger(NATSStreamingNotificationSenderTest.class.getName());

  private URLNotification outNotification;

  @Test
  public void sendNotificationTest() throws Exception {
    // NotificationSender setup
    NATSStreamingNotificationSender notificationSender = new NATSStreamingNotificationSender();
    notificationSender.setServerHost("localhost/");
    notificationSender.setServerPort("4222");
    notificationSender.setClusterId("test-cluster");
    notificationSender.setClientId("test-client");
    notificationSender.setSubject("test-subject");
    notificationSender.setProductStorage(new URLProductStorage());

    // start
    notificationSender.startup();

    ProductId id = new ProductId("test-source","test-type","test-code");
    URLNotification notification = new URLNotification(
      id,
      new Date(),
      new URL("http://localhost/tracker"),
      new URL("http://localhost/product"));

    // send
    notificationSender.sendNotification(notification);

    // verify
    final CountDownLatch doneSignal = new CountDownLatch(1);
    StreamingConnectionFactory factory = new StreamingConnectionFactory("test-cluster","test-client2");
    factory.setNatsUrl("nats://localhost:4222");
    StreamingConnection conn = factory.createConnection();

    Subscription sub = conn.subscribe("test-subject", new MessageHandler() {
      @Override
      public void onMessage(Message msg) {
        try {
          outNotification = URLNotificationJSONConverter.parseJSON(new ByteArrayInputStream(msg.getData()));
        } catch (Exception e) {
          LOGGER.info("Failed to parse notification");
          Assert.fail();
        }
        LOGGER.info("Received message:\n" + new String(msg.getData()));
        doneSignal.countDown();
      }
    }, new SubscriptionOptions.Builder().deliverAllAvailable().build());

    doneSignal.await();
    Assert.assertTrue(notification.equals(outNotification));

    sub.unsubscribe();
    conn.close();

  }
}
