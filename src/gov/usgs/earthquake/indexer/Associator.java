/*
 * Associator
 */
package gov.usgs.earthquake.indexer;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Implement event association logic used by the indexer.
 * 
 * It is the associators job to describe how to find matching events, and then
 * choose the best matching event that was found.
 * 
 * The indexer uses the Associator to build a query used to search the
 * ProductIndex. After querying the ProductIndex, the indexer then calls the
 * chooseEvent method with any found events.
 */
public interface Associator {

	/**
	 * Create a SearchRequest that can be used to find related events.
	 * 
	 * @param summary
	 *            the product summary being associated.
	 * @return a SearchRequest that can be used to search the ProductIndex.
	 */
	public SearchRequest getSearchRequest(ProductSummary summary);

	/**
	 * Choose the best matching event for a product summary from a list of
	 * events. If no events match, returns null.
	 * 
	 * @param events
	 *            a list of candidate events.
	 * @param summary
	 *            the product summary being associated.
	 * @return the best match event, or null if no matching event found.
	 */
	public Event chooseEvent(final List<Event> events,
			final ProductSummary summary);

	/**
	 * Check if two events are associated.
	 * 
	 * @param event1
	 *            the first event
	 * @param event2
	 *            the second event
	 * @return true if they refer to the same event, false otherwise.
	 */
	public boolean eventsAssociated(final Event event1, final Event event2);

	/**
	 * Get a ProductIndexQuery that searches by eventid.
	 * 
	 * @param eventSource
	 * @param eventCode
	 * @return a ProductIndexQuery that searches by eventid.
	 */
	public ProductIndexQuery getEventIdQuery(final String eventSource,
			final String eventCode);

	/**
	 * Get a ProductIndexQuery that searches by location.
	 * 
	 * @param time
	 * @param latitude
	 * @param longitude
	 * @return a ProductIndexQuery that searches by location.
	 */
	public ProductIndexQuery getLocationQuery(final Date time,
			final BigDecimal latitude, final BigDecimal longitude);

}
