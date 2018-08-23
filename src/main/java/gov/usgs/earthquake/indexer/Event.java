/*
 * Event
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.ProductId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An event is a group of products that are nearby in space and time.
 * 
 * Which products appear in an event depend primarily on the
 * ProductIndexQuery.ResultType that is used when retrieving an event from the
 * index. Unless CURRENT is used, you may not get what you expect.
 */
public class Event implements Comparable<Event> {

	public static final String ORIGIN_PRODUCT_TYPE = "origin";
	public static final String ASSOCIATE_PRODUCT_TYPE = "associate";
	public static final String DISASSOCIATE_PRODUCT_TYPE = "disassociate";
	public static final String OTHEREVENTSOURCE_PROPERTY = "othereventsource";
	public static final String OTHEREVENTSOURCECODE_PROPERTY = "othereventsourcecode";

	/** An ID used by the ProductIndex. */
	private Long indexId = null;

	/** Products nearby in space and time. Keyed by type. */
	private Map<String, List<ProductSummary>> products = new HashMap<String, List<ProductSummary>>();

	/** Cached summary. */
	private EventSummary eventSummary = null;

	/**
	 * Default constructor.
	 * 
	 * All fields are set to null, and the list of products is empty.
	 */
	public Event() {
	}

	/**
	 * Construct an event with only an indexId. The products map will be empty.
	 * 
	 * @param indexId
	 *            the indexId to set.
	 */
	public Event(final Long indexId) {
		this.setIndexId(indexId);
	}

	/**
	 * Construct and event with an indexId and a list of products.
	 * 
	 * @param indexId
	 *            the product index id.
	 * @param products
	 *            the list of products.
	 */
	public Event(final Long indexId,
			final Map<String, List<ProductSummary>> products) {
		this.setIndexId(indexId);
		this.setProducts(products);
	}

	/**
	 * Copy constructor for event.
	 * 
	 * The products associated with this event are not cloned, but the list of
	 * products is.
	 * 
	 * @param copy
	 *            the event to clone.
	 */
	public Event(final Event copy) {
		this(copy.getIndexId(), copy.getAllProducts());
	}

	/**
	 * Get the index id.
	 * 
	 * @return the indexId or null if one hasn't been assigned.
	 */
	public Long getIndexId() {
		return indexId;
	}

	/**
	 * Set the index id.
	 * 
	 * @param indexId
	 *            the indexId to set.
	 */
	public void setIndexId(Long indexId) {
		this.indexId = indexId;
	}

	/**
	 * Get all products associated with event, even if they are deleted.
	 * 
	 * @return all products associated with event.
	 */
	public Map<String, List<ProductSummary>> getAllProducts() {
		return products;
	}

	/**
	 * Get the event products.
	 * 
	 * Only returns products that have not been deleted or superseded. This
	 * method returns a copy of the underlying product map that has been
	 * filtered to remove deleted products.
	 * 
	 * @return a map of event products.
	 * @see #getAllProducts()
	 */
	public Map<String, List<ProductSummary>> getProducts() {
		Map<String, List<ProductSummary>> notDeleted = new HashMap<String, List<ProductSummary>>();
		Iterator<String> types = products.keySet().iterator();
		while (types.hasNext()) {
			String type = types.next();
			List<ProductSummary> notDeletedProducts = getProducts(type);
			if (notDeletedProducts.size() > 0) {
				notDeleted.put(type, notDeletedProducts);
			}
		}
		return notDeleted;
	}

	/**
	 * Set products.
	 * 
	 * ProductSummaries are not cloned, but lists are.
	 * 
	 * @param newProducts
	 *            the products to set.
	 */
	public void setProducts(final Map<String, List<ProductSummary>> newProducts) {
		this.products.clear();
		Iterator<String> iter = new TreeSet<String>(newProducts.keySet())
				.iterator();
		while (iter.hasNext()) {
			String type = iter.next();
			this.products.put(type,
					new ArrayList<ProductSummary>(newProducts.get(type)));
		}
		eventSummary = null;
	}

	/**
	 * A convenience method for adding a product summary to an event object.
	 * 
	 * Note: this method does not update any associated product index.
	 * 
	 * @param summary
	 *            the summary to add to this event.
	 */
	public void addProduct(final ProductSummary summary) {
		String type = summary.getId().getType();
		List<ProductSummary> list = products.get(type);
		if (list == null) {
			list = new ArrayList<ProductSummary>();
			products.put(type, list);
		}
		if (!list.contains(summary)) {
			list.add(summary);
		}
		eventSummary = null;
	}

	/**
	 * A convenience method for removing a product summary from an event object.
	 * 
	 * Note: this method does not update any associated product index.
	 * 
	 * @param summary
	 *            the summary to remove from this event.
	 */
	public void removeProduct(final ProductSummary summary) {
		String type = summary.getId().getType();
		// find the list of products of this type
		List<ProductSummary> list = products.get(type);
		if (list != null) {
			// remove the product from the list
			list.remove(summary);
			if (list.size() == 0) {
				// if the list is now empty, remove the list
				products.remove(type);
			}
		}
		eventSummary = null;
	}

