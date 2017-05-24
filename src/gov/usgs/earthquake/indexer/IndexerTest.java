/*
 * IndexerTest
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.distribution.ProductTracker;
import gov.usgs.earthquake.indexer.IndexerChange.IndexerChangeType;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.XmlProductSource;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * IndexerTest is designed to be a unit test suite for the Indexer class.
 * Specifically we must test the "onProduct" method and the archive/cleanup
 * thread. Within tests for the "onProduct" method we must ensure the following
 * IndexerEvents occur if/when expected:
 * 
 * IndexerEvent.EVENT_ADDED IndexerEvent.EVENT_UPDATED
 * IndexerEvent.EVENT_DELETED IndexerEvent.PRODUCT_ADDED
 * IndexerEvent.PRODUCT_UPDATED IndexerEvent.PRODUCT_DELETED
 * 
 * Within tests for the archive/cleanup thread we must ensure the following
 * IndexerEvents occur if/when expected:
 * 
 * IndexerEvent.EVENT_ARCHIVED IndexerEVENT.PRODUCT_ARCHIVED
 * 
 * For all of the test cases it is important we check products/events are
 * actually created, added, updated, deleted and archived into/out of the
 * product storage and index before considering the test a success.
 * 
 * Due to the integral part played by the Indexer class in the overall system we
 * place heavier testing requirements on this class. Specifically we require a
 * minimum of 90% branch coverage and 75% line coverage.
 * 
 * @author emartinez
 * 
 */
public class IndexerTest extends DefaultConfigurable implements IndexerListener {

	/* The indexer object used to run tests. */
	private Indexer indexer = null;
	/* The most recently received indexer event */
	private IndexerEvent indexerEvent = null;

	/* Name of the archiving policy */
	private static final String ARCHIVE_POLICY_PROPERTY = "testPolicy";
	/* A synchronizing object to test notifications (multi-threaded) */
	private final Object syncObject = new Object();
	/* How long to keep products in the index (milliseconds) */
	private static final long minEventAge = 1500L;
	/* How often to run the indexer cleanup thread (milliseconds) */
	private static final long cleanupInterval = 500L;

	// ------------------------------------------------------------------------
	// -- Environment Methods
	// ------------------------------------------------------------------------

	/**
	 * Sets up the test environment. Sets flag whether the product index file
	 * existed before the tests or not. This way we clean up after ourselves,
	 * but we don't mess up any existing index file.
	 */
	@Before
	public void setupTestEnvironment() {
		try {
			Logger.getLogger("").setLevel(Level.FINE);
			indexer = new Indexer();
			Config.setConfig(new Config());
			Config config = Config.getConfig();
			// Tell indexer which archive policy property to use
			config.setProperty(Indexer.INDEX_ARCHIVE_POLICY_PROPERTY,
					ARCHIVE_POLICY_PROPERTY);
			// Keep events for 15 seconds
			config.setSectionProperty(ARCHIVE_POLICY_PROPERTY,
					ArchivePolicy.ARCHIVE_MIN_EVENT_AGE_PROPERTY,
					String.valueOf(minEventAge));
			config.setSectionProperty(ARCHIVE_POLICY_PROPERTY, "type",
					ArchivePolicy.class.getName());

			// Check for expired events every 5 seconds
			config.setProperty(Indexer.INDEX_ARCHIVE_INTERVAL_PROPERTY,
					String.valueOf(cleanupInterval));

			indexer.configure(config);
			indexer.addListener(this);

			// delete storage before test
			FileUtils.deleteTree(new File("productIndex.db"));
			FileUtils.deleteTree(new File("_test_storage_"));

			// piggyback an external indexer listener on this test in case it
			// throws exceptions
			ExternalIndexerListener listener = new ExternalIndexerListener();
			listener.setCommand("echo");
			listener.setMaxTries(1);
			listener.setStorage(new FileProductStorage(new File(
					"_test_storage_")));
			indexer.addListener(listener);

			ExternalIndexerListener listener2 = new ExternalIndexerListener();
			listener2.setCommand("echo");
			listener2.setMaxTries(1);
			listener2.setStorage(new FileProductStorage(new File(
					"_test_storage_")));
			listener2.setProcessOnlyWhenEventChanged(true);
			indexer.addListener(listener2);

			indexer.setDisableArchive(true);
			indexer.startup();

		} catch (Exception e) {
			System.err.println("Error before IndexerTest could run tests.");
			e.printStackTrace(System.err);
		}

	}

	/**
	 * Tears down the test environment. If the index file flag indicates that we
	 * created the index file ourselves we will delete this file at this time.
	 * This way we clean up after ourselves, but don't mess up any existing
	 * index file.
	 */
	@After
	public void teardownTestEnvironment() {
		if (indexer != null) {
			try {
				indexer.shutdown();
				indexer.setDisableArchive(false);
				indexer = null;

				FileUtils.deleteTree(new File("productIndex.db"));
				FileUtils.deleteTree(new File("_test_storage_"));
			} catch (Exception e) {
				System.err.println("Error in shutting down indexer.");
			}
		}
	}

	// ------------------------------------------------------------------------
	// -- Tests
	// ------------------------------------------------------------------------

	/**
	 * Tests the indexer "getModule" method. To test this we create a product
	 * then call the indexer getModule method and see what we get back. We
	 * assert the module is not null and has a positive support level.
	 * 
	 * Additionally, create an indexer with 2 modules that support a particular
	 * type of product. Assert that the module chosen for each type of product
	 * is appropriate.
	 * 
	 * @author emartinez
	 * @throws Exception
	 * @see gov.usgs.earthquake.indexer.Indexer#getModule(Product)
	 */
	@Test
	public void getModuleTest() throws Exception {
		Product product = createProduct();
		IndexerModule module = indexer.getModule(product);
		Assert.assertNotNull(module);
		Assert.assertTrue((module.getSupportLevel(product) > 0));

		// make sure the indexer chooses modules well
		ProductTest test = new ProductTest();
		Product module2Product = test.getProduct();
		final String module2ProductType = module2Product.getId().getType();
		IndexerModule module2 = new IndexerModule() {
			@Override
			public int getSupportLevel(Product product) {
				if (product.getId().getType().equals(module2ProductType)) {
					return 10000;
				} else {
					return -1;
				}
			}

			@Override
			public ProductSummary getProductSummary(Product product)
					throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
		};

		Product module3Product = test.getOtherProduct();
		final String module3ProductType = module3Product.getId().getType();
		IndexerModule module3 = new IndexerModule() {
			@Override
			public int getSupportLevel(Product product) {
				if (product.getId().getType().equals(module3ProductType)) {
					return 10000;
				} else {
					return -1;
				}
			}

			@Override
			public ProductSummary getProductSummary(Product product)
					throws Exception {
				// TODO Auto-generated method stub
				return null;
			}

		};

		Indexer testIndexer = new Indexer();
		testIndexer.addModule(module2);
		testIndexer.addModule(module3);

		Assert.assertEquals(module2, testIndexer.getModule(module2Product));
		Assert.assertEquals(module3, testIndexer.getModule(module3Product));
	}

	/**
	 * Tests the indexer "onProduct" method when a product representing a "new"
	 * event is given. This method creates a new indexer object and adds itself
	 * as a listener for indexer events. It then adds a new event to the index
	 * and waits for notification. Upon notification we assert that the "type"
	 * of IndexerEvent we received is "EVENT_ADDED".
	 * 
	 * After inserting the first product, this test will then create a different
	 * type of product and associate it to the event using eventSource and
	 * eventSourceCode instead of lat, lon, and event time. Adding the second
	 * product should give an IndexerEvent of type "EVENT_UPDATED"
	 * 
	 * @author emartinez
	 * @see gov.usgs.earthquake.indexer.Indexer#onProduct(Product)
	 * @see gov.usgs.earthquake.indexer.IndexerEvent#EVENT_ADDED
	 */
	@Test
	public void onProductNewTest() {
		try {
			indexer.onProduct(createProduct());
			synchronized (syncObject) {
				syncObject.wait();
			}
			Assert.assertEquals(IndexerChange.EVENT_ADDED,
					getLatestIndexerEvent().getIndexerChanges().get(0)
							.getType());

			// We got the product in, it added the event, so now
			// we need to add a different product type from the same source
			// to test associating on eventSource and eventSourceCode
			Product product = createSourceProduct();
			indexer.onProduct(product);
			synchronized (syncObject) {
				syncObject.wait();
			}
			Assert.assertEquals(IndexerChange.EVENT_UPDATED,
					getLatestIndexerEvent().getIndexerChanges().get(0)
							.getType());

		} catch (Exception e) {
			e.printStackTrace(System.err);
			Assert.fail("Exception in onProductNewTest.");
		}
	}

