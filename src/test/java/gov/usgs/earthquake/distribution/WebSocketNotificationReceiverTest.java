package gov.usgs.earthquake.distribution;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.earthquake.util.JSONTrackingFile;

import javax.json.Json;
import javax.json.JsonObject;

public class WebSocketNotificationReceiverTest {

  private WebSocketNotificationReceiver receiver;
  private URLNotification receivedNotification;

  private String testServerHost = "www.test.com";
  private String testServerPath = "/ws/sequence/";
  private String testServerPort = "8080";
  private String testSequence = "99";
  private String testTrackingFileName = "data/WebSocketNotificationReceiverTest.json";

  @Before
  public void setup() {
    //create instance of WebSocketNotificationReceiver
    receiver = new TestWebSocketNotificationReceiver();
    receiver.setServerHost(testServerHost);
    receiver.setServerPath(testServerPath);
    receiver.setServerPort(testServerPort);
    receiver.setSequence(testSequence);
    receiver.setTrackingFile(new JSONTrackingFile(testTrackingFileName));
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
                            .add("updatetime","2019-01-27")
                    )
                    .add("trackerURL","http://www.testTrackerUrl.com")
                    .add("expires", "2019-01-27")
                    .add("url","http://www.testUrl.com")
            ).build();
    String message = json.toString();

    receiver.onMessage(message);

    //ensure received notification is correct (overridden receiveNotification)
    try {
      URLNotification sentNotification = URLNotificationJSONConverter.parseJSON(json.getJsonObject(WebSocketNotificationReceiver.ATTRIBUTE_DATA));
      Assert.assertTrue(sentNotification.equals(receivedNotification));
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }

    //ensure tracking file is written (& correctly)
  }

  private class TestWebSocketNotificationReceiver extends WebSocketNotificationReceiver{
    @Override
    public void receiveNotification(Notification notification) {
      receivedNotification = (URLNotification) notification;
    }
  }
}
