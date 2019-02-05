package gov.usgs.earthquake.distribution;

import java.io.File;
import java.net.URL;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.ProductTest;
import java.sql.Connection;
import java.util.Date;
import java.util.List;

import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.JDBCUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ProductClientTest {

	private ProductClient client;
	private Config config;
	private String indexfile;

	/**
	 * Set the configuration options for the test environment.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setupClient() throws Exception {
		indexfile = "test_index.db";
		client = new ProductClient();
		config = new Config();

		String receiverName = "myTestReceiver";
		config.setProperty(ProductClient.RECEIVERS_PROPERTY_NAME, receiverName);
		config.setSectionProperty(receiverName, "type",
				DefaultNotificationReceiver.class.getCanonicalName());

		String storageName = "myTestStorage";
		config.setSectionProperty(receiverName, "storage", storageName);
		config.setSectionProperty(storageName, "type",
				FileProductStorage.class.getName());

		String indexName = "myTestIndex";
		config.setSectionProperty(receiverName, "index", indexName);
		config.setSectionProperty(indexName, "type",
				JDBCNotificationIndex.class.getName());
		config.setSectionProperty(indexName,
				JDBCNotificationIndex.JDBC_FILE_PROPERTY, indexfile);

		String listenerName = "myTestListener";
		config.setProperty(ProductClient.LISTENERS_PROPERTY_NAME, listenerName);
		config.setSectionProperty(listenerName, "type",
				TestListener.class.getName());

		Config.setConfig(config);
		client.configure(Config.getConfig());
	}

	/**
	 * Make sure that the configuration values setup in setupClient() create the
	 * proper receivers and listeners.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProductClientConfig() throws Exception {
		// Make sure we created 1 receiver
		List<NotificationReceiver> receivers = client.getReceivers();
		Assert.assertEquals(1, receivers.size());

		// Make sure we created 1 listener
		List<NotificationListener> listeners = client.getListeners();
		Assert.assertEquals(1, listeners.size());
	}

	/**
	 * Setup a test JDBCNotificationIndex, create a test notification, "send"
	 * it, and see if it gets to the listener If you don't have a tracker
	 * running on localhost, you will see errors about connecting to the
	 * tracker. These do not invalidate the test.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testClientReceivedNotification() throws Exception {

		// Before the client can be started, we have to setup the database
		// Make sure we always start with a clean index file.
		File f = new File(indexfile);
		if (f.exists()) {
			f.delete();
		}

		// Create a separate SQL connection to audit our progress
		Connection connection = JDBCUtils.getSqliteConnection(f);

		// Create an empty index db file
		connection.createStatement().executeUpdate(
				"CREATE TABLE notification_index ( "
						+ "id INTEGER PRIMARY KEY NOT NULL, "
						+ "product_source TEXT NOT NULL, "
						+ "product_type TEXT NOT NULL, "
						+ "product_code TEXT NOT NULL, "
						+ "product_update LONG NOT NULL, "
						+ "expiration_date LONG NOT NULL, "
						+ "tracker_url TEXT NOT NULL, " + "product_url TEXT "
						+ ")");
		connection.createStatement().executeUpdate(
				"CREATE TABLE tmp_lookup_table (product_source TEXT, "
						+ "product_type TEXT, " + "product_code TEXT)");
		connection.createStatement().executeUpdate(
				"CREATE TABLE notification_queue ( " +
				"  id INTEGER PRIMARY KEY NOT NULL," +
				"  queue_name TEXT NOT NULL," +
				"  product_source TEXT NOT NULL," +
				"  product_type TEXT NOT NULL," +
				"  product_code TEXT NOT NULL," +
				"  product_update LONG NOT NULL" +
				")");

		client.startup();

		ProductTest productTest = new ProductTest();
		Product product1 = productTest.getProduct();

		URL url = new URL("http://earthquake.usgs.gov/");
		Date expires = new Date(System.currentTimeMillis() + 500000);
		DefaultNotification notification = new DefaultNotification(
				product1.getId(), expires, url);

		client.getReceivers().get(0).receiveNotification(notification);

		ProductId lastId = ((TestListener) client.getListeners().get(0))
				.getLastProductId();

		System.out.println("Last message id: " + lastId);

		Assert.assertEquals(product1.getId(), lastId);

		client.shutdown();
	}

	/**
	 * NOTE: This has been moved to Bootstrap, and is no longer tested.
	 * 
	 * Test the static method ProductClient.loadConfig(configFile) by loading
	 * the example client configuration and making sure the loaded values are
	 * correct.
	 * 
	 * @throws Exception
	 */
	public void testClientLoadConfig() throws Exception {
		String configPath = "etc/examples/client/conf/config.ini";
		File configFile = new File(configPath);
		if (configFile.exists()) {
			// ProductClient.loadConfig(configFile);
			config = Config.getConfig();

			// Make sure the loaded values were correct
			// Not every value is checked. I assume that if one property works,
			// they all should
			Assert.assertEquals("listener_exec",
					config.getProperty("listeners"));
			Assert.assertEquals(
					"gov.usgs.earthquake.distribution.ExternalNotificationListener",
					config.getSectionProperty("listener_exec", "type"));
			Assert.assertEquals(
					"gov.usgs.earthquake.distribution.FileProductStorage",
					config.getSectionProperty("listener_storage", "type"));

		} else {
			System.out.println("Could not load example client config file");
			Assert.assertFalse(true);
		}
	}

	/**
	 * A dummy listener with no functionality beyond giving the ProductId of the
	 * last Product sent to this listener.
	 * 
	 * @author sdaugherty
	 * 
	 */
	public static class TestListener extends DefaultConfigurable implements
			NotificationListener {

		private ProductId lastProductId;

		public synchronized void onNotification(NotificationEvent event)
				throws Exception {
			lastProductId = event.getNotification().getProductId();
			System.out.println("\nNEW MESSAGE\n");
			notifyAll();
		}

		public void configure(Config config) throws Exception {

		}

		public void shutdown() throws Exception {

		}

		public void startup() throws Exception {

		}

		public synchronized ProductId getLastProductId() {
			if (lastProductId == null) {
				try {
					wait();
				} catch (Exception e) {
					// Ignore
				}
			}
			return lastProductId;
		}

		@Override
		public int getMaxTries() {
			return 1;
		}

		@Override
		public long getTimeout() {
			return 0;
		}

	}

}
