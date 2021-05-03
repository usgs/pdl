/*
 * ProductIndexQuery
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.ProductId;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Criteria for finding events.
 *
 * All properties are inclusive. When a property is null, it means any value.
 *
 * Expected combinations:
 *
 * 1) find events based on event parameters event time event latitude event
 * longitude
 *
 * 2) find previously received update of product product source product type
 * product code
 *
 * 3) find related products/events product ids
 *
 * 4) find related products/events event ids
 */
public class ProductIndexQuery implements Comparable<ProductIndexQuery> {

	/**
	 * Event search types determine whether to search only preferred event
	 * attributes (faster), or all event attributes from any associated product
	 * (more complete).
	 */
	private static enum EventSearchTypes {
		/**
		 * Search preferred event attributes.
		 *
		 * NOTE: SEARCH_EVENT_PREFERRED should ONLY be used on event queries.
		 * Using this on product queries will more than likely break.
		 */
		SEARCH_EVENT_PREFERRED,

		/** Search event product attributes. */
		SEARCH_EVENT_PRODUCTS
	};

	/**
	 * Result types determine which products associated to an event are
	 * returned.
	 */
	private static enum ResultTypes {
		/** Only include current versions of products in the result. */
		RESULT_TYPE_CURRENT,

		/** Only include superseded (old) versions of products in the result. */

		RESULT_TYPE_SUPERSEDED,

		/**
		 * Include both current and superseded versions of products in the
		 * result.
		 */
		RESULT_TYPE_ALL;
	};

	/** EventSearchType for SEARCH_EVENT_PREFERRED */
	public static EventSearchTypes SEARCH_EVENT_PREFERRED = EventSearchTypes.SEARCH_EVENT_PREFERRED;
	/** EventSearchType for SEARCH_EVENT_PRODCUTS */
	public static EventSearchTypes SEARCH_EVENT_PRODUCTS = EventSearchTypes.SEARCH_EVENT_PRODUCTS;

	/** ResultType for RESULT_TYPE_CURRENT */
	public static ResultTypes RESULT_TYPE_CURRENT = ResultTypes.RESULT_TYPE_CURRENT;
	/** ResultType for RESULT_TYPE_SUPERSEDED */
	public static ResultTypes RESULT_TYPE_SUPERSEDED = ResultTypes.RESULT_TYPE_SUPERSEDED;
	/** ResultType for RESULT_TYPE_ALL */
	public static ResultTypes RESULT_TYPE_ALL = ResultTypes.RESULT_TYPE_ALL;

	/** Search preferred or all event attributes? */
	private EventSearchTypes eventSearchType = SEARCH_EVENT_PRODUCTS;

	/** Include previous versions? */
	private ResultTypes resultType = RESULT_TYPE_CURRENT;

	/** Event source */
	private String eventSource;

	/** Event source code */
	private String eventSourceCode;

	/** Minimum event time, inclusive. */
	private Date minEventTime;

	/** Maximum event time, inclusive. */
	private Date maxEventTime;

	/** Minimum event latitude. */
	private BigDecimal minEventLatitude;

	/** Maximum event latitude. */
	private BigDecimal maxEventLatitude;

	/** Minimum event longitude. */
	private BigDecimal minEventLongitude;

	/** Maximum event longitude. */
	private BigDecimal maxEventLongitude;

	/** Minimum event depth. */
	private BigDecimal minEventDepth;

	/** Maximum event depth. */
	private BigDecimal maxEventDepth;

	/** Minimum event magnitude. */
	private BigDecimal minEventMagnitude;

	/** Maximum event magnitude. */
	private BigDecimal maxEventMagnitude;

	/** A list of product ids to search. */
	private List<ProductId> productIds = new LinkedList<ProductId>();

	/** Minimum product update time. */
	private Date minProductUpdateTime;

	/** Maximum product update time. */
	private Date maxProductUpdateTime;

	/** The product source. */
	private String productSource;

	/** The product type. */
	private String productType;

	/** The product code. */
	private String productCode;

	/** The product version */
	private String productVersion;

	/** The product status */
	private String productStatus;

	/** The product index ID; unique per productIndex */
	private Long minProductIndexId;

	/** The max number of results */
	private Integer limit;

	/** List of columns to order by */
	private String orderBy;

