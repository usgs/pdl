package gov.usgs.earthquake.util;

import gov.usgs.util.DefaultConfigurable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
public abstract class JDBCConnection extends DefaultConfigurable {

	private static final Logger LOGGER = Logger.getLogger(JDBCConnection.class
			.getName());

	/** Connection object. */
	private Connection connection;

	/** Lock prevents statements from mixing during transaction; avoids "synchronized". */
  private final ReentrantLock transactionLock = new ReentrantLock();

	/**
	 * Create a new JDBCConnection object.
	 */
	public JDBCConnection() {
		this.connection = null;
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
	protected abstract Connection connect() throws Exception;

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

}
