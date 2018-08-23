package gov.usgs.earthquake.distribution;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Heartbeat status information for a single component
 * 
 */
public class HeartbeatStatus {

	private Map<String, HeartbeatInfo> statuses = null;

	/**
	 * Create a new HeartbeatStatus.
	 * 
	 */
	public HeartbeatStatus() {
		statuses = new HashMap<String, HeartbeatInfo>();
	}

	/**
	 * Add or update a Heartbeat's key/value pair
	 * 
	 * @param key
	 * @param value
	 */
	public void updateStatus(String key, String value) {
		statuses.put(key, new HeartbeatInfo(value, new Date()));
	}

	public Map<String, HeartbeatInfo> getStatuses() {
		return statuses;
	}

	public boolean isEmpty() {
		return (statuses.size() == 0);
	}

	/**
	 * Purge all heartbeatStatus data for this component older than given date
	 * 
	 * @param purgeDate
	 */
	public void clearDataOlderThanDate(Date purgeDate) {
		Iterator<String> iterator = statuses.keySet().iterator();
		String key = "";

		// find and purge data older than given purgeDate
		while (iterator.hasNext()) {
			key = iterator.next();
			if (statuses.get(key).isExpired(purgeDate)) {
				iterator.remove();
			}
		}

	}

	/**
	 * @return a JsonObject for output.
	 */
	public JsonObject toJsonObject() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for (String key : statuses.keySet()) {
			builder.add(key, statuses.get(key).toJsonObject());
		}
		return builder.build();
	}

}
