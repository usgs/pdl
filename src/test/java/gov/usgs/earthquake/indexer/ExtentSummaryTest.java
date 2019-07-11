/*
 * Extent Summary Test
 */

package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.ProductId;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;

public class ExtentSummaryTest {

  @Test
  public void constructTest() {
    HashMap<String,String> properties = new HashMap<>();
    properties.put(ExtentIndex.EXTENT_START_TIME,"2019-07-01");
    properties.put(ExtentIndex.EXTENT_END_TIME,"2019-07-04");
    properties.put(ExtentIndex.EXTENT_MIN_LAT,"0");
    properties.put(ExtentIndex.EXTENT_MAX_LAT,"45");
    properties.put(ExtentIndex.EXTENT_MIN_LONG,"0");
    properties.put(ExtentIndex.EXTENT_MAX_LONG,"90");

    ProductSummary productSummary = new ProductSummary();
    productSummary.setIndexId((long)5);
    productSummary.setProperties(properties);

    ExtentSummary extentSummary = new ExtentSummary(productSummary);

    Assert.assertEquals(1561960800000l,extentSummary.getStartTime().getTime());
    Assert.assertEquals(1562220000000l,extentSummary.getEndTime().getTime());
    Assert.assertEquals(0.,extentSummary.getMinLatitude());
    Assert.assertEquals(45.,extentSummary.getMaxLatitude());
    Assert.assertEquals(0.,extentSummary.getMinLongitude());
    Assert.assertEquals(90.,extentSummary.getMaxLongitude());
  }

}
