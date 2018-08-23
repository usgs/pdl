package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.JDBCUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JDBCNotificationIndexTest {

	// ------------------------------------------------------------------------
	// Static member variables. These include the notifications etc...
	// ------------------------------------------------------------------------

	public static URL TRACKER_URL = null;

	public static DefaultNotification TEST_DEFAULT_NOTIFICATION = null;
	public static URLNotification TEST_URL_NOTIFICATION = null;
	public static List<Notification> EXPIRED_NOTIFICATIONS = null;
	public static List<Notification> SOME_NOTIFICATIONS = null;

	// ------------------------------------------------------------------------
	// Private member variables.
	// ------------------------------------------------------------------------
	private Connection connection = null;
	private JDBCNotificationIndex index = null;
	private String indexfile = null;

	private PreparedStatement _query_allNotifications = null;

	// ------------------------------------------------------------------------
	// Startup/Cleanup methods
	// ------------------------------------------------------------------------

	static {
		try {
			// The tracker URL used for testing. NOT A REAL URL.
			TRACKER_URL = new URL("http://earthquake.usgs.gov/tracker");

			// Notification used to test the handling of a DefaultNotification
			TEST_DEFAULT_NOTIFICATION = new DefaultNotification(new ProductId(
					"default", "notification", "us2009wxyz"), new Date(
					(new Date().getTime() + 1000000)), // In the future
					TRACKER_URL);

			// Notification used to test the handling of a URLNotification
			TEST_URL_NOTIFICATION = new URLNotification(new ProductId("url",
					"notification", "us2009abcd"), new Date((new Date())
					.getTime() + 1000001), // In the future
					TRACKER_URL, new URL(
							"http://earthquake.usgs.gov/pager/us2009abcd"));

			// A bunch of expired notifications
			EXPIRED_NOTIFICATIONS = new ArrayList<Notification>();
			for (int i = 0; i < 10; ++i) {
				EXPIRED_NOTIFICATIONS.add(createExpiredNotification());
			}

			SOME_NOTIFICATIONS = new ArrayList<Notification>();
			SOME_NOTIFICATIONS.add(createRandomNotification("us", "pager"));
			SOME_NOTIFICATIONS.add(createRandomNotification("us", "pager"));
			SOME_NOTIFICATIONS.add(createRandomNotification("us", "pager"));
			SOME_NOTIFICATIONS.add(createRandomNotification("us", "shakemap"));
			SOME_NOTIFICATIONS.add(createRandomNotification("us", "shakemap"));
			SOME_NOTIFICATIONS.add(createRandomNotification("us", "shakemap"));
			SOME_NOTIFICATIONS.add(createRandomNotification("us", "dyfi"));
			SOME_NOTIFICATIONS.add(createRandomNotification("us", "dyfi"));
			SOME_NOTIFICATIONS.add(createRandomNotification("us", "dyfi"));

			SOME_NOTIFICATIONS.add(createRandomNotification("nc", "pager"));
			SOME_NOTIFICATIONS.add(createRandomNotification("nc", "pager"));
			SOME_NOTIFICATIONS.add(createRandomNotification("nc", "pager"));
			SOME_NOTIFICATIONS.add(createRandomNotification("nc", "shakemap"));
			SOME_NOTIFICATIONS.add(createRandomNotification("nc", "shakemap"));
			SOME_NOTIFICATIONS.add(createRandomNotification("nc", "shakemap"));
			SOME_NOTIFICATIONS.add(createRandomNotification("nc", "dyfi"));
			SOME_NOTIFICATIONS.add(createRandomNotification("nc", "dyfi"));
			SOME_NOTIFICATIONS.add(createRandomNotification("nc", "dyfi"));

			SOME_NOTIFICATIONS.add(createRandomNotification("ci", "pager"));
			SOME_NOTIFICATIONS.add(createRandomNotification("ci", "pager"));
			SOME_NOTIFICATIONS.add(createRandomNotification("ci", "pager"));
			SOME_NOTIFICATIONS.add(createRandomNotification("ci", "shakemap"));
			SOME_NOTIFICATIONS.add(createRandomNotification("ci", "shakemap"));
			SOME_NOTIFICATIONS.add(createRandomNotification("ci", "shakemap"));
			SOME_NOTIFICATIONS.add(createRandomNotification("ci", "dyfi"));
			SOME_NOTIFICATIONS.add(createRandomNotification("ci", "dyfi"));
			SOME_NOTIFICATIONS.add(createRandomNotification("ci", "dyfi"));

			SOME_NOTIFICATIONS.add(new DefaultNotification(new ProductId(
					"random", "test", "2009abcd"), new Date((new Date())
					.getTime() + 1000000), TRACKER_URL));
			SOME_NOTIFICATIONS.add(new DefaultNotification(new ProductId(
					"random", "test", "2009efgh"), new Date((new Date())
					.getTime() + 1000000), TRACKER_URL));
			SOME_NOTIFICATIONS.add(new DefaultNotification(new ProductId(
					"random", "test", "2009ijkl"), new Date((new Date())
					.getTime() + 1000000), TRACKER_URL));

		} catch (MalformedURLException mfe) {
			System.err.println(mfe.getMessage());
			System.exit(-1);
		}
	}

	/**
	 * Creates a dummy notification index with sample notifications etc. This is
	 * used for the testing environment and is run before the tests themselves.
	 */
	@Before
	public void setupEnvironment() {
		try {
			String t = System.getProperty("user.dir"); // CWD-ish
			String s = System.getProperty("file.separator"); // "/" or "\"
			indexfile = t + s + "test-index.db";

			Config c = new Config();
			c.setProperty(JDBCNotificationIndex.JDBC_FILE_PROPERTY, indexfile);

			// Make sure we always start with a clean index file.
			File f = new File(indexfile);
			if (f.exists()) {
				f.delete();
			}

			// Create a separate SQL connection to audit our progress
			connection = JDBCUtils.getSqliteConnection(f);

			// Create an empty index db file
			connection.createStatement().executeUpdate(
					"CREATE TABLE notification_index ( "
							+ "id INTEGER PRIMARY KEY NOT NULL, "
							+ "product_source TEXT NOT NULL, "
							+ "product_type TEXT NOT NULL, "
							+ "product_code TEXT NOT NULL, "
							+ "product_update LONG NOT NULL, "
							+ "expiration_date LONG NOT NULL, "
							+ "tracker_url TEXT NOT NULL, "
							+ "product_url TEXT " + ")");
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


			_query_allNotifications = connection
					.prepareStatement("SELECT * FROM notification_index");

			// Create the index
			index = new JDBCNotificationIndex(c);
			index.startup();

			// Add the sample notifications
			for (int i = 0; i < SOME_NOTIFICATIONS.size(); ++i) {
				if (SOME_NOTIFICATIONS.get(i) == null) {
					System.err.println("WTF, A NULL NOTIFICATION? (" + i + ")");
				}
				index.addNotification(SOME_NOTIFICATIONS.get(i));
			}

			// Add the expired notifications
			for (int i = 0; i < EXPIRED_NOTIFICATIONS.size(); ++i) {
				index.addNotification(EXPIRED_NOTIFICATIONS.get(i));
			}

			Assert.assertEquals(SOME_NOTIFICATIONS.size()
					+ EXPIRED_NOTIFICATIONS.size(), getIndexSize());

		} catch (Exception ex) {
			// Just print any errors
			System.err
					.println("JDBCNotificationIndexTest::setupEnvironment -- "
							+ ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * Cleans up the dummy environment after running all tests.
	 */
	@After
	public void cleanupEnvironment() {
		try {

			// Delete the index file
			(new File(indexfile)).delete();

			// Close the audit SQL connection
			connection.close();

			// Shutdown the index (per the API)
			index.shutdown();

		} catch (Exception ex) {
			// Just print any errors
			System.err.println("JDBCNotificationIndexTest::cleanupEnvironment"
					+ " -- " + ex.getMessage());
			ex.printStackTrace();
		}

	}

	// ------------------------------------------------------------------------
	// Method tests. There are one or more tests for each individual method
	// in the Notification interface. Not testing (at this time) the
	// implementation
	// ------------------------------------------------------------------------

	/**
	 * Tests adding valid notifications to the JDBCNotificationIndex.
	 */
	@Test
	public void testAddNotifications() throws Exception {
		index.addNotification(TEST_DEFAULT_NOTIFICATION);
		Assert.assertTrue(exists(TEST_DEFAULT_NOTIFICATION));

		index.addNotification(TEST_URL_NOTIFICATION);
		Assert.assertTrue(exists(TEST_URL_NOTIFICATION));

		Assert.assertEquals(SOME_NOTIFICATIONS.size()
				+ EXPIRED_NOTIFICATIONS.size() + 2, getIndexSize());
	}

	/**
	 * Tests removal of notifications from the JDBCNotificationIndex.
	 */
	@Test
	public void testRemoveNotifications() throws Exception {
		index.addNotification(TEST_DEFAULT_NOTIFICATION);
		index.addNotification(TEST_URL_NOTIFICATION);

		Assert.assertTrue(exists(TEST_DEFAULT_NOTIFICATION));
		index.removeNotification(TEST_DEFAULT_NOTIFICATION);
		Assert.assertFalse(exists(TEST_DEFAULT_NOTIFICATION));

		Assert.assertTrue(exists(TEST_URL_NOTIFICATION));
		index.removeNotification(TEST_URL_NOTIFICATION);
		Assert.assertFalse(exists(TEST_URL_NOTIFICATION));
	}

	/**
	 * Tests removal of expired notifications.
	 */
	@Test
	public void testRemoveExpiredNotifications() throws Exception {
		List<Notification> found = index.findExpiredNotifications();

		for (int i = 0; i < EXPIRED_NOTIFICATIONS.size(); ++i) {
			Assert.assertTrue(contains(found, EXPIRED_NOTIFICATIONS.get(i)));
		}
	}

	/**
	 * Tests the find notification by ProductId.
	 */
	@Test
	public void testFindNotificationById() throws Exception {

		for (int i = 0; i < SOME_NOTIFICATIONS.size(); ++i) {
			ProductId id = SOME_NOTIFICATIONS.get(i).getProductId();
			List<Notification> found = index.findNotifications(id);

			for (int j = 0; j < found.size(); ++j) {
				Assert.assertEquals(id, found.get(j).getProductId());
			}
		}

	}

	/**
	 * Tests the find notification by discrete field data.
	 */
	@Test
	public void testFindNotificationsByData() throws Exception {
		List<Notification> found = null;

		// Find all the "pager" notifications
		found = index.findNotifications(null, "pager", null);
		Assert.assertEquals(9, found.size());

		// Find all the "shakemap" notifications
		found = index.findNotifications(null, "shakemap", null);
		Assert.assertEquals(9, found.size());

		// Find all the "dyfi" notifications
		found = index.findNotifications(null, "dyfi", null);
		Assert.assertEquals(9, found.size());

		// Find all the "us" notifications
		found = index.findNotifications("us", null, null);
		Assert.assertEquals(9, found.size());

		// Find all the "nc" notifications
		found = index.findNotifications("nc", null, null);
		Assert.assertEquals(9, found.size());

		// Find all the "ci" notifications
		found = index.findNotifications("ci", null, null);
		Assert.assertEquals(9, found.size());

		// Find all "us-pager" events
		found = index.findNotifications("us", "pager", null);
		Assert.assertEquals(3, found.size());

		// Find all "us-shakemap" events
		found = index.findNotifications("us", "shakemap", null);
		Assert.assertEquals(3, found.size());

		// Find all "us-dyfi" events
		found = index.findNotifications("us", "dyfi", null);
		Assert.assertEquals(3, found.size());

		// Find all "nc-pager" events
		found = index.findNotifications("nc", "pager", null);
		Assert.assertEquals(3, found.size());

		// Find all "nc-shakemap" events
		found = index.findNotifications("nc", "shakemap", null);
		Assert.assertEquals(3, found.size());

		// Find all "nc-dyfi" events
		found = index.findNotifications("nc", "dyfi", null);
		Assert.assertEquals(3, found.size());

		// Find all "ci-pager" events
		found = index.findNotifications("ci", "pager", null);
		Assert.assertEquals(3, found.size());

		// Find all "ci-shakemap" events
		found = index.findNotifications("ci", "shakemap", null);
		Assert.assertEquals(3, found.size());

		// Find all "ci-dyfi" events
		found = index.findNotifications("ci", "dyfi", null);
		Assert.assertEquals(3, found.size());

		// Find a specific notification
		found = index.findNotifications("random", "test", "2009abcd");
		Assert.assertEquals(1, found.size());

		// Find a specific notification
		found = index.findNotifications("random", "test", "2009efgh");
		Assert.assertEquals(1, found.size());

		// Find a specific notification
		found = index.findNotifications("random", "test", "2009ijkl");
		Assert.assertEquals(1, found.size());

	}

	/**
	 * Tests the list form of findNotificationsByData
	 */
	@Test
	public void testFindNotificationsByDataLists() throws Exception {
		// This is not a robust test yet, but it does have some basic
		// functionality.

		List<Notification> found = null;
		Iterator<Notification> iter = null;
		List<String> sources = null;
		List<String> types = null;
		List<String> codes = null;

		// Check the null null null case.
		found = index.findNotifications(sources, types, codes);

		List<Notification> all = index
				.getNotifications(_query_allNotifications);
		iter = all.iterator();
		while (iter.hasNext()) {
			Assert.assertTrue(contains(found, iter.next()));
		}

		// Check a search by sources only
		sources = new ArrayList<String>();
		// Should be "us"
		sources.add(SOME_NOTIFICATIONS.get(0).getProductId().getSource());
		found = index.findNotifications(sources, types, codes);
		Assert.assertEquals(9, found.size());

		// Check a search by sources and types only
		types = new ArrayList<String>();
		// Should be "pager" and "shakemap"
		types.add(SOME_NOTIFICATIONS.get(2).getProductId().getType());
		types.add(SOME_NOTIFICATIONS.get(3).getProductId().getType());
		found = index.findNotifications(sources, types, codes);
		Assert.assertEquals(6, found.size());

		// Check a search by sources, types, and codes
		codes = new ArrayList<String>();
		// Should be codes for index 2, 3. These are random but that shouldn't
		// matter
		codes.add(SOME_NOTIFICATIONS.get(2).getProductId().getCode());
		codes.add(SOME_NOTIFICATIONS.get(3).getProductId().getCode());
		found = index.findNotifications(sources, types, codes);
		Assert.assertEquals(2, found.size());
		Assert.assertTrue(contains(found, SOME_NOTIFICATIONS.get(2)));
		Assert.assertTrue(contains(found, SOME_NOTIFICATIONS.get(3)));

		// Check a search by types and codes
		found = index.findNotifications(null, types, codes);
		Assert.assertEquals(2, found.size());

		// Check a search by types only
		found = index.findNotifications(null, types, null);
		Assert.assertEquals(18, found.size());

		// Check a search by codes only
		found = index.findNotifications(null, null, codes);
		Assert.assertTrue(contains(found, SOME_NOTIFICATIONS.get(2)));
		Assert.assertTrue(contains(found, SOME_NOTIFICATIONS.get(3)));
	}

	@Test
	public void testThreadSafety() throws Exception {

		// Create two threads that will perform the same task.
		SimpleInsertThread t1 = new SimpleInsertThread();
		SimpleInsertThread t2 = new SimpleInsertThread();

		// Now try to execute both threads at once (or nearly "at once").
		t1.start();
		t2.start();

		// Wait for the inserts to finish
		t1.join();
		t2.join();

		Assert.assertEquals(SOME_NOTIFICATIONS.size()
				+ EXPIRED_NOTIFICATIONS.size() + 4, getIndexSize());
	}

	// ------------------------------------------------------------------------
	// Helper/Utility Methods
	// ------------------------------------------------------------------------
	private boolean exists(Notification n) throws Exception {
		List<Notification> idx = index
				.getNotifications(_query_allNotifications);

		return contains(idx, n);
	}

	private boolean contains(List<Notification> haystack, Notification needle) {
		for (int i = 0; i < haystack.size(); ++i) {
			if (haystack.get(i).equals(needle)) {
				return true;
			}
		}
		return false;
	}

	private int getIndexSize() throws Exception {
		int i = 0;
		ResultSet rs = connection.createStatement().executeQuery(
				"SELECT * FROM notification_index");

		while (rs.next()) {
			++i;
		}
		rs.close();

		return i;
	}

	private static Notification createExpiredNotification() {

		Notification n;
		Date d = new Date((new Date()).getTime() - 1);
		ProductId id = new ProductId("test", "expired", String.valueOf(Math
				.rint(Math.random() * 1000000)));

		// Just a "randomization" to sometimes create URL vs. Default types
		if ((d.getTime() % 2) == 0) {
			// Even; so create a URL type
			try {
				n = new URLNotification(id, d, TRACKER_URL, new URL(
						"http://earthquake.usgs.gov/expired/" + id.getCode()));
			} catch (MalformedURLException mfe) {
				// Oh well, it doesn't really matter, just use a default
				// notification instead.
				n = new DefaultNotification(id, d, TRACKER_URL);
			}
		} else {
			// Odd; so create a default type
			n = new DefaultNotification(id, d, TRACKER_URL);
		}

		return n;
	}

	private static Notification createRandomNotification(String source,
			String type) {

		Notification n = null;
		ProductId id = new ProductId(source, type, String.valueOf(Math
				.rint(Math.random() * 1000000)));
		String url = "http://earthquake.usgs.gov/pdtester/" + id.getCode();
		Date d = null;

		try {
			long time = (long) (new Date()).getTime()
					+ (long) Math.rint(Math.random() * 1000000000);
			d = new Date(time);

			if ((time % 2) == 0) {
				n = new URLNotification(id, d, TRACKER_URL, new URL(url));
			} else {
				n = new DefaultNotification(id, d, TRACKER_URL);
			}

		} catch (MalformedURLException mfe) {
			n = new DefaultNotification(id, d, TRACKER_URL);
		}

		return n;
	}

	private class SimpleInsertThread extends Thread {
		public void run() {
			try {
				index.addNotification(TEST_DEFAULT_NOTIFICATION);
				Assert.assertTrue(exists(TEST_DEFAULT_NOTIFICATION));
				index.addNotification(TEST_URL_NOTIFICATION);
				Assert.assertTrue(exists(TEST_URL_NOTIFICATION));
			} catch (Exception e) {
				// We blew it
				e.printStackTrace();
			}
		}
	}
}
