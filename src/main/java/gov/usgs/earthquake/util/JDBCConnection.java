package gov.usgs.earthquake.util;

import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for JDBC Connection.
 *
 * Sub-classes must implement the connect method, and extend startup and
 * shutdown methods. The {@link #verifyConnection()} method tests whether the
 * connection is active, and will shutdown() and startup() to reinitialize if it
 * is not active.
 *
 * @author jmfee
 */
public class JDBCConnection extends DefaultConfigurable implements AutoCloseable {

	private static final Logger LOGGER = Logger.getLogger(JDBCConnection.class
			.getName());

	/** Connection object. */
	private Connection connection;

	/** JDBC driver class. */
	private String driver;

	/** JDBC connect url. */
	private String url;

	/**
	 * Create a new JDBCConnection object.
	 */
	public JDBCConnection() {
		this.connection = null;
	}

	/**
	 * Create a new JDBCConnection object with specific driver and URL
	 * @param driver String of driver
	 * @param url String of URL
	 */
	public JDBCConnection(final String driver, final String url) {
		this.driver = driver;
		this.url = url;
	}

	/**
	 * Implement autocloseable.
	 *
	 * Calls {@link #shutdown()}.
	 *
	 * @throws Exception Exception
	 */
	@Override
	public void close() throws Exception {
		shutdown();
	}

	/**
	 * Implement Configurable
	 * @param config Config to set driver and URL in
	 * @throws Exception Exception
	 */
	@Override
	public void configure(final Config config) throws Exception {
		setDriver(config.getProperty("driver"));
		setUrl(config.getProperty("url"));
	}

	/**
	 * Connect to the database.
	 *
	 * Sub-classes determine how connection is made.
	 *
	 * @return the connection.
	 * @throws Exception
	 *             if unable to connect.
	 */
	protected Connection connect() throws Exception {
		// load driver if needed
		Class.forName(driver);
		final Connection conn = DriverManager.getConnection(url);
		return conn;
	}

	/**
	 * Initialize the database connection.
	 *
	 * Sub-classes should call super.startup(), before preparing any statements.
	 * @throws Exception if error occurs
	 */
	@Override
	public void startup() throws Exception {
		this.connection = connect();
	}

	/**
	 * Shutdown the database connection.
	 *
	 * Sub-classes should close any prepared statements (catching any
	 * exceptions), and then call super.shutdown() to close the database
	 * connection.
	 * @throws Exception if error occurs
	 */
	@Override
	public void shutdown() throws Exception {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (Exception e) {
			// log
			e.printStackTrace();
		} finally {
			connection = null;
		}
	}

	/**
	 * Open a transaction on the database connection
	 * @throws Exception if error occurs
	 */
	public synchronized void beginTransaction() throws Exception {
		Connection conn = this.verifyConnection();
		conn.setAutoCommit(false);
	}

	/**
	 * Finalize the transaction by committing all the changes and closing the
	 * transaction.
	 * @throws Exception if error occurs
	 */
	public synchronized void commitTransaction() throws Exception {
		getConnection().setAutoCommit(true);
	}

	/**
	 * Undo all of the changes made during the current transaction
	 * @throws Exception if error occurs
	 */
	public synchronized void rollbackTransaction() throws Exception {
		getConnection().rollback();
	}

	/**
	 * @return current connection object, or null if not connected.
	 */
	public Connection getConnection() {
		return this.connection;
	}

	/**
	 * Check whether database connection is closed, and reconnect if needed.
	 *
	 * Executes the query "select 1" using the current database connection. If
	 * this doesn't succeed, reinitializes the database connection by calling
	 * shutdown() then startup().
	 *
	 * @return Valid connection object.
	 * @throws Exception
	 *             if unable to (re)connect.
	 */
	public synchronized Connection verifyConnection() throws Exception {
		try {
			// usually throws an exception when connection is closed
			if (connection.isClosed()) {
				shutdown();
			}
		} catch (Exception e) {
			shutdown();
		}

		if (connection == null) {
			// connection is null after shutdown()
			startup();
		}

		// isClosed() doesn't check if we can still communicate with the server.
		// current mysql driver doesn't implement isValid(), so check manually.
		String query_text = "SELECT 1";

		try {
			Statement statement = null;
			ResultSet results = null;
			try {
				statement = connection.createStatement();
				results = statement.executeQuery(query_text);
				while (results.next()) {
					if (results.getInt(1) != 1) {
						throw new Exception("[" + getName()
								+ "] Problem checking database connection");
					}
				}
			} finally {
				// close result and statement no matter what
				try {
					results.close();
				} catch (Exception e2) {
					// ignore
				}
				try {
					statement.close();
				} catch (Exception e2) {
					// ignore
				}
			}
		} catch (Exception e) {
			// The connection was dead, so lets try to restart it
			LOGGER.log(Level.FINE, "[" + getName()
					+ "] Restarting database connection");
			shutdown();
			startup();
		}

		return this.connection;
	}

	/** @return driver */
	public String getDriver() { return this.driver; }
	/** @param driver Driver to set */
	public void setDriver(final String driver) { this.driver = driver; }

	/** @return URL */
	public String getUrl() { return this.url; }
	/** @param url URL to set */
	public void setUrl(final String url) { this.url = url; }

}
