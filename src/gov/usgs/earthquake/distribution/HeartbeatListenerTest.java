package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.distribution.HeartbeatListener;
import gov.usgs.util.Config;
import org.junit.Assert;
import org.junit.Test;

public class HeartbeatListenerTest {


	/**
	 * JSON Heartbeat Listener test should pass
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJSONHeartbeatListener() throws Exception {
		HeartbeatListener objListener;
		
		try {
			Config config = new Config();
			config.setProperty("heartbeatFilename", "heartbeat.dat");
			config.setProperty("heartbeatTimeout", "1800000");
			config.setProperty("cleanupInterval", "0");

			objListener = new HeartbeatListener();
			objListener.configure(config);

			HeartbeatListener.sendHeartbeatMessage("TestComponent1", "key11",
					"value11");
			HeartbeatListener.sendHeartbeatMessage("TestComponent1", "key12",
					"value12");
			HeartbeatListener.sendHeartbeatMessage("TestComponent1", "key13",
					"value13");
			HeartbeatListener.sendHeartbeatMessage("TestComponent2", "key21",
					"value21");
			HeartbeatListener.sendHeartbeatMessage("TestComponent2", "key22",
					"value22");
			HeartbeatListener.sendHeartbeatMessage("TestComponent2", "key23",
					"value23");

			String stringJSON = objListener.formatHeartbeatOutput();

			Assert.assertTrue("Non-empty JSON string", !stringJSON.isEmpty());
			System.out.println("Test sendHeartbeatMessage and JSON="
					+ stringJSON);
			objListener.shutdown();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			Assert.fail("Should have had successful data storage and JSON output");
		}
	}

	/**
	 * Write Heartbeat File test should pass
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteHeartbeat() throws Exception {
		HeartbeatListener objListener;
		
		try {

			Config config = new Config();
			config.setProperty("heartbeatFilename", "heartbeat.dat");
			config.setProperty("heartbeatTimeout", "1800000");
			config.setProperty("cleanupInterval", "0");

			objListener = new HeartbeatListener();
			objListener.configure(config);

			HeartbeatListener.sendHeartbeatMessage("TestComponent3", "key31",
					"value31");
			HeartbeatListener.sendHeartbeatMessage("TestComponent3", "key32",
					"value32");
			HeartbeatListener.sendHeartbeatMessage("TestComponent3", "key33",
					"value33");
			HeartbeatListener.sendHeartbeatMessage("TestComponent4", "key41",
					"value41");
			HeartbeatListener.sendHeartbeatMessage("TestComponent4", "key42",
					"value42");
			HeartbeatListener.sendHeartbeatMessage("TestComponent4", "key43",
					"value43");
			
			objListener.writeHeartbeat();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			Assert.fail("Should have had successful data storage and heartbeat.dat file output");
		}
	}

	/**
	 * Manual clear of heartbeat data test should pass
	 * 
	 * @throws Exception
	 */
	@Test
	public void testManualClearHeartbeat() throws Exception {
		HeartbeatListener objListener;
		
		long delayMillSec = 1000L;
		long delayTimeEnd = System.currentTimeMillis() + delayMillSec;

		try {
			Config config = new Config();
			config.setProperty("heartbeatFilename", "heartbeat.dat");
			config.setProperty("heartbeatTimeout", "100");
			config.setProperty("cleanupInterval", "1000");

			objListener = new HeartbeatListener();
			objListener.configure(config);

			HeartbeatListener.sendHeartbeatMessage("TestComponent3", "key31",
					"value31");
			HeartbeatListener.sendHeartbeatMessage("TestComponent3", "key32",
					"value32");
			HeartbeatListener.sendHeartbeatMessage("TestComponent3", "key33",
					"value33");
			HeartbeatListener.sendHeartbeatMessage("TestComponent4", "key41",
					"value41");
			HeartbeatListener.sendHeartbeatMessage("TestComponent4", "key42",
					"value42");
			HeartbeatListener.sendHeartbeatMessage("TestComponent4", "key43",
					"value43");

			// delay test 1 second to "age" storage
			while (System.currentTimeMillis() < delayTimeEnd) {
			}

			objListener.cleanup();
			String stringJSON = objListener.formatHeartbeatOutput();

			Assert.assertTrue("Heartbeat data cleared", stringJSON.length() < 3);
			System.out.println("Manual test heartbeat data cleared, JSON="
					+ stringJSON);

		} catch (Exception e) {
			System.out.println(e.getMessage());
			Assert.fail("Should have had successful data storage and heartbeat.dat file output");
		}
	}

	/**
	 * Automatic clear of heartbeat data test should pass
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAutoClearHeartbeat() throws Exception {
		HeartbeatListener objListener;
		
		long delayMillSec = 5000L;
		long delayTimeEnd = System.currentTimeMillis() + delayMillSec;

		try {
			Config config = new Config();
			config.setProperty("heartbeatFilename", "heartbeat.dat");
			config.setProperty("heartbeatTimeout", "100");
			config.setProperty("cleanupInterval", "1000");

			objListener = new HeartbeatListener();
			objListener.configure(config);
			objListener.startup();

			HeartbeatListener.sendHeartbeatMessage("TestComponent3", "key31",
					"value31");
			HeartbeatListener.sendHeartbeatMessage("TestComponent3", "key32",
					"value32");
			HeartbeatListener.sendHeartbeatMessage("TestComponent3", "key33",
					"value33");
			HeartbeatListener.sendHeartbeatMessage("TestComponent4", "key41",
					"value41");
			HeartbeatListener.sendHeartbeatMessage("TestComponent4", "key42",
					"value42");
			HeartbeatListener.sendHeartbeatMessage("TestComponent4", "key43",
					"value43");

			// delay test 1 second to "age" storage
			while (System.currentTimeMillis() < delayTimeEnd) {
			}

			// objListener.cleanup();
			String stringJSON = objListener.formatHeartbeatOutput();

			Assert.assertTrue("Heartbeat data cleared", stringJSON.length() < 3);
			System.out.println("Test scheduled heartbeat data clearing, JSON="
					+ stringJSON);

		} catch (Exception e) {
			System.out.println(e.getMessage());
			Assert.fail("Should have had successful data storage and heartbeat.dat file output");
		}
	}

}
