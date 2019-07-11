/*
 * Extent Summary Test
 */

package gov.usgs.earthquake.indexer;

import junit.framework.Assert;
import org.junit.Test;

import java.util.GregorianCalendar;
import java.util.HashMap;

public class ExtentSummaryTest {

  @Test
  public void constructTest() {
    HashMap<String,String> properties = new HashMap<>();
    properties.put(ExtentSummary.EXTENT_START_TIME_PROPERTY,"2019-07-01");
    properties.put(ExtentSummary.EXTENT_END_TIME_PROPERTY,"2019-07-04");
    properties.put(ExtentSummary.EXTENT_MIN_LAT_PROPERTY,"0");
    properties.put(ExtentSummary.EXTENT_MAX_LAT_PROPERTY,"45");
    properties.put(ExtentSummary.EXTENT_MIN_LONG_PROPERTY,"0");
    properties.put(ExtentSummary.EXTENT_MAX_LONG_PROPERTY,"90");

    ProductSummary productSummary = new ProductSummary();
    productSummary.setIndexId((long)5);
    productSummary.setProperties(properties);

    ExtentSummary extentSummary = new ExtentSummary(productSummary);

    GregorianCalendar startDate = new GregorianCalendar(2019,6,1);
    GregorianCalendar endDate = new GregorianCalendar(2019,6,4);

    Assert.assertEquals(startDate.getTime().getTime(),extentSummary.getStartTime().getTime());
    Assert.assertEquals(endDate.getTime().getTime(),extentSummary.getEndTime().getTime());
    Assert.assertEquals(0.,extentSummary.getMinLatitude());
    Assert.assertEquals(45.,extentSummary.getMaxLatitude());
    Assert.assertEquals(0.,extentSummary.getMinLongitude());
    Assert.assertEquals(90.,extentSummary.getMaxLongitude());
  }

  @Test
  public void validTest() {
    ExtentSummary summary = new ExtentSummary();

    Assert.assertFalse(summary.isValid());

    summary.setMinLongitude(0.);

    Assert.assertTrue(summary.isValid());
  }

}