	/**
	 * Construct a new ProductIndexQuery.
	 */
	public ProductIndexQuery() {
	}

	/** @param eventSearchType to set */
	public void setEventSearchType(EventSearchTypes eventSearchType) {
		this.eventSearchType = eventSearchType;
	}

	/** @return eventSearchType */
	public EventSearchTypes getEventSearchType() {
		return eventSearchType;
	}

	/** @param resultType to set */
	public void setResultType(ResultTypes resultType) {
		this.resultType = resultType;
	}

	/** @return resultType */
	public ResultTypes getResultType() {
		return resultType;
	}

	/** @param eventSource to set */
	public void setEventSource(String eventSource) {
		this.eventSource = (eventSource == null ? null : eventSource
				.toLowerCase());
	}
	/** @return eventSource */
	public String getEventSource() {
		return eventSource;
	}

	/** @param eventSourceCode to set */
	public void setEventSourceCode(String eventSourceCode) {
		this.eventSourceCode = (eventSourceCode == null ? null
				: eventSourceCode.toLowerCase());
	}

	/** @return eventSourceCode */
	public String getEventSourceCode() {
		return eventSourceCode;
	}

	/** @return minEventTime */
	public Date getMinEventTime() {
		return minEventTime;
	}

	/** @param minEventTime to set */
	public void setMinEventTime(Date minEventTime) {
		this.minEventTime = minEventTime;
	}

	/** @return maxEventTime */
	public Date getMaxEventTime() {
		return maxEventTime;
	}

	/** @param maxEventTime to set */
	public void setMaxEventTime(Date maxEventTime) {
		this.maxEventTime = maxEventTime;
	}

	/** @return minEventLatitude */
	public BigDecimal getMinEventLatitude() {
		return minEventLatitude;
	}

	/** @param minEventLatitude to set */
	public void setMinEventLatitude(BigDecimal minEventLatitude) {
		this.minEventLatitude = minEventLatitude;
	}

	/** @return maxEventLatitude */
	public BigDecimal getMaxEventLatitude() {
		return maxEventLatitude;
	}

	/** @param maxEventLatitude to set */
	public void setMaxEventLatitude(BigDecimal maxEventLatitude) {
		this.maxEventLatitude = maxEventLatitude;
	}

	/** @return minEventLongitude */
	public BigDecimal getMinEventLongitude() {
		return minEventLongitude;
	}

	/** @param minEventLongitude to set */
	public void setMinEventLongitude(BigDecimal minEventLongitude) {
		this.minEventLongitude = minEventLongitude;
	}

	/** @return maxEventLongitude */
	public BigDecimal getMaxEventLongitude() {
		return maxEventLongitude;
	}

	/** @param maxEventLongitude to set */
	public void setMaxEventLongitude(BigDecimal maxEventLongitude) {
		this.maxEventLongitude = maxEventLongitude;
	}

	/** @return minEventDepth */
	public BigDecimal getMinEventDepth() {
		return minEventDepth;
	}

	/** @param minEventDepth to set */
	public void setMinEventDepth(BigDecimal minEventDepth) {
		this.minEventDepth = minEventDepth;
	}

	/** @return maxEventDepth */
	public BigDecimal getMaxEventDepth() {
		return maxEventDepth;
	}

	/** @param maxEventDepth to set */
	public void setMaxEventDepth(BigDecimal maxEventDepth) {
		this.maxEventDepth = maxEventDepth;
	}

	/** @return minEventMagnitude */
	public BigDecimal getMinEventMagnitude() {
		return minEventMagnitude;
	}

	/** @param minEventMagnitude to set */
	public void setMinEventMagnitude(BigDecimal minEventMagnitude) {
		this.minEventMagnitude = minEventMagnitude;
	}

	/** @return maxEventMagnitude */
	public BigDecimal getMaxEventMagnitude() {
		return maxEventMagnitude;
	}

	/** @param maxEventMagnitude to set */
	public void setMaxEventMagnitude(BigDecimal maxEventMagnitude) {
		this.maxEventMagnitude = maxEventMagnitude;
	}

	/** @return list of product Ids */
	public List<ProductId> getProductIds() {
		return productIds;
	}

	/** @param productIds list to set */
	public void setProductIds(List<ProductId> productIds) {
		this.productIds.clear();
		this.productIds.addAll(productIds);
	}

