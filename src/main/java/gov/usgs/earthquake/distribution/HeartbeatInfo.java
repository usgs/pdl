package gov.usgs.earthquake.distribution;

import java.util.Date;

import javax.json.Json;
import javax.json.JsonObject;

/**
 * HeartbeatInfo stores a single heartbeat key/value message, together with a
 * timestamp
 * 
 * @author tene
 * 
 */
public class HeartbeatInfo {

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
	 * @return JSON object of the message
	 */
	public JsonObject toJsonObject() {
		JsonObject object = Json.createObjectBuilder()
				.add("message", message)
				.add("date", String.valueOf(date.getTime()))
				.build();
		return object;
	}

}
