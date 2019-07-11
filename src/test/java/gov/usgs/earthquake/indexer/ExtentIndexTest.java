/*
 * Extent Index Test
 */

package gov.usgs.earthquake.indexer;

import junit.framework.Assert;
import org.junit.Test;

import gov.usgs.util.Config;

import java.sql.Connection;
import java.sql.PreparedStatement;


public class ExtentIndexTest {

  private ExtentIndex index;

  @Test
  public void addExistsTest() throws Exception{
    index = new ExtentIndex();
    index.configure(new Config());
    index.startup();

    //create product
    long testIndex = 9;
    ExtentSummary product = new ExtentSummary();
    product.setId(testIndex);
    product.setMaxLatitude(0.); //Add at least one parameter to pass validity check

    //add product, get product
    index.addExtentSummary(product);
    long response = index.getLastExtentIndexId();

    //verify existence
    Assert.assertEquals(testIndex,response);


    //clean up product
    Connection conn = index.connect();
    PreparedStatement stmnt = conn.prepareStatement("DELETE FROM " + ExtentIndex.EXTENT_TABLE + " WHERE " + ExtentIndex.EXTENT_INDEX_ID + "=" + product.getId());
    stmnt.executeUpdate();

    index.shutdown();
  }
}