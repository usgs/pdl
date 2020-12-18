package gov.usgs.earthquake.util;

import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
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

	static {
		// set default database connect/login timeout in seconds
		DriverManager.setLoginTimeout(10);
	}

	private static final Logger LOGGER = Logger.getLogger(JDBCConnection.class
			.getName());

	/** shared executor for network timeouts */
	private static final Executor TIMEOUT_EXECUTOR = Executors.newCachedThreadPool();

	/** Connection object. */
	private Connection connection;

	/** JDBC driver class. */
	private String driver;

	/** JDBC network timeout. */
	private int networkTimeout = 30000;

	/** JDBC connect url. */
	private String url;

	/** Lock prevents statements from mixing during transaction; avoids "synchronized". */
  private final ReentrantLock transactionLock = new ReentrantLock();

	/**
	 * Create a new JDBCConnection object.
	 */
	public JDBCConnection() {
		this.connection = null;
	}

	public JDBCConnection(final String driver, final String url) {
		this.driver = driver;
		this.url = url;
	}

	/**
	 * Implement autocloseable.
	 *
	 * Calls {@link #shutdown()}.
	 *
	 * @throws Exception
	 */
	@Override
	public void close() throws Exception {
		shutdown();
	}

	/**
	 * Implement Configurable
	 */
  @Override
  public void configure(final Config config) throws Exception {
    setDriver(config.getProperty("driver"));

		String timeout = config.getProperty("networkTimeout");
		if (timeout != null) {
			setNetworkTimeout(Integer.parseInt(timeout));
		}

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
		if (networkTimeout > 0) {
			conn.setNetworkTimeout(TIMEOUT_EXECUTOR, networkTimeout);
		}
		return conn;
  }

	/**
	 * Initialize the database connection.
	 *
	 * Sub-classes should call super.startup(), before preparing any statements.
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
	 */
	@Override
	public void shutdown() throws Exception {
		transactionLock.lock();
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (Exception e) {
			// log
			e.printStackTrace();
		} finally {
			connection = null;
			transactionLock.unlock();
		}
	}

	/**
	 * Open a transaction on the database connection
	 *
	 * A lock is used to ensure no other transactions can begin until either
	 * {@link #commitTransaction()} or {@link #rollbackTransaction()} are called.
	 */
	public void beginTransaction() throws Exception {
		// enter transaction, but allow thread to be interrupted
		transactionLock.lockInterruptibly();
		try {
			Connection conn = this.verifyConnection();
			conn.setAutoCommit(false);
		} catch (Exception e) {
			// if exception occurs, unlock and throw
			transactionLock.unlock();
			throw e;
		}
		// otherwise, remain locked until commit/rollback are called
	}

	/**
	 * Finalize the transaction by committing all the changes and closing the
	 * transaction.
	 */
	public void commitTransaction() throws Exception {
		try {
			getConnection().setAutoCommit(true);
		} finally {
			// if an exception occurred, somethings weird but still unlock
			transactionLock.unlock();
		}
	}

	/**
	 * Undo all of the changes made during the current transaction
	 */
	public void rollbackTransaction() throws Exception {
		try {
			getConnection().rollback();
		} finally {
			// if an exception occurred, somethings weird but still unlock
			transactionLock.unlock();
		}
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
	 * NOTE: this method modifies the stored connection, and should not be called
	 * by multiple threads.
	 *
	 * This is normally called by {@link #beginTransaction()} which uses a lock.
	 *
	 * @return Valid connection object.
	 * @throws Exception
	 *             if unable to (re)connect.
	 */
	public Connection verifyConnection() throws Exception {
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

	public String getDriver() { return this.driver; }
  public void setDriver(final String driver) { this.driver = driver; }

	public int getNetworkTimeout() { return this.networkTimeout; }
	/** NOTE: this does not affect existing connections. */
	public void setNetworkTimeout(final int timeout) { this.networkTimeout = timeout; }

	public String getUrl() { return this.url; }
  public void setUrl(final String url) { this.url = url; }

}
