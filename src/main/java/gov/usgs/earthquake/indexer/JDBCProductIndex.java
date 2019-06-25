/*
 * JDBCProductIndex
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.InvalidProductException;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.util.JDBCConnection;
import gov.usgs.util.Config;
import gov.usgs.util.JDBCUtils;
import gov.usgs.util.StreamUtils;

import java.io.File;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC Implementation of {@link ProductIndex}.
 */
public class JDBCProductIndex extends JDBCConnection implements ProductIndex {

	/** Logging Utility **/
	private static final Logger LOGGER = Logger.getLogger(Indexer.class
			.getName());

	/** _____ First, set up some constants _____ */

	/**
	 * Default index file. Copied into file system as JDBC_DEFAULT_FILE if
	 * doesn't already exist.
	 */
	private static final String JDBC_DEFAULT_INDEX = "etc/schema/productIndex.db";

	private static final String JDBC_DEFAULT_DRIVER = JDBCUtils.SQLITE_DRIVER_CLASSNAME;

	/**
	 * Default index file. Created by copying JDBC_DEFAULT_INDEX out of Jar if
	 * doesn't already exist in file system
	 */
	public static final String JDBC_DEFAULT_FILE = "productIndex.db";

	/**
	 * Constant used to specify what the driver property should be called in the
	 * config file
	 */
	private static final String JDBC_DRIVER_PROPERTY = "driver";

	/**
	 * Constant used to specify the url property should be called in the config
	 * file.
	 */
	private static final String JDBC_URL_PROPERTY = "url";

	/**
	 * Constant used to specify what the index file property should be called in
	 * to config file
	 */
	private static final String JDBC_FILE_PROPERTY = "indexfile";

	/** Prefix for connecting to a sqlite database */
	private static final String JDBC_CONNECTION_PREFIX = "jdbc:sqlite:";

	/** Variables to store the event and product column names */
	private static final String EVENT_TABLE = "event";
	private static final String EVENT_TABLE_ALIAS = "e";
	private static final String EVENT_INDEX_ID = "id";
	private static final String EVENT_CREATED = "created";
	private static final String EVENT_UPDATED = "updated";
	private static final String EVENT_SOURCE = "source";
	private static final String EVENT_SOURCE_CODE = "sourceCode";
	private static final String EVENT_TIME = "eventTime";
	private static final String EVENT_LATITUDE = "latitude";
	private static final String EVENT_LONGITUDE = "longitude";
	private static final String EVENT_DEPTH = "depth";
	private static final String EVENT_MAGNITUDE = "magnitude";
	private static final String EVENT_STATUS = "status";

	private static final String EVENT_STATUS_UPDATE = "UPDATE";
	private static final String EVENT_STATUS_DELETE = "DELETE";

	private static final String SUMMARY_TABLE = "productSummary";
	private static final String SUMMARY_TABLE_ALIAS = "p";
	private static final String SUMMARY_CREATED = "created";
	public static final String SUMMARY_PRODUCT_INDEX_ID = "id";
	private static final String SUMMARY_PRODUCT_ID = "productId";
	private static final String SUMMARY_EVENT_ID = "eventId";
	private static final String SUMMARY_TYPE = "type";
	private static final String SUMMARY_SOURCE = "source";
	private static final String SUMMARY_CODE = "code";
	private static final String SUMMARY_UPDATE_TIME = "updateTime";
	private static final String SUMMARY_EVENT_SOURCE = "eventSource";
	private static final String SUMMARY_EVENT_SOURCE_CODE = "eventSourceCode";
	private static final String SUMMARY_EVENT_TIME = "eventTime";
	private static final String SUMMARY_EVENT_LATITUDE = "eventLatitude";
	private static final String SUMMARY_EVENT_LONGITUDE = "eventLongitude";
	private static final String SUMMARY_EVENT_DEPTH = "eventDepth";
	private static final String SUMMARY_EVENT_MAGNITUDE = "eventMagnitude";
	private static final String SUMMARY_VERSION = "version";
	private static final String SUMMARY_STATUS = "status";
	private static final String SUMMARY_TRACKER_URL = "trackerURL";
	private static final String SUMMARY_PREFERRED = "preferred";
	private static final String SUMMARY_PROPERTY_TABLE = "productSummaryProperty";
	private static final String SUMMARY_PROPERTY_ID = "productSummaryIndexId";
	private static final String SUMMARY_PROPERTY_NAME = "name";
	private static final String SUMMARY_PROPERTY_VALUE = "value";
	private static final String SUMMARY_LINK_TABLE = "productSummaryLink";
	private static final String SUMMARY_LINK_ID = "productSummaryIndexId";
	private static final String SUMMARY_LINK_RELATION = "relation";
	private static final String SUMMARY_LINK_URL = "url";

	/** Query used to insert events */
	private static final String INSERT_EVENT_QUERY = String.format(
			"INSERT INTO %s (%s) VALUES (?)", EVENT_TABLE, EVENT_CREATED);

	/** Query used to update preferred event properties. */
	private static final String UPDATE_EVENT_QUERY = String
			.format("UPDATE %s SET %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=? WHERE %s=?",
					EVENT_TABLE, EVENT_UPDATED, EVENT_SOURCE,
					EVENT_SOURCE_CODE, EVENT_TIME, EVENT_LATITUDE,
					EVENT_LONGITUDE, EVENT_DEPTH, EVENT_MAGNITUDE,
					EVENT_STATUS, EVENT_INDEX_ID);

	/** Query used to update preferred event properties. */
	private static final String UPDATE_DELETED_EVENT_QUERY = String.format(
			"UPDATE %s SET %s=? WHERE %s=?", EVENT_TABLE, EVENT_STATUS,
			EVENT_INDEX_ID);

	/** Query used to insert product summaries */
	private static final String INSERT_SUMMARY_QUERY = String
			.format("INSERT INTO %s ( %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s ) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )",
					SUMMARY_TABLE, SUMMARY_CREATED, SUMMARY_PRODUCT_ID,
					SUMMARY_TYPE, SUMMARY_SOURCE, SUMMARY_CODE,
					SUMMARY_UPDATE_TIME, SUMMARY_EVENT_SOURCE,
					SUMMARY_EVENT_SOURCE_CODE, SUMMARY_EVENT_TIME,
					SUMMARY_EVENT_LATITUDE, SUMMARY_EVENT_LONGITUDE,
					SUMMARY_EVENT_DEPTH, SUMMARY_EVENT_MAGNITUDE,
					SUMMARY_VERSION, SUMMARY_STATUS, SUMMARY_TRACKER_URL,
					SUMMARY_PREFERRED);

	/** Query used to store the property */
	private static final String ADD_PROPERTY_QUERY = String.format(
			"INSERT INTO %s ( %s, %s, %s ) " + "VALUES (?, ?, ?)",
			SUMMARY_PROPERTY_TABLE, SUMMARY_PROPERTY_ID, SUMMARY_PROPERTY_NAME,
			SUMMARY_PROPERTY_VALUE);

	/** Query used to store the link */
	private static final String ADD_LINK_QUERY = String.format(
			"INSERT INTO %s ( %s, %s, %s ) " + "VALUES (?, ?, ?)",
			SUMMARY_LINK_TABLE, SUMMARY_LINK_ID, SUMMARY_LINK_RELATION,
			SUMMARY_LINK_URL);

	/** Query used to store the relation between products and events */
	private static final String ADD_ASSOCIATION_QUERY = String.format(
			"UPDATE %s SET %s=? WHERE %s=? AND %s=? AND %s=?", SUMMARY_TABLE,
			SUMMARY_EVENT_ID, SUMMARY_SOURCE, SUMMARY_TYPE, SUMMARY_CODE);

	/** Query to delete events */
	private static final String DELETE_EVENT_QUERY = String.format(
			"DELETE FROM %s WHERE id=?", EVENT_TABLE);

	/** Query to delete products */
	private static final String DELETE_SUMMARY_QUERY = String.format(
			"DELETE FROM %s WHERE id=?", SUMMARY_TABLE);

	/** Query to delete properties */
	private static final String DELETE_PROPERTIES_QUERY = String.format(
			"DELETE FROM %s WHERE %s=?", SUMMARY_PROPERTY_TABLE,
			SUMMARY_PROPERTY_ID);

	/** Query to delete links */
	private static final String DELETE_LINKS_QUERY = String.format(
			"DELETE FROM %s WHERE %s=?", SUMMARY_LINK_TABLE, SUMMARY_LINK_ID);

	/** Query to remove the association between a product and an event */
	private static final String REMOVE_ASSOCIATION_QUERY = String.format(
			"UPDATE %s SET %s=? WHERE %s=? AND %s=? AND %s=?", SUMMARY_TABLE,
			SUMMARY_EVENT_ID, SUMMARY_SOURCE, SUMMARY_TYPE, SUMMARY_CODE);

