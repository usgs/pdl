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

	public void setIndexId(Long indexId) {
		this.indexId = indexId;
	}

	public Long getIndexId() {
		return indexId;
	}

	public String getId() {
		if (source != null && sourceCode != null) {
			return source + sourceCode;
		}
		return null;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSourceCode() {
		return sourceCode;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}

	public BigDecimal getDepth() {
		return depth;
	}

	public void setDepth(BigDecimal depth) {
		this.depth = depth;
	}

	public BigDecimal getMagnitude() {
		return magnitude;
	}

	public void setMagnitude(BigDecimal magnitude) {
		this.magnitude = magnitude;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}

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
