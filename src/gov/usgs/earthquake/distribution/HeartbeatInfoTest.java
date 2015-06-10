package gov.usgs.earthquake.distribution;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class HeartbeatInfoTest {

	/**
	 * Heartbeat Info timestamp should have expired
	 * 
	 * @throws Exception
	 */

	@Test
	public void testHeartbeatInfoExpired() throws Exception {
		long purgeMicroSec = 10000L;

		try {
			HeartbeatInfo obj = new HeartbeatInfo("test message", new Date());

			// Set purge time to earlier than map entries
			Date purgeDate = new Date(new Date().getTime() + purgeMicroSec);

			obj.isExpired(purgeDate);

			Assert.assertEquals("Expired verification", true,
					obj.isExpired(purgeDate));
			System.out.println("Test successful: HeartbeatInfo expired");

		} catch (Exception e) {
			System.out.println(e.getMessage());
			Assert.fail("Should have had a successful object creation and data storage");
		}
	}

	/**
	 * Heartbeat Info timestamp should NOT have expired
	 * 
	 * @throws Exception
	 */

	@Test
	public void testHeartbeatInfoNotExpired() throws Exception {
		long purgeMicroSec = 10000L;

		try {
			HeartbeatInfo obj = new HeartbeatInfo("test message", new Date());

			// Set purge time to earlier than map entries
			Date purgeDate = new Date(new Date().getTime() - purgeMicroSec);

			obj.isExpired(purgeDate);

			Assert.assertEquals("Expired verification", false,
					obj.isExpired(purgeDate));
			System.out.println("Test successful: HeartbeatInfo NOT expired");

		} catch (Exception e) {
			System.out.println(e.getMessage());
			Assert.fail("Should have had a successful object creation and data storage");
		}
	}

}