	/**
	 * Tests the indexer "onProduct" method when a product representing an
	 * "update" to an existing event is given. This method creates a new indexer
	 * object and adds itself as a listener for indexer events. It then adds a
	 * product that is capable of creating an event to the index. Next this
	 * product has its update time (version) updated to indicate the product is
	 * now an update to what is currently in the index. This updated product is
	 * then added to the index and we wait for notification. Upon notification
	 * we assert that the "type" of IndexerEvent we received is "EVENT_UPDATED".
	 * 
	 * @author emartinez
	 * @see gov.usgs.earthquake.indexer.Indexer#onProduct(Product)
	 * @see gov.usgs.earthquake.indexer.IndexerEvent#EVENT_UPDATED
	 */
	@Test
	public void onProductUpdatedTest() {
		try {
			// Add the new product to the indexer
			Product product = createProduct();
			indexer.onProduct(product);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Now update the product and re-add it to the index
			product.getId().setUpdateTime(new Date()); // Change update time
			indexer.onProduct(product);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Make sure this latest indexer event is an update
			Assert.assertEquals(IndexerChange.EVENT_UPDATED,
					getLatestIndexerEvent().getIndexerChanges().get(0)
							.getType());

		} catch (Exception e) {
			e.printStackTrace(System.err);
			Assert.fail("Exception in onProductUpdatedTest.");
		}
	}

	/**
	 * Tests the indexer "onProduct" method when a product is received that does
	 * not associate to an existing event and is not capable of creating its own
	 * event based on its parameters. Such a product is "unassociatable". This
	 * product is then added to an indexer and we wait notification. Upon
	 * notification we assert that the "type" of IndexerEvent we received is
	 * "UNASSOCIATED_PRODUCT".
	 * 
	 * @author emartinez
	 * @see gov.usgs.earthquake.indexer.Indexer#onProduct(Product)
	 * @see gov.usgs.earthquake.indexer.IndexerEvent#UNASSOCIATED_PRODUCT
	 */
	@Test
	public void onProductUnassociatedTest() {
		try {
			indexer.onProduct(createUnassociatableProduct());
			synchronized (syncObject) {
				syncObject.wait();
			}
			System.err.println(getLatestIndexerEvent().getIndexerChanges()
					.get(0).getType());
			Assert.assertEquals(IndexerChange.PRODUCT_ADDED,
					getLatestIndexerEvent().getIndexerChanges().get(0)
							.getType());
		} catch (Exception e) {
			e.printStackTrace(System.err);
			Assert.fail("Exception in onProductUnassociatedTest.");
		}
	}

	/**
	 * This test checks if an incoming product can successfully cause a merge of
	 * to occur in the product index. This test checks if the location-based
	 * merge works properly on two events.
	 */
	@Test
	public void onProductMergeTest1() {
		Date theDate = new Date();

		try {
			// Step1. Create a product that will add an event to the index.
			ProductId product1Id = new ProductId("us", "testproduct",
					"testproduct-one");

			Product product1 = new Product(product1Id);
			product1.setEventSource("NOTus");
			product1.setEventSourceCode("testproduct-one");
			product1.setEventTime(theDate);
			product1.setMagnitude(new BigDecimal(5.0));
			product1.setLatitude(new BigDecimal(35.0)); // LA area
			product1.setLongitude(new BigDecimal(-118.0));
			product1.setDepth(new BigDecimal(50.0));
			product1.setTrackerURL(new URL("http://localhost/tracker"));
			product1.setVersion("one");

			indexer.onProduct(product1);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Step2. Create a product that will add an event to the index
			// will not by itself associate to the previous event.

			ProductId product2Id = new ProductId("us", "testproduct",
					"testproduct-two");

			Product product2 = new Product(product2Id);
			product2.setEventSource("us");
			product2.setEventSourceCode("testproduct-two");
			product2.setEventTime(theDate);
			product2.setMagnitude(new BigDecimal(5.0));
			product2.setLatitude(new BigDecimal(37.0)); // SF area
			product2.setLongitude(new BigDecimal(-122.0));
			product2.setDepth(new BigDecimal(50.0));
			product2.setTrackerURL(new URL("http://localhost/tracker"));
			product2.setVersion("one");

			indexer.onProduct(product2);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Step3. Create a product (positioned near event from step 1)
			// with an event id matching that from step 2 (so it associates to
			// that event) but with a location near enough to event from step 1
			// such that event 1 merges into event 2.

			ProductId product3Id = new ProductId("us", "testproduct",
					"testproduct-two");

			Product product3 = new Product(product3Id);
			product3.setEventSource("us");
			product3.setEventSourceCode("testproduct-two");
			product3.setEventTime(theDate);
			product3.setMagnitude(new BigDecimal(5.0));
			product3.setLatitude(new BigDecimal(35.0)); // LA area
			product3.setLongitude(new BigDecimal(-118.0));
			product3.setDepth(new BigDecimal(50.0));
			product3.setTrackerURL(new URL("http://localhost/tracker"));
			product3.setVersion("two");

			indexer.onProduct(product3);

			// Step4. Wait for the indexer event update and verify the merge
			// occurred.
			synchronized (syncObject) {
				syncObject.wait();
			}

			Vector<IndexerChange> changes = getLatestIndexerEvent()
					.getIndexerChanges();

			Assert.assertEquals(2, changes.size());

			Event event = changes.get(1).getNewEvent();
			List<ProductSummary> products = event.getProductList();

			Assert.assertEquals("Event has three products associated to it.",
					2, products.size());

			// Make sure preferred-ness is working with a merge.
			ProductSummary ps = event.getPreferredProduct("testproduct");
			Assert.assertEquals("Preferred product is product #3.", product3Id,
					ps.getId());

			List<Event> events = indexer.getProductIndex().getEvents(
					new ProductIndexQuery());
			Assert.assertEquals("There is one event in the index.", 1,
					events.size());

			// We added three products, but one was just an update to a previous
			// product, so there are really only two "products" in thet index.
			Assert.assertEquals(
					"There are two products in the index.",
					2,
					indexer.getProductIndex()
							.getProducts(new ProductIndexQuery()).size());

		} catch (Exception e) {
			e.printStackTrace(System.err);
			Assert.fail("Exception in onProuductMergeTest");
		}
	}

