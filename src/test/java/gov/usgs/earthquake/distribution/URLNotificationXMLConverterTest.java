package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.StreamUtils;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Date;

public class URLNotificationXMLConverterTest {

  @Test
  public void testXMLFormat() throws Exception {
    // create notification
    ProductId id = new ProductId("testsource", "testtype", "testcode");
    // one week in the future
    Date expirationDate = new Date(new Date().getTime() + 604800);
    URL trackerURL = new URL("http://earthquake.usgs.gov/tracker");
    URL productURL = new URL("http://earthquake.usgs.gov/someProduct.xml");
    URLNotification notification = new URLNotification(id, expirationDate,
            trackerURL, productURL);

    // conver notification to xml
    String notificationXML = URLNotificationXMLConverter.toXML(notification);

    System.err.println(notificationXML);

    // read notification back in from xml
    URLNotification notification2 = URLNotificationXMLConverter.parseXML(StreamUtils
            .getInputStream(notificationXML));

    Assert.assertTrue("Notification equal after xml roundtrip",
            notification.equals(notification2));
  }
}
