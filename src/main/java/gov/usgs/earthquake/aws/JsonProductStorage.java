package gov.usgs.earthquake.aws;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;

import gov.usgs.earthquake.distribution.ProductAlreadyInStorageException;
import gov.usgs.earthquake.distribution.ProductStorage;
import gov.usgs.earthquake.distribution.StorageEvent;
import gov.usgs.earthquake.distribution.StorageListener;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.JsonProduct;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.earthquake.util.JDBCConnection;
import gov.usgs.util.Config;

public class JsonProductStorage extends JDBCConnection implements ProductStorage {

  private static final Logger LOGGER = Logger.getLogger(
      JsonProductStorage.class.getName());
  public static final String DEFAULT_DRIVER = "org.sqlite.JDBC";
  public static final String DEFAULT_TABLE = "product";
  public static final String DEFAULT_URL =
      "jdbc:sqlite:json_product_index.db";

  private String driver;
  private String table;
  private String url;

  public JsonProductStorage() {
    this(DEFAULT_DRIVER, DEFAULT_URL);
  }

  public JsonProductStorage(final String driver, final String url) {
    this(driver, url, DEFAULT_TABLE);
  }
  public JsonProductStorage(
      final String driver, final String url, final String table) {
    this.driver = driver;
    this.table = table;
    this.url = url;
  }

  public String getDriver() { return this.driver; }
  public String getTable() { return this.table; }
  public String getUrl() { return this.url; }
  public void setDriver(final String driver) { this.driver = driver; }
  public void setTable(final String table) { this.table = table; }
  public void setUrl(final String url) { this.url = url; }

  @Override
  public void configure(final Config config) throws Exception {
    driver = config.getProperty("driver", DEFAULT_DRIVER);
    LOGGER.config("[" + getName() + "] driver=" + driver);
    table = config.getProperty("table", DEFAULT_TABLE);
    LOGGER.config("[" + getName() + "] table=" + table);
    url = config.getProperty("url", DEFAULT_URL);
    // do not log url, it may contain user/pass
  }

  @Override
  protected Connection connect() throws Exception {
    // load driver if needed
    Class.forName(driver);
    return DriverManager.getConnection(url);
  }

  @Override
  public void startup() throws Exception {
    super.startup();
    // make sure schema exists
    if (!schemaExists()) {
      LOGGER.warning("[" + getName() + "] schema not found, creating");
      createSchema();
    }
  }

  @Override
  public void shutdown() throws Exception {
    // super closes connection
    super.shutdown();
  }

  public boolean schemaExists() throws Exception {
    final String sql = "select * from " + this.table + " limit 1";
    final Connection db = verifyConnection();
    db.setAutoCommit(false);
    try (final PreparedStatement test = db.prepareStatement(sql)) {
      // should throw exception if table does not exist
      try (final ResultSet rs = test.executeQuery()) {
        rs.next();
      }
      db.commit();
      // schema exists
      return true;
    } catch (Exception e) {
      db.rollback();
      return false;
    } finally {
      db.setAutoCommit(true);
    }
  }

  public void createSchema() throws Exception {
    // create schema
    final Connection db = verifyConnection();
    db.setAutoCommit(false);
    try (final Statement statement = db.createStatement()) {
      String autoIncrement = "";
      String engine = "";
      if (driver.contains("mysql")) {
        autoIncrement = " AUTO_INCREMENT";
        engine = " ENGINE=innodb CHARSET=utf8";
      }
      statement.executeUpdate(
          "CREATE TABLE " + this.table
          + " (id INTEGER PRIMARY KEY" + autoIncrement
          + ", source VARCHAR(255)"
          + ", type VARCHAR(255)"
          + ", code VARCHAR(255)"
          + ", updatetime BIGINT"
          + ", data TEXT"
          + ")" + engine);
      statement.executeUpdate(
          "CREATE UNIQUE INDEX product_index ON " + this.table
          + "(source, type, code, updatetime)");
      db.commit();
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.setAutoCommit(true);
    }
  }

  @Override
  public boolean hasProduct(ProductId id) throws Exception {
    return getProduct(id) != null;
  }