	/**
	 * This test checks if an incoming product can successfully cause a merge of
	 * to occur in the product index. This test checks if the eventid-based
	 * merge works properly on an event and unassociated product.
	 */
	@Test
	public void onProductMergeTest2() {
		String source = "us";
		String code = "testproduct-one";
		Date time = new Date();
		String trackerString = "http://localhost/tracker";

		try {
			// Step 1. Insert an unassociatable product with a specific eventid
			// into the index.
			ProductId product1Id = new ProductId(source, "testone", code);

			Product product1 = new Product(product1Id);
			product1.setEventSource(source);
			product1.setEventSourceCode(code);
			product1.setEventTime(time);
			product1.setMagnitude(new BigDecimal(5.0));
			product1.setDepth(new BigDecimal(50.0));
			product1.setTrackerURL(new URL(trackerString));
			product1.setVersion("one");

			indexer.onProduct(product1);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Step 2. Insert a product with specific eventid (same as in Step
			// 1), with location information. This should create an event and
			// then find the previously unassociated product (based on eventid)
			// and merge it onto the event.
			ProductId product2Id = new ProductId(source, "testtwo", code);

			Product product2 = new Product(product2Id);
			product2.setEventSource(source);
			product2.setEventSourceCode(code);
			product2.setEventTime(time);
			product2.setMagnitude(new BigDecimal(6.0));
			product2.setDepth(new BigDecimal(10.0));
			product2.setTrackerURL(new URL(trackerString));
			product2.setVersion("two");

			// Add a location so it can create an event (LA area)
			product2.setLatitude(new BigDecimal(35.0));
			product2.setLongitude(new BigDecimal(-118.0));

			indexer.onProduct(product2);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Step 3. Check the latest indexer event and make sure things
			// changed as expected. Product from step 1 should have been
			// associated to event from step 2.

			Vector<IndexerChange> changes = this.getLatestIndexerEvent()
					.getIndexerChanges();
			Assert.assertNotNull("Indexer changes should not be null", changes);
			Assert.assertEquals("Indexer changes should have length 1", 1,
					changes.size());

			Assert.assertEquals("Latest change should be EVENT_ADDED",
					IndexerChange.EVENT_ADDED, changes.get(0).getType());

			Event event = changes.get(0).getNewEvent();
			Assert.assertEquals("Event should have 1 products on it.", 2, event
					.getAllProductList().size());

			List<ProductSummary> summaries = indexer.getProductIndex()
					.getUnassociatedProducts(new ProductIndexQuery());
			Assert.assertEquals(
					"There should be no unassociated products in the index.",
					0, summaries.size());
		} catch (Exception ex) {

		}
	}

	/**
	 * Tests if an event will properly split into two events
	 */
	@Test
	public void onProductSplitTest1() {
		try {
			Date eventTime = new Date();
			BigDecimal latitude = new BigDecimal(35.0);
			BigDecimal longitude = new BigDecimal(-118.0);
			URL url = new URL("http://localhost/tracker");

			// Step 1. A product arrives with a laittude, longitude, time, and
			// distinct event source and event source code. A new event is
			// created in the product index.
			ProductId product1Id = new ProductId("us", "testproduct", "one");
			Product product1 = new Product(product1Id);
			product1.setEventTime(eventTime);
			product1.setLatitude(latitude);
			product1.setLongitude(longitude);
			product1.setTrackerURL(url);
			product1.setEventSource("us");
			product1.setEventSourceCode("one");

			indexer.onProduct(product1);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Step 2. Another product arrives with a similar latitude,
			// longitude, and time as the product from step 1, but the event
			// source and event source code are distinct. Since the locations
			// are "nearby", this product should be associated to the event
			// created in step 1.
			ProductId product2Id = new ProductId("nc", "testproduct", "uno");
			Product product2 = new Product(product2Id);
			product2.setEventTime(eventTime);
			product2.setLatitude(latitude);
			product2.setLongitude(longitude);
			product2.setTrackerURL(url);
			product2.setEventSource("nc");
			product2.setEventSourceCode("uno");

			indexer.onProduct(product2);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Step 3. A disassociation product arrives with the same event
			// source and event source code as the product from step 2. It wants
			// to force disassociation from the product from step 1. This
			// disassociation product need not have any location information.
			ProductId product3Id = new ProductId("nc", "disassociate", "uno");
			Product product3 = new Product(product3Id);
			product3.setTrackerURL(url);
			product3.setEventSource("nc");
			product3.setEventSourceCode("uno");

			// Turn this into a disassociation product with relevent properties.
			Map<String, String> product3Properties = new HashMap<String, String>();
			product3Properties.put("othereventsource", "us");
			product3Properties.put("othereventsourcecode", "one");
			product3.setProperties(product3Properties);

			indexer.onProduct(product3);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Verify our results. We should have an event (from step 1) with a
			// single product (from step 1) associated to it. We should also
			// have another event (from step 3) with 2 products (from steps 2,3)
			// associated to it. These two events should be in the same location
			// (latitude, longitude, and time).

			// Get all events in the index
			List<Event> events = indexer.getProductIndex().getEvents(
					new ProductIndexQuery());

			Assert.assertEquals("There should be two events in the index.", 2,
					events.size());

			Iterator<Event> eventIter = events.iterator();
			while (eventIter.hasNext()) {
				Event event = eventIter.next();
				String eventId = event.getSource().toLowerCase()
						+ event.getSourceCode().toLowerCase();

				if ("usone".equals(eventId)) {
					// This is the event from step 1. Should have one event.
					Assert.assertEquals("Event should have 1 product.", 1,
							event.getProductList().size());
				} else if ("ncuno".equals(eventId)) {
					Assert.assertEquals("Event should have 2 products.", 2,
							event.getProductList().size());
				} else {
					Assert.fail("Found unexpected event [" + eventId + "]");
				}

				Assert.assertEquals(
						"Latitude should be " + latitude.doubleValue(),
						latitude, event.getLatitude());

				Assert.assertEquals(
						"Longitude should be " + longitude.doubleValue(),
						longitude, event.getLongitude());
			}

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			Assert.fail("Exception testing product split (1).");
		}
	}

	/**
	 * Event splits due to new product version moving a product away from
	 * current event location.
	 */
	@Test
	public void onProductSplitTest2() {

		BigDecimal oldLat = new BigDecimal(35.0);
		BigDecimal oldLng = new BigDecimal(-118.0);
		BigDecimal newLat = new BigDecimal(37.7);
		BigDecimal newLng = new BigDecimal(-122.4);
		Date eventTime = new Date();
		String urlString = "http://localhost/tracker";

		try {

			// Step 1. A product arrives with a latitude, longitude, time and
			// distinct event source and event source code. A new event is
			// created in the product index.
			ProductId product1Id = new ProductId("us", "testproduct", "one");
			Product product1 = new Product(product1Id);

			product1.setEventSource("us");
			product1.setEventSourceCode("one");
			product1.setTrackerURL(new URL(urlString));
			product1.setEventTime(eventTime);
			product1.setLatitude(oldLat);
			product1.setLongitude(oldLng);
			product1.setVersion("one");

			indexer.onProduct(product1);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Step 2. Another product arrives with a similar latitude,
			// longitude, and time as the product from step 1, but the event
			// source and event source code are distinct. Since the locations
			// are, "nearby", this product is associated to the event created in
			// step 1.
			ProductId product2Id = new ProductId("nc", "testproduct", "uno");
			Product product2 = new Product(product2Id);

			product2.setEventSource("nc");
			product2.setEventSourceCode("uno");
			product2.setTrackerURL(new URL(urlString));
			product2.setEventTime(eventTime);
			product2.setLatitude(oldLat);
			product2.setLongitude(oldLng);
			product2.setVersion("one");

			indexer.onProduct(product2);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Save the current event id to check against later.
			String savedEventId = indexer.getProductIndex()
					.getEvents(new ProductIndexQuery()).get(0).getEventId();

			// Step 3. An updated version of the product from step 2 arrives
			// with a
			// location far away from the previous location. Because it has the
			// same
			// event source and event source code as the product from step 2, it
			// defaults to associate on to the event from step 1 as well.
			ProductId product3Id = new ProductId("nc", "testproduct", "uno");
			Product product3 = new Product(product3Id);

			product3.setEventSource("nc");
			product3.setEventSourceCode("uno");
			product3.setTrackerURL(new URL(urlString));
			product3.setEventTime(eventTime);
			product3.setLatitude(newLat);
			product3.setLongitude(newLng);
			product3.setVersion("two");

			indexer.onProduct(product3);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Step 4. Upon checking for splits (in the indexer), the products
			// from
			// step 2 and step 3 should split to the new location from step 3
			// and
			// remove themselves from the event from step 1.

			// There should be two events in the product index.
			List<Event> events = indexer.getProductIndex().getEvents(
					new ProductIndexQuery());
			Assert.assertEquals("There should be two events in the index.", 2,
					events.size());

			// The US event should be near LA, the NC event should be near SF.
			Iterator<Event> eventIter = events.iterator();
			while (eventIter.hasNext()) {
				Event event = eventIter.next();
				if ("usone".equals(event.getEventId())) {
					Assert.assertEquals(
							"us event should have location near Los Angeles",
							oldLat, event.getLatitude());
					Assert.assertEquals(
							"us event should have a location near Los Angeles",
							oldLng, event.getLongitude());
				} else if ("ncuno".equals(event.getEventId())) {
					Assert.assertEquals(
							"nc event should have location near San Francisco",
							newLat, event.getLatitude());
					Assert.assertEquals(
							"nc event should have a location near San Francisco",
							newLng, event.getLongitude());
				}
			}

			// We should see an EVENT_UPDATED followed by an EVENT_SPLIT
			Vector<IndexerChange> changes = getLatestIndexerEvent()
					.getIndexerChanges();

			Assert.assertEquals(
					"There should be two changes occurring in this event.", 2,
					changes.size());
			Assert.assertEquals("The first change should be an EVENT_UPDATED",
					IndexerChange.EVENT_UPDATED.toString(), changes.get(0)
							.getType().toString());
			Assert.assertEquals("The second change should be an EVENT_SPLIT",
					IndexerChange.EVENT_SPLIT.toString(), changes.get(1)
							.getType().toString());

			// The event that was, "UPDATED" should have the same eventId as the
			// savedEventId
			Assert.assertEquals("Checking if the correct event was updated.",
					savedEventId, changes.get(0).getNewEvent().getEventId());
			Assert.assertTrue(
					"Checking if the correct event was split.",
					!savedEventId.equals(changes.get(1).getNewEvent()
							.getEventId()));
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			Assert.fail("Exception during onProductSplitTest2");
		}
	}

	/**
	 * Test if event splits more than once because the product acting as "glue"
	 * between two other products is moved away.
	 */
	@Test
	public void onProductSplitTest3() {
		Date eventTime = new Date();
		String trackerURL = "http://localhost/tracker/";

		BigDecimal epsilon = new BigDecimal(0.000001);
		BigDecimal glueLat = new BigDecimal(0.0);
		BigDecimal glueLng = new BigDecimal(0.0);

		ProductIndexQuery locationQuery = indexer.getAssociator()
				.getLocationQuery(eventTime, glueLat, glueLng);

		BigDecimal leftLat = locationQuery.getMinEventLatitude().add(epsilon);
		BigDecimal leftLng = locationQuery.getMinEventLongitude().add(epsilon);

		BigDecimal rightLat = locationQuery.getMaxEventLatitude().subtract(
				epsilon);
		BigDecimal rightLng = locationQuery.getMaxEventLongitude().subtract(
				epsilon);

		BigDecimal antiGlueLat = new BigDecimal(10.0);
		BigDecimal antiGlueLng = new BigDecimal(10.0);

		try {
			// Step 1. A product arrives with a latitude, longitude, time, and
			// distinct event source and event source code. A new event is
			// created
			// in the product index.
			ProductId glueProductId = new ProductId("us", "testproduct", "one");
			Product glueProduct = new Product(glueProductId);

			glueProduct.setEventSource("us");
			glueProduct.setEventSourceCode("one");
			glueProduct.setTrackerURL(new URL(trackerURL));
			glueProduct.setEventTime(eventTime);
			glueProduct.setLatitude(glueLat);
			glueProduct.setLongitude(glueLng);
			glueProduct.setVersion("one");

			indexer.onProduct(glueProduct);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Step 2. Another product arrives with a similar latitude,
			// longitude,
			// and time as the product from step 1, but the event source and
			// event
			// source code are distinct. The locations are just barely near
			// enough
			// that the product is associated to the event is created created in
			// step 1.
			ProductId leftProductId = new ProductId("nc", "testproduct", "uno");
			Product leftProduct = new Product(leftProductId);

			leftProduct.setEventSource("nc");
			leftProduct.setEventSourceCode("uno");
			leftProduct.setTrackerURL(new URL(trackerURL));
			leftProduct.setEventTime(eventTime);
			leftProduct.setLatitude(leftLat);
			leftProduct.setLongitude(leftLng);
			leftProduct.setVersion("one");

			indexer.onProduct(leftProduct);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Step 3. A third product arrives with latitude, longitude, and
			// time
			// similar to the product from step 1, but different from the
			// product
			// from step 2 (on the opposite side of the product location from
			// step
			// 1). The event source and event source code of this third product
			// is
			// distinct from the product from step 1 and the product from step
			// 2.
			// Since the location is just barely near enough to the product from
			// step 1 (but not near at all to the product from step 2), this
			// product
			// is also associated to the event from step 1.
			ProductId rightProductId = new ProductId("ci", "testproduct", "ein");
			Product rightProduct = new Product(rightProductId);

			rightProduct.setEventSource("ci");
			rightProduct.setEventSourceCode("ein");
			rightProduct.setTrackerURL(new URL(trackerURL));
			rightProduct.setEventTime(eventTime);
			rightProduct.setLatitude(rightLat);
			rightProduct.setLongitude(rightLng);
			rightProduct.setVersion("one");

			indexer.onProduct(rightProduct);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Save the current event id for later checking
			String savedEventId = indexer.getProductIndex()
					.getEvents(new ProductIndexQuery()).get(0).getEventId();

			// An updated version of the product from step 1 moves the location
			// of
			// that product far away from either of the products from step 2 and
			// step 3. Since this "glue" has been moved away and the products
			// from
			// step 2 and step three are not themselves "nearby", the event from
			// step 3 splits into three separate events (one for products in
			// step
			// 1/4, one for product in step 2, and one for product in step 3.
			ProductId antiGlueProductId = new ProductId("us", "testproduct",
					"one");
			Product antiGlueProduct = new Product(antiGlueProductId);

			antiGlueProduct.setEventSource("us");
			antiGlueProduct.setEventSourceCode("one");
			antiGlueProduct.setTrackerURL(new URL(trackerURL));
			antiGlueProduct.setEventTime(eventTime);
			antiGlueProduct.setLatitude(antiGlueLat);
			antiGlueProduct.setLongitude(antiGlueLng);
			antiGlueProduct.setVersion("two");

			indexer.onProduct(antiGlueProduct);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Make sure things are how we expect.

			// There should be three changes in this IndexerEvent
			IndexerEvent indexerEvent = getLatestIndexerEvent();
			Vector<IndexerChange> changes = indexerEvent.getIndexerChanges();
			Assert.assertEquals(
					"There should be three changes in this IndexerEvent.", 3,
					changes.size());

			// The first change should be an EVENT_UPDATED for the savedEventId
			Assert.assertEquals("The first change should be EVENT_UPDATED.",
					IndexerChange.EVENT_UPDATED, changes.get(0).getType());
			Assert.assertEquals(
					"The first change should have the savedEventId.",
					savedEventId, changes.get(0).getNewEvent().getEventId());

			// The second two changes should be EVENT_SPLIT
			Assert.assertEquals("The second change should be an EVENT_SPLIT.",
					IndexerChange.EVENT_SPLIT, changes.get(1).getType());
			Assert.assertEquals("The third change should be an EVENT_SPLIT.",
					IndexerChange.EVENT_SPLIT, changes.get(2).getType());

			// There should be three events in the index.
			List<Event> events = indexer.getProductIndex().getEvents(
					new ProductIndexQuery());
			Assert.assertEquals("There should be three events in the index.",
					3, events.size());

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			Assert.fail("Exception in test onProductSplitTest3");
		}
	}

	/**
	 * Tests the indexer's periodic cleanup functionality to check the index on
	 * regular (configurable) intervals and remove expired (configurable) events
	 * and products.
	 * 
	 * @author emartinez
	 * @see gov.usgs.earthquake.indexer.Indexer#purgeExpiredProducts()
	 * @see gov.usgs.earthquake.indexer.Indexer#INDEX_CLEANUP_INTERVAL_PROPERTY
	 * @see gov.usgs.earthquake.indexer.Indexer#INDEX_MAX_AGE_PROPERTY
	 */
	@Test
	public void indexCleanupThreadTest() {
		ProductIndex productIndex = null;
		try {
			// enable archive policies
			indexer.setDisableArchive(false);

			// Add product to the index
			Product product = createProduct();
			indexer.onProduct(product);

			// Sleep a short time period and assert the product is still there
			Thread.sleep(cleanupInterval);

			// Create a query to find the recently added product summary
			ProductSummary summary = getLatestIndexerEvent().getSummary();
			List<ProductId> productIds = new ArrayList<ProductId>();
			ProductIndexQuery query = new ProductIndexQuery();

			productIds.add(summary.getId());
			query.setProductIds(productIds);

			productIndex = indexer.getProductIndex();
			List<ProductSummary> summaries = productIndex.getProducts(query);
			Assert.assertTrue(summaries.contains(summary));

			// Sleep longer so the product expires and assert the product has
			// been purged.
			Thread.sleep(minEventAge + cleanupInterval);
			summaries = productIndex.getProducts(query);
			Assert.assertFalse(summaries.contains(summary));

		} catch (Exception e) {
			e.printStackTrace(System.err);
			Assert.fail("Exception in indexCleanupThreadTest");
		} finally {
			try {
				productIndex.shutdown();
			} catch (Exception ex) {/* Ignore */
			}
			// disable archive policies
			indexer.setDisableArchive(true);
		}
	}

	@Test
	public void archivePolicySupersededProductTest() {

		ProductIndex productIndex = null;

		try {

			// Add a product that creates an event
			Product product = createProduct();
			indexer.onProduct(product);
			
			// ensure that the product has time to create an event
			synchronized (syncObject) {
				syncObject.wait();
			}
			
			// Add a product, that associates with the event
			Product firstProduct = createSourceProduct();
			firstProduct.getId().setUpdateTime(new Date());
			indexer.onProduct(firstProduct);

			// ensure the first product makes it into the product index
			synchronized (syncObject) {
				syncObject.wait();
			}

			// ProductSummary of the to-be-superseded product
			ProductSummary summary = getLatestIndexerEvent().getSummary();

			List<ProductId> productIds = new ArrayList<ProductId>();
			productIds.add(summary.getId());

			ProductIndexQuery query = new ProductIndexQuery();
			query.setProductIds(productIds);
			query.setResultType(ProductIndexQuery.RESULT_TYPE_ALL);

			// Assert that the first product has been added
			productIndex = indexer.getProductIndex();
			List<ProductSummary> summaries = productIndex.getProducts(query);
			Assert.assertTrue(summaries.contains(summary));

			// Add a second product to supersede the first product
			Product secondProduct = createSourceProduct();
			secondProduct.getId().setUpdateTime(new Date());
			indexer.onProduct(secondProduct);

			// ensure the second product makes it into the product index
			synchronized (syncObject) {
				syncObject.wait();
			}

			// Assert that the first product has been superseded
			query.setResultType(ProductIndexQuery.RESULT_TYPE_CURRENT);
			summaries = productIndex.getProducts(query);
			Assert.assertFalse(summaries.contains(summary));

			// save old archive policies
			List<ArchivePolicy> oldArchivePolicies = new ArrayList<ArchivePolicy>(
					indexer.getArchivePolicies());
			indexer.getArchivePolicies().clear();

			// set new archive policy to delete everything with
			// product.getEventSource()
			ArchivePolicy eventSourceArchivePolicy = new ArchivePolicy();
			eventSourceArchivePolicy.setEventSource(product.getEventSource());
			indexer.getArchivePolicies().add(eventSourceArchivePolicy);

			System.err.println("Enabling archive policy");

			// enable archiving and wait long enough for it to run
			indexer.setDisableArchive(false);
			
			// ensure the archive policy runs
			synchronized (syncObject) {
				syncObject.wait();
			}

			// disable and restore archive policies
			indexer.setDisableArchive(true);
			indexer.getArchivePolicies().clear();
			indexer.getArchivePolicies().addAll(oldArchivePolicies);

			System.err.println("Done archiving");

			// Assert that the first product was archived
			query.setResultType(ProductIndexQuery.RESULT_TYPE_ALL);
			summaries = productIndex.getProducts(query);
			Assert.assertFalse(summaries.contains(summary));

			// Check that all events and products were removed (3 total)
			ProductIndexQuery allProductsQuery = new ProductIndexQuery();
			allProductsQuery.setEventSource(product.getEventSource());
			allProductsQuery.setResultType(ProductIndexQuery.RESULT_TYPE_ALL);
			List<ProductSummary> allSummaries = productIndex
					.getProducts(allProductsQuery);

			// Assert all products were deleted
			Assert.assertTrue(allSummaries.size() == 0);

		} catch (Exception e) {
			e.printStackTrace(System.err);
			Assert.fail("Exception in archivePolicySupersededProductTest. ");
		}

	}

	/**
	 * Normally two events that are not within the association window will not
	 * associate (~100km, 16seconds). The associate product can override this
	 * behavior.
	 * 
	 * This test creates two events that are not nearby (LA, SF) from different
	 * sources. It then sends an associate product to force the association, and
	 * verifies the events merge.
	 * 
	 * @throws Exception
	 */
	@Test
	public void mergeLocationsUsingAssociate() throws Exception {
		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINEST);

		// send two events that are not nearby
		Date theDate = new Date();

		// Step1. Create a product that will add an event to the index.
		ProductId product1Id = new ProductId("us", "testproduct",
				"testproduct-one");

		Product product1 = new Product(product1Id);
		product1.setEventSource("notus");
		product1.setEventSourceCode("testproduct-one");
		product1.setEventTime(theDate);
		product1.setMagnitude(new BigDecimal(5.0));
		product1.setLatitude(new BigDecimal(35.0)); // LA area
		product1.setLongitude(new BigDecimal(-118.0));
		product1.setDepth(new BigDecimal(50.0));
		product1.setTrackerURL(new URL("http://localhost/tracker"));
		product1.setVersion("one");

		indexer.onProduct(product1);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// Step2. Create a product that will add an event to the index
		// will not by itself associate to the previous event.

		ProductId product2Id = new ProductId("us", "testproduct",
				"testproduct-two");

		Product product2 = new Product(product2Id);
		product2.setEventSource("us");
		product2.setEventSourceCode("testproduct-two");
		product2.setEventTime(theDate);
		product2.setMagnitude(new BigDecimal(5.0));
		product2.setLatitude(new BigDecimal(37.0)); // SF area
		product2.setLongitude(new BigDecimal(-122.0));
		product2.setDepth(new BigDecimal(50.0));
		product2.setTrackerURL(new URL("http://localhost/tracker"));
		product2.setVersion("one");

		indexer.onProduct(product2);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// send associate product to force association

		ProductId product3Id = new ProductId("admin",
				Event.ASSOCIATE_PRODUCT_TYPE, product1.getEventId() + "_"
						+ product2.getEventId());

		Product product3 = new Product(product3Id);
		product3.setEventSource(product2.getEventSource());
		product3.setEventSourceCode(product2.getEventSourceCode());
		product3.getProperties().put(Event.OTHEREVENTSOURCE_PROPERTY,
				product1.getEventSource());
		product3.getProperties().put(Event.OTHEREVENTSOURCECODE_PROPERTY,
				product1.getEventSourceCode());
		product3.setTrackerURL(new URL("http://localhost/tracker"));

		indexer.onProduct(product3);

		// Step4. Wait for the indexer event update and verify the merge
		// occurred.
		synchronized (syncObject) {
			syncObject.wait();
		}

		// verify merge event
		Vector<IndexerChange> changes = getLatestIndexerEvent()
				.getIndexerChanges();

		Assert.assertEquals(2, changes.size());
		Assert.assertEquals("First change is EVENT_MERGED", changes.get(0)
				.getType(), IndexerChangeType.EVENT_MERGED);
		Assert.assertEquals("Second change is EVENT_UPDATED", changes.get(1)
				.getType(), IndexerChangeType.EVENT_UPDATED);
	}

