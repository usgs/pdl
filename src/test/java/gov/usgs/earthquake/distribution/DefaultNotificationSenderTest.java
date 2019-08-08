package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

public class DefaultNotificationSenderTest {

  private String sentMessage;
  private Object syncObject = new Object();

  @Test
  public void onProductTest() throws Exception {
    URLProductStorage storage = new URLProductStorage(new File("storage"),new URL("http://localhost/"));

    DefaultNotificationSender notificationSender = new TestDefaultNotificationSender();
    notificationSender.setProductStorage(storage);
    notificationSender.setProductStorageMaxAge(60000); // one minute
    notificationSender.setNotificationIndex(new JDBCNotificationIndex());

    ProductId id = new ProductId("testSource","testType","testCode");
    Product product = new Product(id);
    product.setTrackerURL(new URL("http://localhost/"));

    notificationSender.onProduct(product);

    Assert.assertEquals(id.toString(),sentMessage);

    // clean up
    storage.removeProduct(id);
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
