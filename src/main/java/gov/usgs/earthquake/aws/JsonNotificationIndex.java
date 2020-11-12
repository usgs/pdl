package gov.usgs.earthquake.aws;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.json.Json;


import gov.usgs.earthquake.distribution.DefaultNotification;
import gov.usgs.earthquake.distribution.Notification;
import gov.usgs.earthquake.distribution.NotificationIndex;
import gov.usgs.earthquake.distribution.URLNotification;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.JsonProduct;
import gov.usgs.earthquake.util.JDBCConnection;
import gov.usgs.util.Config;
import gov.usgs.util.StringUtils;

public class JsonNotificationIndex
    extends JDBCConnection
    implements NotificationIndex {

  private static final Logger LOGGER = Logger.getLogger(
      JsonNotificationIndex.class.getName());
  public static final String DEFAULT_DRIVER = "org.sqlite.JDBC";
  public static final String DEFAULT_TABLE = "notification";
  public static final String DEFAULT_URL =
      "jdbc:sqlite:json_notification_index.db";

  private String driver;
  private String table;
  private String url;

  public JsonNotificationIndex() {
    this(DEFAULT_DRIVER, DEFAULT_URL);
  }

  public JsonNotificationIndex(final String driver, final String url) {
    this(driver, url, DEFAULT_TABLE);
  }
  public JsonNotificationIndex(
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
          + ", created VARCHAR(255)"
          + ", expires VARCHAR(255)"
          + ", source VARCHAR(255)"
          + ", type VARCHAR(255)"
          + ", code VARCHAR(255)"
          + ", updatetime BIGINT"
          + ", url TEXT"
          + ", data TEXT"
          + ")" + engine);
      statement.executeUpdate(
          "CREATE INDEX source_index ON " + this.table + "(source)");
      statement.executeUpdate(
          "CREATE INDEX type_index ON " + this.table + "(type)");
      statement.executeUpdate(
          "CREATE INDEX code_index ON " + this.table + "(code)");
      statement.executeUpdate(
          "CREATE INDEX expires_index ON " + this.table + "(expires)");
      db.commit();
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.setAutoCommit(true);
    }
  }

  @Override
  public synchronized void addNotification(Notification notification)
      throws Exception {
    // all notifications
    Instant expires = notification.getExpirationDate().toInstant();
    ProductId id = notification.getProductId();
    // json only
    Instant created = null;
    Product product = null;
    // url only
    URL url = null;
    if (notification instanceof JsonNotification) {
      JsonNotification jsonNotification = (JsonNotification) notification;
      created = jsonNotification.created;
      product = jsonNotification.product;
    } else if (notification instanceof URLNotification) {
      url = ((URLNotification) notification).getProductURL();
    }
    // prepare statement
    final Connection db = verifyConnection();
    db.setAutoCommit(false);
    try (
      final PreparedStatement statement = db.prepareStatement(
          "INSERT INTO " + this.table
          + " (created, expires, source, type, code, updatetime, url, data)"
          + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
    ) {
      try {
        // set parameters
        statement.setString(1, created != null ? created.toString() : "");
        statement.setString(2, expires.toString());
        statement.setString(3, id.getSource());
        statement.setString(4, id.getType());
        statement.setString(5, id.getCode());
        statement.setLong(6, id.getUpdateTime().getTime());
        statement.setString(7, url != null ? url.toString() : "");
        if (product == null) {
          statement.setNull(8, Types.VARCHAR);
        } else {
          statement.setString(8,
              new JsonProduct().getJsonObject(product).toString());
        }
        // execute
        statement.executeUpdate();
        db.commit();
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Exception adding notification", e);
        try {
          // otherwise roll back
          db.rollback();
        } catch (SQLException e2) {
          // ignore
        }
      } finally {
        db.setAutoCommit(true);
      }
    }
  }

  @Override
  public synchronized void removeNotification(Notification notification) throws Exception {
    // all notifications
    Instant expires = notification.getExpirationDate().toInstant();
    ProductId id = notification.getProductId();
    // json only
    Instant created = null;
    Product product = null;
    // url only
    URL url = null;
    if (notification instanceof JsonNotification) {
      JsonNotification jsonNotification = (JsonNotification) notification;
      created = jsonNotification.created;
      product = jsonNotification.product;
    } else if (notification instanceof URLNotification) {
      url = ((URLNotification) notification).getProductURL();
    }
    // prepare statement
    final String sql = "DELETE FROM " + this.table
          + " WHERE created=? AND expires=? AND source=? AND type=? AND code=?"
          + " AND updatetime=? AND url=? AND data"
          + (product == null ? " IS NULL" : "=?");
    final Connection db = verifyConnection();
    db.setAutoCommit(false);
    try (final PreparedStatement statement = db.prepareStatement(sql)) {
      try {
        // set parameters
        statement.setString(1, created != null ? created.toString() : "");
        statement.setString(2, expires.toString());
        statement.setString(3, id.getSource());
        statement.setString(4, id.getType());
        statement.setString(5, id.getCode());
        statement.setLong(6, id.getUpdateTime().getTime());
        statement.setString(7, url != null ? url.toString() : "");
        if (product != null) {
          statement.setString(8,
              new JsonProduct().getJsonObject(product).toString());
        }
        // execute
        statement.executeUpdate();
        db.commit();
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Exception removing notification", e);
        try {
          // otherwise roll back
          db.rollback();
        } catch (SQLException e2) {
          // ignore
        }
      } finally {
        db.setAutoCommit(true);
      }
    }
  }

  @Override
  public synchronized List<Notification> findNotifications(String source, String type, String code) throws Exception {
    final ArrayList<Object> where = new ArrayList<Object>();
    final ArrayList<String> values = new ArrayList<String>();
    if (source != null) {
      where.add("source=?");
      values.add(source);
    }
    if (type != null) {
      where.add("type=?");
      values.add(type);
    }
    if (code != null) {
      where.add("code=?");
      values.add(code);
    }
    String sql = "SELECT * FROM " + this.table;
    if (where.size() > 0) {
      sql += " WHERE " + StringUtils.join(where, " AND ");
    }
    // prepare statement
    final Connection db = verifyConnection();
    db.setAutoCommit(false);
    try (final PreparedStatement statement = db.prepareStatement(sql)) {
      try {

        // set parameters
        for (int i = 0, len=values.size(); i < len; i++) {
          statement.setString(i+1, values.get(i));
        }

        // execute
        return getNotifications(statement);
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Exception finding notifications", e);
        try {
          // otherwise roll back
          db.rollback();
        } catch (SQLException e2) {
          // ignore
        }
      } finally {
        db.setAutoCommit(true);
      }
    }
    return new ArrayList<Notification>();
  }

  @Override
  public synchronized List<Notification> findNotifications(List<String> sources, List<String> types, List<String> codes)
      throws Exception {
    final ArrayList<Object> where = new ArrayList<Object>();
    final ArrayList<String> values = new ArrayList<String>();
    if (sources != null && sources.size() > 0) {
      where.add("source IN (" +
          StringUtils.join(
              Collections.nCopies(sources.size(), (Object)"?"),
              ",")
          + ")");
      values.addAll(sources);
    }
    if (types != null && types.size() > 0) {
      where.add("type IN (" +
          StringUtils.join(
              Collections.nCopies(types.size(), (Object)"?"),
              ",")
          + ")");
      values.addAll(types);
    }
    if (codes != null && codes.size() > 0) {
      where.add("code IN (" +
          StringUtils.join(
              Collections.nCopies(codes.size(), (Object)"?"),
              ",")
          + ")");
      values.addAll(codes);
    }
    String sql = "SELECT * FROM " + this.table;
    if (where.size() > 0) {
      sql += " WHERE " + StringUtils.join(where, " AND ");
    }
    // prepare statement
    final Connection db = verifyConnection();
    db.setAutoCommit(false);
    try (final PreparedStatement statement = db.prepareStatement(sql)) {
      try {

        // set parameters
        for (int i = 0, len=values.size(); i < len; i++) {
          statement.setString(i+1, values.get(i));
        }

        // execute
        return getNotifications(statement);
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Exception finding notifications", e);
        try {
          // otherwise roll back
          db.rollback();
        } catch (SQLException e2) {
          // ignore
        }
      } finally {
        db.setAutoCommit(true);
      }
    }
    return new ArrayList<Notification>();
  }

  @Override
  public synchronized List<Notification> findExpiredNotifications() throws Exception {
    final String sql = "SELECT * FROM " + this.table + " WHERE expires <= ?";
    // prepare statement
    final Connection db = verifyConnection();
    db.setAutoCommit(false);
    try (final PreparedStatement statement = db.prepareStatement(sql)) {
      try {
        // set parameters
        statement.setString(1, Instant.now().toString());

        // execute
        return getNotifications(statement);
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Exception finding notifications", e);
        try {
          // otherwise roll back
          db.rollback();
        } catch (SQLException e2) {
          // ignore
        }
      } finally {
        db.setAutoCommit(true);
      }
    }
    return new ArrayList<Notification>();

  }

  @Override
  public synchronized List<Notification> findNotifications(ProductId id) throws Exception {
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

        // executes and commit ifsuccessful
        return getNotifications(statement);
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Exception finding notifications", e);
        try {
          // otherwise roll back
          db.rollback();
        } catch (SQLException e2) {
          // ignore
        }
      } finally {
        db.setAutoCommit(true);
      }
    }
    return new ArrayList<Notification>();
  }

  /**
   * Parse notifications from a statement ready to be executed.
   */
  protected synchronized List<Notification> getNotifications(PreparedStatement ps)
      throws Exception {
    final List<Notification> n = new ArrayList<Notification>();
    try (final ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        n.add(parseNotification(
            rs.getString("created"),
            rs.getString("expires"),
            rs.getString("source"),
            rs.getString("type"),
            rs.getString("code"),
            rs.getLong("updatetime"),
            rs.getString("url"),
            rs.getString("data")));
      }
    }
    return n;
  }

  /**
   * Creates and returns a <code>Notification</code> based on the provided
   * data. If the <code>download</code> string references a valid URL, then a
   * <code>URLNotification</code> is created, otherwise a
   * <code>DefaultNotification</code> is created.
   */
  protected Notification parseNotification(
      final String created,
      final String expires,
      final String source,
      final String type,
      final String code,
      final Long updateTime,
      final String url,
      final String data) throws Exception {
    final Notification n;
    final ProductId id = new ProductId(source, type, code, new Date(updateTime));
    final Date expiresDate = Date.from(Instant.parse(expires));
    if (!"".equals(created) && data != null) {
      Product product = new JsonProduct().getProduct(
          Json.createReader(
            new ByteArrayInputStream(data.getBytes())
          ).readObject());
      n = new JsonNotification(Instant.parse(created), product);
    } else if (!"".equals(url)) {
      n = new URLNotification(id, expiresDate, null, new URL(url));
    } else {
      n = new DefaultNotification(id, expiresDate, null);
    }
    return n;
  }

}