	/** @return minProductUpdateTime */
	public Date getMinProductUpdateTime() {
		return minProductUpdateTime;
	}

	/** @param minProductUpdateTime to set */
	public void setMinProductUpdateTime(Date minProductUpdateTime) {
		this.minProductUpdateTime = minProductUpdateTime;
	}

	/** @return maxProductUpdateTime */
	public Date getMaxProductUpdateTime() {
		return maxProductUpdateTime;
	}

	/** @param maxProductUpdateTime to set */
	public void setMaxProductUpdateTime(Date maxProductUpdateTime) {
		this.maxProductUpdateTime = maxProductUpdateTime;
	}

	/** @return productSource */
	public String getProductSource() {
		return productSource;
	}

	/** @param productSource to set */
	public void setProductSource(String productSource) {
		this.productSource = productSource;
	}

	/** @return productType */
	public String getProductType() {
		return productType;
	}

	/** @param productType to set */
	public void setProductType(String productType) {
		this.productType = productType;
	}

	/** @return productCode */
	public String getProductCode() {
		return productCode;
	}

	/** @param productCode to set */
	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	/** @param productVersion to set */
	public void setProductVersion(String productVersion) {
		this.productVersion = productVersion;
	}

	/** @return productVersion */
	public String getProductVersion() {
		return productVersion;
	}

	/** @param productStatus to set */
	public void setProductStatus(String productStatus) {
		this.productStatus = productStatus;
	}

	/** @return productStatus */
	public String getProductStatus() {
		return productStatus;
	}

	/** @param minProductIndexId to set */
	public void setMinProductIndexId(final Long minProductIndexId) {
		this.minProductIndexId = minProductIndexId;
	}

	/** @return minProductIndexId */
	public Long getMinProductIndexId() {
		return this.minProductIndexId;
	}

	/** @param limit to set */
	public void setLimit(final Integer limit) {
		this.limit = limit;
	}

	/** @return limit */
	public Integer getLimit() {
		return this.limit;
	}

	/** @param orderBy to set */
	public void setOrderBy(final String orderBy) {
		this.orderBy = orderBy;
	}

	/** @return orderBy */
	public String getOrderBy() {
		return this.orderBy;
	}

	@Override
	public boolean equals(Object that) {
		return (this.compareTo((ProductIndexQuery) that)) == 0;
	}

	@Override
	public int compareTo(ProductIndexQuery that) {
		int r = 0;

		if ((r = compare(this.eventSource, that.eventSource)) != 0) {
			return r;
		}
		if ((r = compare(this.eventSourceCode, that.eventSourceCode)) != 0) {
			return r;
		}
		if ((r = compare(this.maxEventDepth, that.maxEventDepth)) != 0) {
			return r;
		}
		if ((r = compare(this.maxEventLatitude, that.maxEventLatitude)) != 0) {
			return r;
		}
		if ((r = compare(this.maxEventLongitude, that.maxEventLongitude)) != 0) {
			return r;
		}
		if ((r = compare(this.maxEventMagnitude, that.maxEventMagnitude)) != 0) {
			return r;
		}
		if ((r = compare(this.maxEventTime, that.maxEventTime)) != 0) {
			return r;
		}
		if ((r = compare(this.maxProductUpdateTime, that.maxProductUpdateTime)) != 0) {
			return r;
		}

		if ((r = compare(this.minEventDepth, that.minEventDepth)) != 0) {
			return r;
		}
		if ((r = compare(this.minEventLatitude, that.minEventLatitude)) != 0) {
			return r;
		}
		if ((r = compare(this.minEventLongitude, that.minEventLongitude)) != 0) {
			return r;
		}
		if ((r = compare(this.minEventMagnitude, that.minEventMagnitude)) != 0) {
			return r;
		}
		if ((r = compare(this.minEventTime, that.minEventTime)) != 0) {
			return r;
		}
		if ((r = compare(this.minProductUpdateTime, that.minProductUpdateTime)) != 0) {
			return r;
		}

		if ((r = compare(this.productCode, that.productCode)) != 0) {
			return r;
		}
		if ((r = compare(this.productSource, that.productSource)) != 0) {
			return r;
		}
		if ((r = compare(this.productStatus, that.productStatus)) != 0) {
			return r;
		}
		if ((r = compare(this.productType, that.productType)) != 0) {
			return r;
		}
		if ((r = compare(this.productVersion, that.productVersion)) != 0) {
			return r;
		}

		if ((r = (that.productIds.size() - this.productIds.size())) != 0) {
			// different size lists
			return r;
		} else {
			// lists are same size, check contents
			Iterator<ProductId> thisIter = this.productIds.iterator();
			Iterator<ProductId> thatIter = that.productIds.iterator();
			while (thisIter.hasNext() && thatIter.hasNext()) {
				r = thisIter.next().compareTo(thatIter.next());
				if (r != 0) {
					return r;
				}
			}
		}

		return 0;
	}

