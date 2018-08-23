package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.distribution.HeartbeatStatus;
import gov.usgs.util.Config;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObjectBuilder;


/**
 * Heartbeat Listener stores heartbeat messages and writes them to a heartbeat
 * file when a product is received
 * 
 * @author tene
 * 
 */
public class HeartbeatListener extends DefaultNotificationListener {

	private static final Logger LOGGER = Logger
			.getLogger(HeartbeatListener.class.getName());

	/** Storage for heartbeat components */
	private static Map<String, HeartbeatStatus> HASH_HEARTBEATS = new HashMap<String, HeartbeatStatus>();

	/** Configurable property for heartbeat fully qualified directory+filename. */
	public static final String HEARTBEAT_FILENAME_PROPERTY = "heartbeatFilename";

	/** Default heartbeat directory. */
	public static final String DEFAULT_HEARTBEAT_FILENAME = "heartbeat.dat";

	/** Default timeout for HeartbeatStatus key/value pairs. Zero = disabled */
	public static final String DEFAULT_STORAGE_TIMEOUT = "0";

	/**
	 * Default schedule interval for HeartbeatStatus key/value pairs cleanup. 30
	 * minutes
	 */
	public static final String DEFAULT_CLEANUP_INTERVAL = "1800000";

	/** Configurable property for heartbeat key/value expiration */
	public static final String HEARTBEAT_TIMEOUT_PROPERTY = "heartbeatTimeout";

	/** Flag listeners are listening */
	private static boolean LISTENING = false;

	/** Hearbeat registered file. */
	private File heartbeatFile;

	/** Timeout for expiration of key/value pairs */
	private long storageTimeout;

	/**
	 * Create a new HeartbeatListener.
	 * 
	 * Sets up the includeTypes list to contain "heartbeat".
	 */
	public HeartbeatListener() throws Exception {
		LISTENING = true;
		heartbeatFile = new File(DEFAULT_HEARTBEAT_FILENAME);
		storageTimeout = Long.valueOf(DEFAULT_STORAGE_TIMEOUT);
		this.getIncludeTypes().add("heartbeat");
	}

	/**
	 * @return map of component and heartbeat status.
	 */
	protected static Map<String, HeartbeatStatus> getHeartbeats() {
		return HASH_HEARTBEATS;
	}

	/**
	 * heartbeat onProduct processing writes to heartbeat file
	 */
	@Override
	public void onProduct(final Product product) throws Exception {

		// track product
		sendHeartbeatMessage(this.getName(), "lastHeartbeat", product.getId()
				.toString());

		// track current memory usage
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
		MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();

		long heapUsed = heapMemory.getUsed();
		long nonHeapUsed = nonHeapMemory.getUsed();
		long heapCommitted = heapMemory.getCommitted();
		long nonHeapCommitted = nonHeapMemory.getCommitted();
		long heapMax = heapMemory.getMax();
		long nonHeapMax = nonHeapMemory.getMax();
		long totalUsed = heapUsed + nonHeapUsed;
		long totalCommitted = heapCommitted + nonHeapCommitted;
		long totalMax = heapMax + nonHeapMax;

		sendHeartbeatMessage(this.getName(), "totalUsed", Long
				.toString(totalUsed));
		sendHeartbeatMessage(this.getName(), "totalCommitted", Long
				.toString(totalCommitted));
		sendHeartbeatMessage(this.getName(), "totalMax", Long
				.toString(totalMax));

		// write heartbeat information to file
		this.writeHeartbeat();

	}

	/**
	 * Send heartbeat data to heartbeat listener
	 * 
	 * @param component
	 * @param key
	 * @param value
	 */
	public static void sendHeartbeatMessage(final String component,
			final String key, final String value) {

		if (!LISTENING) {
			return;
		}
		HeartbeatStatus objHeartbeat;

		String heartbeatKey = component;
		if (heartbeatKey == null) {
			heartbeatKey = "<null>";
		}

		// register this heartbeat in temporary storage
		if (HASH_HEARTBEATS.containsKey(heartbeatKey)) {
			objHeartbeat = HASH_HEARTBEATS.get(heartbeatKey);
		} else {
			objHeartbeat = new HeartbeatStatus();
			HASH_HEARTBEATS.put(heartbeatKey, objHeartbeat);
		}

		// store the heartbeat key/value in temporary storage
		objHeartbeat.updateStatus(key, value);
	}

	/**
	 * Write heartbeat data for all components to the heartbeat file
	 * 
	 * @return true
	 * @throws IOException
	 */
	public boolean writeHeartbeat() throws IOException {
		String tempFileName = heartbeatFile.getName() + "-temp";
		File tempFile = new File(tempFileName);

		gov.usgs.util.FileUtils.writeFileThenMove(tempFile, heartbeatFile, this
				.formatHeartbeatOutput().getBytes());

		return true;
	}

	/**
	 * Self-configure HeartbeatListener object
	 */
	@Override
	public void configure(Config config) throws Exception {
		// let default notification listener configure itself
		super.configure(config);

		heartbeatFile = new File(config.getProperty(
				HEARTBEAT_FILENAME_PROPERTY, DEFAULT_HEARTBEAT_FILENAME));
		LOGGER.config("[" + getName() + "] heartbeat file = "
				+ heartbeatFile.getCanonicalPath());

		storageTimeout = Long.valueOf(config.getProperty(
				HEARTBEAT_TIMEOUT_PROPERTY, DEFAULT_STORAGE_TIMEOUT));
		LOGGER.config("[" + getName() + "] heartbeat timeout = "
				+ storageTimeout + "ms");
	}

	/**
	 * @return JSON-formatted output from the map of components and their values
	 */
	public String formatHeartbeatOutput() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for (String key : HASH_HEARTBEATS.keySet()) {
			HeartbeatStatus status = HASH_HEARTBEATS.get(key);
			builder.add(key, status == null ? null : status.toJsonObject());
		}
		return builder.build().toString();
	}

	/**
	 * purge heartbeat key/values older than storageTimeout, also purging empty
	 * heartbeats
	 */
	public void cleanup() {
		super.cleanup();

		if (this.storageTimeout == 0) {
			return;
		}

		Map<String, HeartbeatStatus> hashHeartbeats = getHeartbeats();
		HeartbeatStatus objHeartbeat;
		String component;
		Iterator<String> itComponents = hashHeartbeats.keySet().iterator();
		Date purgeDate = new Date(new Date().getTime() - this.storageTimeout);

		// iterate through map of components
		while (itComponents.hasNext()) {
			component = itComponents.next();

			// purge old key/value entries within objHeartbeat
			objHeartbeat = hashHeartbeats.get(component);
			objHeartbeat.clearDataOlderThanDate(purgeDate);

			// if purge has left a objHeartheat with no remaining key/value
			// entries,
			// remove the objHeartbeat (i.e. the component)
			if (objHeartbeat.isEmpty()) {
				itComponents.remove();
			}

		} // END while more components

	}

	public File getHeartbeatFile() {
		return heartbeatFile;
	}

	public void setHeartbeatFile(File heartbeatFile) {
		this.heartbeatFile = heartbeatFile;
	}

	public long getStorageTimeout() {
		return storageTimeout;
	}

	public void setStorageTimeout(long storageTimeout) {
		this.storageTimeout = storageTimeout;
	}

}
