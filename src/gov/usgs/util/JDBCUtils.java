/*
 * JDBCUtils
 *
 * $Id$
 * $HeadURL$
 */
package gov.usgs.util;

import java.lang.reflect.InvocationTargetException;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBC Connection and Statement utility functions.
 * 
 * @author jmfee
 * 
 */
public class JDBCUtils {

	/** Mysql Driver. */
	public static final String MYSQL_DRIVER_CLASSNAME = "com.mysql.jdbc.Driver";

	/** SQLite Driver. */
	public static final String SQLITE_DRIVER_CLASSNAME = "org.sqlite.JDBC";

	/**
	 * Create a new JDBC Connection.
	 * 
	 * @param driver
	 *            driver class name.
	 * @param url
	 *            driver specific url.
	 * @return Connection to database.
	 * @throws ClassNotFoundException
	 *             if driver class is not found.
	 * @throws IllegalAccessException
	 *             if driver empty constructor is not public.
	 * @throws InstantiationException
	 *             if an exception occurs while instantiating driver.
	 * @throws SQLException
	 *             if an error occurs while making connection.
	 */
	public static Connection getConnection(final String driver, final String url)
			throws ClassNotFoundException, IllegalAccessException,
			InstantiationException, InvocationTargetException,
			NoSuchMethodException, SQLException {
		// create driver class, which registers with DriverManager
		Class.forName(driver).getConstructor().newInstance();

		// request connection from DriverManager
		return DriverManager.getConnection(url);
	}

	/**
	 * Set a JDBC prepared statement parameter.
	 * 
	 * Either calls statement.setNull if object is null, or sets the appropriate
	 * type based on the object. If the object is not null, type is ignored.
	 * 
	 * @param statement
	 *            statement with parameters to set.
	 * @param index
	 *            index of parameter being set.
	 * @param object
	 *            value of parameter being set.
	 * @param type
	 *            java.sql.Types constant for column type.
	 * @throws SQLException
	 */
	public static void setParameter(final PreparedStatement statement,
			final int index, final Object object, final int type)
			throws SQLException {

		if (object == null) {
			statement.setNull(index, type);
		} else if (object instanceof Boolean) {
			statement.setBoolean(index, (Boolean) object);
		} else if (object instanceof Byte) {
			statement.setByte(index, (Byte) object);
		} else if (object instanceof Character) {
			statement.setString(index, ((Character) object).toString());
		} else if (object instanceof Double) {
			statement.setDouble(index, (Double) object);
		} else if (object instanceof Float) {
			statement.setFloat(index, (Float) object);
		} else if (object instanceof Integer) {
			statement.setInt(index, (Integer) object);
		} else if (object instanceof Long) {
			statement.setLong(index, (Long) object);
		} else if (object instanceof Short) {
			statement.setShort(index, (Short) object);
		} else if (object instanceof String) {
			statement.setString(index, (String) object);
		} else {
			statement.setObject(index, object, type);
			System.err.printf(
					"Unsupported object type (%s): index=%d, value=%s\n",
					object.getClass().getName(), index, object.toString());
		}
	}

	/**
	 * Get a mysql connection from a URL.
	 * 
	 * Calls getConnection(MYSQL_DRIVER_CLASSNAME, url).
	 * 
	 * @param url
	 *            a Mysql URL.
	 * @return a Connection to a Mysql database.
	 */
	public static Connection getMysqlConnection(final String url)
			throws SQLException, ClassNotFoundException,
			IllegalAccessException, InstantiationException,
			InvocationTargetException, NoSuchMethodException {
		return getConnection(MYSQL_DRIVER_CLASSNAME, url);
	}

	/**
	 * Get a sqlite connection from a file.
	 * 
	 * Builds a sqlite file url and calls getSqliteConnection(url).
	 * 
	 * @param file
	 *            sqlite database file.
	 * @return connection to sqlite database file.
	 */
	public static Connection getSqliteConnection(final File file)
			throws SQLException, ClassNotFoundException,
			IllegalAccessException, InstantiationException,
			InvocationTargetException, NoSuchMethodException {
		String sqliteFileURL = "jdbc:sqlite:" + file.getAbsolutePath();
		return getSqliteConnection(sqliteFileURL);
	}

	/**
	 * Get a sqlite connection from a URL.
	 * 
	 * Calls getConnection(SQLITE_DRIVER_CLASSNAME, url).
	 * 
	 * @param url
	 *            sqlite database URL.
	 * @return a Connection to a sqlite database.
	 */
	public static Connection getSqliteConnection(final String url)
			throws SQLException, ClassNotFoundException,
			IllegalAccessException, InstantiationException,
			InvocationTargetException, NoSuchMethodException {
		return getConnection(SQLITE_DRIVER_CLASSNAME, url);
	}

}