	/**
	 * Convenience method to get products of a given type.
	 * 
	 * This method always returns a copy of the internal list, and may be empty.
	 * Only returns products that have not been deleted or superseded.
	 * 
	 * @param type
	 *            the product type.
	 * @return a list of products of that type, which may be empty.
	 */
	public List<ProductSummary> getProducts(final String type) {
		ArrayList<ProductSummary> typeProducts = new ArrayList<ProductSummary>();

		if (products.containsKey(type)) {
			// only return products that haven't been deleted
			typeProducts.addAll(getWithoutDeleted(getWithoutSuperseded(products
					.get(type))));
		}

		return typeProducts;
	}

	/**
	 * Get all event products (including those that are deleted or superseded).
	 * 
	 * @return a list of event products.
	 */
	public List<ProductSummary> getAllProductList() {
		List<ProductSummary> allProductList = new ArrayList<ProductSummary>();
		Map<String, List<ProductSummary>> allProducts = getAllProducts();
		Iterator<String> iter = allProducts.keySet().iterator();
		while (iter.hasNext()) {
			allProductList.addAll(allProducts.get(iter.next()));
		}
		return allProductList;
	}

	/**
	 * Get all event products that have not been deleted or superseded as a
	 * list.
	 * 
	 * @return a list of event products.
	 */
	public List<ProductSummary> getProductList() {
		List<ProductSummary> productList = new ArrayList<ProductSummary>();
		Map<String, List<ProductSummary>> notDeletedProducts = getProducts();
		Iterator<String> iter = notDeletedProducts.keySet().iterator();
		while (iter.hasNext()) {
			productList.addAll(notDeletedProducts.get(iter.next()));
		}
		return productList;
	}

	/**
	 * Get preferred products of all types.
	 * 
	 * This map will contain one product of each type, chosen by preferred
	 * weight.
	 * 
	 * @return a map from product type to the preferred product of that type.
	 */
	public Map<String, ProductSummary> getPreferredProducts() {
		Map<String, ProductSummary> preferredProducts = new HashMap<String, ProductSummary>();

		Map<String, List<ProductSummary>> notDeletedProducts = getProducts();
		Iterator<String> types = notDeletedProducts.keySet().iterator();
		while (types.hasNext()) {
			String type = types.next();
			preferredProducts.put(type,
					getPreferredProduct(notDeletedProducts.get(type)));
		}

		return preferredProducts;
	}

	/**
	 * Get the preferred product of a specific type.
	 * 
	 * @param type
	 *            type of product to get.
	 * @return most preferred product of that type, or null if no product of
	 *         that type is associated.
	 */
	public ProductSummary getPreferredProduct(final String type) {
		return getPreferredProduct(getProducts(type));
	}

	/**
	 * Get a map of all event ids associated with this event.
	 * 
	 * Same as Event.getEventCodes(this.getAllProductList());
	 * 
	 * @deprecated use {@link #getAllEventCodes(boolean)} instead.
	 * @return map of all event ids associated with this event.
	 */
	public Map<String, String> getEventCodes() {
		return getEventCodes(this.getAllProductList());
	}

	/**
	 * Get a map of all event ids associated with this event.
	 * 
	 * Map key is eventSource, Map value is eventSourceCode.
	 * 
	 * @deprecated use {@link #getAllEventCodes(boolean)} instead.
	 * @param summaries
	 *            the summaries list to extract event codes from.
	 * @return map of all event ids associated with this event.
	 */
	public static Map<String, String> getEventCodes(
			final List<ProductSummary> summaries) {
		Map<String, String> eventIds = new HashMap<String, String>();
		// order most preferred last,
		// to minimize impact of multiple codes from same source
		List<ProductSummary> sorted = getSortedMostPreferredFirst(
				getWithoutSuperseded(summaries));
		Collections.reverse(sorted);
		// done ordering
		Iterator<ProductSummary> iter = sorted.iterator();
		while (iter.hasNext()) {
			ProductSummary product = iter.next();
			String source = product.getEventSource();
			String code = product.getEventSourceCode();
			if (source != null && code != null) {
				eventIds.put(source.toLowerCase(), code.toLowerCase());
			}
		}
		return eventIds;
	}

