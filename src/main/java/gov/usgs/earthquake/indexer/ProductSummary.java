/*
 * ProductSummary
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.XmlUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.net.URI;

/**
 * A product summary is essentially a product without its contents.
 */
public class ProductSummary {

	/** An ID used by the ProductIndex. */
	private Long indexId = null;

	/** The product id. */
	private ProductId id = null;

	/** The product status. */
	private String status = null;

	/** The product tracker url. */
	private URL trackerURL = null;

	/** The product properties. */
	private Map<String, String> properties = new HashMap<String, String>();

	/** The product links. */
	private Map<String, List<URI>> links = new HashMap<String, List<URI>>();

	/** Unique identifier for this event. */
	private String eventSource = null;

	/** Unique identifier from the event network. */
	private String eventSourceCode = null;

	/** When the event occurred. */
	private Date eventTime = null;

	/** Where the event occurred. */
	private BigDecimal eventLatitude = null;

	/** Where the event occurred. */
	private BigDecimal eventLongitude = null;

	/** Where the event occurred. */
	private BigDecimal eventDepth = null;

	/** How big the event was. */
	private BigDecimal eventMagnitude = null;

	/** Product version. */
	private String version = null;

	/** Whether this product is "preferred". */
	private long preferredWeight = 0;

	/**
	 * Empty constructor.
	 */
	public ProductSummary() {
	}

	/**
	 * Copy constructor for ProductSummary.
	 * 
	 * Does a deep copy of properties and links maps. All other attributes are
	 * copied by reference.
	 * 
	 * @param copy
	 *            product summary to copy.
	 */
	public ProductSummary(final ProductSummary copy) {
		this.indexId = copy.getIndexId();
		this.id = copy.getId();
		this.status = copy.getStatus();
		this.trackerURL = copy.getTrackerURL();
		this.properties.putAll(copy.getProperties());

		Map<String, List<URI>> copyLinks = copy.getLinks();
		Iterator<String> iter = copyLinks.keySet().iterator();
		while (iter.hasNext()) {
			String relation = iter.next();
			links.put(relation, new LinkedList<URI>(copyLinks.get(relation)));
		}

		this.setEventSource(copy.getEventSource());
		this.setEventSourceCode(copy.getEventSourceCode());
		this.setEventTime(copy.getEventTime());
		this.setEventLatitude(copy.getEventLatitude());
		this.setEventLongitude(copy.getEventLongitude());
		this.setEventDepth(copy.getEventDepth());
		this.setEventMagnitude(copy.getEventMagnitude());
		this.setVersion(copy.getVersion());
		this.setPreferredWeight(copy.getPreferredWeight());
	}

	/**
	 * Create a ProductSummary from a product.
	 * 
	 * All attributes are copied from the product, and preferredWeight is set to
	 * 1L.
	 * 
	 * @param product
	 *            the product to summarize.
	 */
	public ProductSummary(final Product product) {
		this.id = product.getId();
		this.status = product.getStatus();
		this.trackerURL = product.getTrackerURL();
		this.properties.putAll(product.getProperties());

		Map<String, List<URI>> copyLinks = product.getLinks();
		Iterator<String> iter = copyLinks.keySet().iterator();
		while (iter.hasNext()) {
			String relation = iter.next();
			links.put(relation, new LinkedList<URI>(copyLinks.get(relation)));
		}

		this.setEventSource(product.getEventSource());
		this.setEventSourceCode(product.getEventSourceCode());
		this.setEventTime(product.getEventTime());
		this.setEventLatitude(product.getLatitude());
		this.setEventLongitude(product.getLongitude());
		this.setEventDepth(product.getDepth());
		this.setEventMagnitude(product.getMagnitude());
		this.setVersion(product.getVersion());
		this.setPreferredWeight(1L);
	}

	public Long getIndexId() {
		return indexId;
	}

	public void setIndexId(Long indexId) {
		this.indexId = indexId;
	}

	public ProductId getId() {
		return id;
	}

