package gov.usgs.earthquake.distribution;

import java.util.Date;
import java.util.HashMap;

import org.json.simple.JSONAware;
import org.json.simple.JSONValue;

/**
 * HeartbeatInfo stores a single heartbeat key/value message, together with a
 * timestamp
 * 
 * @author tene
 * 
 */
public class HeartbeatInfo implements JSONAware {

	private String message = null;
	private Date date = null;

	/**
	 * Message constructor
	 * 
	 * @param message
	 * @param date
	 */
	public HeartbeatInfo(String message, Date date) {
		this.message = message;
		this.date = date;
	}

	/**
	 * @return message contents
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return message timestamp
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Set message content
	 * 
	 * @param message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Set message timestamp
	 * 
	 * @param date
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Test if a message is older than a purgeDate
	 * 
	 * @param purgeDate
	 * @return true if {@link #getDate()} is before purgeDate
	 */
	public boolean isExpired(Date purgeDate) {
		return date.before(purgeDate);
	}

	/**
	 * @return JSON string of the message
	 */
	@Override
	public String toJSONString() throws RuntimeException {
		HashMap<String, String> object = new HashMap<String, String>();
		object.put("message", message);
		object.put("date", String.valueOf(date.getTime()));
		return JSONValue.toJSONString(object);
	}
}
