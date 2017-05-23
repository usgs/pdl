/*
 * JDBCProductIndexTest
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.util.Config;
import gov.usgs.earthquake.product.ProductTest;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JDBCProductIndexTest {

	private static final BigDecimal TEST_SUMMARY_LATITUDE = new BigDecimal(0.0);
	private static final BigDecimal TEST_SUMMARY_LONGITUDE = new BigDecimal(0.0);
	private static final BigDecimal TEST_SUMMARY_DENORMALIZED_LONGITUDE = new BigDecimal(
			190.0);
	private static final String INDEX_DB_FILE = "productIndex.db";

	// The test for this is commented out!
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String JDBC_URL = "jdbc:mysql://127.0.0.1/product_index?user=test&password=test";

	private boolean deleteIndexOnClose = false;

	private ProductTest productFactory = null;
	private JDBCProductIndex index = null;

	private ProductSummary summary = null;
	private ProductIndexQuery query = null;
	private Event event = null;

	@Before
	public void setup() throws Exception {
		deleteIndexOnClose = !(new File(INDEX_DB_FILE)).exists();

		index = new JDBCProductIndex();
		index.configure(new Config());
		index.startup();

		productFactory = new ProductTest();
	}

	@After
	public void shutdown() throws Exception {
		index.shutdown();

		// Toss the productIndex.db file if we created it
		if (deleteIndexOnClose) {
			(new File(INDEX_DB_FILE)).delete();
		}
	}

	/**
	 * Insert a dummy event into the database. Check the event indexId after
	 * adding it to the database and it should be not null.
	 *
	 * @throws Exception
	 */
	@Test
	public void addEventTest() throws Exception {
		event = new Event();
		Assert.assertNull(event.getIndexId());

		event = index.addEvent(event);
		Assert.assertNotNull(event.getIndexId());
	}

	/**
	 * Insert a dummy product summary into the database. Pull it back out and
	 * make sure we got back out what we put in.
	 *
	 * @throws Exception
	 */
	@Test
	public void addSummaryTest() throws Exception {

		// Create a new product summary
		summary = new ProductSummary(productFactory.getProduct());
		summary.setEventLatitude(TEST_SUMMARY_LATITUDE);
		summary.setEventLongitude(TEST_SUMMARY_LONGITUDE);
		summary.setEventDepth(null);

		// Should be null before we add it to the index
		Assert.assertNull(summary.getIndexId());

		// Now add the summary to the index
		summary = index.addProductSummary(summary);

		// Should _not_ be null after we add it to the index
		Assert.assertNotNull(summary.getIndexId());
		// The depth, however, should be null and not 0.0
		Assert.assertEquals(summary.getEventDepth(), null);
	}

	/**
	 * Insert a dummy product summary. Normalize the longitude of the summary,
	 * and pull it out using a full ProductIndexQuery
	 */
	@Test
	public void addNormailizedSummaryTest() throws Exception {

		// Create the summary
		summary = new ProductSummary(productFactory.getProduct());
		summary.setEventLongitude(TEST_SUMMARY_DENORMALIZED_LONGITUDE);
		summary.setEventLatitude(TEST_SUMMARY_LATITUDE);
		summary.setEventTime(new Date());

		Assert.assertNull(summary.getIndexId());

		// Add the summary to the index
		summary = index.addProductSummary(summary);

		Assert.assertNotNull(summary.getIndexId());

		// Create a query
		// Use the associator to build a query for this summary
		//query = associator.getProductIndexQuery(summary);
		query = new ProductIndexQuery();

		query.setEventSource(summary.getEventSource());
		query.setEventSourceCode(summary.getEventSourceCode());
		// Use a longitude span that wraps around the world
		query.setMinEventLongitude(TEST_SUMMARY_DENORMALIZED_LONGITUDE
				.add(new BigDecimal(10.0)));
		query.setMaxEventLongitude(TEST_SUMMARY_DENORMALIZED_LONGITUDE
				.add(new BigDecimal(5.0)));
		query.setMinEventDepth(summary.getEventDepth());
		query.setMaxEventDepth(summary.getEventDepth());
		query.setMinEventMagnitude(summary.getEventMagnitude());
		query.setMaxEventMagnitude(summary.getEventMagnitude());
		query.setProductVersion(summary.getVersion());
		query.setProductSource(summary.getSource());
		query.setProductType(summary.getType());
		query.setProductStatus(summary.getStatus());
		query.setProductCode(summary.getCode());

		List<ProductSummary> summaries = index.getProducts(query);
		Assert.assertTrue(summaries.contains(summary));
	}

	/**
	 * Test that we can associate a product with an event. Pull the event back
	 * out and check that it is actually associated as expected.
	 *
	 * @throws Exception
	 */
	@Test
	public void addAssociationTest() throws Exception {
		// Create an event and summary to add to our index
		event = new Event();
		summary = new ProductSummary(productFactory.getProduct());

		// Add the event and summary to our index. indexId fields are updated.
		event = index.addEvent(event);
		summary = index.addProductSummary(summary);

		event = index.addAssociation(event, summary);
		List<ProductSummary> products = event.getProducts().get(
				summary.getId().getType());
		Assert.assertTrue(products.contains(summary));
	}

	/**
	 * Adds a single, unassociatable product to the index and then checks that
	 * we can pull it back out.
	 *
	 * @throws Exception
	 */
	@Test
	public void getUnassociatedProductTest() throws Exception {
		// Add a product to the index that can not create an event (and won't
		// associate).
		summary = new ProductSummary(productFactory.getProduct());
		index.addProductSummary(summary);

		query = new ProductIndexQuery();
		query.getProductIds().add(summary.getId());
		List<ProductSummary> summaries = index.getUnassociatedProducts(query);

		// Make sure we got it back out
		Assert.assertTrue(summaries.contains(summary));
	}

	/**
	 * Adds some products to the index and checks that we can pull them back
	 * out.
	 */
	@Test
	public void getProductsTest() throws Exception {
		// Create a product and add it to the index
		summary = new ProductSummary(productFactory.getProduct());
		summary = index.addProductSummary(summary);

		// Use the associator to build a query for this summary
		//query = associator.getProductIndexQuery(summary);
		query = new ProductIndexQuery();


		// Pull the summary back out and make sure it worked.
		List<ProductSummary> summaries = index.getProducts(query);
		Assert.assertTrue(summaries.contains(summary));

		// Create another product. This time, add location/time information
		ProductSummary summary2 = new ProductSummary(
				productFactory.getOtherProduct());
		summary2.setEventLatitude(TEST_SUMMARY_LATITUDE);
		summary2.setEventLongitude(TEST_SUMMARY_LONGITUDE);
		summary2.setEventTime(new Date());
		summary2 = index.addProductSummary(summary2);

		// Add it twice so we can test superseded products
		ProductSummary summary3 = new ProductSummary(
				productFactory.getOtherProduct());
		summary3.setEventLatitude(TEST_SUMMARY_LATITUDE);
		summary3.setEventLongitude(TEST_SUMMARY_LONGITUDE);
		summary3.setEventTime(new Date());
		summary3 = index.addProductSummary(summary3);

		// Use the associator to build a query for this new summary
		//query = associator.getProductIndexQuery(summary3);
		query = new ProductIndexQuery();
		query.setResultType(ProductIndexQuery.RESULT_TYPE_CURRENT);
		// Pull the summary back out and make sure it worked.
		summaries = index.getProducts(query);
		Assert.assertTrue(summaries.contains(summary3));

		// Now look for superseded products
		//query = associator.getProductIndexQuery(summary2);
		query = new ProductIndexQuery();
		query.setResultType(ProductIndexQuery.RESULT_TYPE_SUPERSEDED);
		query.setMaxEventTime(null);
		query.setMinEventTime(null);
		summaries = index.getProducts(query);
		Assert.assertTrue(summaries.contains(summary2));

		// Now make sure we can get both current and superseded products
		query.setResultType(ProductIndexQuery.RESULT_TYPE_ALL);
		summaries = index.getProducts(query);
		Assert.assertTrue(summaries.contains(summary2));
		Assert.assertTrue(summaries.contains(summary3));
	}

	/**
	 * Adds some events to the index and tests that we can pull them back out.
	 */
	@Test
	public void getEventsTest() throws Exception {
		event = new Event();
		event = index.addEvent(event);
		Assert.assertNotNull(event.getIndexId());
	}

	/**
	 * Open a connection to a mysql database called productIndex on localhost
	 * with the user: "test" and the password: "test". This test will
	 * insert an event, a product, and then remove them.
	 *
	 * @throws Exception
	 */
	//@Test
	public void mysqlConnectionTest() throws Exception {
		deleteIndexOnClose = !(new File(INDEX_DB_FILE)).exists();

		index = new JDBCProductIndex();
		Config c = new Config();
		c.setProperty("driver", JDBC_DRIVER);
		c.setProperty("url", JDBC_URL);
		index.configure(c);
		try {
			index.startup();
		} catch (Exception e) {
			Assert.fail("Could not connect to mysql database at localhost on "
					+ "port 3306 using user='test' and pass='test'.");
		}
		// Create a dummy event and summary
		summary = new ProductSummary(productFactory.getProduct());
		Date d = new Date();
		summary.setEventTime(d);
		Date dUpdated = summary.getUpdateTime();
		event = new Event();
		try {
			index.beginTransaction();
			// Save them to the DB
			summary = index.addProductSummary(summary);
			event = index.addEvent(event);
			// Associate them
			event = index.addAssociation(event, summary);
			index.commitTransaction();
		} catch (Exception e) {
			Assert.fail("Could not insert into database. Make sure you have a database "
					+ "named 'product_index' on localhost with the schema found "
					+ "in etc/schema/productIndexSchemaMysql.sql");
		}

		Assert.assertNotNull(event.getIndexId());

		// Now lets try to delete the event.
		// This should cascade so the product gets deleted too.
		index.beginTransaction();
		index.removeEvent(event);
		index.commitTransaction();

		ProductIndexQuery q = new ProductIndexQuery();
		q.setMaxEventTime(d);
		q.setMinEventTime(d);
		// Now lets check if the event and product are gone
		Assert.assertTrue(index.getEvents(q).isEmpty());
		q = new ProductIndexQuery();
		q.setMaxProductUpdateTime(dUpdated);
		q.setMinProductUpdateTime(dUpdated);
		Assert.assertTrue((index.getProducts(q) == null || index.getProducts(q)
				.isEmpty()));

		index.shutdown();
	}

}