	/** Query to get a summary using its id */
	private static final String GET_SUMMARY_BY_PRODUCT_INDEX_ID = String
			.format("SELECT %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s FROM %s WHERE %s = ?",
					SUMMARY_PRODUCT_ID, SUMMARY_TYPE, SUMMARY_SOURCE,
					SUMMARY_CODE, SUMMARY_UPDATE_TIME, SUMMARY_EVENT_SOURCE,
					SUMMARY_EVENT_SOURCE_CODE, SUMMARY_EVENT_TIME,
					SUMMARY_EVENT_LATITUDE, SUMMARY_EVENT_LONGITUDE,
					SUMMARY_EVENT_DEPTH, SUMMARY_EVENT_MAGNITUDE,
					SUMMARY_VERSION, SUMMARY_STATUS, SUMMARY_TRACKER_URL,
					SUMMARY_PREFERRED, SUMMARY_TABLE, SUMMARY_PRODUCT_INDEX_ID);

	/** Query to get product ids that share an event id */
	private static final String GET_SUMMARIES_BY_EVENT_INDEX_ID = String
			.format("SELECT %s FROM %s WHERE %s = ?", SUMMARY_PRODUCT_INDEX_ID,
					SUMMARY_TABLE, SUMMARY_EVENT_ID);

	/** Query to get all the links for a product */
	private static final String GET_LINKS_BY_PRODUCT_INDEX_ID = String.format(
			"SELECT %s, %s FROM %s WHERE %s = ?", SUMMARY_LINK_RELATION,
			SUMMARY_LINK_URL, SUMMARY_LINK_TABLE, SUMMARY_LINK_ID);

	/** Query to get all the properties for a product */
	private static final String GET_PROPS_BY_PRODUCT_INDEX_ID = String
			.format("SELECT %s, %s FROM %s WHERE %s = ?",
					SUMMARY_PROPERTY_NAME, SUMMARY_PROPERTY_VALUE,
					SUMMARY_PROPERTY_TABLE, SUMMARY_PROPERTY_ID);

	/** Create some prepared statements */
	private PreparedStatement insertEvent;
	private PreparedStatement updateEvent;
	private PreparedStatement updateDeletedEvent;
	private PreparedStatement insertSummary;
	private PreparedStatement insertProperty;
	private PreparedStatement insertLink;
	private PreparedStatement addAssociation;
	private PreparedStatement deleteEvent;
	private PreparedStatement deleteSummary;
	private PreparedStatement deleteProperties;
	private PreparedStatement deleteLinks;
	private PreparedStatement removeAssociation;
	private PreparedStatement getSummary;
	private PreparedStatement getSummaries;
	private PreparedStatement getProductLinks;
	private PreparedStatement getProductProperties;

	private String driver;
	private String url;
	private String index_file;

	/**
	 * Constructor. Sets index_file to the default value JDBC_DEFAULT_FILE
	 *
	 * @throws Exception
	 */
	public JDBCProductIndex() throws Exception {
		// Default index file, so calling configure() isn't required
		index_file = JDBC_DEFAULT_FILE;
		driver = JDBC_DEFAULT_DRIVER;
	}

	public JDBCProductIndex(final String sqliteFileName) throws Exception {
		index_file = sqliteFileName;
		driver = JDBC_DEFAULT_DRIVER;
	}

	// ____________________________________
	// Public Methods
	// ____________________________________

	/**
	 * Grab values from the Config object and put them into private variables.
	 *
	 * @param config
	 *            Configuration for the product index
	 */
	@Override
	public void configure(Config config) throws Exception {

		driver = config.getProperty(JDBC_DRIVER_PROPERTY);
		index_file = config.getProperty(JDBC_FILE_PROPERTY);
		url = config.getProperty(JDBC_URL_PROPERTY);

		if (driver == null || "".equals(driver)) {
			driver = JDBC_DEFAULT_DRIVER;
		}

		if (index_file == null || "".equals(index_file)) {
			index_file = JDBC_DEFAULT_FILE;
		}
	}

	/**
	 * Connect to the database and set up some prepared statements
	 */
	@Override
	public synchronized void startup() throws Exception {
		// initialize connection
		super.startup();
		Connection connection = getConnection();

		// Prepare statements for interacting with the database
		try {
			insertEvent = connection.prepareStatement(INSERT_EVENT_QUERY,
					new String[] { EVENT_INDEX_ID });
		} catch (SQLException e) {
			// sqlite doesn't support RETURN_GENERATED_KEYS, but appears to
			// return generated keys anyways
			insertEvent = connection.prepareStatement(INSERT_EVENT_QUERY);
		}
		updateEvent = connection.prepareStatement(UPDATE_EVENT_QUERY);
		updateDeletedEvent = connection
				.prepareStatement(UPDATE_DELETED_EVENT_QUERY);
		try {
			insertSummary = connection.prepareStatement(INSERT_SUMMARY_QUERY,
					new String[] { SUMMARY_PRODUCT_INDEX_ID });
		} catch (SQLException e) {
			// sqlite doesn't support RETURN_GENERATED_KEYS, but appears to
			// return generated keys anyways
			insertSummary = connection.prepareStatement(INSERT_SUMMARY_QUERY);
		}
		insertProperty = connection.prepareStatement(ADD_PROPERTY_QUERY);
		insertLink = connection.prepareStatement(ADD_LINK_QUERY);
		deleteEvent = connection.prepareStatement(DELETE_EVENT_QUERY);
		deleteSummary = connection.prepareStatement(DELETE_SUMMARY_QUERY);
		deleteProperties = connection.prepareStatement(DELETE_PROPERTIES_QUERY);
		deleteLinks = connection.prepareStatement(DELETE_LINKS_QUERY);
		removeAssociation = connection
				.prepareStatement(REMOVE_ASSOCIATION_QUERY);
		addAssociation = connection.prepareStatement(ADD_ASSOCIATION_QUERY);
		getSummary = connection
				.prepareStatement(GET_SUMMARY_BY_PRODUCT_INDEX_ID);
		getSummaries = connection
				.prepareStatement(GET_SUMMARIES_BY_EVENT_INDEX_ID);
		getProductLinks = connection
				.prepareStatement(GET_LINKS_BY_PRODUCT_INDEX_ID);
		getProductProperties = connection
				.prepareStatement(GET_PROPS_BY_PRODUCT_INDEX_ID);
	}

	/**
	 * Return a connection to the database.
	 *
	 * @return Connection object
	 * @throws Exception
	 */
	@Override
	public Connection connect() throws Exception {
		// If they are using the sqlite driver, we need to try to create the
		// file
		if (driver.equals(JDBCUtils.SQLITE_DRIVER_CLASSNAME)) {
			// Make sure file exists or copy it out of the JAR
			File indexFile = new File(index_file);
			if (!indexFile.exists()) {
				// extract schema from jar
				URL schemaURL = JDBCProductIndex.class.getClassLoader()
						.getResource(JDBC_DEFAULT_INDEX);
				if (schemaURL != null) {
					StreamUtils.transferStream(schemaURL, indexFile);
				} else {
					// Failed. Probably because we're not in a Jar file
					File defaultIndex = new File(JDBC_DEFAULT_INDEX);
					StreamUtils.transferStream(defaultIndex, indexFile);
				}
			}

			// Build the JDBC url
			url = JDBC_CONNECTION_PREFIX + index_file;
			driver = JDBCUtils.SQLITE_DRIVER_CLASSNAME;
		}

		return JDBCUtils.getConnection(driver, url);
	}

	/**
	 * Close the database connection and each of the prepared statements. Before
	 * closing each resource, this method checks if it is already closed.
	 */
	@Override
	public synchronized void shutdown() throws Exception {
		// Close each of the prepared statements, then close the connection.
		// Make sure exceptions don't prevent closing of any statements.

		if (insertEvent != null) {
			try {
				insertEvent.close();
			} catch (Exception e) {
				// ignore
			}
			insertEvent = null;
		}

		if (updateEvent != null) {
			try {
				updateEvent.close();
			} catch (Exception e) {
				// ignore
			}
			updateEvent = null;
		}

		if (updateDeletedEvent != null) {
			try {
				updateDeletedEvent.close();
			} catch (Exception e) {
				// ignore
			}
			updateDeletedEvent = null;
		}

		if (insertSummary != null) {
			try {
				insertSummary.close();
			} catch (Exception e) {
				// ignore
			}
			insertSummary = null;
		}

		if (insertProperty != null) {
			try {
				insertProperty.close();
			} catch (Exception e) {
				// ignore
			}
			insertProperty = null;
		}

		if (insertLink != null) {
			try {
				insertLink.close();
			} catch (Exception e) {
				// ignore
			}
			insertLink = null;
		}

		if (deleteEvent != null) {
			try {
				deleteEvent.close();
			} catch (Exception e) {
				// ignore
			}
			deleteEvent = null;
		}

		if (deleteSummary != null) {
			try {
				deleteSummary.close();
			} catch (Exception e) {
				// ignore
			}
			deleteSummary = null;
		}

		if (deleteProperties != null) {
			try {
				deleteProperties.close();
			} catch (Exception e) {
				// ignore
			}
			deleteProperties = null;
		}

		if (deleteLinks != null) {
			try {
				deleteLinks.close();
			} catch (Exception e) {
				// ignore
			}
			deleteLinks = null;
		}

		if (removeAssociation != null) {
			try {
				removeAssociation.close();
			} catch (Exception e) {
				// ignore
			}
			removeAssociation = null;
		}

		if (addAssociation != null) {
			try {
				addAssociation.close();
			} catch (Exception e) {
				// ignore
			}
			addAssociation = null;
		}

		if (getSummary != null) {
			try {
				getSummary.close();
			} catch (Exception e) {
				// ignore
			}
			getSummary = null;
		}

		if (getSummaries != null) {
			try {
				getSummaries.close();
			} catch (Exception e) {
				// ignore
			}
			getSummaries = null;
		}

		if (getProductLinks != null) {
			try {
				getProductLinks.close();
			} catch (Exception e) {
				// ignore
			}
			getProductLinks = null;
		}

		if (getProductProperties != null) {
			try {
				getProductProperties.close();
			} catch (Exception e) {
				// ignore
			}
			getProductProperties = null;
		}

		// disconnect
		super.shutdown();
	}

