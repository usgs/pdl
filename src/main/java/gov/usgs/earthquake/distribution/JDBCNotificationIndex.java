/*
 * JDBCNotificationIndex
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.util.JDBCConnection;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Stores and retrieves Notifications.
 *
 * This is typically used by a NotificationReceiver to track its Notifications,
 * but may also be used by NotificationListeners. Each object should maintain a
 * separate NotificationIndex.
 *
 * This implementation uses a SQLite Database as the index.
 *
 * @see gov.usgs.earthquake.distribution.NotificationIndex
 */
public class JDBCNotificationIndex extends JDBCConnection implements
		NotificationIndex {

	private static final Logger LOGGER = Logger
			.getLogger(JDBCNotificationIndex.class.getName());

	/**
	 * Default (empty) DB Schema SQLite file for the index. If the configured
	 * index file does not exist in the file system at the time this instance's
	 * "startup()" method is called, then this file is copied out of the JAR
	 * file into the file system as configured.
	 *
	 * The schema contained in this file is very simple:
	 *
	 * CREATE TABLE notification_index ( id INTEGER PRIMARY KEY NOT NULL,
	 * product_source TEXT NOT NULL, product_type TEXT NOT NULL, product_code
	 * TEXT NOT NULL, product_update LONG NOT NULL, expiration_date LONG NOT
	 * NULL, tracker_url TEXT NOT NULL, product_url TEXT );
	 *
	 * CREATE TABLE tmp_lookup_table ( product_source TEXT, product_type TEXT,
	 * product_code TEXT);
	 *
	 * CREATE INDEX expired_index on notification_index (expiration_date);
	 * CREATE INDEX id_index on notification_index (product_source,
	 * product_type, product_code, product_update);
	 *
	 * CREATE TABLE notification_queue ( id INTEGER PRIMARY KEY NOT NULL,
	 * queue_name TEXT NOT NULL, product_source TEXT NOT NULL, product_type TEXT
	 * NOT NULL, product_code TEXT NOT NULL, product_update LONG NOT NULL );
	 *
	 * CREATE INDEX queue_index on notification_queue (queue_name,
	 * product_source, product_type, product_code, product_update);
	 *
	 */
	private static final String JDBC_DB_SCHEMA = "etc/schema/notificationIndex.db";

	// The following variables reference database information and are used for
	// binding/fetching query parameters in the prepared statements
	private static final String TABLE_NAME = "notification_index";
	private static final String TMP_TABLE = "tmp_lookup_table";
	private static final String ID_COLUMN = "id";
	private static final String PRODUCT_SOURCE_COLUMN = "product_source";
	private static final String PRODUCT_TYPE_COLUMN = "product_type";
	private static final String PRODUCT_CODE_COLUMN = "product_code";
	private static final String PRODUCT_UPDATE_COLUMN = "product_update";
	private static final String EXPIRATION_DATE_COLUMN = "expiration_date";
	private static final String TRACKER_URL_COLUMN = "tracker_url";
	private static final String PRODUCT_URL_COLUMN = "product_url";

	// SQLite driver information
	/** SQLite driver class name. */
	private static final String JDBC_DRIVER_CLASS = "org.sqlite.JDBC";
	/** SQLite connect url without a filename. */
	private static final String JDBC_CONNECT_URL = "jdbc:sqlite:";
	/** Default SQLite database filename. */
	private static final String JDBC_DEFAULT_FILE = "pd_index.db";

	// This is the property key used in the configuration file to specify a
	// different SQLite database file. If this file doesn't exist it will be
	// created at startup time
	protected static final String JDBC_FILE_PROPERTY = "indexfile";

	/** SQL stub for adding a notification to the index. */
	private static final String DML_ADD_NOTIFICATION = String.format(
			"INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s) VALUES "
					+ "(?, ?, ?, ?, ?, ?, ?)", TABLE_NAME,
			PRODUCT_SOURCE_COLUMN, PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN,
			PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN, TRACKER_URL_COLUMN,
			PRODUCT_URL_COLUMN);

	/** SQL stub for removing a notification from the index. */
	private static final String DML_REMOVE_NOTIFICATION = String.format(
			"DELETE FROM %s WHERE %s = ? AND %s = ? AND %s = ? "
					+ "AND %s = ? AND %s = ? AND %s = ? AND %s = ?",
			TABLE_NAME, PRODUCT_SOURCE_COLUMN, PRODUCT_TYPE_COLUMN,
			PRODUCT_CODE_COLUMN, PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN,
			TRACKER_URL_COLUMN, PRODUCT_URL_COLUMN);

	/** SQL stub for finding expired notifications. */
	private static final String QUERY_FIND_EXPIRED_NOTIFICATIONS = String
			.format("SELECT %s, %s, %s, %s, %s, %s, %s, %s FROM %s "
					+ "WHERE %s <= ?", ID_COLUMN, PRODUCT_SOURCE_COLUMN,
					PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN,
					PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN,
					TRACKER_URL_COLUMN, PRODUCT_URL_COLUMN, TABLE_NAME,
					EXPIRATION_DATE_COLUMN);

	/** SQL stub for finding notifications about a particular productId */
	private static final String QUERY_FIND_NOTIFICATIONS_BY_ID = String.format(
			"SELECT %s, %s, %s, %s, %s, %s, %s, %s FROM %s "
					+ "WHERE %s = ? AND %s = ? AND %s = ? AND %s = ?",
			ID_COLUMN, PRODUCT_SOURCE_COLUMN, PRODUCT_TYPE_COLUMN,
			PRODUCT_CODE_COLUMN, PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN,
			TRACKER_URL_COLUMN, PRODUCT_URL_COLUMN, TABLE_NAME,
			PRODUCT_SOURCE_COLUMN, PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN,
			PRODUCT_UPDATE_COLUMN);

	/**
	 * SQL stub for finding notifications about products based on discrete data.
	 */
	private static final String QUERY_FIND_NOTIFICATIONS_BY_DATA = String
			.format("SELECT %s, %s, %s, %s, %s, %s, %s FROM %s WHERE "
					+ "UPPER(%s) LIKE ? AND UPPER(%s) LIKE ? AND "
					+ "UPPER(%s) LIKE ?", PRODUCT_SOURCE_COLUMN,
					PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN,
					PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN,
					TRACKER_URL_COLUMN, PRODUCT_URL_COLUMN, TABLE_NAME,
					PRODUCT_SOURCE_COLUMN, PRODUCT_TYPE_COLUMN,
					PRODUCT_CODE_COLUMN);

	/** Name of the SQLite DB file to use. This is configurable. */
	private String _jdbc_index_file = null;

	/**
	 * Connection URL. Created from the JDBC_CONNET_URL and configured index
	 * file.
	 */
	private String _jdbc_connect_url = null;

	// These are the prepared statements we will use for all DB interactions. //

	private PreparedStatement _dml_addNotification = null;
	private PreparedStatement _dml_removeNotification = null;

	private PreparedStatement _query_findExpiredNotifications = null;
	private PreparedStatement _query_findNotificationsById = null;
	private PreparedStatement _query_findNotificationsByData = null;

	// Stubs used in the list version of find by data method. //
	private static final String DML_CREATE_TMP_TABLE = String.format(
			"CREATE TABLE IF NOT EXISTS %s (%s TEXT, %s TEXT, %s TEXT)",
			TMP_TABLE, PRODUCT_SOURCE_COLUMN, PRODUCT_TYPE_COLUMN,
			PRODUCT_CODE_COLUMN);
	private static final String DML_ADD_TMP_SOURCE = String.format(
			"INSERT INTO %s (%s) VALUES (?)", TMP_TABLE, PRODUCT_SOURCE_COLUMN);
	private static final String DML_ADD_TMP_TYPE = String.format(
			"INSERT INTO %s (%s) VALUES (?)", TMP_TABLE, PRODUCT_TYPE_COLUMN);
	private static final String DML_ADD_TMP_CODE = String.format(
			"INSERT INTO %s (%s) VALUES (?)", TMP_TABLE, PRODUCT_CODE_COLUMN);

	private static final String QUERY_SEARCH_BY_SOURCE_TYPE_CODE = String
			.format("SELECT %s, %s, %s, %s, %s, %s, %s FROM %s WHERE %s IN "
					+ "(SELECT %s FROM %s) AND %s IN (SELECT %s FROM %s) AND "
					+ "%s IN (SELECT %s FROM %s)", PRODUCT_SOURCE_COLUMN,
					PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN,
					PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN,
					TRACKER_URL_COLUMN, PRODUCT_URL_COLUMN, TABLE_NAME,
					PRODUCT_SOURCE_COLUMN, PRODUCT_SOURCE_COLUMN, TMP_TABLE,
					PRODUCT_TYPE_COLUMN, PRODUCT_TYPE_COLUMN, TMP_TABLE,
					PRODUCT_CODE_COLUMN, PRODUCT_CODE_COLUMN, TMP_TABLE);

	private static final String QUERY_SEARCH_BY_SOURCE_TYPE = String.format(
			"SELECT %s, %s, %s, %s, %s, %s, %s FROM %s WHERE %s IN "
					+ "(SELECT %s FROM %s) AND %s IN (SELECT %s FROM %s)",
			PRODUCT_SOURCE_COLUMN, PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN,
			PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN, TRACKER_URL_COLUMN,
			PRODUCT_URL_COLUMN, TABLE_NAME, PRODUCT_SOURCE_COLUMN,
			PRODUCT_SOURCE_COLUMN, TMP_TABLE, PRODUCT_TYPE_COLUMN,
			PRODUCT_TYPE_COLUMN, TMP_TABLE);

	private static final String QUERY_SEARCH_BY_SOURCE_CODE = String.format(
			"SELECT %s, %s, %s, %s, %s, %s, %s FROM %s WHERE %s IN "
					+ "(SELECT %s FROM %s) AND %s IN (SELECT %s FROM %s)",
			PRODUCT_SOURCE_COLUMN, PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN,
			PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN, TRACKER_URL_COLUMN,
			PRODUCT_URL_COLUMN, TABLE_NAME, PRODUCT_SOURCE_COLUMN,
			PRODUCT_SOURCE_COLUMN, TMP_TABLE, PRODUCT_CODE_COLUMN,
			PRODUCT_CODE_COLUMN, TMP_TABLE);

	private static final String QUERY_SEARCH_BY_TYPE_CODE = String.format(
			"SELECT %s, %s, %s, %s, %s, %s, %s FROM %s WHERE %s IN "
					+ "(SELECT %s FROM %s) AND %s IN (SELECT %s FROM %s)",
			PRODUCT_SOURCE_COLUMN, PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN,
			PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN, TRACKER_URL_COLUMN,
			PRODUCT_URL_COLUMN, TABLE_NAME, PRODUCT_TYPE_COLUMN,
			PRODUCT_TYPE_COLUMN, TMP_TABLE, PRODUCT_CODE_COLUMN,
			PRODUCT_CODE_COLUMN, TMP_TABLE);

	private static final String QUERY_SEARCH_BY_SOURCE = String
			.format("SELECT %s, %s, %s, %s, %s, %s, %s FROM %s WHERE %s IN "
					+ "(SELECT %s FROM %s)", PRODUCT_SOURCE_COLUMN,
					PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN,
					PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN,
					TRACKER_URL_COLUMN, PRODUCT_URL_COLUMN, TABLE_NAME,
					PRODUCT_SOURCE_COLUMN, PRODUCT_SOURCE_COLUMN, TMP_TABLE);

	private static final String QUERY_SEARCH_BY_TYPE = String.format(
			"SELECT %s, %s, %s, %s, %s, %s, %s FROM %s WHERE %s IN "
					+ "(SELECT %s FROM %s)", PRODUCT_SOURCE_COLUMN,
			PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN, PRODUCT_UPDATE_COLUMN,
			EXPIRATION_DATE_COLUMN, TRACKER_URL_COLUMN, PRODUCT_URL_COLUMN,
			TABLE_NAME, PRODUCT_TYPE_COLUMN, PRODUCT_TYPE_COLUMN, TMP_TABLE);

	private static final String QUERY_SEARCH_BY_CODE = String.format(
			"SELECT %s, %s, %s, %s, %s, %s, %s FROM %s WHERE %s IN "
					+ "(SELECT %s FROM %s)", PRODUCT_SOURCE_COLUMN,
			PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN, PRODUCT_UPDATE_COLUMN,
			EXPIRATION_DATE_COLUMN, TRACKER_URL_COLUMN, PRODUCT_URL_COLUMN,
			TABLE_NAME, PRODUCT_CODE_COLUMN, PRODUCT_CODE_COLUMN, TMP_TABLE);
	/*
	 * private static final String QUERY_ALL_NOTIFICATIONS = String.format(
	 * "SELECT %s, %s, %s, %s, %s, %s, %s FROM %s", PRODUCT_SOURCE_COLUMN,
	 * PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN, PRODUCT_UPDATE_COLUMN,
	 * EXPIRATION_DATE_COLUMN, TRACKER_URL_COLUMN, PRODUCT_URL_COLUMN,
	 * TABLE_NAME);
	 */

	private static final String QUERY_LATEST_NOTIFICATIONS = String.format(
			"SELECT n.%s, n.%s, n.%s, n.%s, n.%s, n.%s, n.%s FROM %s n, "
					+ "(select max(id) as id, product_source, product_type, "
					+ "product_code, product_update from notification_index "
					+ "group by product_source, product_type, product_code, "
					+ "product_update) latest where n.id=latest.id "
					+ " order by n.product_update asc",
			PRODUCT_SOURCE_COLUMN, PRODUCT_TYPE_COLUMN, PRODUCT_CODE_COLUMN,
			PRODUCT_UPDATE_COLUMN, EXPIRATION_DATE_COLUMN, TRACKER_URL_COLUMN,
			PRODUCT_URL_COLUMN, TABLE_NAME);

	// These are for searching the DB index by specific parameters. */
	private PreparedStatement _dml_createTmpTable = null;
	private PreparedStatement _dml_addTmpSource = null;
	private PreparedStatement _dml_addTmpType = null;
	private PreparedStatement _dml_addTmpCode = null;

	private PreparedStatement _query_searchBySourceTypeCode = null;
	private PreparedStatement _query_searchBySourceType = null;
	private PreparedStatement _query_searchBySourceCode = null;
	private PreparedStatement _query_searchByTypeCode = null;
	private PreparedStatement _query_searchBySource = null;
	private PreparedStatement _query_searchByType = null;
	private PreparedStatement _query_searchByCode = null;
	private PreparedStatement _query_getAllNotifications = null;

	/**
	 * Default, no-arg constructor. This just ensures the JDBC SQLite driver is
	 * appropriately on the classpath for proper runtime execution. This
	 * probably will not get called directly in favor of the configurable
	 * constructor.
	 *
	 * @throws Exception
	 *             If the JDBC driver class is not found.
	 * @see #JDBC_DRIVER_CLASS
	 */
	public JDBCNotificationIndex() throws Exception {
		this((String) null);
	}

	public JDBCNotificationIndex(final String filename) throws Exception {
		Class.forName(JDBC_DRIVER_CLASS);
		_jdbc_index_file = filename;
		if (_jdbc_index_file == null) {
			_jdbc_index_file = JDBC_DEFAULT_FILE;
		}
		_jdbc_connect_url = JDBC_CONNECT_URL + _jdbc_index_file;
	}

	/**
	 * Constructor called from the config object conforming to the
	 * <code>Configurable</code> interface specification. This internally calls
	 * its no-arg constructor then configures itself.
	 *
	 * @param config
	 *            The config object from which this instance will be configured.
	 * @throws Exception
	 *             If the JDBC driver class is not found.
	 * @see gov.usgs.util.Configurable
	 * @see #JDBC_DRIVER_CLASS
	 */
	public JDBCNotificationIndex(Config config) throws Exception {
		this();
		this.configure(config);
	}

	/**
	 * Reads the given <code>config</code> object and sets values appropriately.
	 *
	 * @param config
	 *            The config object from which this instance will be configured.
	 * @see gov.usgs.util.Configurable
	 */
	public void configure(Config config) throws Exception {
		_jdbc_index_file = config.getProperty(JDBC_FILE_PROPERTY);
		if (_jdbc_index_file == null || "".equals(_jdbc_index_file)) {
			_jdbc_index_file = JDBC_DEFAULT_FILE;
		}
		LOGGER.config("Notification index database is '" + _jdbc_index_file
				+ "'");
		_jdbc_connect_url = JDBC_CONNECT_URL + _jdbc_index_file;
	}

	@Override
	protected Connection connect() throws Exception {
		// Make sure file exists or copy it out of the JAR
		File indexFile = new File(_jdbc_index_file);
		if (!indexFile.exists()) {
			// extract schema from jar
			URL schemaURL = JDBCNotificationIndex.class.getClassLoader()
					.getResource(JDBC_DB_SCHEMA);
			if (schemaURL == null) {
				schemaURL = new File(JDBC_DB_SCHEMA).toURI().toURL();
			}
			StreamUtils.transferStream(schemaURL, indexFile);
		}

		return DriverManager.getConnection(_jdbc_connect_url);
	}

	/**
	 * Connects to the JDBC DB index and prepares the DML/Query statements that
	 * will execute at runtime. If the JDBC DB index file does not exist then an
	 * empty schema will be copied out of the executing JAR file to be used.
	 *
	 * @see gov.usgs.util.Configurable
	 */
	public void startup() throws Exception {
		// call super startup to connect
		super.startup();
		Connection conn = getConnection();

		// prepare statements
		_dml_addNotification = conn.prepareStatement(DML_ADD_NOTIFICATION);
		_dml_removeNotification = conn
				.prepareStatement(DML_REMOVE_NOTIFICATION);

		_query_findExpiredNotifications = conn
				.prepareStatement(QUERY_FIND_EXPIRED_NOTIFICATIONS);
		_query_findNotificationsById = conn
				.prepareStatement(QUERY_FIND_NOTIFICATIONS_BY_ID);
		_query_findNotificationsByData = conn
				.prepareStatement(QUERY_FIND_NOTIFICATIONS_BY_DATA);

		_dml_createTmpTable = conn.prepareStatement(DML_CREATE_TMP_TABLE);
		_dml_addTmpSource = conn.prepareStatement(DML_ADD_TMP_SOURCE);
		_dml_addTmpType = conn.prepareStatement(DML_ADD_TMP_TYPE);
		_dml_addTmpCode = conn.prepareStatement(DML_ADD_TMP_CODE);

		_query_searchBySourceTypeCode = conn
				.prepareStatement(QUERY_SEARCH_BY_SOURCE_TYPE_CODE);
		_query_searchBySourceType = conn
				.prepareStatement(QUERY_SEARCH_BY_SOURCE_TYPE);
		_query_searchBySourceCode = conn
				.prepareStatement(QUERY_SEARCH_BY_SOURCE_CODE);
		_query_searchByTypeCode = conn
				.prepareStatement(QUERY_SEARCH_BY_TYPE_CODE);
		_query_searchBySource = conn.prepareStatement(QUERY_SEARCH_BY_SOURCE);
		_query_searchByType = conn.prepareStatement(QUERY_SEARCH_BY_TYPE);
		_query_searchByCode = conn.prepareStatement(QUERY_SEARCH_BY_CODE);
		_query_getAllNotifications = conn
				.prepareStatement(QUERY_LATEST_NOTIFICATIONS);

	}

	/**
	 * Closes the JDBC connection and all it's associated prepared statements.
	 *
	 * @see gov.usgs.util.Configurable
	 */
	public synchronized void shutdown() throws Exception {
		// Close the DML statements
		try {
			_dml_addNotification.close();
		} catch (Exception e) {
		} finally {
			_dml_addNotification = null;
		}
		try {
			_dml_removeNotification.close();
		} catch (Exception e) {
		} finally {
			_dml_removeNotification = null;
		}
		try {
			_dml_createTmpTable.close();
		} catch (Exception e) {
		} finally {
			_dml_createTmpTable = null;
		}
		try {
			_dml_addTmpSource.close();
		} catch (Exception e) {
		} finally {
			_dml_addTmpSource = null;
		}
		try {
			_dml_addTmpType.close();
		} catch (Exception e) {
		} finally {
			_dml_addTmpType = null;
		}
		try {
			_dml_addTmpCode.close();
		} catch (Exception e) {
		} finally {
			_dml_addTmpCode = null;
		}

		// Close the query statements
		try {
			_query_findExpiredNotifications.close();
		} catch (Exception e) {
		} finally {
			_query_findExpiredNotifications = null;
		}
		try {
			_query_findNotificationsById.close();
		} catch (Exception e) {
		} finally {
			_query_findNotificationsById = null;
		}
		try {
			_query_findNotificationsByData.close();
		} catch (Exception e) {
		} finally {
			_query_findNotificationsByData = null;
		}
		try {
			_query_searchBySourceTypeCode.close();
		} catch (Exception e) {
		} finally {
			_query_searchBySourceTypeCode = null;
		}
		try {
			_query_searchBySourceType.close();
		} catch (Exception e) {
		} finally {
			_query_searchBySourceType = null;
		}
		try {
			_query_searchBySourceCode.close();
		} catch (Exception e) {
		} finally {
			_query_searchBySourceCode = null;
		}
		try {
			_query_searchByTypeCode.close();
		} catch (Exception e) {
		} finally {
			_query_searchByTypeCode = null;
		}
		try {
			_query_searchBySource.close();
		} catch (Exception e) {
		} finally {
			_query_searchBySource = null;
		}
		try {
			_query_searchByType.close();
		} catch (Exception e) {
		} finally {
			_query_searchByType = null;
		}
		try {
			_query_searchByCode.close();
		} catch (Exception e) {
		} finally {
			_query_searchByCode = null;
		}
		try {
			_query_getAllNotifications.close();
		} catch (Exception e) {
		} finally {
			_query_getAllNotifications = null;
		}

		// call super shutdown to disconnect
		super.shutdown();
	}

	/**
	 * Add a notification to the index.
	 *
	 * If an identical notification is already in the index, the implementation
	 * may choose whether or not to store the duplicate information.
	 *
	 * @param notification
	 *            the notification to add.
	 * @throws Exception
	 *             if an error occurs while storing the notification.
	 * @see gov.usgs.earthquake.distribution.NotificationIndex
	 */
	public synchronized void addNotification(Notification notification)
			throws Exception {
		// verify connection
		this.verifyConnection();

		// Read the product id from the notification
		ProductId productId = notification.getProductId();

		// Parse the update date from the product id
		java.sql.Date updateDate = new java.sql.Date(productId.getUpdateTime()
				.getTime());

		// Parse the expiration date from the notification
		java.sql.Date expirationDate = new java.sql.Date(notification
				.getExpirationDate().getTime());

		// Read the URL value from the notification
		String trackerUrl = notification.getTrackerURL().toString();

		// Set the values we parsed above
		_dml_addNotification.setString(1, productId.getSource());
		_dml_addNotification.setString(2, productId.getType());
		_dml_addNotification.setString(3, productId.getCode());
		_dml_addNotification.setDate(4, updateDate);
		_dml_addNotification.setDate(5, expirationDate);
		_dml_addNotification.setString(6, trackerUrl);

		// If this is a URL notification, set the product URL value as well
		if (notification instanceof URLNotification) {
			String productUrl = ((URLNotification) notification)
					.getProductURL().toString();
			_dml_addNotification.setString(7, productUrl);
		} else {
			_dml_addNotification.setString(7, "");
		}

		// already verified above
		Connection conn = getConnection();
		try {
			// Begin a transaction
			conn.setAutoCommit(false);
			// Execute the query
			_dml_addNotification.executeUpdate();
			// Commit the changes
			conn.setAutoCommit(true);
		} catch (SQLException sqx) {
			// Undo any changes that may be in an unknown state. Ignore
			// exceptions that occur in this call since we're already throwing
			// an exception
			try {
				conn.rollback();
			} catch (SQLException ex) {
			}

			// Re-throw this exception
			throw sqx;
		} finally {
			conn.setAutoCommit(true);
		}
	}

	/**
	 * Remove a notification from the index.
	 *
	 * All matching notifications should be removed from the index.
	 *
	 * @param notification
	 *            the notification to remove.
	 * @throws Exception
	 *             if an error occurs while removing the notification.
	 * @see gov.usgs.earthquake.distribution.NotificationIndex
	 */
	public synchronized void removeNotification(Notification notification)
			throws Exception {
		// verify connection
		this.verifyConnection();

		// Read the product id from the notification
		ProductId productId = notification.getProductId();
		// Parse the update date from the product id
		java.sql.Date updateDate = new java.sql.Date(productId.getUpdateTime()
				.getTime());
		// Parse the expiration date from the notification
		java.sql.Date expirationDate = new java.sql.Date(notification
				.getExpirationDate().getTime());
		// Read the URL value from the notification
		String trackerUrl = notification.getTrackerURL().toString();

		// Set the values we parsed above
		_dml_removeNotification.setString(1, productId.getSource());
		_dml_removeNotification.setString(2, productId.getType());
		_dml_removeNotification.setString(3, productId.getCode());
		_dml_removeNotification.setDate(4, updateDate);
		_dml_removeNotification.setDate(5, expirationDate);
		_dml_removeNotification.setString(6, trackerUrl);

		// If this is a URL notification, set the product URL value as well
		if (notification instanceof URLNotification) {
			String productUrl = ((URLNotification) notification)
					.getProductURL().toString();
			_dml_removeNotification.setString(7, productUrl);
		} else {
			// _dml_removeNotification.setNull(7, java.sql.Types.VARCHAR);
			_dml_removeNotification.setString(7, "");
		}

		// already verified above
		Connection conn = getConnection();
		try {
			// Begin a transaction
			conn.setAutoCommit(false);
			// Execute the query
			_dml_removeNotification.executeUpdate();
			// Commit the changes
			conn.setAutoCommit(true);
		} catch (SQLException sqx) {
			// Undo any changes that may be in an unknown state. Ignore
			// exceptions that occur in this call since we're already throwing
			// an exception
			try {
				conn.rollback();
			} catch (SQLException ex) {
			}
			// Re-throw this exception
			throw sqx;
		} finally {
			conn.setAutoCommit(true);
		}
	}

	/**
	 * Search the index for notifications matching id.
	 *
	 * If more than one notification matches, all should be returned.
	 *
	 * @param id
	 *            the ProductId to find.
	 * @return a list of matching notifications.
	 * @throws Exception
	 *             if an error occurs while searching the index.
	 * @see gov.usgs.earthquake.distribution.NotificationIndex
	 */
	public synchronized List<Notification> findNotifications(ProductId id)
			throws Exception {
		// verify connection
		this.verifyConnection();

		String source = id.getSource();
		String type = id.getType();
		String code = id.getCode();
		java.sql.Date update = new java.sql.Date(id.getUpdateTime().getTime());

		_query_findNotificationsById.setString(1, source);
		_query_findNotificationsById.setString(2, type);
		_query_findNotificationsById.setString(3, code);
		_query_findNotificationsById.setDate(4, update);

		return getNotifications(_query_findNotificationsById);
	}

	/**
	 * Search the index for notifications matching the sources, types, and
	 * codes.
	 *
	 * Only one notification for each unique ProductId
	 * (source+type+code+updateTime) should be returned. If sources, types,
	 * and/or codes are null, that parameter should be considered a wildcard. If
	 * sources, types, and codes are all null, a notification for each unique
	 * ProductId in the index should be returned.
	 *
	 * @param source
	 *            sources to include, or all if null.
	 * @param type
	 *            types to include, or all if null.
	 * @param code
	 *            codes to include, or all if null.
	 * @return a list of matching notifications.
	 * @throws Exception
	 *             if an error occurs while searching the index.
	 * @see gov.usgs.earthquake.distribution.NotificationIndex
	 */
	public synchronized List<Notification> findNotifications(String source,
			String type, String code) throws Exception {
		// verify connection
		this.verifyConnection();

		source = (source == null) ? "%" : source.toUpperCase();
		type = (type == null) ? "%" : type.toUpperCase();
		code = (code == null) ? "%" : code.toUpperCase();

		_query_findNotificationsByData.setString(1, source);
		_query_findNotificationsByData.setString(2, type);
		_query_findNotificationsByData.setString(3, code);

		return getNotifications(_query_findNotificationsByData);
	}

	/**
	 * Search the index for notifications matching the sources, types, and
	 * codes.
	 *
	 * Only one notification for each unique ProductId
	 * (source+type+code+updateTime) should be returned. If sources, types,
	 * and/or codes are null, that parameter should be considered a wildcard. If
	 * sources, types, and codes are all null, a notification for each unique
	 * ProductId in the index should be returned.
	 *
	 * This implementation require synchronization to prevent SQLExceptions
	 * caused by concurrent access. SQLite locks the database whenever there is
	 * an open ResultSet resource. So even read queries can end up causing SQL
	 * concurrent access problems.
	 *
	 * @param sources
	 *            sources to include, or all if null.
	 * @param types
	 *            types to include, or all if null.
	 * @param codes
	 *            codes to include, or all if null.
	 * @return a list of matching notifications.
	 * @throws Exception
	 *             if an error occurs while searching the index.
	 */
	public synchronized List<Notification> findNotifications(
			List<String> sources, List<String> types, List<String> codes)
			throws Exception {
		// verify connection
		this.verifyConnection();

		List<Notification> n = null;

		Connection conn = getConnection();
		try {
			// begin a transaction
			conn.setAutoCommit(false);

			// Create a temporary lookup table
			_dml_createTmpTable.executeUpdate();

			// Populate the temporary lookup table with our given lists
			if (sources != null) {
				// Not null, insert values
				Iterator<String> iter = sources.iterator();
				while (iter.hasNext()) {
					_dml_addTmpSource.setString(1, iter.next());
					_dml_addTmpSource.addBatch();
				}
				_dml_addTmpSource.executeBatch();
			}

			if (types != null) {
				// Not null, insert values
				Iterator<String> iter = types.iterator();
				while (iter.hasNext()) {
					_dml_addTmpType.setString(1, iter.next());
					_dml_addTmpType.addBatch();
				}
				_dml_addTmpType.executeBatch();
			}

			if (codes != null) {
				// Not null, insert values
				Iterator<String> iter = codes.iterator();
				while (iter.hasNext()) {
					_dml_addTmpCode.setString(1, iter.next());
					_dml_addTmpCode.addBatch();
				}
				_dml_addTmpCode.executeBatch();
			}

			// TODO: is this the problem? reading with uncommitted writes?
			PreparedStatement ps = getCorrectStatement(sources, types, codes);
			n = getNotifications(ps);
		} finally {
			conn.rollback();
			// todo: this looks funky, but it's re-enabling autoCommit, which is
			// needed for selects to not block other transactions
			conn.setAutoCommit(true);
		}

		return n;
	}

	/**
	 * Search the index for expired notifications.
	 *
	 * All expired notifications, even if duplicate, should be returned.
	 *
	 * @return a list of expired notifications.
	 * @throws Exception
	 *             if an error occurs while searching the index.
	 * @see gov.usgs.earthquake.distribution.NotificationIndex
	 */
	public synchronized List<Notification> findExpiredNotifications()
			throws Exception {
		// verify connection
		this.verifyConnection();

		// Create a new calendar object set to current date/time
		java.sql.Date curDate = new java.sql.Date((new Date()).getTime());

		// Bind the expiration date parameter and run the query
		_query_findExpiredNotifications.setDate(1, curDate);

		return getNotifications(_query_findExpiredNotifications);
	}

	/**
	 * Executes a prepared statement and parses the result set into a list of
	 * notifications. The prepared statement can have any set of criteria and
	 * all required parameters should be bound before calling this method. The
	 * result set of the prepared statement must include at least: -
	 * PRODUCT_SOURCE_COLUMN<br>
	 * - PRODUCT_TYPE_COLUMN<br>
	 * - PRODUCT_CODE_COLUMN<br>
	 * - PRODUCT_UPDATE_COLUMN<br>
	 * - EXPIRATION_DATE_COLUMN<br>
	 * - TRACKER_URL_COLUMN<br>
	 * - PRODUCT_URL_COLUMN<br>
	 *
	 * @param ps
	 *            The prepared statement to execute.
	 * @return A list of notifications returned by executing the statement.
	 * @throws Exception
	 *             If a <code>SQLException</code> occurs.
	 */
	protected List<Notification> getNotifications(PreparedStatement ps)
			throws Exception {
		List<Notification> n = new ArrayList<Notification>();
		ResultSet rs = null;

		try {
			rs = ps.executeQuery();
			while (rs.next()) {
				n.add(parseNotification(rs.getString(PRODUCT_SOURCE_COLUMN),
						rs.getString(PRODUCT_TYPE_COLUMN),
						rs.getString(PRODUCT_CODE_COLUMN),
						rs.getDate(PRODUCT_UPDATE_COLUMN),
						rs.getDate(EXPIRATION_DATE_COLUMN),
						rs.getString(TRACKER_URL_COLUMN),
						rs.getString(PRODUCT_URL_COLUMN)));
			}
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
				//ignore
			}
		}

		return n;
	}

	/**
	 * Creates and returns a <code>Notification</code> based on the provided
	 * data. If the <code>download</code> string references a valid URL, then a
	 * <code>URLNotification</code> is created, otherwise a
	 * <code>DefaultNotification</code> is created.
	 *
	 * @param source
	 *            The product source string.
	 * @param type
	 *            The product type string.
	 * @param code
	 *            The product code string.
	 * @param update
	 *            The latest update date/time for the product.
	 * @param expires
	 *            The date/time when this notification expires.
	 * @param tracker
	 *            A reference to a URL where information about this product is
	 *            posted.
	 * @param download
	 *            A reference to a URL where one can download this product, or
	 *            <code>null</code> if this is not a
	 *            <code>URLNotification</code>.
	 *
	 * @return The generated notification, or <code>null</code> if one could not
	 *         be created (but an exception did not occur).
	 *
	 * @throws Exception
	 *             If the <code>tracker</code> string cannot be successfully
	 *             parsed into a valid URL.
	 */
	protected Notification parseNotification(String source, String type,
			String code, java.sql.Date update, java.sql.Date expires,
			String tracker, String download) throws Exception {
		Notification n = null;
		ProductId productId = new ProductId(source, type, code, update);
		try {
			n = new URLNotification(productId, expires, new URL(tracker),
					new URL(download));
		} catch (MalformedURLException mux) {
			n = new DefaultNotification(productId, expires, new URL(tracker));
		}
		return n;
	}

	protected PreparedStatement getCorrectStatement(List<String> sources,
			List<String> types, List<String> codes) throws Exception {
		if (sources != null && types != null && codes != null) {
			return _query_searchBySourceTypeCode;
		} else if (sources != null && types != null && codes == null) {
			return _query_searchBySourceType;
		} else if (sources != null && types == null && codes != null) {
			return _query_searchBySourceCode;
		} else if (sources == null && types != null && codes != null) {
			return _query_searchByTypeCode;
		} else if (sources != null && types == null && codes == null) {
			return _query_searchBySource;
		} else if (sources == null && types != null && codes == null) {
			return _query_searchByType;
		} else if (sources == null && types == null && codes != null) {
			return _query_searchByCode;
		} else if (sources == null && types == null && codes == null) {
			return _query_getAllNotifications;
		}

		return null;
	}

}