	/**
	 * Get a map of all event ids associated with this event, recognizing that
	 * one source may have multiple codes (they broke the rules, but it
	 * happens).
	 * 
	 * @param includeDeleted
	 *            whether to include ids for sub events whose products have all
	 *            been deleted.
	 * @return Map from source to a list of codes from that source.
	 */
	public Map<String, List<String>> getAllEventCodes(
			final boolean includeDeleted) {
		Map<String, List<String>> allEventCodes = new HashMap<String, List<String>>();

		Map<String, Event> subEvents = getSubEvents();
		Iterator<String> iter = subEvents.keySet().iterator();
		while (iter.hasNext()) {
			Event subEvent = subEvents.get(iter.next());
			if (!includeDeleted && subEvent.isDeleted()) {
				// check for non-deleted products that should
				// keep the event code alive
				List<ProductSummary> nonDeletedProducts = getWithoutDeleted(
						getWithoutSuperseded(subEvent.getAllProductList()));
				if (nonDeletedProducts.size() == 0) {
					// filter deleted events
					continue;
				}
				// otherwise, event has active products;
				// prevent same source associations
			}

			// add code to list for source
			String source = subEvent.getSource();
			String sourceCode = subEvent.getSourceCode();
			List<String> sourceEventCodes = allEventCodes.get(source);
			if (sourceEventCodes == null) {
				// create list for source
				sourceEventCodes = new ArrayList<String>();
				allEventCodes.put(source, sourceEventCodes);
			}
			// keep list distinct
			if (!sourceEventCodes.contains(sourceCode)) {
				sourceEventCodes.add(sourceCode);
			}
		}

		return allEventCodes;
	}

	/**
	 * Get a list of all the preferred products sorted based on their
	 * authoritative weights
	 * 
	 * @return sorted list of ProductSummary objects
	 */
	public List<ProductSummary> getPreferredProductsSorted() {
		Map<String, ProductSummary> preferred = getPreferredProducts();

		// Transform the preferred HashMap into a List so we can sort based on
		// preferred weight
		List<ProductSummary> productList = new ArrayList<ProductSummary>(preferred.values());

		// Sort the list, then iterate through it until we find the specified
		// property
		Collections.sort(productList, new MostPreferredFirstComparator());
		return productList;
	}

	/**
	 * Get the event id.
	 * 
	 * The event id is the combination of event source and event source code.
	 * 
	 * @return the event id, or null if either event source or event source code
	 *         is null.
	 * @see #getSource()
	 * @see #getSourceCode()
	 */
	public String getEventId() {
		ProductSummary product = getEventIdProduct();
		if (product != null) {
			return product.getEventId();
		}
		return null;
	}

	/*
	 * Get the preferred source for this event. If an origin product exists,
	 * it's value is used.
	 * 
	 * @return Source from preferred product or null
	 */
	public String getSource() {
		ProductSummary product = getEventIdProduct();
		if (product != null) {
			return product.getEventSource();
		}
		return null;
	}

	/*
	 * Get the preferred source code for this event. If an origin product
	 * exists, it's value is used.
	 * 
	 * @return Source code from preferred product or null
	 */
	public String getSourceCode() {
		ProductSummary product = getEventIdProduct();
		if (product != null) {
			return product.getEventSourceCode();
		}
		return null;
	}

	/**
	 * Get the product used for eventsource and eventsourcecode.
	 * 
	 * Event ID comes from the preferred origin product.
	 * 
	 * @return The most preferred product summary. This summary is used to
	 *         determine the eventsouce and eventsourcecode.
	 * @see #getPreferredOriginProduct()
	 */
	protected ProductSummary getEventIdProduct() {
		ProductSummary product = getPreferredOriginProduct();
		if (product == null) {
			product = getProductWithOriginProperties();
		}
		return product;
	}

	/**
	 * Get the most recent product with origin properties (id, lat, lon, time).
	 * 
	 * <strong>NOTE</strong>: this product may have been superseded by a delete.
	 * When an event has not been deleted, this method should be consistent with
	 *  {@link #getPreferredOriginProduct()}.
	 *
	 * Products are checked in the following order, sorted most preferred first
	 * within each group.  The first matching product is returned:
	 * <ol>
	 * <li>"origin" products not superseded or deleted,
	 * 		that have origin properties</li>
	 * <li>"origin" products superseded by a delete,
	 * 		that have origin properties</li>
	 * <li>products not superseded or deleted,
	 * 		that have origin properties</li>
	 * <li>products superseded by a delete,
	 * 		that have origin properties</li>
	 * </ol>
	 * 
	 * @return the most recent product with origin properties.
	 * @see #productHasOriginProperties(ProductSummary)
	 */
	public ProductSummary getProductWithOriginProperties() {
		Map<String, List<ProductSummary>> allProducts = getAllProducts();
		List<ProductSummary> productsList = null;
		ProductSummary preferredProduct = null;
		Iterator<ProductSummary> iter = null;

		productsList = allProducts.get(ORIGIN_PRODUCT_TYPE);
		if (productsList != null) {
			// "origin" products not superseded or deleted
			productsList = getSortedMostPreferredFirst(getWithoutDeleted(
					getWithoutSuperseded(allProducts.get(ORIGIN_PRODUCT_TYPE))));
			iter = productsList.iterator();
			while (iter.hasNext()) {
				preferredProduct = iter.next();
				if (productHasOriginProperties(preferredProduct)) {
					return preferredProduct;
				}
			}

			// "origin" products superseded by a delete
			productsList = getSortedMostPreferredFirst(getWithoutSuperseded(
					getWithoutDeleted(allProducts.get(ORIGIN_PRODUCT_TYPE))));
			iter = productsList.iterator();
			while (iter.hasNext()) {
				preferredProduct = iter.next();
				if (productHasOriginProperties(preferredProduct)) {
					return preferredProduct;
				}
			}
		}

		// products not superseded or deleted
		productsList = getSortedMostPreferredFirst(getWithoutDeleted(
				getWithoutSuperseded(productTypeMapToList(allProducts))));
		iter = productsList.iterator();
		while (iter.hasNext()) {
			preferredProduct = iter.next();
			if (productHasOriginProperties(preferredProduct)) {
				return preferredProduct;
			}
		}

		// products superseded by a delete
		productsList = getSortedMostPreferredFirst(getWithoutSuperseded(
				getWithoutDeleted(productTypeMapToList(allProducts))));
		iter = productsList.iterator();
		while (iter.hasNext()) {
			preferredProduct = iter.next();
			if (productHasOriginProperties(preferredProduct)) {
				return preferredProduct;
			}
		}

		return null;
	}

