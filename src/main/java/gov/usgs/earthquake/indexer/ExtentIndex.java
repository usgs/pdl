/**
 * Extent Index
 */
package gov.usgs.earthquake.indexer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqlite.SQLiteException;

/**
 * ExtentIndex is a type of JDBCProductIndex that can also send updates to the extentSummary table.
 */
public class ExtentIndex extends JDBCProductIndex {

  private static final Logger LOGGER = Logger.getLogger(ExtentIndex.class.getName());
  public static final String EXTENT_TABLE = "extentSummary";
  public static final String EXTENT_INDEX_ID = "productid";
  public static final String EXTENT_START_TIME = "starttime";
  public static final String EXTENT_END_TIME = "endtime";
  public static final String EXTENT_MAX_LAT = "maxlatitude";
  public static final String EXTENT_MIN_LAT = "minlatitude";
  public static final String EXTENT_MAX_LONG = "maxlongitude";
  public static final String EXTENT_MIN_LONG = "minlongitude";

  public ExtentIndex() throws Exception {
    super();
  }

  /**
   * Queries extentSummary table for the largest index id.
   * 
   * @throws Exception if something goes wrong with database transaction
   */
  public long getLastExtentIndexId() throws Exception{
    long lastIndex;

    //Prepare statement
    Connection connection = connect();
    PreparedStatement getLastIndex;
    try {
      getLastIndex = connection.prepareStatement(
              "SELECT MAX(" + EXTENT_INDEX_ID +
                      ") AS " + EXTENT_INDEX_ID +
                      " FROM " + EXTENT_TABLE);
    } catch (SQLiteException e) {
      throw new SQLiteException(e.getMessage() + ". SQL query was: SELECT MAX(" + EXTENT_INDEX_ID + ") AS " + EXTENT_INDEX_ID + " FROM " + EXTENT_TABLE, e.getResultCode());
    }

    //Parse Results
    ResultSet results = getLastIndex.executeQuery();
    if (results.next()) {
      lastIndex = results.getLong(EXTENT_INDEX_ID);
    } else {
      //No index in extentSummary table
      lastIndex = 0;
      LOGGER.log(Level.FINEST,"[" + getName() + "] no products in extentSummary table; using index 0");
    }

    //Cleanup
    getLastIndex.close(); //Never needed again
    return lastIndex;
  }

  /**
   * Inserts valid ExtentSummary products into extentSummary table
   * 
   * @param product the product to be added
   * 
   * @throws Exception if something goes wrong with the database transaction
   */
  public void addExtentSummary(ExtentSummary product) throws Exception {
    if (!product.isValid()) {
      LOGGER.log(Level.FINE,"[" + getName() + "] product " + product.getId() + " has no extent information; won't add to extent table");
      return;
    }

    //Prepare statement
    Connection connection = connect();
    PreparedStatement addProduct = connection.prepareStatement(
      "INSERT INTO " + EXTENT_TABLE +
      "(" + 
      EXTENT_INDEX_ID + "," +
      EXTENT_START_TIME + "," + 
      EXTENT_END_TIME + "," +
      EXTENT_MIN_LAT + "," +
      EXTENT_MAX_LAT + "," +
      EXTENT_MIN_LONG + "," +
      EXTENT_MAX_LONG +
      ") VALUES (?, ?, ?, ?, ?, ?, ?)" );

    //Add values

    addProduct.setLong(1, product.getId());
    if (product.getStartTime() != null) {
      addProduct.setLong(2, product.getStartTime().getTime());
    } else {
      addProduct.setNull(2,Types.BIGINT);
    }
    if (product.getEndTime() != null) {
      addProduct.setLong(3, product.getEndTime().getTime());
    } else {
      addProduct.setNull(3,Types.BIGINT);
    }
    if (product.getMinLatitude() != null) {
      addProduct.setDouble(4, product.getMinLatitude());
    } else {
      addProduct.setNull(4,Types.DECIMAL);
    }
    if (product.getMaxLatitude() != null) {
      addProduct.setDouble(5, product.getMaxLatitude());
    } else {
      addProduct.setNull(5, Types.DECIMAL);
    }
    if (product.getMinLongitude() != null) {
      addProduct.setDouble(6, product.getMinLongitude());
    } else {
      addProduct.setNull(6,Types.DECIMAL);
    }
    if (product.getMaxLongitude() != null) {
      addProduct.setDouble(7, product.getMaxLongitude());
    } else {
      addProduct.setNull(7,Types.DECIMAL);
    }

    //Add to extentSummary table
    addProduct.executeUpdate();
    addProduct.clearParameters();

    //Cleanup
    addProduct.close();
  }

}