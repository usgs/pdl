/**
 * Extent Index
 */
package gov.usgs.earthquake.indexer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.sql.SQLException;

/**
 * ExtentIndex is a type of JDBCProductIndex that can also send updates to the
 * extentSummary table.
 */
public class ExtentIndex extends JDBCProductIndex {

  /** Table for extentSummary */
  public static final String EXTENT_TABLE = "extentSummary";
  /** Extent index id - productSummaryIndexId */
  public static final String EXTENT_INDEX_ID = "productSummaryIndexId";
  /** Extent start time */
  public static final String EXTENT_START_TIME = "starttime";
  /** Extent end time */
  public static final String EXTENT_END_TIME = "endtime";
  /** Extent max latitude */
  public static final String EXTENT_MAX_LAT = "maximum_latitude";
  /** Extent minimum latitude */
  public static final String EXTENT_MIN_LAT = "minimum_latitude";
  /** Extent max longitude */
  public static final String EXTENT_MAX_LONG = "maximum_longitude";
  /** Extent min longitude */
  public static final String EXTENT_MIN_LONG = "minimum_longitude";

  /**
   * Default constructor
   * @throws Exception if error occurs
   */
  public ExtentIndex() throws Exception {
    super();
  }

  /**
   * Queries extentSummary table for the largest index id.
   *
   * @return long last extent index id
   * @throws Exception if something goes wrong with database transaction
   */
  public long getLastExtentIndexId() throws Exception {
    long lastIndex;

    //Prepare statement
    String sql = "SELECT MAX("
                 + EXTENT_INDEX_ID
                 + ") AS "
                 + EXTENT_INDEX_ID
                 + " FROM "
                 + EXTENT_TABLE;
    beginTransaction();
    try (PreparedStatement getLastIndex = getConnection().prepareStatement(sql)) {
      //Parse Results
      ResultSet results = getLastIndex.executeQuery();
      if (results.next()) {
        lastIndex = results.getLong(EXTENT_INDEX_ID);
      } else {
        //No index in extentSummary table
        lastIndex = 0;
      }
      commitTransaction();
    } catch (SQLException e) {
      try {
        rollbackTransaction();
      } catch (Exception e2) {}
      //Throws exception with SQL for debugging
      throw new SQLException(e.getMessage() + ". SQL query was: " + sql, e);
    }
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
    //Prepare statement
    String sql = "INSERT INTO " + EXTENT_TABLE +
        "(" +
          EXTENT_INDEX_ID + "," +
          EXTENT_START_TIME + "," +
          EXTENT_END_TIME + "," +
          EXTENT_MIN_LAT + "," +
          EXTENT_MAX_LAT + "," +
          EXTENT_MIN_LONG + "," +
          EXTENT_MAX_LONG +
        ") VALUES (?, ?, ?, ?, ?, ?, ?)";

    beginTransaction();
    try (PreparedStatement addProduct = getConnection().prepareStatement(sql)) {
      //Add values
      addProduct.setLong(1, product.getIndexId());
      if (product.getStartTime() != null) {
        addProduct.setLong(2, product.getStartTime().getTime());
      } else {
        addProduct.setNull(2, Types.BIGINT);
      }
      if (product.getEndTime() != null) {
        addProduct.setLong(3, product.getEndTime().getTime());
      } else {
        addProduct.setNull(3, Types.BIGINT);
      }
      if (product.getMinLatitude() != null) {
        addProduct.setBigDecimal(4, product.getMinLatitude());
      } else {
        addProduct.setNull(4, Types.DECIMAL);
      }
      if (product.getMaxLatitude() != null) {
        addProduct.setBigDecimal(5, product.getMaxLatitude());
      } else {
        addProduct.setNull(5, Types.DECIMAL);
      }
      if (product.getMinLongitude() != null) {
        addProduct.setBigDecimal(6, product.getMinLongitude());
      } else {
        addProduct.setNull(6, Types.DECIMAL);
      }
      if (product.getMaxLongitude() != null) {
        addProduct.setBigDecimal(7, product.getMaxLongitude());
      } else {
        addProduct.setNull(7, Types.DECIMAL);
      }

      //Add to extentSummary table
      addProduct.executeUpdate();
      addProduct.clearParameters();
      commitTransaction();
    } catch (Exception e) {
      try {
        rollbackTransaction();
      } catch (Exception e2) {}
      throw e;
    }
  }

}