	/**
	 * Get the most preferred origin-like product for this event.
	 * 
	 * The event is considered deleted if the returned product is null, deleted,
	 * or does not have origin properties.  Information about the event
	 * may still be available using {@link #getProductWithOriginProperties()}.
	 *
	 * Products are checked in the following order, sorted most preferred first
	 * within each group.  The first matching product is returned:
	 * <ul>
	 * <li>If any "origin" products exist:
	 * 		<ol>
	 * 		<li>"origin" products not superseded or deleted,
	 * 				that have origin properties.</li>
	 * 		<li>"origin" products not superseded,
	 * 				that have an event id.</li>
	 * 		</ol>
	 * </li>
	 * <li>If no "origin" products exist:
	 * 		<ol>
	 * 		<li>products not superseded or deleted,
	 * 				that have origin properties.</li>
	 * 		<li>products not superseded,
	 * 				that have an event id.</li>
	 * 		</ol>
	 * </li>
	 * </ul>
	 * 
	 * @return the most recent product with origin properties.
	 * @see #productHasOriginProperties(ProductSummary)
	 */
	public ProductSummary getPreferredOriginProduct() {
		Map<String, List<ProductSummary>> allProducts = getAllProducts();
		List<ProductSummary> productsList = null;
		ProductSummary preferredProduct = null;
		Iterator<ProductSummary> iter = null;

		productsList = allProducts.get(ORIGIN_PRODUCT_TYPE);
		if (productsList != null) {
			// "origin" products not superseded or deleted,
			// that have origin properties
			productsList = getSortedMostPreferredFirst(getWithoutDeleted(
					getWithoutSuperseded(allProducts.get(ORIGIN_PRODUCT_TYPE))));
			iter = productsList.iterator();
			while (iter.hasNext()) {
				preferredProduct = iter.next();
				if (productHasOriginProperties(preferredProduct)) {
					return preferredProduct;
				}
			}

			// "origin" products not superseded,
			// that have event id
			productsList = getSortedMostPreferredFirst(getWithoutSuperseded(
					allProducts.get(ORIGIN_PRODUCT_TYPE)));
			iter = productsList.iterator();
			while (iter.hasNext()) {
				preferredProduct = iter.next();
				if (preferredProduct.getEventSource() != null
						&& preferredProduct.getEventSourceCode() != null) {
					return preferredProduct;
				}
			}

			return null;
		}

		// products not superseded or deleted,
		// that have origin properties
		productsList = getSortedMostPreferredFirst(getWithoutDeleted(
				getWithoutSuperseded(productTypeMapToList(allProducts))));
		iter = productsList.iterator();
		while (iter.hasNext()) {
			preferredProduct = iter.next();
			if (productHasOriginProperties(preferredProduct)) {
				return preferredProduct;
			}
		}

		// products not superseded,
		// that have event id
		productsList = getSortedMostPreferredFirst(getWithoutSuperseded(
				productTypeMapToList(allProducts)));
		iter = productsList.iterator();
		while (iter.hasNext()) {
			preferredProduct = iter.next();
			if (preferredProduct.getEventSource() != null
					&& preferredProduct.getEventSourceCode() != null) {
				return preferredProduct;
			}
		}

		return null;
	}

	/**
	 * Check if a product can define an event (id, lat, lon, time).
	 * 
	 * @param product
	 *            product to check.
	 * @return true if product has id, lat, lon, and time properties.
	 */
	public static boolean productHasOriginProperties(
			final ProductSummary product) {
		return (product.getEventSource() != null
				&& product.getEventSourceCode() != null
				&& product.getEventLatitude() != null
				&& product.getEventLongitude() != null && product
					.getEventTime() != null);
	}

