/**
 * Extent Index
 */

package gov.usgs.earthquake.indexer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.product.Product;
import gov.usgs.util.Config;
import gov.usgs.util.Configurable;
import gov.usgs.earthquake.indexer.JDBCProductIndex;


public class ExtentIndex implements Configurable{

  private static final Logger LOGGER = Logger.getLogger(ExtentIndex.class.getName());
  private static final String EXTENT_TABLE = "extentSummary";
  private static final String EXTENT_START_TIME = "starttime";
  private static final String EXTENT_END_TIME = "endtime";
  private static final String EXTENT_MAX_LAT = "maxlatitude";
  private static final String EXTENT_MIN_LAT = "minlatitude";
  private static final String EXTENT_MAX_LONG = "maxlongitude";
  private static final String EXTENT_MIN_LONG = "minlongitude";

  private String name;

  private JDBCProductIndex productIndex;
  private long lastIndex;

  private PreparedStatement getLastIndex;
  private PreparedStatement addProduct;

  public ExtentIndex() {
    super();
  }

  @Override
  public void configure(Config config) throws Exception {
    String indexName = config.getProperty(Indexer.INDEX_CONFIG_PROPERTY);
    if (indexName != null) {
      LOGGER.config("[" + getName() + "] loading ProductIndex '"
          + indexName + "'");
      productIndex = (JDBCProductIndex) config.getObject(indexName);
    } else {
      throw new ConfigurationException("[" + getName() + "] JDBCProductIndex is required");
    }

    Connection connection = productIndex.getConnection();

    getLastIndex = connection.prepareStatement(
      "SELECT TOP 1 " + JDBCProductIndex.SUMMARY_PRODUCT_INDEX_ID + 
      " FROM " + EXTENT_TABLE + 
      " ORDER BY " + JDBCProductIndex.SUMMARY_PRODUCT_INDEX_ID + 
      " DESC");

    addProduct = connection.prepareStatement(
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

  }

  @Override
  public void startup() throws Exception {
    //Get database connection
    Connection connection = productIndex.getConnection();
    //Get last index
    ResultSet results = getLastIndex.executeQuery();
    results.first();
    lastIndex = results.getLong(JDBCProductIndex.SUMMARY_PRODUCT_INDEX_ID); //Check with jmfee if I need to do error catching
    getLastIndex.close(); //Never needed again
  }

  @Override
  public void shutdown() throws Exception {
    if (addProduct != null) {
      addProduct.close();
    }
  }

  public long getLastIndexId() {
    //Fetch the last index
    return lastIndex;
  }

  //Currently using ExtentIndexQuery:
  //  - Not sure if that's the best name
  //  - That places product validation outside this class; is that the right spot?
  //  - Should ExtentIndexQuery map ProductSummaries to ExtentIndexQueries?
  public void addProduct(ExtentIndexQuery product) throws Exception {
    //Preparing statement (Could also be done in a loop if passed in as an array)
    addProduct.setLong(1, product.getId());
    addProduct.setLong(2, product.getStartTime());
    addProduct.setLong(3, product.getEndTime());
    addProduct.setDouble(4, product.getMaxLatitude());
    addProduct.setDouble(5, product.getMinLatitude());
    addProduct.setDouble(6, product.getMaxLongitude());
    addProduct.setDouble(7, product.getMinLongitude());
    //Add to extentSummary table
    addProduct.executeUpdate();
    addProduct.clearParameters();

    //Update lastIndex
    lastIndex = product.getId();
  }



  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void setName(String string) {
    this.name = string;
  }


   
 }