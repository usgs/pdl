/*
 * JDBCProductIndex
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.util.JDBCConnection;
import gov.usgs.util.Config;
import gov.usgs.util.JDBCUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
	 * Constant used to specify what the index file property should be called in
	 * to config file
	 */
	private static final String JDBC_FILE_PROPERTY = "indexfile";

	/** Prefix for connecting to a sqlite database */
	private static final String JDBC_CONNECTION_PREFIX = "jdbc:sqlite:";

	/** Variables to store the event and product column names */
	// private static final String EVENT_TABLE = "event";
	private static final String EVENT_TABLE_ALIAS = "e";
	// private static final String EVENT_INDEX_ID = "id";
	// private static final String EVENT_CREATED = "created";
	// private static final String EVENT_UPDATED = "updated";
	// private static final String EVENT_SOURCE = "source";
	// private static final String EVENT_SOURCE_CODE = "sourceCode";
	private static final String EVENT_TIME = "eventTime";
	private static final String EVENT_LATITUDE = "latitude";
	private static final String EVENT_LONGITUDE = "longitude";
	private static final String EVENT_DEPTH = "depth";
	private static final String EVENT_MAGNITUDE = "magnitude";
	// private static final String EVENT_STATUS = "status";

	private static final String EVENT_STATUS_UPDATE = "UPDATE";
	private static final String EVENT_STATUS_DELETE = "DELETE";

	private static final String SUMMARY_TABLE = "productSummary";
	private static final String SUMMARY_TABLE_ALIAS = "p";
	// private static final String SUMMARY_CREATED = "created";
	public static final String SUMMARY_PRODUCT_INDEX_ID = "id";
	private static final String SUMMARY_PRODUCT_ID = "productId";
	// private static final String SUMMARY_EVENT_ID = "eventId";
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
	// private static final String SUMMARY_PROPERTY_TABLE = "productSummaryProperty";
	// private static final String SUMMARY_PROPERTY_ID = "productSummaryIndexId";
	// private static final String SUMMARY_PROPERTY_NAME = "name";
	// private static final String SUMMARY_PROPERTY_VALUE = "value";
	// private static final String SUMMARY_LINK_TABLE = "productSummaryLink";
	// private static final String SUMMARY_LINK_ID = "productSummaryIndexId";
	// private static final String SUMMARY_LINK_RELATION = "relation";
	// private static final String SUMMARY_LINK_URL = "url";

	private String index_file;

	/**
	 * Constructor. Sets index_file to the default value JDBC_DEFAULT_FILE
	 *
	 * @throws Exception
	 */
	public JDBCProductIndex() throws Exception {
		// Default index file, so calling configure() isn't required
		index_file = JDBC_DEFAULT_FILE;
		setDriver(JDBC_DEFAULT_DRIVER);
	}

	public JDBCProductIndex(final String sqliteFileName) throws Exception {
		index_file = sqliteFileName;
		setDriver(JDBC_DEFAULT_DRIVER);
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
		super.configure(config);
		index_file = config.getProperty(JDBC_FILE_PROPERTY);

		if (getDriver() == null) { setDriver(JDBC_DEFAULT_DRIVER); }
		if (index_file == null || "".equals(index_file)) {
			index_file = JDBC_DEFAULT_FILE;
		}
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
		if (getDriver().equals(JDBCUtils.SQLITE_DRIVER_CLASSNAME)) {
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
			indexFile = null;

			// Build the JDBC url
			setUrl(JDBC_CONNECTION_PREFIX + index_file);
		}
		return super.connect();
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
		if (query == null) {
			return new ArrayList<Event>();
		}
		// map of events (index id => event), so products can be added incrementally
		final Map<Long, Event> events = new HashMap<>();
		// all products for loading details
		ArrayList<ProductSummary> products = new ArrayList<>();

		// Build up our clause list like always
		// These clauses may only match certain products within events,
		// and are used to find a list of event ids
		List<String> clauses = buildProductClauses(query);

		// Build the SQL Query from our ProductIndexQuery object
		String sql = "SELECT DISTINCT ps2.*"
				+ " FROM productSummary ps2,"
				+ " (SELECT DISTINCT e.id FROM event e, productSummary p"
				+ " WHERE e.id=p.eventId";
		// Add all appropriate where clauses
		for (final String clause : clauses) {
			sql = sql + " AND " + clause;
		}
		sql = sql + ") eventids"
				+ " WHERE ps2.eventid=eventids.id";

		// add current clause to outer query
		if (query.getResultType() == ProductIndexQuery.RESULT_TYPE_CURRENT) {
			sql = sql + " AND NOT EXISTS ("
					+ " SELECT * FROM productSummary"
					+ " WHERE source=ps2.source"
					+ " AND type=ps2.type"
					+ " AND code=ps2.code"
					+ " AND updateTime>ps2.updateTime"
					+ ")";
		}

		// load event products
		try (
			final PreparedStatement statement = getConnection().prepareStatement(sql);
			final ResultSet results = statement.executeQuery();
		) {
			statement.setQueryTimeout(60);
			while (results.next()) {
				// eventid not part of product summary object,
				// so need to do this as products are parsed...
				final Long id = results.getLong("eventId");
				Event event = events.get(id);
				if (event == null) {
					// create event to hold products
					event = new Event();
					event.setIndexId(id);
					events.put(id, event);
				}
				final ProductSummary productSummary = parseProductSummary(results);
				event.addProduct(productSummary);
				products.add(productSummary);
			}
		}

		// load product details
		loadProductSummaries(products);

		return events.values().stream().collect(Collectors.toList());
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

		final String sql = "INSERT INTO event (created) VALUES (?)";
		try (
			final PreparedStatement insertEvent =
					getConnection().prepareStatement(sql, new String[] {"id"});
		) {
			insertEvent.setQueryTimeout(60);
			// Add the values to the prepared statement
			JDBCUtils.setParameter(insertEvent, 1, new Date().getTime(), Types.BIGINT);

			// Execute the prepared statement
			int rows = insertEvent.executeUpdate();

			if (rows == 1) {
				long id = 0;
				try (final ResultSet keys = insertEvent.getGeneratedKeys()) {
					while (keys.next()) {
						id = keys.getLong(1);
					}
					e = new Event(event);
					e.setIndexId(id);
				}
				LOGGER.finest("Added event id=" + id);
			} else {
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] Exception when adding new event to database");
				throw new Exception("Error adding new event to database");
			}
		}
		LOGGER.log(Level.FINEST, "[" + getName() + "] Added event to Product Index");
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

		// remove event products
		final List<ProductId> productIds = removeProductSummaries(event.getProductList());

		// and now remove event
		final String sql = "DELETE FROM event WHERE id=?";
		try (
			final PreparedStatement deleteEvent = getConnection().prepareStatement(sql);
		) {
			deleteEvent.setQueryTimeout(60);
			JDBCUtils.setParameter(deleteEvent, 1, id, Types.BIGINT);
			int rows = deleteEvent.executeUpdate();
			// If we didn't delete a row, or we deleted more than 1 row, throw an
			// exception
			if (rows != 1) {
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] Exception when deleting an event from the database");
				throw new Exception("Error deleting event from database");
			}

			LOGGER.finest("[" + getName() + "] Removed event id=" + id);
		}

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

		final ArrayList<ProductSummary> products = new ArrayList<ProductSummary>();

		final List<String> clauseList = buildProductClauses(query);
		// Add the unassociated quantifier to the clause list
		clauseList.add("eventId IS NULL");
		final String sql = buildProductQuery(clauseList);

		try (
			final PreparedStatement statement = getConnection().prepareStatement(sql);
		) {
			statement.setQueryTimeout(60);
			try (
				final ResultSet results = statement.executeQuery();
			) {
				// Now lets build product objects from each row in the result set
				while (results.next()) {
					products.add(parseProductSummary(results));
				}
			}
		}

		// load properties and links
		loadProductSummaries(products);

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
		// load full product summaries by default
		return getProducts(query, true);
	}

	/**
	 * Load product summaries.
	 *
	 * @param query
	 *     product query
	 * @param loadDetails
	 *     whether to call {@link #loadProductSummaries(List)},
	 *     which loads links and properties with additional queries.
	 */
	public synchronized List<ProductSummary> getProducts(ProductIndexQuery query, final boolean loadDetails)
			throws Exception {
		final List<String> clauseList = buildProductClauses(query);
		final String sql = buildProductQuery(clauseList);

		final List<ProductSummary> products = new LinkedList<ProductSummary>();
		try (
			final PreparedStatement statement = getConnection().prepareStatement(sql);
		) {
			statement.setQueryTimeout(60);
			try (
				final ResultSet results = statement.executeQuery();
			) {
				// Now lets build product objects from each row in the result set
				while (results.next()) {
					products.add(parseProductSummary(results));
				}
			}
		}

		if (loadDetails) {
			// load properties and links
			loadProductSummaries(products);
		}

		return products;
	}

	/**
	 * Check whether product summary is in index.
	 *
	 * @param id
	 *     product to search.
	 */
	public synchronized boolean hasProduct(final ProductId id) throws Exception {
		final String sql = "SELECT id FROM productSummary"
				+ " WHERE source=? AND type=? AND code=? AND updateTime=?";
		try (
			final PreparedStatement statement = getConnection().prepareStatement(sql);
		) {
			statement.setQueryTimeout(60);
			statement.setString(1, id.getSource());
			statement.setString(2, id.getType());
			statement.setString(3, id.getCode());
			statement.setLong(4, id.getUpdateTime().getTime());

			try (
				final ResultSet results = statement.executeQuery();
			) {
				// return true if there is a matching row, false otherwise
				return results.next();
			}
		}
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
		long productId = 0;
		final ProductId sid = summary.getId();

		final String sql = "INSERT INTO productSummary"
				+ "(created, productId, type, source, code"
				+ ", updateTime, eventSource, eventSourceCode, eventTime"
				+ ", eventLatitude, eventLongitude, eventDepth, eventMagnitude"
				+ ", version, status, trackerURL, preferred"
				+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (
			final PreparedStatement insertSummary =
					getConnection().prepareStatement(sql, new String[] {"id"});
		) {
			insertSummary.setQueryTimeout(60);
			// Set the created timestamp
			JDBCUtils.setParameter(insertSummary, 1, new Date().getTime(),
					Types.BIGINT);

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

			try (final ResultSet keys = insertSummary.getGeneratedKeys()) {
				while (keys.next()) {
					productId = keys.getLong(1);
				}
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
		List<ProductId> removed = removeProductSummaries(Arrays.asList(summary));
		return removed.get(0);
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

		final ProductId sid = summary.getId();
		final String sql = "UPDATE productSummary"
				+ " SET eventId=? WHERE source=? AND type=? AND code=?";
		try (
			final PreparedStatement addAssociation = getConnection().prepareStatement(sql);
		) {
			addAssociation.setQueryTimeout(60);
			JDBCUtils.setParameter(addAssociation, 1, event.getIndexId(), Types.BIGINT);
			// these will target EVERY version of the given product
			JDBCUtils.setParameter(addAssociation, 2, sid.getSource(), Types.VARCHAR);
			JDBCUtils.setParameter(addAssociation, 3, sid.getType(), Types.VARCHAR);
			JDBCUtils.setParameter(addAssociation, 4, sid.getCode(), Types.VARCHAR);

			addAssociation.executeUpdate();
		}

		final Event e = new Event(event);
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

		final ProductId sid = summary.getId();
		final String sql = "UPDATE productSummary"
				+ " SET eventId=? WHERE source=? AND type=? AND code=?";
		try (
			final PreparedStatement removeAssociation = getConnection().prepareStatement(sql);
		) {
			removeAssociation.setQueryTimeout(60);
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
	 * Populate links and properties for provided product summaries.
	 *
	 * @param summaries
	 * @throws Exception
	 */
	protected synchronized void loadProductSummaries(final List<ProductSummary> summaries)
			throws Exception {
		if (summaries.size() == 0) {
			// nothing to load
			return;
		}

		// index by id
		final Map<Long, ProductSummary> summaryMap = new HashMap<>();
		for (final ProductSummary summary : summaries) {
			summaryMap.put(summary.getIndexId(), summary);
		}

		// load all links in one query
		final String linkSql = "SELECT productSummaryIndexId as id, relation, url"
				+ " FROM productSummaryLink"
				+ " WHERE productSummaryIndexId IN ("
				+ StringUtils.join(
						summaryMap.keySet().stream().collect(Collectors.toList()),
						",")
				+ ")";
		try (
			final PreparedStatement statement = getConnection().prepareStatement(linkSql);
		) {
			statement.setQueryTimeout(60);
			try (
				final ResultSet results = statement.executeQuery();
			) {
				while (results.next()) {
					Long id = results.getLong("id");
					String relation = results.getString("relation");
					String uri = results.getString("url");
					// add properties to existing objects
					summaryMap.get(id).addLink(relation, new URI(uri));
				}
			}
		}

		// load all properties in one query
		final String propertySql = "SELECT productSummaryIndexId as id, name, value"
				+ " FROM productSummaryProperty"
				+ " WHERE productSummaryIndexId IN ("
				+ StringUtils.join(
						summaryMap.keySet().stream().collect(Collectors.toList()),
						",")
				+ ")";
		try (
			final PreparedStatement statement =
					getConnection().prepareStatement(propertySql);
		) {
			statement.setQueryTimeout(60);
			try (
				final ResultSet results = statement.executeQuery();
			) {
				while (results.next()) {
					Long id = results.getLong("id");
					String name = results.getString("name");
					String value = results.getString("value");
					// add properties to existing objects
					summaryMap.get(id).getProperties().put(name, value);
				}
			}
		}
	}

	/**
	 * Parse ProductSummary without loading links or properties.
	 *
	 * @param results
	 * @return ProductSummary object without links or properties.
	 * @throws Exception
	 */
	protected ProductSummary parseProductSummary(ResultSet results)
			throws Exception {
		ProductSummary p = new ProductSummary();
		p.setIndexId(results.getLong(SUMMARY_PRODUCT_INDEX_ID));
		ProductId pid = ProductId.parse(results.getString(SUMMARY_PRODUCT_ID));
		p.setId(pid);
		p.setEventSource(results.getString(SUMMARY_EVENT_SOURCE));
		p.setEventSourceCode(results.getString(SUMMARY_EVENT_SOURCE_CODE));
		try {
			p.setEventTime(new Date(results.getLong(SUMMARY_EVENT_TIME)));
		} catch (Exception e) {
			p.setEventTime(null);
		}

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

		return p;
	}

	public synchronized List<ProductId> removeProductSummaries(
			final List<ProductSummary> summaries) throws Exception {
		// index by id
		final ArrayList<ProductId> ids = new ArrayList<>();
				// index by id
		final Map<Long, ProductSummary> summaryMap = new HashMap<>();
		for (final ProductSummary summary : summaries) {
			if (summary.getIndexId() == null) {
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] Could not delete product summary. Index id not found");
				throw new Exception("[" + getName()
						+ "] Could not delete summary. Index id not found.");
			}
			summaryMap.put(summary.getIndexId(), summary);
			ids.add(summary.getId());
		}

		if (summaries.size() == 0) {
			return ids;
		}

		// remove all products in one query
		// on delete cascade wasn't always set...
		final String[] sqls = {
			"DELETE FROM productSummaryLink WHERE productSummaryIndexId IN",
			"DELETE FROM productSummaryProperty WHERE productSummaryIndexId IN",
			"DELETE FROM productSummary WHERE id IN",
		};
		final String idsIn =" ("
				+ StringUtils.join(
						summaryMap.keySet().stream().collect(Collectors.toList()),
						",")
				+ ")";
		for (final String sql : sqls) {
			try (
				final PreparedStatement statement =
						verifyConnection().prepareStatement(sql + idsIn);
			) {
				statement.setQueryTimeout(60);
				int rows = statement.executeUpdate();
				LOGGER.log(Level.FINER, "[" + getName() + "] removed " + rows + " rows");
			}
		}

		return ids;
	}

	/**
	 * Save the properties in the database and associate them to the given
	 * productId
	 *
	 * @param productId
	 * @param properties
	 * @throws SQLException
	 */
	protected synchronized void addProductProperties(final long productId,
			final Map<String, String> properties) throws SQLException {
		// Loop through the properties list and add them all to the database
		final String sql = "INSERT INTO productSummaryProperty"
				+ " (productSummaryIndexId, name, value) VALUES (?, ?, ?)";
		try (
			final PreparedStatement insertProperty = getConnection().prepareStatement(sql);
		) {
			insertProperty.setQueryTimeout(60);
			for (String key : properties.keySet()) {
				JDBCUtils.setParameter(insertProperty, 1, productId, Types.BIGINT);
				JDBCUtils.setParameter(insertProperty, 2, key, Types.VARCHAR);
				JDBCUtils.setParameter(insertProperty, 3, properties.get(key),
						Types.VARCHAR);
				insertProperty.addBatch();
				if (LOGGER.isLoggable(Level.FINEST)) {
					LOGGER.log(Level.FINEST, "[" + getName() + "] Added property "
							+ key + ":" + properties.get(key) + " for product "
							+ productId);
				}
			}
			insertProperty.executeBatch();
		}
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
		final String sql = "INSERT INTO productSummaryLink"
				+ " (productSummaryIndexId, relation, url) VALUES (?, ?, ?)";
		try (
			final PreparedStatement insertLink = getConnection().prepareStatement(sql);
		) {
			insertLink.setQueryTimeout(60);
			for (final String relation : links.keySet()) {
				for (final URI uri : links.get(relation)) {
					JDBCUtils.setParameter(insertLink, 1, productId, Types.BIGINT);
					JDBCUtils.setParameter(insertLink, 2, relation, Types.VARCHAR);
					JDBCUtils.setParameter(insertLink, 3, uri.toString(), Types.VARCHAR);
					insertLink.addBatch();
					LOGGER.log(Level.FINEST, "[" + getName() + "] Added link "
							+ relation + ":" + uri.toString() + " for product "
							+ productId);
				}
			}
			insertLink.executeBatch();
		}
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

		return BigDecimal.valueOf(normalizeLongitude(lon.doubleValue()));
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

		final String deletedSql = "UPDATE event SET status=? WHERE id=?";
		final String updatedSql = "UPDATE event"
				+ " SET updated=?, source=?, sourceCode=?, eventTime=?"
				+ " , latitude=?, longitude=?, depth=?, magnitude=?, status=?"
				+ " WHERE id=?";

		try (
			final PreparedStatement updateDeletedEvent =
					getConnection().prepareStatement(deletedSql);
			final PreparedStatement updateEvent =
					getConnection().prepareStatement(updatedSql);
		) {
			// big events take time...
			updateDeletedEvent.setQueryTimeout(300);
			updateEvent.setQueryTimeout(300);
			Iterator<Event> iter = events.iterator();
			while (iter.hasNext()) {
				Event updated = iter.next();

				indexId = updated.getIndexId();
				LOGGER.finer("[" + getName() + "] Updating event indexid=" + indexId);
				updated.log(LOGGER);

				try {
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

}