	/**
	 * Get the most preferred magnitude product for event.
	 * 
	 * Currently calls {@link #getPreferredOriginProduct()}.
	 * 
	 * @return the most preferred magnitude product for event.
	 */
	public ProductSummary getPreferredMagnitudeProduct() {
		return getPreferredOriginProduct();
	}

	/*
	 * Get the preferred time for this event. If an origin product exists, it's
	 * value is used.
	 * 
	 * @return Time from preferred product or null
	 */
	public Date getTime() {
		ProductSummary preferred = getProductWithOriginProperties();
		if (preferred != null) {
			return preferred.getEventTime();
		}
		return null;
	}

	/*
	 * Get the preferred latitude for this event. If an origin product exists,
	 * it's value is used.
	 * 
	 * @return Latitude from preferred product or null
	 */
	public BigDecimal getLatitude() {
		ProductSummary preferred = getProductWithOriginProperties();
		if (preferred != null) {
			return preferred.getEventLatitude();
		}
		return null;

	}

	/*
	 * Get the preferred longitude for this event. If an origin product exists,
	 * it's value is used.
	 * 
	 * @return Longitude from preferred product or null
	 */
	public BigDecimal getLongitude() {
		ProductSummary preferred = getProductWithOriginProperties();
		if (preferred != null) {
			return preferred.getEventLongitude();
		}
		return null;

	}

	/**
	 * Event update time is most recent product update time.
	 * 
	 * @return the most recent product update time.
	 */
	public Date getUpdateTime() {
		Date updateTime = null;
		Date time = null;
		Iterator<ProductSummary> iter = getAllProductList().iterator();
		while (iter.hasNext()) {
			time = iter.next().getId().getUpdateTime();
			if (updateTime == null || time.after(updateTime)) {
				time = updateTime;
			}
		}
		return updateTime;
	}

	/*
	 * Get the preferred depth for this event. If an origin product exists, it's
	 * value is used.
	 * 
	 * @return Depth from preferred product or null
	 */
	public BigDecimal getDepth() {
		ProductSummary preferred = getProductWithOriginProperties();
		if (preferred != null) {
			return preferred.getEventDepth();
		}
		return null;
	}

	public BigDecimal getMagnitude() {
		ProductSummary preferred = getPreferredMagnitudeProduct();
		if (preferred != null) {
			return preferred.getEventMagnitude();
		}
		return null;
	}

	public boolean isDeleted() {
		ProductSummary preferred = getPreferredOriginProduct();
		if (preferred != null && !preferred.isDeleted() &&
				Event.productHasOriginProperties(preferred)) {
			// have "origin" type product, that isn't deleted, 
			// and has origin properties
			return false;
		}
		// otherwise, deleted
		return true;
	}

	/**
	 * Get the most preferred product from a list of products.
	 * 
	 * @param all
	 *            a list of products containing only one type of product.
	 * @return the product with the highest preferred weight, and if tied the
	 *         most recent update time wins.
	 */
	public static ProductSummary getPreferredProduct(
			final List<ProductSummary> all) {
		ProductSummary preferred = null;

		Iterator<ProductSummary> iter = all.iterator();
		while (iter.hasNext()) {
			ProductSummary summary = iter.next();
			if (preferred == null) {
				preferred = summary;
			} else {
				long summaryWeight = summary.getPreferredWeight();
				long preferredWeight = preferred.getPreferredWeight();
				if (summaryWeight > preferredWeight
						|| (summaryWeight == preferredWeight && summary.getId()
								.getUpdateTime()
								.after(preferred.getId().getUpdateTime()))) {
					preferred = summary;
				}
			}
		}
		return preferred;
	}

	/**
	 * Summarize this event into preferred values.
	 * 
	 * NOTE: the event summary may include information from an origin product,
	 * even when the preferred origin for the event has been deleted.  Use
	 * getPreferredOriginProduct() to check the preferred origin of the event.
	 * 
	 * @return an event summary.
	 */
	public EventSummary getEventSummary() {
		if (eventSummary != null) {
			return eventSummary;
		}

		EventSummary summary = new EventSummary();
		summary.setIndexId(this.getIndexId());
		summary.setDeleted(this.isDeleted());

		ProductSummary eventIdProduct = this.getEventIdProduct();
		if (eventIdProduct != null) {
			summary.setSource(eventIdProduct.getEventSource());
			summary.setSourceCode(eventIdProduct.getEventSourceCode());
		}

		ProductSummary originProduct = this.getProductWithOriginProperties();
		if (originProduct != null) {
			summary.setLatitude(originProduct.getEventLatitude());
			summary.setLongitude(originProduct.getEventLongitude());
			summary.setTime(originProduct.getEventTime());
			summary.setDepth(originProduct.getEventDepth());
		}
		
		ProductSummary magnitudeProduct = this.getPreferredMagnitudeProduct();
		if (magnitudeProduct != null) {
			summary.setMagnitude(magnitudeProduct.getEventMagnitude());
		}

		// we may be able to avoid implementing this here, since the mapping
		// interface will be driven by the PHP product index.
		summary.getEventCodes().putAll(this.getEventCodes());

		// cache summary
		eventSummary = summary;

		return summary;
	}