	/**
	 * Compare function
	 * @param <T> Type
	 * @param o1 First item to compare
	 * @param o2 Second to comoare
	 * @return 0 if equal, 1 if o1 is null, -1 if o2 null, or the comparison
	 */
	protected <T extends Comparable<T>> int compare(T o1, T o2) {
		if (o1 == null && o2 == null) {
			return 0;
		} else if (o1 == null && o2 != null) {
			return 1;
		} else if (o1 != null && o2 == null) {
			return -1;
		} else {
			return o1.compareTo(o2);
		}
	}

	/**
	 * Log function
	 * @param logger logger object
	 */
	public void log(final Logger logger) {
		if (!logger.isLoggable(Level.FINEST)) {
			return;
		}
		StringBuffer buf = new StringBuffer("Product Index Query");
		buf.append("\neventSearchType=").append(this.eventSearchType);
		buf.append("\nresultType=").append(this.resultType);
		if (this.eventSource != null) {
			buf.append("\neventSource=").append(this.eventSource);
		}
		if (this.eventSourceCode != null) {
			buf.append("\neventSourceCode=").append(this.eventSourceCode);
		}
		if (this.minEventTime != null) {
			buf.append("\nminEventTime=").append(this.minEventTime);
		}
		if (this.maxEventTime != null) {
			buf.append("\nmaxEventTime=").append(this.maxEventTime);
		}
		if (this.minEventLatitude != null) {
			buf.append("\nminEventLatitude=").append(this.minEventLatitude);
		}
		if (this.maxEventLatitude != null) {
			buf.append("\nmaxEventLatitude=").append(this.maxEventLatitude);
		}
		if (this.minEventLongitude != null) {
			buf.append("\nminEventLongitude=").append(this.minEventLongitude);
		}
		if (this.maxEventLongitude != null) {
			buf.append("\nmaxEventLongitude=").append(this.maxEventLongitude);
		}
		if (this.minEventDepth != null) {
			buf.append("\nminEventDepth=").append(this.minEventDepth);
		}
		if (this.maxEventDepth != null) {
			buf.append("\nmaxEventDepth=").append(this.maxEventDepth);
		}
		if (this.minEventMagnitude != null) {
			buf.append("\nminEventMagnitude=").append(this.minEventMagnitude);
		}
		if (this.maxEventMagnitude != null) {
			buf.append("\nmaxEventMagnitude=").append(this.maxEventMagnitude);
		}
		if (this.productIds.size() > 0) {
			buf.append("\nproduct ids=");
			Iterator<ProductId> iter = this.productIds.iterator();
			while (iter.hasNext()) {
				buf.append(iter.next().toString()).append(" ");
			}
		}
		if (this.minProductUpdateTime != null) {
			buf.append("\nminProductUpdateTime=").append(
					this.minProductUpdateTime);
		}
		if (this.maxProductUpdateTime != null) {
			buf.append("\nmaxProductUpdateTime=").append(
					this.maxProductUpdateTime);
		}
		if (this.productSource != null) {
			buf.append("\nproductSource=").append(this.productSource);
		}
		if (this.productType != null) {
			buf.append("\nproductType=").append(this.productType);
		}
		if (this.productCode != null) {
			buf.append("\nproductCode=").append(this.productCode);
		}
		if (this.productVersion != null) {
			buf.append("\nproductVersion=").append(this.productVersion);
		}
		if (this.productStatus != null) {
			buf.append("\nproductStatus=").append(this.productStatus);
		}

		logger.finest(buf.toString());
	}
}
