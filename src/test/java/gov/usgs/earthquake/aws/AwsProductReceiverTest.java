package gov.usgs.earthquake.aws;

import java.time.Instant;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import gov.usgs.earthquake.distribution.Notification;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.JsonProduct;

public class AwsProductReceiverTest {

  TestAwsProductReceiver receiver;

  @BeforeEach
  public void before() throws Exception {
    receiver = new TestAwsProductReceiver();
    receiver.startCatchUpThread();
  }

  @AfterEach
  public void after() throws Exception {
    receiver.stopCatchUpThread();
    receiver = null;
  }

  @Test
  public void testSwitchToBroadcast() throws Exception {
    TestSession testSession = new TestSession();
    // connect
    receiver.onOpen(testSession);

    // receive broadcast
    Instant created = Instant.now();
    receiver.onMessage(getNotification("broadcast", 10, created).toString());
    Assert.assertFalse("not in broadcast mode yet", receiver.isProcessBroadcast());
    Assert.assertNull("didn't process broadcast", receiver.lastJsonNotification);
    Assert.assertEquals("saved broadcast id",
        Long.valueOf(10L), receiver.getLastBroadcastId());

    // receive response to products_created_after
    receiver.onMessage(getNotification("product", 10, created).toString());
    Assert.assertNotNull(
        "processed product during catch up",
        receiver.lastJsonNotification);

    // receive end of products_created_after response
    receiver.onMessage(getProductsCreatedAfter(created, 1).toString());
    Assert.assertTrue("switched to broadcast mode", receiver.isProcessBroadcast());
  }


  @Test
  public void testBroadcastOutOfOrder() throws Exception {
    TestSession testSession = new TestSession();
    // connect
    receiver.onOpen(testSession);
    // complete catch up process (switch to broadcast)
    receiver.onMessage(getProductsCreatedAfter(Instant.now(), 0).toString());

    // receive broadcast in order
    receiver.setLastBroadcastId(10L);
    receiver.onMessage(getNotification("broadcast", 11, Instant.now()).toString());
    Assert.assertTrue("still in broadcast mode", receiver.isProcessBroadcast());
    Assert.assertNotNull("processed broadcast", receiver.lastJsonNotification);
    Assert.assertEquals("saved broadcast id",
        Long.valueOf(11L), receiver.getLastBroadcastId());

    // clear any previous products created after message
    testSession.testBasicRemote.lastSendText = null;
    receiver.lastJsonNotification = null;

    // receive broadcast out of order
    receiver.onMessage(getNotification("broadcast", 13, Instant.now()).toString());
    Assert.assertFalse("no longer in broadcast mode", receiver.isProcessBroadcast());
    Assert.assertNull("did not process broadcast", receiver.lastJsonNotification);
    Assert.assertEquals("still saved broadcast id",
        Long.valueOf(13L), receiver.getLastBroadcastId());
    String sent = testSession.waitForBasicSendText(100L);
    Assert.assertTrue(
        "sent products_created_after",
        sent.contains("\"action\":\"products_created_after\""));
  }

  @Test
  public void testStartCatchUpWhenConnected() throws Exception {
    // set flags being tested to opposite values
    receiver.setProcessBroadcast(true);
    TestSession testSession = new TestSession();

    // call onOpen to simulate connection
    receiver.onOpen(testSession);

    // processBroadcast disabled and created after request sent
    Assert.assertFalse("not in process broadcast mode", receiver.isProcessBroadcast());

    String sent = testSession.waitForBasicSendText(1000L);
    Assert.assertTrue(
        "sent products_created_after",
        sent.contains("\"action\":\"products_created_after\""));
  }

  @Test
  public void testThrottleQueue() throws Exception {
    TestSession testSession = new TestSession();
    TestListenerNotifier testNotifier = new TestListenerNotifier(receiver);
    receiver.setNotifier(testNotifier);
    // simulate queue that needs to be throttled
    testNotifier.queueSize = 5001;
    testNotifier.setThrottleStartThreshold(5000);
    testNotifier.setThrottleStopThreshold(2500);
    testNotifier.setThrottleWaitInterval(100L);

    // call onOpen to simulate connection
    receiver.onOpen(testSession);
    Assert.assertNull(
        "throttling should prevent message",
        testSession.waitForBasicSendText(100L));

    // now simulate queue that is done throttling
    testNotifier.queueSize = 2499;
    String sent = testSession.waitForBasicSendText(500L);
    Assert.assertTrue(
        "sent products_created_after",
        sent.contains("\"action\":\"products_created_after\""));
  }

  static JsonObject getNotification(final String action, final long id, final Instant created) throws Exception {
    Product product = new Product(new ProductId("source", "type", "code"));
    return Json.createObjectBuilder()
        .add("action", action)
        .add("notification",
            Json.createObjectBuilder()
                .add("id", id)
                .add("created", created.toString())
                .add("product", new JsonProduct().getJsonObject(product)))
        .build();
  }

  static JsonObject getProductsCreatedAfter(final Instant createdAfter, final int count) {
    return Json.createObjectBuilder()
        .add("action", "products_created_after")
        .add("created_after", createdAfter.toString())
        .add("count", count)
        .build();
  }

  /**
   * Stub socket connections to test message handling behavior.
   */
  static class TestAwsProductReceiver extends AwsProductReceiver {
    public JsonNotification lastJsonNotification;
    public boolean onJsonNotificationCalled = false;

    @Override
    protected void onJsonNotification(JsonNotification notification) throws Exception {
      onJsonNotificationCalled = true;
      lastJsonNotification = notification;
      super.onJsonNotification(notification);
    }

    @Override
    public void receiveNotification(Notification notification) throws Exception {
      // skip actual processing
    }

    @Override
    public void writeTrackingData() {
      // skip tracking
    }

    // getter/setter to control state for testing

    public Instant getCreatedAfter() { return this.createdAfter; }
    public void setCreatedAfter(final Instant c) { this.createdAfter = c; }

    public JsonNotification getLastBroadcast() { return this.lastBroadcast; }
    public void setLastBroadcast(final JsonNotification j) { this.lastBroadcast = j; }

    public Long getLastBroadcastId() { return this.lastBroadcastId; }
    public void setLastBroadcastId(final Long l) { this.lastBroadcastId = l; }

    public boolean isProcessBroadcast() { return this.processBroadcast; }
    public void setProcessBroadcast(final boolean b) { this.processBroadcast = b; }
  }

}