	/**
	 * Comparison class that compares two ProductSummary objects based on their
	 * preferred weight and update time.
	 * 
	 */
	static class MostPreferredFirstComparator implements
			Comparator<ProductSummary> {

		@Override
		public int compare(ProductSummary p1, ProductSummary p2) {
			if (p1.getPreferredWeight() > p2.getPreferredWeight()) {
				return -1;
			} else if (p1.getPreferredWeight() < p2.getPreferredWeight()) {
				return 1;
			} else {
				Date p1Update = p1.getUpdateTime();
				Date p2Update = p2.getUpdateTime();
				if (p1Update.after(p2Update)) {
					return -1;
				} else if (p2Update.after(p1Update)) {
					return 1;
				} else {
					return 0;
				}
			}
		}
	}

	@Override
	public int compareTo(Event that) {
		int r;

		List<ProductSummary> thisProducts = this.getProductList();
		List<ProductSummary> thatProducts = that.getProductList();
		if ((r = (thatProducts.size() - thisProducts.size())) != 0) {
			return r;
		}

		Iterator<ProductSummary> thisIter = thisProducts.iterator();
		Iterator<ProductSummary> thatIter = thatProducts.iterator();
		while (thisIter.hasNext() && thatIter.hasNext()) {
			// just compare product ids for now
			r = thisIter.next().getId().compareTo(thatIter.next().getId());
			if (r != 0) {
				return r;
			}
		}

		return 0;
	}

	/**
	 * Find the most preferred product.
	 * 
	 * If preferredType is not null, products of this type are favored over
	 * those not of this type.
	 * 
	 * If preferredNotNullProperty is not null, products that have this property
	 * set are favored over those without this property set.
	 * 
	 * @param products
	 *            the list of products to search.
	 * @param preferredType
	 *            the preferred product type, if available.
	 * @param preferredNotNullProperty
	 *            the preferred property name, if available.
	 * @return The most preferred product summary of the given type.
	 */
	public static ProductSummary getMostPreferred(
			final List<ProductSummary> products, final String preferredType,
			final String preferredNotNullProperty) {
		ProductSummary mostPreferred = null;

		Iterator<ProductSummary> iter = products.iterator();
		while (iter.hasNext()) {
			ProductSummary next = iter.next();

			if (preferredNotNullProperty != null) {
				// ignore products that don't have the preferredNotNullProperty
				if (next.getProperties().get(preferredNotNullProperty) == null) {
					continue;
				}
			}

			if (mostPreferred == null) {
				// first product is most preferred so far
				mostPreferred = next;
				continue;
			}

			if (preferredType != null) {
				if (next.getType().equals(preferredType)) {
					if (!mostPreferred.getType().equals(preferredType)) {
						// prefer products of this type
						mostPreferred = next;
					}
				} else if (mostPreferred.getType().equals(preferredType)) {
					// already have preferred product of preferred type
					continue;
				}
			}

			if (next.getPreferredWeight() > mostPreferred.getPreferredWeight()) {
				// higher preferred weight
				mostPreferred = next;
			} else if (next.getPreferredWeight() == mostPreferred
					.getPreferredWeight()
					&& next.getUpdateTime()
							.after(mostPreferred.getUpdateTime())) {
				// same preferred weight, newer update
				mostPreferred = next;
			}
		}

		return mostPreferred;
	}

	/**
	 * Remove deleted products from the list.
	 * 
	 * @param products
	 *            list of products to filter.
	 * @return copy of the products list with deleted products removed.
	 */
	public static List<ProductSummary> getWithoutDeleted(
			final List<ProductSummary> products) {
		List<ProductSummary> withoutDeleted = new ArrayList<ProductSummary>();

		Iterator<ProductSummary> iter = products.iterator();
		while (iter.hasNext()) {
			ProductSummary next = iter.next();
			if (!next.isDeleted()) {
				withoutDeleted.add(next);
			}
		}

		return withoutDeleted;
	}

	/**
	 * Remove deleted products from the list.
	 * 
	 * @param products
	 *            list of products to filter.
	 * @return copy of the products list with deleted products removed.
	 */
	public static List<ProductSummary> getWithEventId(
			final List<ProductSummary> products) {
		List<ProductSummary> withEventId = new ArrayList<ProductSummary>();

		Iterator<ProductSummary> iter = products.iterator();
		while (iter.hasNext()) {
			ProductSummary next = iter.next();
			if (next.getEventId() != null) {
				withEventId.add(next);
			}
		}

		return withEventId;
	}

