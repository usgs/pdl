package gov.usgs.earthquake.distribution;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;

public class WebSocketNotificationReceiverTest {

  private WebSocketNotificationReceiver receiver;
  private Notification receivedNotification;

  @Before
  public void setup() {
    //create instance of WebSocketNotificationReceiver
    receiver = new TestWebSocketNotificationReceiver();
  }

  @Test
  public void writeTrackingFileWriteReadTest() {
    //write & read tracking file for WebSocketNotificationReceiver
    //Confirm contents are correct
  }

  @Test
  public void onMessageTest() {
    //generate fake (well-formatted) notification
    JsonObject json = Json.createObjectBuilder()
            .add(WebSocketNotificationReceiver.SEQUENCE_PROPERTY,"testSequence")
            .add(WebSocketNotificationReceiver.TIMESTAMP_PROPERTY, "testTime")
            .add(WebSocketNotificationReceiver.ATTRIBUTE_DATA,Json.createObjectBuilder()
                    .add("id",Json.createObjectBuilder()
                            .add("source","testSource")
                            .add("type","testType")
                            .add("code","testCode")
                            .add("updateTime","testUpdateTime")
                    )
                    .add("trackerUrl","testTrackerUrl")
                    .add("expires","testExpires")
                    .add("url","testUrl")
            ).build();
    String message = json.toString();

    receiver.onMessage(message);

    //ensure received notification is correct (overridden receiveNotification)
    try {
      Notification sentNotification = URLNotificationJSONConverter.parseJSON(json.getJsonObject(WebSocketNotificationReceiver.ATTRIBUTE_DATA));
      Assert.assertTrue(sentNotification.equals(receivedNotification));
    }
    catch (Exception e) {
      Assert.fail();
    }
    
    //ensure tracking file is written (& correctly)
  }

  private class TestWebSocketNotificationReceiver extends WebSocketNotificationReceiver{
    @Override
    public void receiveNotification(Notification notification) {
      receivedNotification = notification;
    }
  }
}