	/**
	 * Normally two event codes from the same event source prevents association.
	 * The associate product can override this behavior.
	 * 
	 * This test creates two events that are nearby, but from the same source.
	 * It then sends an associate product to force the association, and verifies
	 * the events merge.
	 * 
	 * @throws Exception
	 */
	@Test
	public void mergeEventsFromSameSourceUsingAssociate() throws Exception {
		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINEST);

		// send two events that are not nearby
		Date theDate = new Date();

		// Step1. Create a product that will add an event to the index.
		ProductId product1Id = new ProductId("us", "testproduct",
				"testproduct-one");

		Product product1 = new Product(product1Id);
		product1.setEventSource("us");
		product1.setEventSourceCode("testproduct-one");
		product1.setEventTime(theDate);
		product1.setMagnitude(new BigDecimal(5.0));
		product1.setLatitude(new BigDecimal(35.0)); // LA area
		product1.setLongitude(new BigDecimal(-118.0));
		product1.setDepth(new BigDecimal(50.0));
		product1.setTrackerURL(new URL("http://localhost/tracker"));
		product1.setVersion("one");

		indexer.onProduct(product1);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// Step2. Create a product that will add an event to the index
		// will not by itself associate to the previous event.

		ProductId product2Id = new ProductId("us", "testproduct",
				"testproduct-two");