  @Override
  public synchronized Product getProduct(ProductId id) throws Exception {
    final String sql = "SELECT * FROM " + this.table
        + " WHERE source=? AND type=? AND code=? AND updatetime=?";
    // prepare statement
    final Connection db = verifyConnection();
    db.setAutoCommit(false);
    try (final PreparedStatement statement = db.prepareStatement(sql)) {
      try {

        // set parameters
        statement.setString(1, id.getSource());
        statement.setString(2, id.getType());
        statement.setString(3, id.getCode());
        statement.setLong(4, id.getUpdateTime().getTime());

        // execute
        try (final ResultSet rs = statement.executeQuery()) {
          if (rs.next()) {
            // found product
            final String data = rs.getString("data");
            Product product = new JsonProduct().getProduct(
                Json.createReader(
                  new ByteArrayInputStream(data.getBytes())
                ).readObject());
            return product;
          }
        }
      } catch (SQLException e) {
        try {
          // otherwise roll back
          db.rollback();
        } catch (SQLException e2) {
          // ignore
        }
        LOGGER.log(
            Level.INFO,
            "[" + getName() + "] exception in getProduct("
                + id.toString() + ")",
            e);
      }
    } finally {
      db.setAutoCommit(true);
    }
    return null;
  }

  @Override
  public synchronized ProductId storeProduct(Product product) throws Exception {
    // prepare statement
    final Connection db = verifyConnection();
    db.setAutoCommit(false);
    try (
      final PreparedStatement statement = db.prepareStatement(
          "INSERT INTO " + this.table
          + " (source, type, code, updatetime, data)"
          + " VALUES (?, ?, ?, ?, ?)")
    ) {
      try {
        final ProductId id = product.getId();
        // set parameters
        statement.setString(1, id.getSource());
        statement.setString(2, id.getType());
        statement.setString(3, id.getCode());
        statement.setLong(4, id.getUpdateTime().getTime());
        statement.setString(5,
            product != null
            ? new JsonProduct().getJsonObject(product).toString()
            : "");
        // execute
        statement.executeUpdate();
        db.commit();
        return id;
      } catch (SQLException e) {
        try {
          // otherwise roll back
          db.rollback();
        } catch (SQLException e2) {
          // ignore
        }
        if (e.toString().contains("Duplicate entry")) {
          throw new ProductAlreadyInStorageException(e.toString());
        }
        throw e;
      }
    } finally {
      db.setAutoCommit(true);
    }
  }

  @Override
  public ProductSource getProductSource(ProductId id) throws Exception {
    final Product product = getProduct(id);
    if (product == null) {
      return null;
    }
    return new ObjectProductSource(product);
  }

  @Override
  public ProductId storeProductSource(ProductSource input) throws Exception {
    final ObjectProductHandler handler = new ObjectProductHandler();
    input.streamTo(handler);
    return storeProduct(handler.getProduct());
  }

  @Override
  public synchronized void removeProduct(ProductId id) throws Exception {
    // prepare statement
    final String sql = "DELETE FROM " + this.table
          + " WHERE source=? AND type=? AND code=? AND updatetime=?";
    final Connection db = verifyConnection();
    db.setAutoCommit(false);
    try (final PreparedStatement statement = db.prepareStatement(sql)) {
      try {
        // set parameters
        statement.setString(1, id.getSource());
        statement.setString(2, id.getType());
        statement.setString(3, id.getCode());
        statement.setLong(4, id.getUpdateTime().getTime());
        // execute
        statement.executeUpdate();
        db.commit();
      } catch (SQLException e) {
        try {
          // otherwise roll back
          db.rollback();
        } catch (SQLException e2) {
          // ignore
        }
        throw e;
      }
    } finally {
      db.setAutoCommit(true);
    }
  }

  @Override
  public void notifyListeners(StorageEvent event) {
    // listeners not supported
  }

  @Override
  public void addStorageListener(StorageListener listener) {
    // listeners not supported
  }

  @Override
  public void removeStorageListener(StorageListener listener) {
    // listeners not supported
  }

}
