/*
 * ProductIndex
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Configurable;

import java.util.List;

/**
 * An index of products.
 * 
 * The Indexer uses a ProductIndex to store received Products, and associate
 * them together into Events.
 * 
 * The transaction methods are used when one product results in several changes
 * to the database. For instance, add a ProductSummary then add an association
 * to an event.
 */
public interface ProductIndex extends Configurable {

	/**
	 * If the index supports transactions, begin a transaction.
	 * 
	 * @throws Exception
	 */
	public void beginTransaction() throws Exception;

	/**
	 * If the index supports transactions, and beginTransaction was previously
	 * called, commit the pending transaction.
	 * 
	 * @throws Exception
	 */
	public void commitTransaction() throws Exception;

	/**
	 * If the index supports transactions, and beginTransaction was previously
	 * called, rollback the pending transaction.
	 * 
	 * @throws Exception
	 */
	public void rollbackTransaction() throws Exception;

	/**
	 * Get events in this index.
	 * 
	 * @param query
	 *            a description of which events to retrieve.
	 * @return a list of matching events.
	 * @throws Exception
	 */
	public List<Event> getEvents(ProductIndexQuery query) throws Exception;

	/**
	 * Get products in this index.
	 * 
	 * @param query
	 *            a description of which products to retrieve.
	 * @return a list of matching products.
	 * @throws Exception
	 */
	public List<ProductSummary> getProducts(ProductIndexQuery query)
			throws Exception;

	/**
	 * Get products in this index that aren't associated to any event.
	 * 
	 * @param query
	 *            a description of which products to retrieve.
	 * @return a list of unassociated products
	 * @throws Exception
	 */
	public List<ProductSummary> getUnassociatedProducts(ProductIndexQuery query)
			throws Exception;

	/**
	 * Add an event to the index.
	 * 
	 * @param event
	 *            the event to add.
	 * @return Copy of event with the eventId attribute set to the id in the
	 *         database
	 * @throws Exception
	 */
	public Event addEvent(final Event event) throws Exception;

	/**
	 * Remove an event from the index.
	 * 
	 * @param event
	 *            the event to remove.
	 * @return Copy of the event removed
	 * @throws Exception
	 *             if the event is associated to products.
	 */
	public List<ProductId> removeEvent(final Event event) throws Exception;

	/**
	 * Add a product summary to the index.
	 * 
	 * @param summary
	 *            the summary to add.
	 * @return Copy of the product summary object with the indexId set to the
	 *         newly inserted id.
	 * @throws Exception
	 */
	public ProductSummary addProductSummary(final ProductSummary summary)
			throws Exception;

	/**
	 * Remove a product summary from the index.
	 * 
	 * @param summary
	 *            the summary to remove.
	 * @return id of removed summary.
	 * @throws Exception
	 */
	public ProductId removeProductSummary(final ProductSummary summary)
			throws Exception;

	/**
	 * Associate an Event and ProductSummary that are already in the index.
	 * 
	 * @param event
	 *            the event.
	 * @param summary
	 *            the summary.
	 * @return Copy of event with summary added to the products list
	 * @throws Exception
	 */
	public Event addAssociation(final Event event, final ProductSummary summary)
			throws Exception;

	/**
	 * Remove an association between and Event and ProductSummary.
	 * 
	 * @param event
	 *            the event.
	 * @param summary
	 *            the summary.
	 * @return Copy of event with summary removed from the products list
	 * @throws Exception
	 */
	public Event removeAssociation(final Event event,
			final ProductSummary summary) throws Exception;

	/**
	 * An opportunity for the ProductIndex to update summary information it may
	 * or may not store for efficient event searches.
	 * 
	 * This method is called by the indexer after it has finished updating
	 * events during onProduct.
	 * 
	 * @param events
	 *            events that may have new preferred attributes.
	 * @throws Exception
	 */
	public void eventsUpdated(final List<Event> events) throws Exception;

}
