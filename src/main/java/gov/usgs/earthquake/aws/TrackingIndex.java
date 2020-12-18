package gov.usgs.earthquake.aws;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import gov.usgs.earthquake.util.JDBCConnection;
import gov.usgs.util.Config;

/**
 * Tracking index stores component state in a database.
 *
 * Only SQLITE or local development should rely on createSchema.
 *
 * Mysql Schema Example:<br>
 * <pre>
 * CREATE TABLE IF NOT EXISTS tracking_index
 * (id INTEGER PRIMARY KEY AUTO_INCREMENT
 * , created VARCHAR(255)
 * , name VARCHAR(255)
 * , data LONGTEXT
 * , UNIQUE KEY name_index (name)
 * ) ENGINE=innodb CHARSET=utf8;
 * </pre>
 */
public class TrackingIndex extends JDBCConnection {

  private static final Logger LOGGER = Logger.getLogger(
      TrackingIndex.class.getName());

  public static final String DEFAULT_DRIVER = "org.sqlite.JDBC";
  public static final String DEFAULT_TABLE = "tracking";
  public static final String DEFAULT_URL = "jdbc:sqlite:json_tracking_index.db";

  /** JDBC driver classname. */
  private String driver;
  /** Database table name. */
  private String table;
  /** JDBC database connect url. */
  private String url;

  /**
   * Construct a TrackingIndex using defaults.
   */
  public TrackingIndex() {
    this(DEFAULT_DRIVER, DEFAULT_URL);
  }

  /**
   * Construct a TrackingIndex with the default table.
   */
  public TrackingIndex(final String driver, final String url) {
    this(driver, url, DEFAULT_TABLE);
  }

  /**
   * Construct a TrackingIndex with custom driver, url, and table.
   */
  public TrackingIndex(
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

  /**
   * Connect to database.
   *
   * Implements abstract JDBCConnection method.
   */
  @Override
  protected Connection connect() throws Exception {
    // load driver if needed
    Class.forName(driver);
    return DriverManager.getConnection(url);
  }

  /**
   * After normal startup, check whether schema exists and attempt to create.
   */
  @Override
  public void startup() throws Exception {
    super.startup();
    // make sure schema exists
    if (!schemaExists()) {
      LOGGER.warning("[" + getName() + "] schema not found, creating");
      createSchema();
    }
  }

  /**
   * Check whether schema exists.
   *
   * @return
   * @throws Exception
   */
  public boolean schemaExists() throws Exception {
    final String sql = "select * from " + this.table + " limit 1";
    beginTransaction();
    try (final PreparedStatement test = getConnection().prepareStatement(sql)) {
      // should throw exception if table does not exist
      try (final ResultSet rs = test.executeQuery()) {
        rs.next();
      }
      commitTransaction();
      // schema exists
      return true;
    } catch (Exception e) {
      rollbackTransaction();
      return false;
    }
  }

  /**
   * Attempt to create schema.
   *
   * Only supports sqlite or mysql.  When not using sqlite, relying on this
   * method is only recommended for local development.
   *
   * @throws Exception
   */
  public void createSchema() throws Exception {
    // create schema
    beginTransaction();
    try (final Statement statement = getConnection().createStatement()) {
      String autoIncrement = "";
      if (driver.contains("mysql")) {
        autoIncrement = "AUTO_INCREMENT";
      }
      statement.executeUpdate(
          "CREATE TABLE " + this.table
          + " (id INTEGER PRIMARY KEY " + autoIncrement
          + ", created VARCHAR(255)"
          + ", name VARCHAR(255)"
          + ", data TEXT"
          + ")");
      statement.executeUpdate(
          "CREATE UNIQUE INDEX name_index ON " + this.table + " (name)");
      commitTransaction();
    } catch (Exception e) {
      rollbackTransaction();
      throw e;
    }
  }

  /**
   * Get tracking data for specified name.
   *
   * @param name
   *     name of tracking data.
   * @return null if data not found.
   */
  public JsonObject getTrackingData(final String name) throws Exception {
    JsonObject data = null;

    final String sql = "SELECT * FROM " + this.table + " WHERE name=?";
    beginTransaction();
    try (final PreparedStatement statement = getConnection().prepareStatement(sql)) {
      statement.setString(1, name);

      // execute and parse data
      try (final ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          final String json = rs.getString("data");
          try (
            final JsonReader jsonReader = Json.createReader(
                new ByteArrayInputStream(json.getBytes()))
          ) {
            data = jsonReader.readObject();
          }
        }
      }
      commitTransaction();
    } catch (Exception e) {
      rollbackTransaction();
    }

    return data;
  }

  /**
   * Remove tracking data.
   *
   * @param name
   *     name of tracking data.
   * @throws Exception
   */
  public void removeTrackingData(final String name) throws Exception {
    final String sql = "DELETE FROM " + this.table + " WHERE name=?";
    // create schema
    beginTransaction();
    try (final PreparedStatement statement = getConnection().prepareStatement(sql)) {
      statement.setString(1, name);

      statement.executeUpdate();
      commitTransaction();
    } catch (Exception e) {
      rollbackTransaction();
      throw e;
    }
  }

  /**
   * Add or update tracking data.
   *
   * @param name
   *     name of tracking data.
   * @param data
   *     data to store.
   * @throws Exception
   */
  public void setTrackingData(final String name, final JsonObject data) throws Exception {
    final String update = "UPDATE " + this.table + " SET data=? WHERE name=?";
    // usually updated, try update first
    beginTransaction();
    try (final PreparedStatement updateStatement = getConnection().prepareStatement(update)) {
      updateStatement.setString(1, data.toString());
      updateStatement.setString(2, name);
      // execute update
      final int count = updateStatement.executeUpdate();
      // check number of rows updated (whether row already exists)
      if (count == 0) {
        final String insert = "INSERT INTO " + this.table + " (data, name) VALUES (?, ?)";
        // no rows updated
        try (final PreparedStatement insertStatement = getConnection().prepareStatement(insert)) {
          insertStatement.setString(1, data.toString());
          insertStatement.setString(2, name);
          // execute insert
          insertStatement.executeUpdate();
        }
      }
      commitTransaction();
    } catch (Exception e) {
      rollbackTransaction();
      throw e;
    }
  }

}
