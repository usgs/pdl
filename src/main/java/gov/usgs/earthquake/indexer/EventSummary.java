package gov.usgs.earthquake.indexer;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Summary of an Event and its products.
 */
public class EventSummary implements Comparable<EventSummary> {

	/** The product index id for the original event. */
	private Long indexId;

	/** Event attributes. */
	private String source;
	private String sourceCode;
	private Date time;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private BigDecimal depth;
	private BigDecimal magnitude;
	private boolean deleted = false;

	/** A map of all event codes associated with this event. */
	private final Map<String, String> eventCodes = new HashMap<String, String>();

	/** Summary properties for an event. */
	private final Map<String, String> properties = new HashMap<String, String>();

	/**
	 * Create a new EventSummary.
	 */
	public EventSummary() {
	}

	/** @param indexId to set */
	public void setIndexId(Long indexId) {
		this.indexId = indexId;
	}

	/** @return indexID */
	public Long getIndexId() {
		return indexId;
	}

	/**
	 * Combines source + source code for ID or returns null
	 * @return Id or null
	 */
	public String getId() {
		if (source != null && sourceCode != null) {
			return source + sourceCode;
		}
		return null;
	}

	/** @return source */
	public String getSource() {
		return source;
	}

	/** @param source to set */
	public void setSource(String source) {
		this.source = source;
	}

	/** @return sourceCode */
	public String getSourceCode() {
		return sourceCode;
	}

	/** @param sourceCode to set */
	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	/** @return time */
	public Date getTime() {
		return time;
	}

	/** @param time to set */
	public void setTime(Date time) {
		this.time = time;
	}

	/** @return latitude */
	public BigDecimal getLatitude() {
		return latitude;
	}

	/** @param latitude to set */
	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}

	/** @return longitude */
	public BigDecimal getLongitude() {
		return longitude;
	}

	/** @param longitude to set */
	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}

	/** @return depth */
	public BigDecimal getDepth() {
		return depth;
	}

	/** @param depth to set */
	public void setDepth(BigDecimal depth) {
		this.depth = depth;
	}

	/** @return magnitude */
	public BigDecimal getMagnitude() {
		return magnitude;
	}

	/** @param magnitude to set */
	public void setMagnitude(BigDecimal magnitude) {
		this.magnitude = magnitude;
	}

	/** @param deleted to set */
	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}

	/** @return deleted */
	public boolean isDeleted() {
		return deleted;
	}

	/**
	 * These properties are derived from product properties, and are those
	 * desirable for searching at an event level.
	 *
	 * @return The properties of this event.
	 */
	public Map<String, String> getProperties() {
		return this.properties;
	}

	/**
	 *
	 * @return A map of event codes associated with this event.
	 */
	public Map<String, String> getEventCodes() {
		return this.eventCodes;
	}

	@Override
	public int compareTo(EventSummary that) {
		int r;

		r = this.getSource().compareTo(that.getSource());
		if (r != 0) {
			return r;
		}
		r = this.getSourceCode().compareTo(that.getSourceCode());

		return r;
	}

}
