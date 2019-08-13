package gov.usgs.earthquake.nats;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NATSStreamingNotificationReceiverTest {

  private NATSStreamingNotificationReceiver notificationReceiver;

  @Before
  public void setup() throws Exception{
    notificationReceiver = new NATSStreamingNotificationReceiver();
    notificationReceiver.setServerHost("localhost");
    notificationReceiver.setServerPort(4222);
    notificationReceiver.setClusterId("test-cluster");
    notificationReceiver.setClientId("test-client");
    notificationReceiver.setSubject("test-subject");
    notificationReceiver.setTrackingFileName("nats_data.json");

    notificationReceiver.startup();
  }

  @After
  public void shutdown() throws Exception{
    notificationReceiver.shutdown();
  }

  @Test
  public void trackingFileWriteReadTest() {

  }

  @Test
  public void onMessageTest() {

  }
}
