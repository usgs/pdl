package gov.usgs.earthquake.distribution;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class HeartbeatStatusTest {

	/** Representation of internal map within HeartbeatStatus */
	private Map<String, HeartbeatInfo> hashValues;

	/** Iterator for HeartbeatStatus internal map */
	Iterator<String> itHeartbeat;

	/** work field key */
	String key;

	/** work field key */
	HeartbeatInfo value;

	/** work field date */
	Date date;

	/**
	 * Basic Heartbeat Status test should pass
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBasicHeartbeatStatus() throws Exception {

		try {
			HeartbeatStatus obj = new HeartbeatStatus();
			obj.updateStatus("key1", "value1");
			obj.updateStatus("key2", "value2");
			obj.updateStatus("key3", "value3");

			hashValues = obj.getStatuses();
			itHeartbeat = hashValues.keySet().iterator();

			while (itHeartbeat.hasNext()) {
				key = itHeartbeat.next();
				value = hashValues.get(key);

				if (key.contentEquals("key1")) {
					Assert.assertEquals("Value verification", "value1",
							value.getMessage());
					System.out
							.println("Test storage/retrieval value1="
									+ value.getMessage());
				}

				if (key.contentEquals("key2")) {
					Assert.assertEquals("Value verification", "value2",
							value.getMessage());
					System.out
							.println("Test storage/retrieval value2="
									+ value.getMessage());
				}

				if (key.contentEquals("key3")) {
					Assert.assertEquals("Value verification", "value3",
							value.getMessage());
					System.out
							.println("Test storage/retrieval value3="
									+ value.getMessage());
				}

			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			Assert.fail("Should have had a successful object creation and data storage");
		}
	}

	/**
	 * Heartbeat Date-Time basic test
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBasicHeartbeatTime() throws Exception {

		try {
			HeartbeatStatus obj = new HeartbeatStatus();
			obj.updateStatus("key1", "value1");
			obj.updateStatus("key2", "value2");
			obj.updateStatus("key3", "value3");

			hashValues = obj.getStatuses();
			itHeartbeat = hashValues.keySet().iterator();

			while (itHeartbeat.hasNext()) {
				key = itHeartbeat.next();
				date = hashValues.get(key).getDate();

				Assert.assertNotNull("Date verification", date);
				System.out.println("Test Timestamp " + key + "=" + date);
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			Assert.fail("Should have had a successful object creation and data storage"
					);
		}
	}

	/**
	 * purge of old Heartbeat Status test should pass
	 * 
	 * @throws Exception
	 */

	@Test
	public void testPurgeTimeHeartbeatStatus() throws Exception {
		long purgeMicroSec = 500000L;

		try {
			HeartbeatStatus obj = new HeartbeatStatus();
			obj.updateStatus("key1", "value1");
			obj.updateStatus("key2", "value2");
			obj.updateStatus("key3", "value3");

			// Set purge time to later than map entries
			Date purgeDate = new Date(new Date().getTime() + purgeMicroSec);
			obj.clearDataOlderThanDate(purgeDate);

			if (!obj.isEmpty()) {
				Assert.fail("All storage should have purged.");
			} else {
				System.out.println("Test successful: all map entries purged");
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			Assert.fail("Should have had a successful object creation and data storage"
					);
		}
	}

	/**
	 * purge of old Heartbeat Status test should pass
	 * 
	 * @throws Exception
	 */

	@Test
	public void testNonPurgeTimeHeartbeatStatus() throws Exception {
		long purgeMicroSec = 500000L;

		try {
			HeartbeatStatus obj = new HeartbeatStatus();
			obj.updateStatus("key1", "value1");
			obj.updateStatus("key2", "value2");
			obj.updateStatus("key3", "value3");

			// Set purge time to earlier than map entries
			Date purgeDate = new Date(new Date().getTime() - purgeMicroSec);

			obj.clearDataOlderThanDate(purgeDate);
			hashValues = obj.getStatuses();

			if (hashValues.size() != 3) {
				Assert.fail("No storage should have purged = " +
						Integer.toString(hashValues.size()));
			} else {
				System.out.println("Test successful: no map entries purged");
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			Assert.fail("Should have had a successful object creation and data storage"
					);
		}
	}

}
