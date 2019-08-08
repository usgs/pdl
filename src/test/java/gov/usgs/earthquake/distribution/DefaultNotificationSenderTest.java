package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Date;

public class DefaultNotificationSenderTest {

  private String sentMessage;
  URLProductStorage storage;
  DefaultNotificationSender notificationSender;
  ProductId testId;
  Product testProduct;

  @Before
  public void setup() throws Exception{
    storage = new URLProductStorage(new File("storage"),new URL("http://localhost/"));

    notificationSender = new TestDefaultNotificationSender();
    notificationSender.setProductStorage(storage);
    notificationSender.setProductStorageMaxAge(60000); // one minute
    notificationSender.setNotificationIndex(new JDBCNotificationIndex());

    testId = new ProductId("testSource","testType","testCode");
    testProduct = new Product(testId);
    testProduct.setTrackerURL(new URL("http://localhost/"));
  }

  @Test
  public void onExpiredNotificationTest() throws Exception {
    storage.storeProduct(testProduct);
    URLNotification testNotification = new URLNotification(
            testId,
            new Date(new Date().getTime() + 60000),
            testProduct.getTrackerURL(),
            storage.getProductURL(testId));
    notificationSender.onExpiredNotification(testNotification);

    Assert.assertNull(storage.getProduct(testId));
  }

  @Test
  public void onProductTest() throws Exception {
    notificationSender.onProduct(testProduct);

    Assert.assertEquals(testId.toString(),sentMessage);

    // clean up
    storage.removeProduct(testId);
  }

  private class TestDefaultNotificationSender extends DefaultNotificationSender {

    @Override
    public String notificationToString(final Notification notification) throws Exception {
      return notification.getProductId().toString();
    }

    @Override
    public void sendMessage (String message) throws Exception {
      sentMessage = message;
    }
  }

}
