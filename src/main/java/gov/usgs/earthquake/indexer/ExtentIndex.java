/**
 * Extent Index
 */

package gov.usgs.earthquake.indexer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import gov.usgs.earthquake.indexer.JDBCProductIndex;


public class ExtentIndex extends JDBCProductIndex {

  private static final Logger LOGGER = Logger.getLogger(ExtentIndex.class.getName());
  private static final String EXTENT_TABLE = "extentSummary";
  public static final String EXTENT_START_TIME = "starttime";
  public static final String EXTENT_END_TIME = "endtime";
  public static final String EXTENT_MAX_LAT = "maxlatitude";
  public static final String EXTENT_MIN_LAT = "minlatitude";
  public static final String EXTENT_MAX_LONG = "maxlongitude";
  public static final String EXTENT_MIN_LONG = "minlongitude";

  public ExtentIndex() throws Exception{
    super();
  }

  public long getLastExtentIndexId() throws Exception{
    long lastIndex;

    //Prepare statement
    Connection connection = connect();
    PreparedStatement getLastIndex = connection.prepareStatement(
      "SELECT TOP 1 " + JDBCProductIndex.SUMMARY_PRODUCT_INDEX_ID + 
      " FROM " + EXTENT_TABLE + 
      " ORDER BY " + JDBCProductIndex.SUMMARY_PRODUCT_INDEX_ID + 
      " DESC");

    //Parse Results
    ResultSet results = getLastIndex.executeQuery();
    if (results.first()) {
      lastIndex = results.getLong(JDBCProductIndex.SUMMARY_PRODUCT_INDEX_ID);
    } else {
      //No index in extentSummary table
      lastIndex = 1;
      LOGGER.info("[" + getName() + "] no products in extentSummary table; using index 1");
    }

    //Cleanup
    getLastIndex.close(); //Never needed again
    return lastIndex;
  }

  public void addExtentSummary(ExtentSummary product) throws Exception {
    if (!product.isValid()) {
      LOGGER.info("[" + getName() + "] product " + product.getId() + " has no extent information; won't add to extent table");
      return;
    }

    //Prepare statement
    Connection connection = connect();
    PreparedStatement addProduct = connection.prepareStatement(
      "INSERT INTO " + EXTENT_TABLE +
      "(" + 
      JDBCProductIndex.SUMMARY_PRODUCT_INDEX_ID + "," + 
      EXTENT_START_TIME + "," + 
      EXTENT_END_TIME + "," + 
      EXTENT_MAX_LAT + "," + 
      EXTENT_MIN_LAT + "," + 
      EXTENT_MAX_LONG + "," + 
      EXTENT_MIN_LONG + 
      ") VALUES (?, ?, ?, ?, ?, ?, ?)" );

    //Add values

    addProduct.setLong(1, product.getId());
    addProduct.setLong(2, product.getStartTime().getTime());
    addProduct.setLong(3, product.getEndTime().getTime());
    addProduct.setDouble(4, product.getMaxLatitude());
    addProduct.setDouble(5, product.getMinLatitude());
    addProduct.setDouble(6, product.getMaxLongitude());
    addProduct.setDouble(7, product.getMinLongitude());

    //Add to extentSummary table
    addProduct.executeUpdate();
    addProduct.clearParameters();

    //Cleanup
    addProduct.close();
  }

}