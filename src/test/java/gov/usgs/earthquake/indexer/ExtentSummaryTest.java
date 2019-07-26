/*
 * Extent Summary Test
 */
package gov.usgs.earthquake.indexer;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.GregorianCalendar;
import java.util.HashMap;

public class ExtentSummaryTest {

  @Test
  public void constructTest() {
    //Create extentSummary properties
    HashMap<String,String> properties = new HashMap<>();
    properties.put(ExtentSummary.EXTENT_START_TIME_PROPERTY,"2019-07-01");
    properties.put(ExtentSummary.EXTENT_END_TIME_PROPERTY,"2019-07-04");
    properties.put(ExtentSummary.EXTENT_MIN_LAT_PROPERTY,"0");
    properties.put(ExtentSummary.EXTENT_MAX_LAT_PROPERTY,"45");
    properties.put(ExtentSummary.EXTENT_MIN_LONG_PROPERTY,"0");
    properties.put(ExtentSummary.EXTENT_MAX_LONG_PROPERTY,"90");

    //Create productSummary with properties
    ProductSummary productSummary = new ProductSummary();
    productSummary.setIndexId((long)5);
    productSummary.setProperties(properties);

    //Create extentSummary with productSummary
    ExtentSummary extentSummary = new ExtentSummary(productSummary);

    GregorianCalendar startDate = new GregorianCalendar(2019,6,1);
    GregorianCalendar endDate = new GregorianCalendar(2019,6,4);

    //Verify that extentSummary was constructed correctly
    Assert.assertEquals(startDate.getTime().getTime(),extentSummary.getStartTime().getTime());
    Assert.assertEquals(endDate.getTime().getTime(),extentSummary.getEndTime().getTime());
    Assert.assertEquals(new BigDecimal(0),extentSummary.getMinLatitude());
    Assert.assertEquals(new BigDecimal(45),extentSummary.getMaxLatitude());
    Assert.assertEquals(new BigDecimal(0),extentSummary.getMinLongitude());
    Assert.assertEquals(new BigDecimal(90),extentSummary.getMaxLongitude());
  }

  @Test
  public void validTest() {
    ExtentSummary summary = new ExtentSummary();

    //Make sure extentSummary with no information is invalid
    Assert.assertFalse(summary.isValid());

    summary.setMinLongitude(new BigDecimal(0));

    //Make sure extentSummary with at least one information is valid
    Assert.assertTrue(summary.isValid());
  }

}