	/**
	 * Open a transaction on the database connection
	 */
	@Override
	public synchronized void beginTransaction() throws Exception {
		Connection conn = this.verifyConnection();
		conn.setAutoCommit(false);
	}

	/**
	 * Finalize the transaction by committing all the changes and closing the
	 * transaction.
	 */
	@Override
	public synchronized void commitTransaction() throws Exception {
		getConnection().setAutoCommit(true);
	}

	/**
	 * Undo all of the changes made during the current transaction
	 */
	@Override
	public synchronized void rollbackTransaction() throws Exception {
		getConnection().rollback();
	}

	/**
	 * Return all events from the database that meet the parameters specified in
	 * the ProductIndexQuery object.
	 *
	 * @param query
	 *            A description of which events to retrieve.
	 * @return List of Event objects
	 */
	@Override
	public synchronized List<Event> getEvents(ProductIndexQuery query)
			throws Exception {
		List<Event> events = new LinkedList<Event>();

		// Get a list of event indexIds from the database that match this query
		List<Long> eventIndexIds = getEventIndexIds(query);

		Iterator<Long> iter = eventIndexIds.iterator();
		while (iter.hasNext()) {
			Long eventIndexId = iter.next();
			events.add(getEvent(eventIndexId));
		}

		return events;
	}