	public void setId(ProductId id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isDeleted() {
		if (Product.STATUS_DELETE.equalsIgnoreCase(this.status)) {
			return true;
		} else {
			return false;
		}
	}

	public URL getTrackerURL() {
		return trackerURL;
	}

	public void setTrackerURL(URL trackerURL) {
		this.trackerURL = trackerURL;
	}

	public long getPreferredWeight() {
		return preferredWeight;
	}

	public void setPreferredWeight(long weight) {
		this.preferredWeight = weight;
	}

	/**
	 * @return the properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(final Map<String, String> properties) {
		this.properties.putAll(properties);
	}

	/**
	 * Returns a reference to the links map.
	 * 
	 * @return the links
	 */
	public Map<String, List<URI>> getLinks() {
		return links;
	}

	/**
	 * Copies entries from provided map.
	 * 
	 * @param links
	 *            the links to set
	 */
	public void setLinks(final Map<String, List<URI>> links) {
		this.links.clear();
		this.links.putAll(links);
	}

	/**
	 * Add a link to a product.
	 * 
	 * @param relation
	 *            how link is related to product.
	 * @param href
	 *            actual link.
	 */
	public void addLink(final String relation, final URI href) {
		List<URI> relationLinks = links.get(relation);
		if (relationLinks == null) {
			relationLinks = new LinkedList<URI>();
			links.put(relation, relationLinks);
		}
		relationLinks.add(href);
	}

	public String getEventId() {
		if (eventSource == null || eventSourceCode == null) {
			return null;
		}
		return (eventSource + eventSourceCode).toLowerCase();
	}

	public String getEventSource() {
		return eventSource;
	}

	public void setEventSource(String eventSource) {
		this.eventSource = eventSource;

		// event ids are case insensitive, force lower.
		if (this.eventSource != null) {
			this.eventSource = this.eventSource.toLowerCase();
		}

		if (this.eventSource != null) {
			this.properties.put(Product.EVENTSOURCE_PROPERTY, this.eventSource);
		} else {
			this.properties.remove(Product.EVENTSOURCE_PROPERTY);
		}
	}

	public String getEventSourceCode() {
		return eventSourceCode;
	}

	public void setEventSourceCode(String eventSourceCode) {
		this.eventSourceCode = eventSourceCode;

		// event ids are case insensitive, force lower.
		if (this.eventSourceCode != null) {
			this.eventSourceCode = this.eventSourceCode.toLowerCase();
		}

		if (this.eventSourceCode != null) {
			this.properties.put(Product.EVENTSOURCECODE_PROPERTY,
					this.eventSourceCode);
		} else {
			this.properties.remove(Product.EVENTSOURCECODE_PROPERTY);
		}
	}

	public Date getEventTime() {
		return eventTime;
	}

	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
		if (eventTime != null) {
			this.properties.put(Product.EVENTTIME_PROPERTY,
					XmlUtils.formatDate(eventTime));
		} else {
			this.properties.remove(Product.EVENTTIME_PROPERTY);
		}

	}

	public BigDecimal getEventLatitude() {
		return eventLatitude;
	}

	public void setEventLatitude(BigDecimal eventLatitude) {
		this.eventLatitude = eventLatitude;
		if (eventLatitude != null) {
			this.properties.put(Product.LATITUDE_PROPERTY,
					eventLatitude.toString());
		} else {
			this.properties.remove(Product.LATITUDE_PROPERTY);
		}
	}

	public BigDecimal getEventLongitude() {
		return eventLongitude;
	}

	public void setEventLongitude(BigDecimal eventLongitude) {
		this.eventLongitude = eventLongitude;
		if (eventLongitude != null) {
			this.properties.put(Product.LONGITUDE_PROPERTY,
					eventLongitude.toString());
		} else {
			this.properties.remove(Product.LONGITUDE_PROPERTY);
		}
	}

	public BigDecimal getEventDepth() {
		return eventDepth;
	}

	public void setEventDepth(BigDecimal eventDepth) {
		this.eventDepth = eventDepth;
		if (eventDepth != null) {
			this.properties.put(Product.DEPTH_PROPERTY, eventDepth.toString());
		} else {
			this.properties.remove(Product.DEPTH_PROPERTY);
		}
	}

	public BigDecimal getEventMagnitude() {
		return eventMagnitude;
	}

	public void setEventMagnitude(BigDecimal eventMagnitude) {
		this.eventMagnitude = eventMagnitude;
		if (eventMagnitude != null) {
			this.properties.put(Product.MAGNITUDE_PROPERTY,
					eventMagnitude.toString());
		} else {
			this.properties.remove(Product.MAGNITUDE_PROPERTY);
		}
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
		if (version != null) {
			this.properties.put(Product.VERSION_PROPERTY, version);
		} else {
			this.properties.remove(Product.VERSION_PROPERTY);
		}
	}

	public String getType() {
		return getId().getType();
	}

	public String getSource() {
		return getId().getSource();
	}

	public String getCode() {
		return getId().getCode();
	}

	public Date getUpdateTime() {
		return getId().getUpdateTime();
	}

	/**
	 * Compares two ProductSummaries to determine if they are equal.
	 * 
	 * This first implementation just considers the ProductId of each summary.
	 * This is probably not the best way to check for equality.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ProductSummary) {
			return ((ProductSummary) o).getId().equals(this.getId());
		}
		return false;
	}
}