	/**
	 * Remove old versions of products from the list.
	 * 
	 * @param products
	 *            list of products to filter.
	 * @return a copy of the products list with products of the same
	 *         source+type+code but with older updateTimes (superseded) removed.
	 */
	public static List<ProductSummary> getWithoutSuperseded(
			final List<ProductSummary> products) {
		// place product into latest, keyed by source+type+code,
		// keeping only most recent update for each key
		Map<String, ProductSummary> latest = new HashMap<String, ProductSummary>();
		Iterator<ProductSummary> iter = products.iterator();
		while (iter.hasNext()) {
			ProductSummary summary = iter.next();
			ProductId id = summary.getId();

			// key is combination of source, type, and code
			// since none of these may contain ":", it is used as a delimiter to
			// prevent collisions.
			String key = new StringBuffer(id.getSource()).append(":").append(
					id.getType()).append(":").append(id.getCode()).toString();
			if (!latest.containsKey(key)) {
				// first product
				latest.put(key, summary);
			} else {
				// keep latest product
				ProductSummary other = latest.get(key);
				if (other.getId().getUpdateTime().before(id.getUpdateTime())) {
					latest.put(key, summary);
				}
			}
		}

		// those that are in the latest map have not been superseded
		return new ArrayList<ProductSummary>(latest.values());
	}

	/**
	 * Sort a list of products, most preferred first.
	 * 
	 * @param products
	 *            the list of products to sort.
	 * @return a copy of the list sorted with most preferred first.
	 */
	public static List<ProductSummary> getSortedMostPreferredFirst(
			final List<ProductSummary> products) {
		List<ProductSummary> mostPreferredFirst = new ArrayList<ProductSummary>(
				products);
		Collections
				.sort(mostPreferredFirst, new MostPreferredFirstComparator());
		return mostPreferredFirst;
	}

	static List<ProductSummary> productTypeMapToList(
			final Map<String, List<ProductSummary>> products) {
		List<ProductSummary> list = new ArrayList<ProductSummary>();

		Iterator<String> iter = products.keySet().iterator();
		while (iter.hasNext()) {
			list.addAll(products.get(iter.next()));
		}

		return list;
	}

	static Map<String, List<ProductSummary>> productListToTypeMap(
			final List<ProductSummary> products) {
		Map<String, List<ProductSummary>> typeMap = new HashMap<String, List<ProductSummary>>();

		Iterator<ProductSummary> iter = products.iterator();
		while (iter.hasNext()) {
			ProductSummary product = iter.next();
			List<ProductSummary> typeProducts = typeMap.get(product.getType());
			if (typeProducts == null) {
				typeProducts = new ArrayList<ProductSummary>();
				typeMap.put(product.getType(), typeProducts);
			}
			typeProducts.add(product);
		}

		return typeMap;
	}

	/**
	 * Return a list of sub-events that make up this event.
	 * 
	 * Event lines are drawn by eventid. Products that have no eventid are
	 * included with the sub event whose id is considered preferred.
	 * 
	 * @return map from eventid to event object with products for that eventid.
	 */
	public Map<String, Event> getSubEvents() {
		// Map of sub-events keyed by product "eventId"
		Map<String, Event> subEvents = new HashMap<String, Event>();
		
		// Map of events by source_type_code
		Map<String, Event> productEvents = new HashMap<String, Event>();

		// this is the event that will have products without event id...
		String preferredEventId = this.getEventId();
		Event preferredSubEvent = new Event();
		// put a placeholder with no products into the map for this purpose.
		subEvents.put(preferredEventId, preferredSubEvent);

		// List of all products associated to the current event
		List<ProductSummary> allProducts = this.getAllProductList();

		// handle products with a current version
		HashSet<ProductSummary> withoutSuperseded = new HashSet<ProductSummary>(getWithoutSuperseded(allProducts));
		Iterator<ProductSummary> products = withoutSuperseded.iterator();
		while (products.hasNext()) {
			ProductSummary product = products.next();
			Event subEvent = null;

			String subEventId = product.getEventId();
			if (subEventId == null) {
				// maybe try to find another version of product with id?
				subEvent = preferredSubEvent;
			} else {
				subEvent = subEvents.get(subEventId);
				if (subEvent == null) {
					// first product for this sub event
					subEvent = new Event();
					subEvents.put(subEventId, subEvent);
				}
			}
			subEvent.addProduct(product);

			ProductId id = product.getId();
			String key = id.getSource() + "_" + id.getType() + "_" + id.getCode();
			productEvents.put(key, subEvent);
		}

		// handle superseded products
		HashSet<ProductSummary> superseded = new HashSet<ProductSummary>(allProducts);
		superseded.removeAll(withoutSuperseded);
		products = superseded.iterator();
		while (products.hasNext()) {
			ProductSummary next = products.next();
			ProductId id = next.getId();
			String key = id.getSource() + "_" + id.getType() + "_" + id.getCode();
			Event subEvent = productEvents.get(key);
			subEvent.addProduct(next);
		}

		return subEvents;
	}