		Product product2 = new Product(product2Id);
		product2.setEventSource("us");
		product2.setEventSourceCode("testproduct-two");
		product2.setEventTime(theDate);
		product2.setMagnitude(new BigDecimal(5.0));
		product2.setLatitude(new BigDecimal(35.1)); // still LA area
		product2.setLongitude(new BigDecimal(-118.0));
		product2.setDepth(new BigDecimal(50.0));
		product2.setTrackerURL(new URL("http://localhost/tracker"));
		product2.setVersion("one");

		indexer.onProduct(product2);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// send associate product to force association

		ProductId product3Id = new ProductId("admin",
				Event.ASSOCIATE_PRODUCT_TYPE, product1.getEventId() + "_"
						+ product2.getEventId());

		Product product3 = new Product(product3Id);
		product3.setEventSource(product2.getEventSource());
		product3.setEventSourceCode(product2.getEventSourceCode());
		product3.getProperties().put(Event.OTHEREVENTSOURCE_PROPERTY,
				product1.getEventSource());
		product3.getProperties().put(Event.OTHEREVENTSOURCECODE_PROPERTY,
				product1.getEventSourceCode());
		product3.setTrackerURL(new URL("http://localhost/tracker"));

		indexer.onProduct(product3);

		// Step4. Wait for the indexer event update and verify the merge
		// occurred.
		synchronized (syncObject) {
			syncObject.wait();
		}

