package gov.usgs.earthquake.natsws;

import gov.usgs.earthquake.distribution.*;
import gov.usgs.earthquake.nats.NATSStreamingNotificationSender;
import gov.usgs.earthquake.product.ProductId;

import java.net.URL;
import java.util.Date;

public class WebSocketNotificationReceivedTest {

  private static URLNotification receivedNotification = null;

  public static void main(String[] args) throws Exception {
    // sender setup
    NATSStreamingNotificationSender sender = new NATSStreamingNotificationSender();
    sender.setServerHost("localhost");
    sender.setServerPort("4222");
    sender.getClient().setClusterId("usgs");
    sender.getClient().setClientId("socket-tester");
    sender.setSubject("anss.pdl.realtime");
    sender.setProductStorage(new URLProductStorage());
    sender.setName("sender");
    try {
      sender.startup();
    } catch (Exception e) {
      System.out.println("Exception starting notification sender. Is a NATS server running?");
      throw e;
    }

    // receiver setup
    WebSocketNotificationReceiver receiver = new WebSocketNotificationReceiver() {
      @Override
      public void receiveNotification(Notification notification) {
        receivedNotification = (URLNotification) notification;
      }
    };
    receiver.setServerHost("ws://localhost");
    receiver.setServerPath("/subscribe/");
    receiver.setServerPort("8080");
    receiver.setProductStorage(new URLProductStorage());
    receiver.setNotificationIndex(new JDBCNotificationIndex());
    receiver.setTrackingFileName(WebSocketNotificationReceiver.DEFAULT_TRACKING_FILE_NAME + "_test");
    receiver.setAttempts(3);
    receiver.setTimeout(300);
    receiver.setName("receiver");
    try {
      System.out.println("Connecting to webservice: " + receiver.getServerHost() + ":" + receiver.getServerPort() + receiver.getServerPath() + receiver.getSequence());
      receiver.startup();
    } catch (Exception e) {
      System.out.println("Exception starting notification receiver. Is the micronaut ws running?");
      throw e;
    }

    // generate notification
    ProductId id = new ProductId("test-source", "test-type", "test-code");
    URLNotification notification = new URLNotification(
            id,
            new Date(),
            new URL("http://localhost/tracker"),
            new URL("http://localhost/product"));

    System.out.println("Sending notification...");

    // send notification
    try {
      sender.sendNotification(notification);
    } catch (Exception e) {
      System.out.println("Failed on notification send. Is a NATS server running?");
      throw e;
    }

    System.out.println("Notification sent! Waiting for receipt...");

    // wait 5 seconds to receive notification; check every second to see if notification received
    for (int i = 1; i <= 5; i++) {
      Thread.sleep(1000);
      System.out.println(i + "...");
      if (receivedNotification != null) {
        System.out.println("Notification received.");
        if (notification.equals(receivedNotification)) {
          System.out.println("Notification correct!");
        } else {
          System.out.println("Error in notification.");
        }
        break;
      }
    }

    if (receivedNotification == null) {
      System.out.println("Notification never received.");
    }

    sender.shutdown();
    receiver.shutdown();

    System.out.println("Done.");
    System.exit(0);
  }
}
