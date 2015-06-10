package gov.usgs.earthquake.distribution;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONAware;
import org.json.simple.JSONValue;

/**
 * Heartbeat status information for a single component
 * 
 */
public class HeartbeatStatus implements JSONAware {

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

	@Override
	public String toJSONString() {
		return JSONValue.toJSONString(statuses);
	}

}
