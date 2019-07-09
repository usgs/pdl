/*
 * Extent Index Test
 */

package gov.usgs.earthquake.indexer;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.util.Config;


public class ExtentIndexTest {

  private ExtentIndex index;

  @Before
  public void setup() throws Exception{
    index = new ExtentIndex();
		index.configure(new Config());
		index.startup();
  }

  @After
  public void shutdown() throws Exception{
    index.shutdown();
  }

  @Test
  public void addExistsTest() throws Exception{
    long testIndex = 9;

    ExtentSummary product = new ExtentSummary();
    product.setId(testIndex);
    product.setMaxLatitude(0.); //Add at least one parameter to pass validity check

    index.addExtentSummary(product);
    long response = index.getLastExtentIndexId();

    Assert.assertEquals(testIndex,response);

  }
}