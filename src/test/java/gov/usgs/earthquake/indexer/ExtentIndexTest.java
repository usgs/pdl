/*
 * Extent Index Test
 */

package gov.usgs.earthquake.indexer;

import org.junit.Assert;
import org.junit.Test;

import gov.usgs.util.Config;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;


public class ExtentIndexTest {

  @Test
  public void addExistsTest() throws Exception{
    ExtentIndex index = new ExtentIndex();
    index.configure(new Config());
    index.startup();

    //create product
    long testIndex = 9;
    ExtentSummary product = new ExtentSummary();
    product.setIndexId(testIndex);
    product.setMaxLatitude(BigDecimal.ZERO); //Add at least one parameter to pass validity check

    //add product, get product
    index.addExtentSummary(product);
    long response = index.getLastExtentIndexId();

    //verify existence
    Assert.assertEquals(testIndex,response);


    //clean up product
    Connection conn = index.connect();
    PreparedStatement stmnt = conn.prepareStatement("DELETE FROM " + ExtentIndex.EXTENT_TABLE + " WHERE " + ExtentIndex.EXTENT_INDEX_ID + "=" + product.getIndexId());
    stmnt.executeUpdate();

    index.shutdown();
  }
}