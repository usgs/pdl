package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.StreamUtils;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Date;

public class URLNotificationJSONConverterTest {

  @Test
  public void testJSONFormat() throws Exception{
    URLNotification testNotification = new URLNotification(new ProductId("testSource","testType","testCode"), new Date(),
            new URL("http://localhost/tracker/"), new URL("http://localhost/product/"));

    String JSON = URLNotificationJSONConverter.toJSON(testNotification);
    URLNotification outNotification = URLNotificationJSONConverter.parseJSON(StreamUtils.getInputStream(JSON));

    Assert.assertTrue("Notification equal after JSON roundtrip",testNotification.equals(outNotification));
  }
}
