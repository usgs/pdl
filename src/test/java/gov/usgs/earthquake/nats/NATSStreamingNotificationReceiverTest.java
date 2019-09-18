package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.*;
import gov.usgs.earthquake.product.ProductId;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import org.junit.*;

import javax.json.JsonObject;
import java.io.File;
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
    notificationReceiver.getClient().setSubject("test-subject");
    notificationReceiver.setTrackingFileName(NATSStreamingNotificationReceiver.DEFAULT_TRACKING_FILE_NAME_PROPERTY + ".tmp");
    notificationReceiver.setProductStorage(new URLProductStorage());
    notificationReceiver.setNotificationIndex(new JDBCNotificationIndex());

    notificationReceiver.startup();
  }

  @After
  public void shutdown() throws Exception{
    notificationReceiver.shutdown();
  }

  @Test
  public void trackingFileWriteReadTest() throws Exception {
    notificationReceiver.writeTrackingFile();
    JsonObject json = notificationReceiver.readTrackingFile();

    // assert properties are as configured
    Assert.assertEquals(notificationReceiver.getClient().getServerHost(), json.getString(NATSClient.SERVER_HOST_PROPERTY));
    Assert.assertEquals(notificationReceiver.getClient().getServerPort(), json.getInt(NATSClient.SERVER_PORT_PROPERTY));
    Assert.assertEquals(notificationReceiver.getClient().getClusterId(), json.getString(NATSClient.CLUSTER_ID_PROPERTY));
    Assert.assertEquals(notificationReceiver.getClient().getClientId(), json.getString(NATSClient.CLIENT_ID_PROPERTY));
    Assert.assertEquals(notificationReceiver.getClient().getSubject(), json.getString(NATSClient.SUBJECT_PROPERTY));

    // clean up
    //TODO: Figure out why tracking file isn't being deleted
    File trackingFile = new File(notificationReceiver.getTrackingFileName());
    System.out.println(trackingFile.getAbsolutePath());
    if (trackingFile.delete()) {
      System.out.println("Deleted tracking file");
    } else {
      System.out.println("Failed to delete tracking file");
    }
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
