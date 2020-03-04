package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.*;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.util.JSONTrackingFile;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import org.junit.*;

import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;

@Ignore ("Requires running NATS Streaming server")
public class NATSStreamingNotificationReceiverTest {

  private static final Logger LOGGER = Logger
          .getLogger(NATSStreamingNotificationReceiverTest.class.getName());

  private NATSStreamingNotificationReceiver notificationReceiver;
  private LinkedList<URLNotification> receivedNotifications = new LinkedList<>();
  private Object publishedLock = new Object();

  @Before
  public void setup() throws Exception{
    notificationReceiver = new TestNATSStreamingNotificationReceiver();
    notificationReceiver.getClient().setServerHost("localhost");
    notificationReceiver.getClient().setServerPort("4222");
    notificationReceiver.getClient().setClusterId("test-cluster");
    notificationReceiver.setSubject("test-subject");
    notificationReceiver.getClient().setClientId("test-id");
    notificationReceiver.setTrackingFile(new JSONTrackingFile(NATSStreamingNotificationReceiver.DEFAULT_TRACKING_FILE_NAME_PROPERTY + ".tmp"));
    notificationReceiver.setProductStorage(new URLProductStorage());
    notificationReceiver.setNotificationIndex(new JDBCNotificationIndex());

    notificationReceiver.startup();
  }

  @After
  public void shutdown() throws Exception{
    notificationReceiver.shutdown();
  }

  @Test
  public void onMessageTest() throws Exception {
    // stan connection
    StreamingConnectionFactory factory = new StreamingConnectionFactory("test-cluster","test-client");
    factory.setNatsUrl("nats://localhost:4222");
    StreamingConnection conn = factory.createConnection();

    // generate notification
    ProductId id = new ProductId("test-source","test-type","test-code");
    URLNotification notification = new URLNotification(
      id,
      new Date(),
      new URL("http://localhost/tracker"),
      new URL("http://localhost/product"));
    String message = URLNotificationJSONConverter.toJSON(notification);

    LOGGER.info("Publishing message: " + message);

    // publish, wait
    synchronized(publishedLock) {
      conn.publish("test-subject", message.getBytes());
      publishedLock.wait();
    }

    // iterate over received notifications searching for ours
    boolean foundSent = false;
    for (URLNotification received : receivedNotifications) {
      if (notification.equals(received)) {
        foundSent = true;
        break;
      }
    }

    Assert.assertTrue(foundSent);
  }

  private class TestNATSStreamingNotificationReceiver extends NATSStreamingNotificationReceiver {
    @Override
    public void receiveNotification(Notification notification) {
      synchronized (publishedLock) {
        receivedNotifications.add((URLNotification) notification);
        LOGGER.info("Received message: " + URLNotificationJSONConverter.toJSON((URLNotification) notification));
        publishedLock.notify();
      }
    }
  }
}