		// verify merge event
		Vector<IndexerChange> changes = getLatestIndexerEvent()
				.getIndexerChanges();

		Assert.assertEquals(2, changes.size());
		Assert.assertEquals("First change is EVENT_MERGED", changes.get(0)
				.getType(), IndexerChangeType.EVENT_MERGED);
		Assert.assertEquals("Second change is EVENT_UPDATED", changes.get(1)
				.getType(), IndexerChangeType.EVENT_UPDATED);
	}

	@Test
	public void mergeEventsFromSameSourceByDeletingSecondCodeFromSameSource() throws Exception {
		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINER);
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINER);

		// disable tracker
		ProductTracker.setTrackerEnabled(false);

		// send two events that are nearby, but with different codes from same source
		Date theDate = new Date();

		// Step1. Create a product that will add an event to the index.
		ProductId product1Id = new ProductId("us", "testproduct",
				"event1");

		Product product1 = new Product(product1Id);
		product1.setEventSource("us");
		product1.setEventSourceCode("event1");
		product1.setEventTime(theDate);
		product1.setMagnitude(new BigDecimal(5.0));
		product1.setLatitude(new BigDecimal(35.0)); // LA area
		product1.setLongitude(new BigDecimal(-118.0));
		product1.setDepth(new BigDecimal(50.0));
		product1.setTrackerURL(new URL("http://localhost/tracker"));
		product1.setVersion("one");

		indexer.onProduct(product1);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// Step2. Create a product that will add an event to the index
		// will not by itself associate to the previous event.

		ProductId product2Id = new ProductId("us", "testproduct",
				"event2");

		Product product2 = new Product(product2Id);
		product2.setEventSource("us");
		product2.setEventSourceCode("event2");
		product2.setEventTime(theDate);
		product2.setMagnitude(new BigDecimal(5.0));
		product2.setLatitude(new BigDecimal(35.1)); // still LA area
		product2.setLongitude(new BigDecimal(-118.0));
		product2.setDepth(new BigDecimal(50.0));
		product2.setTrackerURL(new URL("http://localhost/tracker"));
		product2.setVersion("one");

		indexer.onProduct(product2);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// Step3. send product from separate source to keep second event "alive"

		ProductId product3Id = new ProductId("notus", "testproduct",
				"otherevent");

		Product product3 = new Product(product3Id);
		product3.setEventSource("notus");
		product3.setEventSourceCode("otherevent");
		product3.setEventTime(theDate);
		product3.setMagnitude(new BigDecimal(5.0));
		product3.setLatitude(new BigDecimal(35.2)); // still LA area, closer to event2 than event1
		product3.setLongitude(new BigDecimal(-118.0));
		product3.setDepth(new BigDecimal(50.0));
		product3.setTrackerURL(new URL("http://localhost/tracker"));
		product3.setVersion("one");

		indexer.onProduct(product3);
		synchronized (syncObject) {
			syncObject.wait();
		}


		// Step4. delete product that forced event to split
		ProductId product4Id = new ProductId("us", "testproduct",
				"event2");

		Product product4 = new Product(product4Id);
		product4.setEventSource("us");
		product4.setEventSourceCode("event2");
		product4.setStatus(Product.STATUS_DELETE);
		product4.setTrackerURL(new URL("http://localhost/tracker"));
		product4.setVersion("two");

		indexer.onProduct(product4);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// verify merge event
		Vector<IndexerChange> changes = getLatestIndexerEvent()
				.getIndexerChanges();

		Assert.assertEquals(2, changes.size());
		Assert.assertEquals("First change is EVENT_MERGED", changes.get(0)
				.getType(), IndexerChangeType.EVENT_MERGED);
		Assert.assertEquals("Second change is EVENT_UPDATED", changes.get(1)
				.getType(), IndexerChangeType.EVENT_UPDATED);
	}

	@Test
	public void testTrump() throws Exception {
		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINEST);

		Date eventTime = new Date();
		BigDecimal latitude = new BigDecimal(35.0);
		BigDecimal longitude = new BigDecimal(-118.0);
		URL url = new URL("http://localhost/tracker");

		// Step 1. A product arrives with a laittude, longitude, time, and
		// distinct event source and event source code. A new event is
		// created in the product index.
		ProductId product1Id = new ProductId("us", "testproduct", "one");
		Product product1 = new Product(product1Id);
		product1.setEventTime(eventTime);
		product1.setLatitude(latitude);
		product1.setLongitude(longitude);
		product1.setTrackerURL(url);
		product1.setEventSource("us");
		product1.setEventSourceCode("one");

		indexer.onProduct(product1);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// Step 2. Another product arrives with a similar latitude,
		// longitude, and time as the product from step 1, but the event
		// source and event source code are distinct. Since the locations
		// are "nearby", this product should be associated to the event
		// created in step 1.
		ProductId product2Id = new ProductId("sc", "testproduct", "uno");
		Product product2 = new Product(product2Id);
		product2.setEventTime(eventTime);
		product2.setLatitude(latitude);
		product2.setLongitude(longitude);
		product2.setTrackerURL(url);
		product2.setEventSource("sc");
		product2.setEventSourceCode("uno");

		indexer.onProduct(product2);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// at this point sc should be authoritative
		Assert.assertTrue(
				"sc authoritative before trump",
				getLatestIndexerEvent().getEvents().get(0).getEventId()
						.equals("scuno"));

		// send trump to make us more preferred
		ProductId trumpId = new ProductId("admin", "trump",
				"us-testproduct-one");
		Product trump = new Product(trumpId);
		trump.addLink("product", new URI(product1Id.toString()));
		trump.getProperties().put("weight", "1000");
		trump.setTrackerURL(url);
		// trump.setEventSource("us");
		// trump.setEventSourceCode("one");

		indexer.onProduct(trump);
		synchronized (syncObject) {
			syncObject.wait();
		}

		Assert.assertTrue(
				"us authoritative after trump",
				getLatestIndexerEvent().getEvents().get(0).getEventId()
						.equals("usone"));

		// delete trump to make us less preferred
		trump.getId().setUpdateTime(new Date());
		// don't send eventsource/eventsourcecode
		trump.getProperties().clear();
		trump.getLinks().clear();
		trump.setStatus(Product.STATUS_DELETE);
		indexer.onProduct(trump);
		synchronized (syncObject) {
			syncObject.wait();
		}

		Assert.assertTrue(
				"sc authoritative after deleting trump",
				getLatestIndexerEvent().getEvents().get(0).getEventId()
						.equals("scuno"));
	}

	@Test
	public void testDelete() throws Exception {
		try {
			// turn up logging during test
			LogManager.getLogManager().reset();
			ConsoleHandler handler = new ConsoleHandler();
			handler.setLevel(Level.FINEST);
			handler.setFormatter(new SimpleLogFormatter());
			Logger rootLogger = Logger.getLogger("");
			rootLogger.addHandler(handler);
			rootLogger.setLevel(Level.FINEST);

			Date eventTime = new Date();
			BigDecimal latitude = new BigDecimal(35.0);
			BigDecimal longitude = new BigDecimal(-118.0);
			URL url = new URL("http://localhost/tracker");

			// Step 1. A product arrives with a latitude, longitude, time, and
			// distinct event source and event source code. A new event is
			// created in the product index.
			ProductId product1Id = new ProductId("us", "origin", "one");
			Product product1 = new Product(product1Id);
			product1.setEventTime(eventTime);
			product1.setLatitude(latitude);
			product1.setLongitude(longitude);
			product1.setTrackerURL(url);
			product1.setEventSource("us");
			product1.setEventSourceCode("one");

			indexer.onProduct(product1);
			synchronized (syncObject) {
				syncObject.wait();
			}

			Assert.assertFalse("not deleted after first product",
					getLatestIndexerEvent().getEvents().get(0).isDeleted());

			// Step 2. Same product from step 1 is deleted, and no event
			// parameters
			// except event id are included.
			ProductId product2Id = new ProductId("us", "origin", "one");
			Product product2 = new Product(product2Id);
			product2.setTrackerURL(url);
			product2.setEventSource("us");
			product2.setEventSourceCode("one");

			indexer.onProduct(product2);
			synchronized (syncObject) {
				syncObject.wait();
			}

			// at this point sc should be authoritative
			Assert.assertTrue("deleted after second product",
					getLatestIndexerEvent().getEvents().get(0).isDeleted());
		} finally {

		}
	}

	/**
	 * This test reproduce(d/s) an exception caused by a product archive policy
	 * bug.
	 * 
	 * The exception was: <code>
	 * java.sql.SQLException: [SQLITE_CONSTRAINT]  Abort due to constraint violation (columns source, sourceCode are not unique)
	 * </code>
	 * 
	 * The cause was in a patch to the Indexer.removeSummary method.
	 * removeSummary was calling index.removeAssociation, which removes the
	 * association of all versions of a product and event, instead of just the
	 * association between one product and the event.
	 * 
	 * Simply removing the product is enough to remove the association, so the
	 * removeSummary method was updated to remove the summary from the index and
	 * event object and then update the index; leaving newer/other versions of
	 * the product intact.
	 * 
	 * @throws Exception
	 */
	@Test
	public void eventArchiveIssue() throws Exception {
		try {
			// turn up logging during test
			LogManager.getLogManager().reset();
			ConsoleHandler handler = new ConsoleHandler();
			handler.setLevel(Level.FINER);
			handler.setFormatter(new SimpleLogFormatter());
			Logger rootLogger = Logger.getLogger("");
			rootLogger.addHandler(handler);
			rootLogger.setLevel(Level.FINER);

			Product product1 = ObjectProductHandler
					.getProduct(new XmlProductSource(
							StreamUtils
									.getInputStream(new File(
											"etc/test_products/nc71932931/nc_origin_nc71932931_1359717290440.xml"))));
			Product product2 = ObjectProductHandler
					.getProduct(new XmlProductSource(
							StreamUtils
									.getInputStream(new File(
											"etc/test_products/nc71932931/nc_origin_nc71932931_1359717370445.xml"))));
			Product product3 = ObjectProductHandler
					.getProduct(new XmlProductSource(
							StreamUtils
									.getInputStream(new File(
											"etc/test_products/nc71932931/nc_origin_nc71932931_1359740134563.xml"))));

			indexer.onProduct(product1);
			synchronized (syncObject) {
				syncObject.wait();
			}

			indexer.onProduct(product2);
			synchronized (syncObject) {
				syncObject.wait();
			}

			System.err.println("Enabling archive policy");
			// save these for now
			List<ArchivePolicy> oldArchivePolicies = new ArrayList<ArchivePolicy>(
					indexer.getArchivePolicies());
			indexer.getArchivePolicies().clear();
			ProductArchivePolicy oldVersionsPolicy = new ProductArchivePolicy();
			oldVersionsPolicy.setOnlySuperseded(true);
			oldVersionsPolicy.setMaxProductAge(1L);
			indexer.getArchivePolicies().add(oldVersionsPolicy);

			// enable archiving and wait long enough for it to run
			indexer.setDisableArchive(false);
			Thread.sleep(2000);

			// disable and restore
			indexer.setDisableArchive(true);
			indexer.getArchivePolicies().clear();
			indexer.getArchivePolicies().addAll(oldArchivePolicies);

			System.err.println("Done archiving");

			// onProduct with the new product caused the constraint violation
			indexer.onProduct(product3);
			synchronized (syncObject) {
				syncObject.wait();
			}
		} finally {

		}
	}

	// -- END: Tests -- //

	// ------------------------------------------------------------------------
	// -- Helper Methods
	// ------------------------------------------------------------------------

	/**
	 * Creates a product that can not in-and-of itself create an event. It also
	 * lacks the basic parameters required to be able to associate to any
	 * existing event using the default associator.
	 * 
	 * Namely, the generated product lacks a latitude, longitude, and event
	 * time.
	 * 
	 * @return The generated product
	 * @author emartinez
	 * @see gov.usgs.earthquake.product.ProductTest#getProduct()
	 */
	private Product createUnassociatableProduct() {
		ProductTest factory = new ProductTest();
		return factory.getProduct();
	}

	/**
	 * Creates a product that can in-and-of itself create an event. It may also
	 * associate to another product assuming its basic parameters are within
	 * range of the default associator thresholds.
	 * 
	 * Namely this product sets its latitude and longitude values to 0.0 and
	 * uses the current date/time as the event time.
	 * 
	 * 
	 * @return The generated product
	 * @author emartinez
	 * @see #createUnassociatableProduct()
	 * @see gov.usgs.earthquake.product.ProductTest#getProduct()
	 */
	private Product createProduct() {
		Product product = createUnassociatableProduct();
		product.setLatitude(new BigDecimal(0.0));
		product.setLongitude(new BigDecimal(0.0));
		product.setEventTime(new Date());
		ProductId id = product.getId();
		product.setEventSource(id.getSource());
		product.setEventSourceCode(id.getCode());
		return product;
	}

	/**
	 * Create a product that can only be associated with an existing event based
	 * on the eventSource and eventSourceCode properties. In other words, the
	 * generated product doesn't have a lat, lon, or event time.
	 * 
	 * @return The generated product
	 * @see #createUnassociatableProduct()
	 * @see gov.usgs.earthquake.product.ProductTest#getProduct()
	 */
	private Product createSourceProduct() {
		Product product = createUnassociatableProduct();
		ProductId id = product.getId();
		id.setType("pager");
		product.setId(id);
		product.setEventSource(id.getSource());
		product.setEventSourceCode(id.getCode());
		return product;
	}

	/**
	 * Simple setter event for the most recently received IndexerEvent. Set from
	 * the "onIndexerEvent" method.
	 * 
	 * @param event
	 *            The most recently received IndexerEvent from the
	 *            "onIndexerEvent" method.
	 * @see #onIndexerEvent(IndexerEvent)
	 */
	private void setLatestIndexerEvent(IndexerEvent event) {
		indexerEvent = event;
	}

	/**
	 * Simple getter method to return the most recently received IndexerEvent
	 * via the "onIndexerEvent" method.
	 * 
	 * @return The most recently received IndexerEvent
	 * @see #onIndexerEvent(IndexerEvent)
	 */
	private IndexerEvent getLatestIndexerEvent() {
		return indexerEvent;
	}

	/**
	 * Implements the IndexerListener interface. Upon receiving notification we
	 * wait for 50 milliseconds to ensure the syncObject has been able to go
	 * into its "wait" mode before we notify. Next we update our notification
	 * event with the incoming event. Finally we notify our syncObject so it can
	 * continue with processing in the test.
	 * 
	 * @see gov.usgs.earthquake.indexer.IndexerListener
	 * @see #onProductNewTest()
	 * @see #onProductUpdatedTest()
	 * @see #onProductUnassociatedTest()
	 */
	@Override
	public void onIndexerEvent(IndexerEvent event) {
		try {
			Thread.sleep(50);
			setLatestIndexerEvent(event);
			System.out.println("IndexerEvent");
			Vector<IndexerChange> changes = event.getIndexerChanges();
			for (int i = 0; i < changes.size(); i++) {
				IndexerChange change = changes.get(i);
				System.out.printf("     %s\n", change.toString());
			}
			synchronized (syncObject) {
				syncObject.notify();
			}
		} catch (InterruptedException ie) {
			System.err.println("onIndexerEvent:: Thread.sleep interrupted.");
		}

	}

	@Override
	public int getMaxTries() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public long getTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}


	/**
	 * Test for indexer persistent trump product support.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPersistentTrump() throws Exception {
		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINEST);

		Date eventTime = new Date();
		BigDecimal latitude = new BigDecimal(35.0);
		BigDecimal longitude = new BigDecimal(-118.0);
		URL url = new URL("http://localhost/tracker");

		// Step 1. A product arrives with a laittude, longitude, time, and
		// distinct event source and event source code. A new event is
		// created in the product index.
		ProductId product1Id = new ProductId("us", "testproduct", "one");
		Product product1 = new Product(product1Id);
		product1.setEventTime(eventTime);
		product1.setLatitude(latitude);
		product1.setLongitude(longitude);
		product1.setTrackerURL(url);
		product1.setEventSource("us");
		product1.setEventSourceCode("one");

		indexer.onProduct(product1);
		synchronized (syncObject) {
			syncObject.wait();
		}

		Event latestEvent = getLatestIndexerEvent().getEvents().get(0);
		List<ProductSummary> latestProducts = Event.getSortedMostPreferredFirst(
				latestEvent.getProducts("testproduct"));
		long originalPreferredWeight = latestProducts.get(0).getPreferredWeight();

		// Step 2. Another product arrives with a similar latitude,
		// longitude, and time as the product from step 1, but the event
		// source and event source code are distinct (and authoritative at this
		// location). Since the locations
		// are "nearby", this product should be associated to the event
		// created in step 1.
		ProductId product2Id = new ProductId("ci", "testproduct", "uno");
		Product product2 = new Product(product2Id);
		product2.setEventTime(eventTime);
		product2.setLatitude(latitude);
		product2.setLongitude(longitude);
		product2.setTrackerURL(url);
		product2.setEventSource("ci");
		product2.setEventSourceCode("uno");

		indexer.onProduct(product2);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// at this point ci should be authoritative
		latestEvent = getLatestIndexerEvent().getEvents().get(0);
		Assert.assertTrue(
				"ci authoritative before trump",
				latestEvent.getEventId().equals("ciuno"));

		// send trump to make us more preferred
		ProductId trumpId = new ProductId("admin", "trump-testproduct",
				"us-testproduct-one");
		Product trump = new Product(trumpId);
		trump.getProperties().put("trump-source", product1Id.getSource());
		trump.getProperties().put("trump-code", product1Id.getCode());
		trump.setTrackerURL(url);
		trump.setEventSource(product1.getEventSource());
		trump.setEventSourceCode(product1.getEventSourceCode());

		indexer.onProduct(trump);
		synchronized (syncObject) {
			syncObject.wait();
		}

		latestEvent = getLatestIndexerEvent().getEvents().get(0);
		latestProducts = Event.getSortedMostPreferredFirst(
				latestEvent.getProducts("testproduct"));
		Assert.assertTrue(
				"us authoritative after trump",
				latestEvent.getEventId().equals("usone"));
		Assert.assertEquals("us is preferred after trump",
				latestProducts.get(0).getSource(), "us");
		Assert.assertEquals("us has trump preferred weight",
				latestProducts.get(0).getPreferredWeight(),
				Indexer.TRUMP_PREFERRED_WEIGHT);



		// send update to original product (now trumped)
		// make sure trump persists
		product1.getId().setUpdateTime(new Date());
		indexer.onProduct(product1);
		synchronized (syncObject) {
			syncObject.wait();
		}

		latestEvent = getLatestIndexerEvent().getEvents().get(0);
		latestProducts = Event.getSortedMostPreferredFirst(
				latestEvent.getProducts("testproduct"));
		Assert.assertTrue(
				"us authoritative after update",
				latestEvent.getEventId().equals("usone"));
		Assert.assertEquals("us is still preferred after update",
				latestProducts.get(0).getSource(), "us");
		Assert.assertEquals("us still has trump preferred weight",
				latestProducts.get(0).getPreferredWeight(),
				Indexer.TRUMP_PREFERRED_WEIGHT);

		// send second trump to make sc more preferred
		ProductId trump2Id = new ProductId("admin", "trump-testproduct",
				"sc-testproduct-uno");
		Product trump2 = new Product(trump2Id);
		trump2.getProperties().put("trump-source", product2Id.getSource());
		trump2.getProperties().put("trump-code", product2Id.getCode());
		trump2.setTrackerURL(url);
		trump2.setEventSource(product2.getEventSource());
		trump2.setEventSourceCode(product2.getEventSourceCode());

		indexer.onProduct(trump2);
		synchronized (syncObject) {
			syncObject.wait();
		}

		latestEvent = getLatestIndexerEvent().getEvents().get(0);
		latestProducts = Event.getSortedMostPreferredFirst(
				latestEvent.getProducts("testproduct"));
		Assert.assertTrue("ci authoritative after second trump",
				latestEvent.getEventId().equals("ciuno"));
		Assert.assertEquals("ci is preferred after trump",
				latestProducts.get(0).getSource(), "ci");
		Assert.assertEquals("ci has trump preferred weight",
				latestProducts.get(0).getPreferredWeight(),
				Indexer.TRUMP_PREFERRED_WEIGHT);
		Assert.assertEquals("us product has original preferred weight",
				latestProducts.get(1).getPreferredWeight(),
				originalPreferredWeight);

		// delete second trump to once again make us more preferred
		trump2.getId().setUpdateTime(new Date());
		trump2.setStatus(Product.STATUS_DELETE);
		indexer.onProduct(trump2);
		synchronized (syncObject) {
			syncObject.wait();
		}

		latestEvent = getLatestIndexerEvent().getEvents().get(0);
		latestProducts = Event.getSortedMostPreferredFirst(
				latestEvent.getProducts("testproduct"));
		Assert.assertTrue("us authoritative after deleting second trump",
				latestEvent.getEventId().equals("usone"));
		Assert.assertEquals("us is preferred after deleting second trump",
				latestProducts.get(0).getSource(), "us");
		Assert.assertEquals("us has trump preferred weight after deleting second trump",
				latestProducts.get(0).getPreferredWeight(),
				Indexer.TRUMP_PREFERRED_WEIGHT);
		Assert.assertNotSame("ci product has regular preferred weight",
				latestProducts.get(1).getPreferredWeight(),
				Indexer.TRUMP_PREFERRED_WEIGHT);

		// delete trump to make us less preferred
		trump.getId().setUpdateTime(new Date());
		// don't send eventsource/eventsourcecode
		trump.setStatus(Product.STATUS_DELETE);
		indexer.onProduct(trump);
		synchronized (syncObject) {
			syncObject.wait();
		}

		latestEvent = getLatestIndexerEvent().getEvents().get(0);
		latestProducts = Event.getSortedMostPreferredFirst(
				latestEvent.getProducts("testproduct"));
		Assert.assertTrue(
				"ci authoritative after deleting trump",
				latestEvent.getEventId().equals("ciuno"));
		Assert.assertEquals("testproduct has original preferred weight",
				latestEvent.getProducts("testproduct").get(1).getPreferredWeight(),
				originalPreferredWeight);

	}
}