	/**
	 * Add an event to the database
	 *
	 * @param event
	 *            Event to store
	 * @return Event object with eventId set to the database id
	 */
	@Override
	public synchronized Event addEvent(Event event) throws Exception {
		Event e = null;
		ResultSet keys = null;

		try {
			// Add the values to the prepared statement
			JDBCUtils.setParameter(insertEvent, 1, new Date().getTime(),
					Types.BIGINT);

			// Execute the prepared statement
			int rows = insertEvent.executeUpdate();

			if (rows == 1) {
				keys = insertEvent.getGeneratedKeys();
				long id = 0;
				while (keys.next()) {
					id = keys.getLong(1);
				}
				e = new Event(event);
				e.setIndexId(id);

				LOGGER.finest("Added event id=" + id);
			} else {
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] Exception when adding new event to database");
				throw new Exception();

			}
		} finally {
			try {
				keys.close();
			} catch (Exception e2) {
			}
		}
		LOGGER.log(Level.FINEST, "[" + getName()
				+ "] Added event to Product Index");
		return e;
	}

	/**
	 * Delete an event from the database.
	 *
	 * @param event
	 *            Event to remove
	 * @return List containing all the ProductIds that were deleted by the
	 *         method call
	 */
	@Override
	public synchronized List<ProductId> removeEvent(Event event)
			throws Exception {

		Long id = event.getIndexId();

		// If there is no index id on the event, we can assume its
		// not in the database
		if (id == null) {
			return null;
		}

		// A list of all the productIds that got deleted
		ArrayList<ProductId> productIds = new ArrayList<ProductId>();

		// We need to remove all the products associated with this event
		List<ProductSummary> summaries = event.getProductList();
		Iterator<ProductSummary> summaryIter = summaries.iterator();
		while (summaryIter.hasNext()) {
			ProductId productId = removeProductSummary(summaryIter.next());
			productIds.add(productId);
		}

		JDBCUtils.setParameter(deleteEvent, 1, id, Types.BIGINT);
		int rows = deleteEvent.executeUpdate();
		// If we didn't delete a row, or we deleted more than 1 row, throw an
		// exception
		if (rows != 1) {
			LOGGER.log(Level.WARNING, "[" + getName()
					+ "] Exception when deleting an event from the database");
			throw new Exception();
		}

		LOGGER.finest("[" + getName() + "] Removed event id=" + id);

		return productIds;
	}

	/**
	 * Return all products that aren't associated with an event.
	 *
	 * @param query
	 *            ProductIndexQuery used to further limit the results
	 * @return List of unassociated Products
	 * @throws IllegalArgumentException
	 *             when query event search type is SEARCH_EVENT_PREFERRED.
	 */
	@Override
	public synchronized List<ProductSummary> getUnassociatedProducts(
			ProductIndexQuery query) throws Exception {
		if (query.getEventSearchType() == ProductIndexQuery.SEARCH_EVENT_PREFERRED) {
			throw new IllegalArgumentException(
					"getUnassociatedProducts does not support SEARCH_EVENT_PREFERRED");
		}

		ArrayList<ProductSummary> products = new ArrayList<ProductSummary>();

		List<String> clauseList = buildProductClauses(query);
		// Add the unassociated quantifier to the clause list
		clauseList.add("eventId IS NULL");
		String query_text = buildProductQuery(clauseList);

		Statement statement = null;
		ResultSet results = null;
		try {
			// Great. We have the query built up, so lets run it
			statement = verifyConnection().createStatement();
			results = statement.executeQuery(query_text);

			// Now lets build an Event object from each row in the result set
			while (results.next()) {
				ProductSummary p = parseSummaryResult(results);
				products.add(p);
			}
		} finally {
			try {
				results.close();
			} catch (Exception e) {
			}
			try {
				statement.close();
			} catch (Exception e) {
			}
		}

		return products;
	}

	/**
	 * Return all products that meet the parameters specified in the
	 * ProductIndexQuery object.
	 *
	 * @param query
	 *            A description of which products to retrieve.
	 * @return List of ProductSummary objects
	 * @throws IllegalArgumentException
	 *             when query event search type is SEARCH_EVENT_PREFERRED.
	 */
	@Override
	public synchronized List<ProductSummary> getProducts(ProductIndexQuery query)
			throws Exception {
		if (query.getEventSearchType() == ProductIndexQuery.SEARCH_EVENT_PREFERRED) {
			throw new IllegalArgumentException(
					"getUnassociatedProducts does not support SEARCH_EVENT_PREFERRED");
		}

		List<ProductSummary> summaries = new LinkedList<ProductSummary>();

		Iterator<Long> summaryIndexIds = getSummaryIndexIds(query).iterator();
		while (summaryIndexIds.hasNext()) {
			summaries.add(getSummary(summaryIndexIds.next()));
		}

		return summaries;
	}

	/**
	 * Add a product summary to the database
	 *
	 * @param summary
	 *            ProductSummary object to store. Must not be null.
	 * @return Copy of the product summary object with the indexId set to the
	 *         newly inserted id.
	 * @throws Exception
	 */
	@Override
	public synchronized ProductSummary addProductSummary(ProductSummary summary)
			throws Exception {
		// Add values to the prepared statement

		// Set the created timestamp
		JDBCUtils.setParameter(insertSummary, 1, new Date().getTime(),
				Types.BIGINT);

		ProductId sid = summary.getId();
		if (sid != null) {
			JDBCUtils.setParameter(insertSummary, 2, sid.toString(),
					Types.VARCHAR);
			JDBCUtils.setParameter(insertSummary, 3, sid.getType(),
					Types.VARCHAR);
			JDBCUtils.setParameter(insertSummary, 4, sid.getSource(),
					Types.VARCHAR);
			JDBCUtils.setParameter(insertSummary, 5, sid.getCode(),
					Types.VARCHAR);
			JDBCUtils.setParameter(insertSummary, 6,
					(sid.getUpdateTime() != null) ? sid.getUpdateTime()
							.getTime() : null, Types.BIGINT);
		} else {
			// Summary product id is null. Set all these parameter to null
			JDBCUtils.setParameter(insertSummary, 2, null, Types.VARCHAR);
			JDBCUtils.setParameter(insertSummary, 3, null, Types.VARCHAR);
			JDBCUtils.setParameter(insertSummary, 4, null, Types.VARCHAR);
			JDBCUtils.setParameter(insertSummary, 5, null, Types.VARCHAR);
			JDBCUtils.setParameter(insertSummary, 6, null, Types.BIGINT);
		}

		JDBCUtils.setParameter(insertSummary, 7, summary.getEventSource(),
				Types.VARCHAR);
		JDBCUtils.setParameter(insertSummary, 8, summary.getEventSourceCode(),
				Types.VARCHAR);

		Date eventTime = summary.getEventTime();
		JDBCUtils.setParameter(insertSummary, 9,
				(eventTime != null) ? eventTime.getTime() : null, Types.BIGINT);

		JDBCUtils
				.setParameter(insertSummary, 10,
						(summary.getEventLatitude() != null) ? summary
								.getEventLatitude().doubleValue() : null,
						Types.DECIMAL);
		JDBCUtils
				.setParameter(
						insertSummary,
						11,
						(summary.getEventLongitude() != null) ? normalizeLongitude(summary
								.getEventLongitude().doubleValue()) : null,
						Types.DECIMAL);
		JDBCUtils.setParameter(insertSummary, 12,
				(summary.getEventDepth() != null) ? summary.getEventDepth()
						.doubleValue() : null, Types.DECIMAL);
		JDBCUtils.setParameter(insertSummary, 13,
				(summary.getEventMagnitude() != null) ? summary
						.getEventMagnitude().doubleValue() : null,
				Types.DECIMAL);
		JDBCUtils.setParameter(insertSummary, 14, summary.getVersion(),
				Types.VARCHAR);
		JDBCUtils.setParameter(insertSummary, 15, summary.getStatus(),
				Types.VARCHAR);
		JDBCUtils.setParameter(insertSummary, 16,
				(summary.getTrackerURL() != null) ? summary.getTrackerURL()
						.toString() : null, Types.VARCHAR);
		JDBCUtils.setParameter(insertSummary, 17, summary.getPreferredWeight(),
				Types.BIGINT);

		// Execute the prepared statement
		insertSummary.executeUpdate();

		ResultSet keys = null;
		long productId = 0;

		try {
			keys = insertSummary.getGeneratedKeys();
			while (keys.next()) {
				productId = keys.getLong(1);
			}
		} finally {
			try {
				keys.close();
			} catch (Exception e) {
			}
		}

		// Now that the summary is stored, lets try to store the properties
		addProductProperties(productId, summary.getProperties());
		// And try to store the links
		addProductLinks(productId, summary.getLinks());

		ProductSummary p = new ProductSummary(summary);
		p.setIndexId(productId);

		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("[" + getName() + "] Added productSummary " + sid
					+ ", indexid=" + productId + " to product index");
		}
		return p;
	}

	/**
	 * Delete a product summary from the database If the summary doesn't have an
	 * indexId value set, throw an exception
	 *
	 * @param summary
	 *            ProductSummary object to delete
	 */
	@Override
	public synchronized ProductId removeProductSummary(ProductSummary summary)
			throws Exception {
		if (summary.getIndexId() != 0 && summary.getIndexId() != null) {
			// First remove all the properties and links
			long id = summary.getIndexId();
			removeProductProperties(id);
			removeProductLinks(id);

			JDBCUtils.setParameter(deleteSummary, 1, id, Types.BIGINT);
			deleteSummary.executeUpdate();

			LOGGER.finest("[" + getName() + "] Removed productSummary id=" + id);

			// Return the id of the product deleted
			return summary.getId();
		} else {
			LOGGER.log(Level.WARNING, "[" + getName()
					+ "] Could not delete product summary. Index id not found");
			throw new Exception("[" + getName()
					+ "] Could not delete summary. Index id not found.");
		}
	}

	/**
	 * Create an association between the given event and product summary. This
	 * assumes that both the event and the product are already stored in their
	 * respective tables.
	 *
	 * @param event
	 * @param summary
	 * @return Copy of event with summary added to the event's products list
	 */
	@Override
	public synchronized Event addAssociation(Event event, ProductSummary summary)
			throws Exception {

		if (event.getIndexId() == null || summary.getIndexId() == null) {
			throw new Exception(
					"["
							+ getName()
							+ "] Cannot add association between event or summary that are not already in index.");
		}

		ProductId sid = summary.getId();

		JDBCUtils.setParameter(addAssociation, 1, event.getIndexId(),
				Types.BIGINT);
		// these will target EVERY version of the given product
		JDBCUtils.setParameter(addAssociation, 2, sid.getSource(),
				Types.VARCHAR);
		JDBCUtils.setParameter(addAssociation, 3, sid.getType(), Types.VARCHAR);
		JDBCUtils.setParameter(addAssociation, 4, sid.getCode(), Types.VARCHAR);

		addAssociation.executeUpdate();
		Event e = new Event(event);
		e.addProduct(summary);
		LOGGER.log(
				Level.FINER,
				"[" + getName() + "] Added associations event id="
						+ event.getIndexId() + ", productSummary source="
						+ sid.getSource() + ", type=" + sid.getType()
						+ ", code=" + sid.getCode() + " (id="
						+ summary.getIndexId() + ")");

		return e;
	}

	/**
	 * Delete the association, if it exists, between the given event and product
	 * summary.
	 *
	 * NOTE: this removes the association between the event and ALL versions of the product summary.
	 *
	 * @param event
	 * @param summary
	 */
	@Override
	public synchronized Event removeAssociation(Event event,
			ProductSummary summary) throws Exception {

		// Deleting the association is really just removing the foreign key
		// on the products table

		// First check that this summary and event are both in the database

		// What happens if runtime objects are set up, but not added to index.
		// This would return the event with the association in-tact. Is that
		// okay?

		Long eventIndexId = event.getIndexId();
		Long productIndexId = summary.getIndexId();
		if (eventIndexId == null || productIndexId == null) {
			return event;
		}

		ProductId sid = summary.getId();

		// Now run the query
		JDBCUtils.setParameter(removeAssociation, 1, null, Types.BIGINT);
		// these will target EVERY version of the given product
		JDBCUtils.setParameter(removeAssociation, 2, summary.getId()
				.getSource(), Types.VARCHAR);
		JDBCUtils.setParameter(removeAssociation, 3, summary.getId().getType(),
				Types.VARCHAR);
		JDBCUtils.setParameter(removeAssociation, 4, summary.getId().getCode(),
				Types.VARCHAR);

		int rows = removeAssociation.executeUpdate();
		// Throw an exception if we didn't update any
		if (rows < 1) {
			LOGGER.log(Level.INFO, "[" + getName()
					+ "] Failed to remove an association in the Product Index");
			throw new Exception("Failed to remove association");
		}

		LOGGER.finer("[" + getName() + "] Removed associations event id="
				+ eventIndexId + ", productSummary source=" + sid.getSource()
				+ ", type=" + sid.getType() + ", code=" + sid.getCode()
				+ " (id=" + productIndexId + ")");

		// Should this method remove the summary from the event's list? Yes.
		Event updatedEvent = new Event(event);
		List<ProductSummary> productsList = updatedEvent.getAllProducts().get(
				summary.getType());

		// pre 1.7.6 archive policies didn't always clean up after themselves
		// handle it gracefully
		if (productsList != null) {
			// remove all product with given source, type, and code
			Iterator<ProductSummary> iter = productsList.iterator();
			while (iter.hasNext()) {
				ProductId id = iter.next().getId();
				if (id.isSameProduct(summary.getId())) {
					iter.remove();
				}
			}
			if (productsList.size() == 0) {
				// if this was the last product of that type, remove the list
				// too
				updatedEvent.getAllProducts().remove(summary.getType());
			}
		} else {
			LOGGER.warning("Products list is empty for summary type "
					+ summary.getId().toString()
					+ ", when removing association");
		}
		return updatedEvent;
	}

	// ____________________________________
	// Protected Methods
	// ____________________________________

	/**
	 * Query the database to get the event with the given event index id
	 *
	 * @param eventIndexId
	 * @return Event object
	 * @throws SQLException
	 * @throws InvalidProductException
	 */
	protected synchronized Event getEvent(Long eventIndexId)
			throws SQLException, InvalidProductException {
		// Create an event object with its eventIndexId set
		Event event = new Event(eventIndexId);

		// Find a list of summary index ids whose summaries are associated to
		// the given eventIndexId
		Iterator<Long> summaryIndexIds = getSummaryIndexIds(eventIndexId)
				.iterator();

		while (summaryIndexIds.hasNext()) {
			// Create the product summary for each returned summary index id and
			// add the created summary to the event
			Long summaryIndexId = summaryIndexIds.next();
			event.addProduct(getSummary(summaryIndexId));
		}

		// Return our results. There may or may not be any products
		return event;
	}

	/**
	 * Query the database to get a list of event index ids that have products
	 * matching the given ProductIndexQuery.
	 *
	 * @param query
	 * @return List of index ids
	 * @throws Exception
	 */
	protected synchronized List<Long> getEventIndexIds(ProductIndexQuery query)
			throws Exception {
		// Object to return
		List<Long> eventIndexIds = new LinkedList<Long>();

		if (query == null) {
			// a null query shouldn't match ALL events
			return eventIndexIds;
		}

		// Build up our clause list like always
		List<String> clauses = buildProductClauses(query);

		// Build the SQL Query from our ProductIndexQuery object
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT e.");
		sql.append(EVENT_INDEX_ID);
		sql.append(" FROM ");
		sql.append(SUMMARY_TABLE).append(" p,");
		sql.append(EVENT_TABLE).append(" e");
		sql.append(" WHERE ");
		// this join is effectively the same as SUMMARY_EVENT_ID IS NOT NULL
		sql.append("e.").append(EVENT_INDEX_ID).append("=p.")
				.append(SUMMARY_EVENT_ID);

		// Add all appropriate where clauses
		Iterator<String> clauseIter = clauses.iterator();
		while (clauseIter.hasNext()) {
			sql.append(" AND ");
			sql.append(clauseIter.next());
		}

		// Query the database.
		Statement statement = null;
		ResultSet results = null;

		try {
			statement = verifyConnection().createStatement();
			results = statement.executeQuery(sql.toString());

			// Loop over our results and add each eventIndexId to the list
			while (results.next()) {
				// EVENT_INDEX_ID
				eventIndexIds.add(Long.valueOf(results.getLong(1)));
			}
		} finally {
			try {
				results.close();
			} catch (Exception e) {
			}
			try {
				statement.close();
			} catch (Exception e) {
			}
		}

		// Return our result. Note this is never null but may be empty.
		return eventIndexIds;
	}

	/**
	 * Use the index id to get a ProductSummary from the database.
	 *
	 * @param summaryIndexId
	 * @return ProductSummary pulled from the database
	 * @throws SQLException
	 * @throws InvalidProductException
	 */
	protected synchronized ProductSummary getSummary(Long summaryIndexId)
			throws SQLException, InvalidProductException {
		ProductSummary summary = new ProductSummary();
		summary.setIndexId(summaryIndexId);

		// -------------------------------------------------------------------
		// -- Add basic summary information
		// -------------------------------------------------------------------
		ResultSet results = null;

		try {
			// Query the index for raw information
			getSummary.setLong(1, summaryIndexId);
			results = getSummary.executeQuery();

			// Order of results is (taken from getSummary SQL)
			// 1) SUMMARY_PRODUCT_ID,
			// 2) SUMMARY_TYPE,
			// 3) SUMMARY_SOURCE,
			// 4) SUMMARY_CODE,
			// 5) SUMMARY_UPDATE_TIME,
			// 6) SUMMARY_EVENT_SOURCE,
			// 7) SUMMARY_EVENT_SOURCE_CODE,
			// 8) SUMMARY_EVENT_TIME,
			// 9) SUMMARY_EVENT_LATITUDE,
			// 10) SUMMARY_EVENT_LONGITUDE,
			// 11) SUMMARY_EVENT_DEPTH,
			// 12) SUMMARY_EVENT_MAGNITUDE,
			// 13) SUMMARY_VERSION,
			// 14) SUMMARY_STATUS,
			// 15) SUMMARY_TRACKER_URL,
			// 16) SUMMARY_PREFERRED

			// Parse the raw information and set the summary parameters
			if (results.next()) {
				try {
					// SUMMARY_PRODUCT_ID
					summary.setId(ProductId.parse(results.getString(1)));
				} catch (NullPointerException npx) {
					// Product ID not allowed to be null
					// Remove from index?
					LOGGER.log(
							Level.WARNING,
							"["
									+ getName()
									+ "] Failed to get summary. Product ID was null, summary index id="
									+ summaryIndexId, npx);
					throw new InvalidProductException("Product ID was null",
							npx);
				}

				// Set some simple types. Null values are fine.
				try {
					// SUMMARY_EVENT_SOURCE
					summary.setEventSource(results.getString(6));
				} catch (Exception e) {
					// ignore
				}
				if (results.wasNull()) {
					summary.setEventSource(null);
				}

				try {
					// SUMMARY_EVENT_SOURCE_CODE
					summary.setEventSourceCode(results.getString(7));
				} catch (Exception e) {
					// ignore
				}
				if (results.wasNull()) {
					summary.setEventSourceCode(null);
				}

				try {
					// SUMMARY_EVENT_TIME
					summary.setEventTime(new Date(results.getLong(8)));
				} catch (Exception e) {
					// ignore
				}
				if (results.wasNull()) {
					summary.setEventTime(null);
				}

				try {
					// SUMMARY_EVENT_LATITUDE
					summary.setEventLatitude(new BigDecimal(results
							.getDouble(9)));
				} catch (Exception e) {
					// ignore
				}
				if (results.wasNull()) {
					summary.setEventLatitude(null);
				}

				try {
					// SUMMARY_EVENT_LONGITUDE
					summary.setEventLongitude(new BigDecimal(results
							.getDouble(10)));
				} catch (Exception e) {
					// ignore
				}
				if (results.wasNull()) {
					summary.setEventLongitude(null);
				}

				try {
					// SUMMARY_EVENT_DEPTH
					summary.setEventDepth(new BigDecimal(results.getDouble(11)));
				} catch (Exception e) {
					// ignore
				}
				if (results.wasNull()) {
					summary.setEventDepth(null);
				}

				try {
					// SUMMARY_EVENT_MAGNITUDE
					summary.setEventMagnitude(new BigDecimal(results
							.getDouble(12)));
				} catch (Exception e) {
					// ignore
				}
				if (results.wasNull()) {
					summary.setEventMagnitude(null);
				}

				// Set some more simple values
				try {
					// SUMMARY_VERSION
					summary.setVersion(results.getString(13));
				} catch (Exception e) {
					// ignore
				}
				if (results.wasNull()) {
					summary.setVersion(null);
				}

				try {
					// SUMMARY_STATUS
					summary.setStatus(results.getString(14));
				} catch (Exception e) {
					// ignore
				}
				if (results.wasNull()) {
					summary.setStatus(null);
				}

				try {
					// SUMMARY_TRACKER_URL
					summary.setTrackerURL(new URL(results.getString(15)));
				} catch (MalformedURLException mux) {

					// Tracker URL is not allowed to be null
					// Log a message?
					// Remove this product from the index?

					// Throw a more informative exception
					LOGGER.log(Level.INFO, "[" + getName()
							+ "] Bad TrackerURL value", mux);
					throw new InvalidProductException("[" + getName()
							+ "] Bad TrackerURL value", mux);
				}

				// This will default to 0 if not set in index db
				// SUMMARY_PREFERRED
				summary.setPreferredWeight(results.getLong(16));
			}
		} finally {
			// must close result set to keep from blocking transaction
			try {
				results.close();
			} catch (Exception e) {
			}
		}

		// Add summary link information
		summary.setLinks(getSummaryLinks(summaryIndexId));

		// Add summary property information
		Map<String, String> properties = getSummaryProperties(summaryIndexId);
		summary.setProperties(properties);

		// set numeric attributes based on string values to preserve original precision
		if (properties.containsKey(Product.DEPTH_PROPERTY)) {
			summary.setEventDepth(new BigDecimal(
					properties.get(Product.DEPTH_PROPERTY)));
		}
		if (properties.containsKey(Product.LATITUDE_PROPERTY)) {
			summary.setEventLatitude(new BigDecimal(
					properties.get(Product.LATITUDE_PROPERTY)));
		}
		if (properties.containsKey(Product.LONGITUDE_PROPERTY)) {
			summary.setEventLongitude(new BigDecimal(
					properties.get(Product.LONGITUDE_PROPERTY)));
		}
		if (properties.containsKey(Product.MAGNITUDE_PROPERTY)) {
			summary.setEventMagnitude(new BigDecimal(
					properties.get(Product.MAGNITUDE_PROPERTY)));
		}

		// Return our generated result. Note this is never null.
		return summary;
	}

	/**
	 * Use the event index id to get a list of all of the product summary ids
	 * associated with that event
	 *
	 * @param eventIndexId
	 * @return List of product index ids
	 * @throws SQLException
	 */
	protected synchronized List<Long> getSummaryIndexIds(Long eventIndexId)
			throws SQLException {
		// Create a list object to return
		List<Long> summaryIndexIds = new LinkedList<Long>();

		ResultSet results = null;

		try {
			// Query database for a list of product summary index ids
			getSummaries.setLong(1, eventIndexId.longValue());
			results = getSummaries.executeQuery();

			// Add each product summary index id to our list
			while (results.next()) {
				// SUMMARY_PRODUCT_INDEX_ID
				summaryIndexIds.add(Long.valueOf(results.getLong(1)));
			}
		} finally {
			// must close result set to keep from blocking transaction
			try {
				results.close();
			} catch (Exception e) {
			}
		}

		// Return our results. Note this is never null but may be empty.
		return summaryIndexIds;
	}

	/**
	 * Query the database for a list of product summary index ids for summaries
	 * that match the given query.
	 *
	 * @param query
	 * @return List of product index ids
	 * @throws SQLException
	 */
	protected synchronized List<Long> getSummaryIndexIds(ProductIndexQuery query)
			throws SQLException {
		// Object to return
		List<Long> summaryIndexIds = new LinkedList<Long>();

		// Build up our clause list like always
		List<String> clauses = buildProductClauses(query);

		// Build the SQL Query from our ProductIndexQuery object
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT ");
		sql.append(SUMMARY_PRODUCT_INDEX_ID);
		sql.append(" FROM ");
		sql.append(SUMMARY_TABLE).append(" ").append(SUMMARY_TABLE_ALIAS);
		sql.append(" WHERE ");
		sql.append(SUMMARY_TABLE_ALIAS).append(".")
				.append(SUMMARY_PRODUCT_INDEX_ID);
		sql.append(" IS NOT NULL");

		// Add all appropriate where clauses
		Iterator<String> clauseIter = clauses.iterator();
		while (clauseIter.hasNext()) {
			sql.append(" AND ");
			sql.append(clauseIter.next());
		}

		String orderBy = query.getOrderBy();
		if (orderBy != null) {
			sql.append(" ORDER BY " + orderBy);
		}

		Integer limit = query.getLimit();
		if (limit != null) {
			sql.append(" LIMIT " + limit);
		}

		Statement statement = null;
		ResultSet results = null;
		try {
			LOGGER.finest("[" + getName() + "] running query \n"
					+ sql.toString());
			// Query the database.
			statement = verifyConnection().createStatement();
			results = statement.executeQuery(sql.toString());

			// Loop over our results and add each eventIndexId to the list
			while (results.next()) {
				// SUMMARY_PRODUCT_INDEX_ID
				summaryIndexIds.add(Long.valueOf(results.getLong(1)));
			}

			LOGGER.finest("[" + getName() + "] query complete");
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "[" + getName()
					+ "] exception querying index", e);
		} finally {
			// must close result set to keep from blocking transaction
			try {
				results.close();
			} catch (Exception e) {
			}
			try {
				statement.close();
			} catch (Exception e) {
			}
		}

		// Return our result. Note this is never null but may be empty.
		return summaryIndexIds;
	}

	/**
	 * Build a list of all the pieces of the WHERE clause relevant to the
	 * productSummary table. If the query doesn't set any properties, this
	 * method will return an empty list. It is up to the calling methods to
	 * check if the clause list is empty when they build their WHERE clause.
	 *
	 * @param query
	 * @return list containing clauses in the form: column="value"
	 */
	protected List<String> buildProductClauses(ProductIndexQuery query) {
		List<String> clauseList = new ArrayList<String>();

		if (query == null) {
			return clauseList; /* No query = No clauses */
		}

		// If they only want current products make a clause that contains a
		// subquery
		if (query.getResultType() == ProductIndexQuery.RESULT_TYPE_CURRENT) {
			String queryCode,
					querySource,
					queryType;

			queryCode = query.getProductCode();
			querySource = query.getProductSource();
			queryType = query.getProductType();

			if (queryCode != null && querySource != null && queryType != null) {
				// Better sub-select when these properties are specified
				clauseList
						.add(String
								.format("%s.%s = (SELECT %s FROM %s ps WHERE ps.%s='%s' AND ps.%s='%s' AND ps.%s='%s' AND ps.%s <> 'DELETE' ORDER BY ps.%s DESC LIMIT 1)",
										SUMMARY_TABLE_ALIAS, SUMMARY_PRODUCT_INDEX_ID,
										SUMMARY_PRODUCT_INDEX_ID, SUMMARY_TABLE,
										SUMMARY_SOURCE, querySource,
										SUMMARY_TYPE, queryType,
										SUMMARY_CODE, queryCode,
										SUMMARY_STATUS,
										SUMMARY_UPDATE_TIME));
			} else {
				clauseList
						.add(String
								.format("NOT EXISTS (SELECT %s FROM %s ps WHERE ps.%s=p.%s AND ps.%s=p.%s AND ps.%s=p.%s AND ps.%s > p.%s AND ps.%s <> 'DELETE')",
										SUMMARY_PRODUCT_INDEX_ID, SUMMARY_TABLE,
										SUMMARY_TYPE, SUMMARY_TYPE, SUMMARY_SOURCE,
										SUMMARY_SOURCE, SUMMARY_CODE, SUMMARY_CODE,
										SUMMARY_UPDATE_TIME, SUMMARY_UPDATE_TIME,
										SUMMARY_STATUS));
			}
		}
		// If they only want superseded products, make a slightly different
		// clause that has a subquery
		else if (query.getResultType() == ProductIndexQuery.RESULT_TYPE_SUPERSEDED) {
			clauseList
					.add(String
							.format("EXISTS (SELECT %s FROM %s ps WHERE ps.%s=p.%s AND ps.%s=p.%s AND ps.%s=p.%s AND ps.%s > p.%s AND ps.%s <> 'DELETE')",
									SUMMARY_PRODUCT_INDEX_ID, SUMMARY_TABLE,
									SUMMARY_TYPE, SUMMARY_TYPE, SUMMARY_SOURCE,
									SUMMARY_SOURCE, SUMMARY_CODE, SUMMARY_CODE,
									SUMMARY_UPDATE_TIME, SUMMARY_UPDATE_TIME,
									SUMMARY_STATUS));
		}

		// Interested in "any" productId in the query.
		Iterator<ProductId> productIter = query.getProductIds().iterator();

		// If there are one or more productIds we should build this clause
		if (productIter.hasNext()) {
			// Begin an "IN" clause
			StringBuilder clause = new StringBuilder();
			clause.append(String.format("%s.%s IN ('%s", SUMMARY_TABLE_ALIAS,
					SUMMARY_PRODUCT_ID, productIter.next().toString()));

			// Loop over any remaining productIds and add them to clause
			while (productIter.hasNext()) {
				clause.append("', '");
				clause.append(productIter.next().toString());
			}

			// Finish off our clause and add it to our clauseList
			clause.append("')");
			clauseList.add(clause.toString());
		}

		// Build clauses for all specified columns
		String eventSource = query.getEventSource();
		if (eventSource != null) {
			clauseList.add(String.format("%s.%s='%s'", SUMMARY_TABLE_ALIAS,
					SUMMARY_EVENT_SOURCE, eventSource));
		}

		String eventSourceCode = query.getEventSourceCode();
		if (eventSourceCode != null) {
			clauseList.add(String.format("%s.%s='%s'", SUMMARY_TABLE_ALIAS,
					SUMMARY_EVENT_SOURCE_CODE, eventSourceCode));
		}

		String eventTimeColumn;
		String eventLatitudeColumn;
		String eventLongitudeColumn;
		String eventMagnitudeColumn;
		String eventDepthColumn;

		// which table is used for event properties
		if (query.getEventSearchType() == ProductIndexQuery.SEARCH_EVENT_PREFERRED) {
			// search preferred event parameters in event table
			eventTimeColumn = EVENT_TABLE_ALIAS + "." + EVENT_TIME;
			eventLatitudeColumn = EVENT_TABLE_ALIAS + "." + EVENT_LATITUDE;
			eventLongitudeColumn = EVENT_TABLE_ALIAS + "." + EVENT_LONGITUDE;
			eventMagnitudeColumn = EVENT_TABLE_ALIAS + "." + EVENT_MAGNITUDE;
			eventDepthColumn = EVENT_TABLE_ALIAS + "." + EVENT_DEPTH;
		} else {
			// search product summary parameters in summary table
			eventTimeColumn = SUMMARY_TABLE_ALIAS + "." + SUMMARY_EVENT_TIME;
			eventLatitudeColumn = SUMMARY_TABLE_ALIAS + "."
					+ SUMMARY_EVENT_LATITUDE;
			eventLongitudeColumn = SUMMARY_TABLE_ALIAS + "."
					+ SUMMARY_EVENT_LONGITUDE;
			eventMagnitudeColumn = SUMMARY_TABLE_ALIAS + "."
					+ SUMMARY_EVENT_MAGNITUDE;
			eventDepthColumn = SUMMARY_TABLE_ALIAS + "." + SUMMARY_EVENT_DEPTH;
		}

		Date minTime = query.getMinEventTime();
		if (minTime != null) {
			clauseList.add(String.format("%s>=%d", eventTimeColumn,
					minTime.getTime()));
		}
		Date maxTime = query.getMaxEventTime();
		if (maxTime != null) {
			clauseList.add(String.format("%s<=%d", eventTimeColumn,
					maxTime.getTime()));
		}

		BigDecimal minLat = query.getMinEventLatitude();
		if (minLat != null) {
			clauseList.add(String.format("%s>=%f", eventLatitudeColumn,
					minLat.doubleValue()));
		}
		BigDecimal maxLat = query.getMaxEventLatitude();
		if (maxLat != null) {
			clauseList.add(String.format("%s<=%f", eventLatitudeColumn,
					maxLat.doubleValue()));
		}

		BigDecimal minDepth = query.getMinEventDepth();
		if (minDepth != null) {
			clauseList.add(String.format("%s>=%f", eventDepthColumn,
					minDepth.doubleValue()));
		}
		BigDecimal maxDepth = query.getMaxEventDepth();
		if (maxDepth != null) {
			clauseList.add(String.format("%s<=%f", eventDepthColumn,
					maxDepth.doubleValue()));
		}

		BigDecimal minMag = query.getMinEventMagnitude();
		if (minMag != null) {
			clauseList.add(String.format("%s>=%f", eventMagnitudeColumn,
					minMag.doubleValue()));
		}
		BigDecimal maxMag = query.getMaxEventMagnitude();
		if (maxMag != null) {
			clauseList.add(String.format("%s<=%f", eventMagnitudeColumn,
					maxMag.doubleValue()));
		}

		Date minUpdateTime = query.getMinProductUpdateTime();
		if (minUpdateTime != null) {
			clauseList.add(String.format("%s>=%d", SUMMARY_UPDATE_TIME,
					minUpdateTime.getTime()));
		}
		Date maxUpdateTime = query.getMaxProductUpdateTime();
		if (maxUpdateTime != null) {
			clauseList.add(String.format("%s<=%d", SUMMARY_UPDATE_TIME,
					maxUpdateTime.getTime()));
		}

		String source = query.getProductSource();
		if (source != null) {
			clauseList.add(String.format("%s='%s'", SUMMARY_SOURCE, source));
		}

		String type = query.getProductType();
		if (type != null) {
			clauseList.add(String.format("%s='%s'", SUMMARY_TYPE, type));
		}

		String code = query.getProductCode();
		if (code != null) {
			clauseList.add(String.format("%s='%s'", SUMMARY_CODE, code));
		}

		String version = query.getProductVersion();
		if (version != null) {
			clauseList.add(String.format("%s='%s'", SUMMARY_VERSION, version));
		}

		String status = query.getProductStatus();
		if (status != null) {
			clauseList.add(String.format("%s='%s'", SUMMARY_STATUS, status));
		}

		Long minProductIndexId = query.getMinProductIndexId();
		if (minProductIndexId != null) {
			clauseList.add(String.format("%s>=%d", SUMMARY_PRODUCT_INDEX_ID, minProductIndexId));
		}

		BigDecimal minLon = query.getMinEventLongitude();
		BigDecimal maxLon = query.getMaxEventLongitude();
		// Normalize the longitudes between -180 and 180
		minLon = normalizeLongitude(minLon);
		maxLon = normalizeLongitude(maxLon);

		if (minLon != null && maxLon != null) {
			if (maxLon.doubleValue() < minLon.doubleValue()) {
				// If the normalized maxLon is less than the normalized minLon,
				// the
				// span crosses
				// the date line
				Double minLonDouble = minLon.doubleValue();
				Double maxLonDouble = maxLon.doubleValue();
				// If the range crosses the date line, split it into 2 clauses
				String lonClause = String.format(
						"((%s > %f AND %s <= 180) OR (%s < %f AND %s > -180))",
						eventLongitudeColumn, minLonDouble,
						eventLongitudeColumn, eventLongitudeColumn,
						maxLonDouble, eventLongitudeColumn);
				clauseList.add(lonClause);
			} else {
				clauseList.add(String.format("%s>=%f and %s<=%f",
						eventLongitudeColumn, minLon.doubleValue(),
						eventLongitudeColumn, maxLon.doubleValue()));
			}
		} else if (minLon != null) {
			clauseList.add(String.format("%s>=%f", eventLongitudeColumn,
					minLon.doubleValue()));
		} else if (maxLon != null) {
			clauseList.add(String.format("%s<=%f", eventLongitudeColumn,
					maxLon.doubleValue()));
		}
		return clauseList;
	}

	/**
	 * Create the full SELECT query for the products table using the clauseList
	 * as the WHERE clause
	 *
	 * @param clauseList
	 *            List of Strings to be AND'd together in the WHERE clause
	 * @param orderby
	 *            Complete ORDER BY clause to be added after the WHERE clause
	 * @return String containing the full SELECT query
	 */
	protected String buildProductQuery(List<String> clauseList, String orderby) {
		// Join all the clauses into a WHERE clause
		StringBuilder whereClause = new StringBuilder();
		String and = " AND ";
		boolean first = true;
		for (String clause : clauseList) {
			if (!first) {
				whereClause.append(and);
			} else {
				first = false;
			}
			whereClause.append(clause);
		}

		String query_prefix = String
				.format("SELECT * FROM %s p", SUMMARY_TABLE);
		String query_suffix = "";
		if (whereClause.length() > 0) {
			query_suffix = String.format(" WHERE %s", whereClause.toString());
		}
		String query_text = query_prefix + query_suffix + " " + orderby;

		return query_text;
	}

	/**
	 * Create the full SELECT query for the products table using the clauseList
	 * as the WHERE clause. This method is a wrapper for
	 * {@link #buildProductQuery(List, String)} with an empty
	 * orderby string
	 *
	 * @param clauseList
	 * @return String containing the full SELECT query
	 */
	protected String buildProductQuery(List<String> clauseList) {
		return buildProductQuery(clauseList, "");
	}

	/**
	 * Parse the next item in the result set into a ProductSummary object
	 *
	 * @param results
	 * @return ProductSummary object with attributes filled from database
	 * @throws Exception
	 */
	protected ProductSummary parseSummaryResult(ResultSet results)
			throws Exception {
		ProductSummary p = new ProductSummary();
		p.setIndexId(results.getLong("id"));
		ProductId pid = ProductId.parse(results.getString(SUMMARY_PRODUCT_ID));
		p.setId(pid);
		p.setEventSource(results.getString(SUMMARY_EVENT_SOURCE));
		p.setEventSourceCode(results.getString(SUMMARY_EVENT_SOURCE_CODE));
		p.setEventTime(new Date(results.getLong(SUMMARY_EVENT_TIME)));

		// getDouble() returns 0 if the value was actually NULL. In this case,
		// we are going to set the value to null
		String latitude = results.getString(SUMMARY_EVENT_LATITUDE);
		if (latitude == null) {
			p.setEventLatitude(null);
		} else {
			p.setEventLatitude(new BigDecimal(latitude));
		}
		String longitude = results.getString(SUMMARY_EVENT_LONGITUDE);
		if (longitude == null) {
			p.setEventLongitude(null);
		} else {
			p.setEventLongitude(new BigDecimal(longitude));
		}
		String depth = results.getString(SUMMARY_EVENT_DEPTH);
		if (depth == null) {
			p.setEventDepth(null);
		} else {
			p.setEventDepth(new BigDecimal(depth));
		}
		String magnitude = results.getString(SUMMARY_EVENT_MAGNITUDE);
		if (magnitude == null) {
			p.setEventMagnitude(null);
		} else {
			p.setEventMagnitude(new BigDecimal(magnitude));
		}
		p.setVersion(results.getString(SUMMARY_VERSION));
		p.setStatus(results.getString(SUMMARY_STATUS));
		p.setTrackerURL((results.getString(SUMMARY_TRACKER_URL) != null) ? new URL(
				results.getString(SUMMARY_TRACKER_URL)) : null);
		p.setPreferredWeight(results.getLong(SUMMARY_PREFERRED));

		// Set product links
		Long indexId = p.getIndexId();
		ResultSet links = null;
		try {
			JDBCUtils.setParameter(getProductLinks, 1, indexId, Types.BIGINT);
			links = getProductLinks.executeQuery();
			while (links.next()) {
				p.addLink(links.getString(SUMMARY_LINK_RELATION),
						new URI(links.getString(SUMMARY_LINK_URL)));
			}
		} finally {
			try {
				links.close(); // Free this result set
			} catch (Exception e) {
			}
		}

		ResultSet props = null;
		try {
			// Set product properties
			JDBCUtils.setParameter(getProductProperties, 1, indexId,
					Types.BIGINT);
			props = getProductProperties.executeQuery();
			Map<String, String> properties = p.getProperties();
			while (props.next()) {
				properties.put(props.getString(SUMMARY_PROPERTY_NAME),
						props.getString(SUMMARY_PROPERTY_VALUE));
			}
			p.setProperties(properties);
		} finally {
			try {
				props.close();
			} catch (Exception e) {
			}
		}

		return p;
	}

	/**
	 * Look in the database for all the properties associated with the given
	 * product summary.
	 *
	 * @param summaryIndexId
	 * @return Map of property name to property value
	 * @throws SQLException
	 * @throws InvalidProductException
	 */
	protected synchronized Map<String, String> getSummaryProperties(
			Long summaryIndexId) throws SQLException, InvalidProductException {
		// Create our object to populate and return
		Map<String, String> properties = new HashMap<String, String>();

		ResultSet results = null;
		try {
			getProductProperties.setLong(1, summaryIndexId.longValue());
			results = getProductProperties.executeQuery();
			while (results.next()) {
				// SUMMARY_PROPERTY_NAME
				String name = results.getString(1);
				// SUMMARY_PROPERTY_VALUE
				String value = results.getString(2);

				if (name == null || value == null) {

					// Both name and value are required
					// Log something?
					// Remove link from product index db?
					InvalidProductException ipx = new InvalidProductException(
							"Bad Product Property");
					ipx.fillInStackTrace();
					LOGGER.log(Level.INFO, "[" + getName()
							+ "] Bad Product Property", ipx);
					throw ipx;
				}

				// Add this link back to the map of links
				properties.put(name, value);
			}
		} finally {
			// must close result set to keep from blocking transaction
			try {
				results.close();
			} catch (Exception e) {
			}
		}
		// Return our mapping of generated properties. Note this is never null
		// but may be empty.
		return properties;
	}

	/**
	 * Save the properties in the database and associate them to the given
	 * productId
	 *
	 * @param productId
	 * @param properties
	 * @throws SQLException
	 */
	protected synchronized void addProductProperties(long productId,
			Map<String, String> properties) throws SQLException {
		// Loop through the properties list and add them all to the database
		Set<String> keys = properties.keySet();
		for (String key : keys) {
			JDBCUtils.setParameter(insertProperty, 1, productId, Types.BIGINT);
			JDBCUtils.setParameter(insertProperty, 2, key, Types.VARCHAR);
			JDBCUtils.setParameter(insertProperty, 3, properties.get(key),
					Types.VARCHAR);

			insertProperty.executeUpdate();
			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.log(Level.FINEST, "[" + getName() + "] Added property "
						+ key + ":" + properties.get(key) + " for product "
						+ productId);
			}
		}
	}

	/**
	 * Delete the given properties from the index
	 *
	 * @param productId
	 */
	protected synchronized void removeProductProperties(long productId)
			throws Exception {
		JDBCUtils.setParameter(deleteProperties, 1, productId, Types.BIGINT);

		deleteProperties.executeUpdate();
	}

	/**
	 * Look in the database for all the links associated with the given product
	 * summary.
	 *
	 * @param summaryIndexId
	 * @return Map of link relation (link type) to URL
	 * @throws SQLException
	 * @throws InvalidProductException
	 */
	protected synchronized Map<String, List<URI>> getSummaryLinks(
			Long summaryIndexId) throws SQLException, InvalidProductException {
		// Create our object to populate and return
		Map<String, List<URI>> links = new HashMap<String, List<URI>>();

		ResultSet results = null;
		try {
			getProductLinks.setLong(1, summaryIndexId.longValue());
			results = getProductLinks.executeQuery();

			while (results.next()) {
				// SUMMARY_LINK_RELATION
				String relation = results.getString(1);
				// SUMMARY_LINK_URL
				String uriStr = results.getString(2);

				if (relation == null || uriStr == null) {

					// Both relation and uri are required
					// Log something?
					// Remove link from product index db?

					InvalidProductException ipx = new InvalidProductException(
							"[" + getName() + "] Bad Product Link");
					ipx.fillInStackTrace();
					LOGGER.log(Level.INFO, "[" + getName()
							+ "] Bad Product link", ipx);
					throw ipx;
				}
				List<URI> l = links.get(relation);

				// Case when no links for this relation yet
				if (l == null) {
					l = new LinkedList<URI>();
				}

				try {
					l.add(new URI(uriStr));
				} catch (URISyntaxException usx) {

					// Link URI String in DB was malformed.
					// Log something?
					// Remove from index?
					LOGGER.log(Level.INFO, "[" + getName()
							+ "] Bad Product Link", usx);
					throw new InvalidProductException("[" + getName()
							+ "] Bad Product Link", usx);
				}

				// Add this link back to the map of links
				links.put(relation, l);
			}
		} finally {
			// must close result set to keep from blocking transaction
			try {
				results.close();
			} catch (Exception e) {
			}
		}
		// Return our mapping of generated links. Note this is never null but
		// may be empty.
		return links;
	}

	/**
	 * Save the links in the database and associate them to the given productId
	 *
	 * @param productId
	 *            Index id of the product to select
	 * @param links
	 *            Map of relations to URIs
	 * @throws SQLException
	 */
	protected synchronized void addProductLinks(long productId,
			Map<String, List<URI>> links) throws SQLException {
		// Loop through the properties list and add them all to the database
		Set<String> keys = links.keySet();
		for (String key : keys) {
			List<URI> uris = links.get(key);
			for (URI uri : uris) {
				JDBCUtils.setParameter(insertLink, 1, productId, Types.BIGINT);
				JDBCUtils.setParameter(insertLink, 2, key, Types.VARCHAR);
				JDBCUtils.setParameter(insertLink, 3, uri.toString(),
						Types.VARCHAR);

				insertLink.executeUpdate();
				LOGGER.log(Level.FINEST, "[" + getName() + "] Added link "
						+ key + ":" + uri.toString() + " for product "
						+ productId);
			}
		}
	}

	/**
	 * Delete the given links from the index
	 *
	 * @param productId
	 */
	protected synchronized void removeProductLinks(long productId)
			throws Exception {
		JDBCUtils.setParameter(deleteLinks, 1, productId, Types.BIGINT);

		deleteLinks.executeUpdate();
	}

	/**
	 * Convert the given longitude to be between -180 and 180. If the given
	 * value is already in the range, this method just returns the value.
	 *
	 * @param lon
	 * @return double normalized between -180 and 180
	 */
	protected double normalizeLongitude(double lon) {
		double normalizedLon = lon;

		if (normalizedLon <= 180 && normalizedLon > -180) {
			return normalizedLon;
		}

		// If the value is above 180, make it negative by subtracting 360
		if (normalizedLon > 180) {
			normalizedLon = normalizedLon % 360;
			normalizedLon = normalizedLon - 360;
			return normalizedLon;
		}

		// If the value is below 180, make it positive by adding 360
		if (normalizedLon <= -180) {
			normalizedLon = normalizedLon % 360;
			normalizedLon = normalizedLon + 360;
			return normalizedLon;
		}

		return normalizedLon;
	}

	/**
	 * Wrapper to normalize BigDecimal longitudes
	 *
	 * @param lon
	 * @return Normalized BigDecimal latitude
	 */
	protected BigDecimal normalizeLongitude(BigDecimal lon) {
		if (lon == null) {
			return null;
		}

		return new BigDecimal(normalizeLongitude(lon.doubleValue()));
	}

	/**
	 * Called when the indexer is done updating events after a product is
	 * processed. Stores the preferred attributes for each event in the list
	 *
	 * @param events
	 *            the events that have been updated.
	 */
	@Override
	public synchronized void eventsUpdated(List<Event> events) throws Exception {
		Long indexId = null;

		Iterator<Event> iter = events.iterator();
		while (iter.hasNext()) {
			Event updated = iter.next();

			LOGGER.finer("[" + getName() + "] Updating event indexid="
					+ updated.getIndexId());
			updated.log(LOGGER);

			try {
				indexId = updated.getIndexId();

				if (updated.isDeleted()) {
					// only update status if event deleted, leave other
					// parameters intact
					JDBCUtils.setParameter(updateDeletedEvent, 1,
							EVENT_STATUS_DELETE, Types.VARCHAR);
					JDBCUtils.setParameter(updateDeletedEvent, 2, indexId,
							Types.BIGINT);

					updateDeletedEvent.executeUpdate();
				} else {
					EventSummary summary = updated.getEventSummary();

					// otherwise update event parameters
					JDBCUtils.setParameter(updateEvent, 1,
							new Date().getTime(), Types.BIGINT);
					JDBCUtils.setParameter(updateEvent, 2, summary.getSource(),
							Types.VARCHAR);
					JDBCUtils.setParameter(updateEvent, 3,
							summary.getSourceCode(), Types.VARCHAR);

					Long eventTime = null;
					if (summary.getTime() != null) {
						eventTime = summary.getTime().getTime();
					}
					JDBCUtils.setParameter(updateEvent, 4, eventTime,
							Types.BIGINT);

					Double latitude = null;
					if (summary.getLatitude() != null) {
						latitude = summary.getLatitude().doubleValue();
					}
					JDBCUtils.setParameter(updateEvent, 5, latitude,
							Types.DOUBLE);

					Double longitude = null;
					if (summary.getLongitude() != null) {
						longitude = summary.getLongitude().doubleValue();
					}
					JDBCUtils.setParameter(updateEvent, 6, longitude,
							Types.DOUBLE);

					// these may be null, handle carefully
					Double depth = null;
					if (summary.getDepth() != null) {
						depth = summary.getDepth().doubleValue();
					}
					JDBCUtils.setParameter(updateEvent, 7, depth, Types.DOUBLE);

					Double magnitude = null;
					if (summary.getMagnitude() != null) {
						magnitude = summary.getMagnitude().doubleValue();
					}
					JDBCUtils.setParameter(updateEvent, 8, magnitude,
							Types.DOUBLE);

					JDBCUtils.setParameter(updateEvent, 9, EVENT_STATUS_UPDATE,
							Types.VARCHAR);

					JDBCUtils.setParameter(updateEvent, 10, indexId,
							Types.BIGINT);

					updateEvent.executeUpdate();
				}

				LOGGER.log(Level.FINEST, "[" + getName()
						+ "] Updated event properties in Product Index");
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] Error updating event properties, eventid="
						+ indexId, e);
				// trigger a rollback
				throw e;
			}
		}
	}

}