	/**
	 * Check if this event has an associate product for another given Event.
	 * 
	 * @param otherEvent
	 *            the other event.
	 * @return true if there is an associate product, false otherwise.
	 */
	public boolean hasAssociateProduct(final Event otherEvent) {
		if (otherEvent == null) {
			// cannot have an association to a null event...
			return false;
		}

		String otherEventSource = otherEvent.getSource();
		String otherEventSourceCode = otherEvent.getSourceCode();
		if (otherEventSource == null || otherEventSourceCode == null) {
			// same without source+code
			return false;
		}

		// search associate products
		Iterator<ProductSummary> iter = getProducts(ASSOCIATE_PRODUCT_TYPE)
				.iterator();
		while (iter.hasNext()) {
			ProductSummary associate = iter.next();

			if (otherEventSource.equalsIgnoreCase(associate.getProperties()
					.get(OTHEREVENTSOURCE_PROPERTY))
					&& otherEventSourceCode
							.equalsIgnoreCase(associate.getProperties().get(
									OTHEREVENTSOURCECODE_PROPERTY))) {
				// associated
				return true;
			}
		}

		return false;
	}

	/**
	 * Check if this event has an disassociate product for another given Event.
	 * 
	 * @param otherEvent
	 *            the other event.
	 * @return true if there is an disassociate product, false otherwise.
	 */
	public boolean hasDisassociateProduct(final Event otherEvent) {
		if (otherEvent == null) {
			// cannot have an disassociation to a null event...
			return false;
		}

		String otherEventSource = otherEvent.getSource();
		String otherEventSourceCode = otherEvent.getSourceCode();
		if (otherEventSource == null || otherEventSourceCode == null) {
			// same without source+code
			return false;
		}

		// search disassociate products
		Iterator<ProductSummary> iter = getProducts(DISASSOCIATE_PRODUCT_TYPE)
				.iterator();
		while (iter.hasNext()) {
			ProductSummary associate = iter.next();

			if (otherEventSource.equalsIgnoreCase(associate.getProperties()
					.get(OTHEREVENTSOURCE_PROPERTY))
					&& otherEventSourceCode
							.equalsIgnoreCase(associate.getProperties().get(
									OTHEREVENTSOURCECODE_PROPERTY))) {
				// disassociated
				return true;
			}
		}

		return false;
	}

	/**
	 * Same as isAssociated(that, new DefaultAssociator());
	 */
	public boolean isAssociated(final Event that) {
		return this.isAssociated(that, new DefaultAssociator());
	}

	/**
	 * Check if an event is associated to this event.
	 * 
	 * Reasons events may be considered disassociated:
	 * <ol>
	 * <li>Share a common EVENTSOURCE with different EVENTSOURCECODE.</li>
	 * <li>Either has a disassociate product for the other.</li>
	 * <li>Preferred location in space and time is NOT nearby, and no other
	 * reason to associate.</li>
	 * </ol>
	 * 
	 * Reasons events may be considered associated:
	 * <ol>
	 * <li>Share a common EVENTID</li>
	 * <li>Either has an associate product for the other.</li>
	 * <li>Their preferred location in space and time is nearby.</li>
	 * </ol>
	 * 
	 * @param that
	 *            candidate event to test.
	 * @return true if associated, false otherwise.
	 */
	public boolean isAssociated(final Event that, final Associator associator) {
		return associator.eventsAssociated(this, that);
	}

	public void log(final Logger logger) {
		if (logger.isLoggable(Level.FINE)) {
			EventSummary summary = this.getEventSummary();
			logger.fine(new StringBuffer("Event")
					.append("indexid=").append(summary.getIndexId())
					.append(", eventid=").append(summary.getId())
					.append(", latitude=").append(summary.getLatitude())
					.append(", longitude=").append(summary.getLongitude())
					.append(", time=").append(summary.getTime())
					.append(", deleted=").append(summary.isDeleted()).toString());

			if (logger.isLoggable(Level.FINER)) {
				StringBuffer buf = new StringBuffer("Products in event");
				List<ProductSummary> products = this.getAllProductList();
				Iterator<ProductSummary> iter = products.iterator();
				while (iter.hasNext()) {
					ProductSummary next = iter.next();
					buf.append("\n\tstatus=").append(next.getStatus())
							.append(", id=").append(next.getId().toString())
							.append(", eventid=").append(next.getEventId())
							.append(", latitude=").append(next.getEventLatitude())
							.append(", longitude=").append(next.getEventLongitude())
							.append(", time=").append(next.getEventTime());
				}
				logger.finer(buf.toString());
			}
		}
	}

}
