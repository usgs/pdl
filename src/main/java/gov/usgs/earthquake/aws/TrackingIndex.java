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

public class TrackingIndex extends JDBCConnection {

  private static final Logger LOGGER = Logger.getLogger(
      TrackingIndex.class.getName());
  public static final String DEFAULT_DRIVER = "org.sqlite.JDBC";
  public static final String DEFAULT_TABLE = "tracking";
  public static final String DEFAULT_URL =
      "jdbc:sqlite:json_tracking_index.db";

  private String driver;
  private String table;
  private String url;


  public TrackingIndex() {
    this(DEFAULT_DRIVER, DEFAULT_URL);
  }

  public TrackingIndex(final String driver, final String url) {
    this(driver, url, DEFAULT_TABLE);
  }
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
    final Connection db = getConnection();

    final String sql = "select * from " + this.table + " limit 1";
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
    final Connection db = getConnection();

    // create schema
    db.setAutoCommit(false);
    try (final Statement statement = db.createStatement()) {
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
          "CREATE UNIQUE INDEX name_index ON " + this.table + "(name)");
      db.commit();
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.setAutoCommit(true);
    }
  }

  public synchronized JsonObject getTrackingData(final String name) throws Exception {
    final Connection db = getConnection();
    JsonObject data = null;

    final String sql = "SELECT * FROM " + this.table + " WHERE name=?";
    db.setAutoCommit(false);
    try (final PreparedStatement statement = db.prepareStatement(sql)) {
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
      db.commit();
    } catch (Exception e) {
      db.rollback();
    } finally {
      db.setAutoCommit(true);
    }

    return data;
  }

  public synchronized void removeTrackingData(final String name) throws Exception {
    final Connection db = getConnection();

    final String sql = "DELETE FROM " + this.table + " WHERE name=?";
    // create schema
    db.setAutoCommit(false);
    try (final PreparedStatement statement = db.prepareStatement(sql)) {
      statement.setString(1, name);

      statement.executeUpdate();
      db.commit();
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.setAutoCommit(true);
    }
  }

  public synchronized void setTrackingData(final String name, final JsonObject data) throws Exception {
    final Connection db = getConnection();

    final String update = "UPDATE " + this.table + " SET data=? WHERE name=?";
    // usually updated, try update first
    db.setAutoCommit(false);
    try (final PreparedStatement updateStatement = db.prepareStatement(update)) {
      updateStatement.setString(1, data.toString());
      updateStatement.setString(2, name);
      // execute update
      final int count = updateStatement.executeUpdate();
      // check number of rows updated (whether row already exists)
      if (count == 0) {
        final String insert = "INSERT INTO " + this.table + " (data, name) VALUES (?, ?)";
        // no rows updated
        try (final PreparedStatement insertStatement = db.prepareStatement(insert)) {
          insertStatement.setString(1, data.toString());
          insertStatement.setString(2, name);
          // execute insert
          insertStatement.executeUpdate();
        }
      }
      db.commit();
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.setAutoCommit(true);
    }
  }

}
