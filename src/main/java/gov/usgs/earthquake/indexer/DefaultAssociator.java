/*
 * DefaultAssociator
 */
package gov.usgs.earthquake.indexer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for associating events.
 * 
 * Based on the QDM EQEventsUtils class.
 */
public class DefaultAssociator implements Associator {

	private static final Logger LOGGER = Logger
			.getLogger(DefaultAssociator.class.getName());

	// time
	/** Distance between related events in time, in milliseconds. */
	public static final long TIME_DIFF_MILLISECONDS = 16 * 1000;

	// space
	/** Distance between related events in space, in kilometers. */
	public static final BigDecimal LOCATION_DIFF_KILOMETER = new BigDecimal(100);

	/** Number of kilometers in a degree at the equator. */
	public static final BigDecimal KILOMETERS_PER_DEGREE = new BigDecimal("111.12");

	/**
	 * Distance between related events latitude, in degrees.
	 * 
	 * This is based on the max number of kilometers per degree, and provides
	 * the maximum latitude separation (assuming events share a longitude).
	 * 
	 * Used as a pre-filter before more expensive checks.
	 */
	public static final BigDecimal LOCATION_DIFF_DEGREES = new BigDecimal(
			LOCATION_DIFF_KILOMETER.doubleValue()
					/ KILOMETERS_PER_DEGREE.doubleValue());

	/**
	 * Build an index search that searches for associated products. Products are
	 * considered associated if the eventid matches or their location is within
	 * a certain distance.
	 */
	public SearchRequest getSearchRequest(ProductSummary summary) {
		SearchRequest request = new SearchRequest();

		// Order is important here. The eventId query must be added first
		ProductIndexQuery eventIdQuery = getEventIdQuery(
				summary.getEventSource(), summary.getEventSourceCode());
		if (eventIdQuery != null) {
			request.addQuery(new EventDetailQuery(eventIdQuery));
		}

		// Now a query that looks for location
		ProductIndexQuery locationQuery = getLocationQuery(
				summary.getEventTime(), summary.getEventLatitude(),
				summary.getEventLongitude());
		if (locationQuery != null) {
			request.addQuery(new EventDetailQuery(locationQuery));
		}

		return request;
	}

	/**
	 * Choose and return the most closely associated event.
	 * 
	 * @param events
	 *            a list of candidate events.
	 * @param summary
	 *            the summary being associated.
	 * @return the best match event from the list of events.
	 */
	public Event chooseEvent(final List<Event> events,
			final ProductSummary summary) {
		List<Event> filteredEvents = new LinkedList<Event>();

		// remove events that are from the same source with a different code
		String summarySource = summary.getEventSource();
		String summaryCode = summary.getEventSourceCode();
		if (summarySource == null || summaryCode == null) {
			// can't check if same source with different code
			filteredEvents = events;
		} else {
			// try to associate by event id
			Iterator<Event> iter = events.iterator();
			while (iter.hasNext()) {
				Event event = iter.next();

				boolean sameSourceDifferentCode = false;
				Iterator<ProductSummary> summaryIter;

				if (event.isDeleted()) {
					// ignore delete products before checking
					summaryIter = Event.getWithoutSuperseded(
							Event.getWithoutDeleted(event.getAllProductList())).iterator();
				} else {
					summaryIter = event.getProductList()
							.iterator();
				}
				while (summaryIter.hasNext()) {
					ProductSummary nextSummary = summaryIter.next();
					if (summarySource.equalsIgnoreCase(nextSummary
							.getEventSource())) {
						if (summaryCode.equalsIgnoreCase(nextSummary
								.getEventSourceCode())) {
							// this is the event we are looking for! so stop
							// already
							return event;
						} else {
							// different event code from same source, probably a
							// different event. Don't give up yet, because
							// associate may force multiple codes from same
							// source in same event.
							sameSourceDifferentCode = true;
						}
					}
				}

				if (!sameSourceDifferentCode) {
					filteredEvents.add(event);
				}
			}
		}

		// no events found
		if (filteredEvents.size() == 0) {
			return null;
		}

		// more than one event found
		else if (filteredEvents.size() > 1) {
			ArrayList<String> matches = new ArrayList<String>();
			Iterator<Event> iter = filteredEvents.iterator();
			while (iter.hasNext()) {
				Event match = iter.next();
				matches.add(match.getEventId());
			}
			LOGGER.log(Level.WARNING, "Potential merge, product id="
					+ summary.getId().toString() + ", nearby events: "
					+ matches.toString());

			// Return the "closest" event
			Event mostSimilar = chooseMostSimilar(summary, filteredEvents);
			if (mostSimilar != null) {
				LOGGER.log(Level.FINE, "Associated product id="
						+ summary.getId().toString() + ", to event id="
						+ mostSimilar.getEventId());
			}
			return mostSimilar;
		}

		// one event found
		else {
			return filteredEvents.get(0);
		}
	}

	/**
	 * For the given list of events, find the one that is "closest" to the given
	 * product. Similarity is calculated by first subtracting the event
	 * parameter from the product parameter, normalizing between 1 and -1, then
	 * calculating the Euclidean distance in the 3D space composed of the
	 * normalized lat, lon, and time vectors.
	 * 
	 * @param summary
	 * @param events
	 * @return Event with lowest distance
	 */
	protected Event chooseMostSimilar(ProductSummary summary, List<Event> events) {
		double lowest = Double.POSITIVE_INFINITY;
		Event bestMatch = null;

		if (summary.getEventLatitude() == null
				|| summary.getEventLongitude() == null
				|| summary.getEventTime() == null) {
			// cannot choose most similar
			if (events.size() > 0) {
				// choose first
				return events.get(0);
			} else {
				return null;
			}
		}

		// find "closest" event
		Iterator<Event> iter = events.iterator();
		while (iter.hasNext()) {
			Event event = iter.next();
			try {
				EventSummary eventSummary = event.getEventSummary();
				// First get the difference between the lat, lon, and time
				double deltaLat = summary.getEventLatitude()
						.subtract(eventSummary.getLatitude()).doubleValue();
				double deltaLon = summary.getEventLongitude()
						.subtract(eventSummary.getLongitude()).doubleValue();
				double deltaTime = summary.getEventTime().getTime()
						- eventSummary.getTime().getTime();
				// Each of the deltas will now be between the range
				// -TIME_DIFF_MILLISECONDS to +TIME_DIFF_MILLISECONDS (or
				// whatever
				// the units are). To normalize, between -1 and 1, we just need
				// to
				// divide by TIME_DIFF_MILLISECONDS
				deltaLat = deltaLat / LOCATION_DIFF_DEGREES.doubleValue();
				deltaLon = deltaLon / LOCATION_DIFF_DEGREES.doubleValue();
				deltaTime = deltaTime / TIME_DIFF_MILLISECONDS;

				// Calculate the Euclidean distance between the summary and the
				// vector representing this event
				double distance = Math.sqrt(deltaLat * deltaLat + deltaLon
						* deltaLon + deltaTime * deltaTime);
				if (distance < lowest) {
					lowest = distance;
					bestMatch = event;
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING,
						"Exception checking for most similar event", e);
				// only log, but continue processing
				if (bestMatch == null) {
					// pick an event, but don't update "lowest"
					bestMatch = event;
				}
			}
		}

		return bestMatch;
	}

	/**
	 * Check if two events are associated to each other.
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
	 * @param event1
	 *            candidate event to test.
	 * @param event2
	 *            candidate event to test.
	 * @return true if associated, false otherwise.
	 */
	@Override
	public boolean eventsAssociated(Event event1, Event event2) {

		// ---------------------------------------------------------//
		// -- Is there an explicit association or disassocation? -- //
		// ---------------------------------------------------------//

		// check disassociation first
		if (event1.hasDisassociateProduct(event2)
				|| event2.hasDisassociateProduct(event1)) {
			// explicitly disassociated
			return false;
		}

		// associate overrides usual event source rules.
		if (event1.hasAssociateProduct(event2)
				|| event2.hasAssociateProduct(event1)) {
			// explicitly associated
			return true;
		}

		EventSummary event1Summary = event1.getEventSummary();
		EventSummary event2Summary = event2.getEventSummary();

		// ---------------------------------- //
		// -- Do events share an eventid ? -- //
		// ---------------------------------- //
		// this check happens after associate and disassociate to allow two
		// events from the same source to be forced to associate
		// (bad network, bad)

		// THIS CHECKS PREFERRED EVENT ID
		// if source is same, check code
		String event1Source = event1Summary.getSource();
		String event2Source = event2Summary.getSource();
		if (event1Source != null && event2Source != null
				&& event1Source.equalsIgnoreCase(event2Source)) {
			String event1Code = event1Summary.getSourceCode();
			String event2Code = event2Summary.getSourceCode();
			// this is somewhat implied, (preferred source+code are
			// combination) but be safe anyways
			if (event1Code != null && event2Code != null) {
				if (event1Code.equalsIgnoreCase(event2Code)) {
					// same event id
					return true;
				} else {
					// different event id from same source
					return false;
				}
			}
		}

		// THIS CHECKS NON-PREFERRED EVENT IDS Map<String, String>
		// ignore deleted sub events for this comparison
		Map<String, List<String>> event1Codes = event1
				.getAllEventCodes(false);
		Map<String, List<String>> event2Codes = event2
				.getAllEventCodes(false);
		Set<String> commonSources = event1Codes.keySet();
		commonSources.retainAll(event2Codes.keySet());

		Iterator<String> eventSourceIter = commonSources.iterator();
		while (eventSourceIter.hasNext()) {
			String source = eventSourceIter.next();
			List<String> event1SourceCodes = event1Codes.get(source);
			List<String> event2SourceCodes = event2Codes.get(source);

			Iterator<String> iter = event1SourceCodes.iterator();
			while (iter.hasNext()) {
				if (!event2SourceCodes.contains(iter.next())) {
					return false;
				}
			}

			iter = event1SourceCodes.iterator();
			while (iter.hasNext()) {
				if (!event1SourceCodes.contains(iter.next())) {
					return false;
				}
			}
		}

		// --------------------------------------------------- //
		// -- Are event locations (lat/lon/time) "nearby" ? -- //
		// --------------------------------------------------- //
		if (queryContainsLocation(
				getLocationQuery(event1Summary.getTime(), event1Summary.getLatitude(),
						event1Summary.getLongitude()), event2Summary.getTime(),
				event2Summary.getLatitude(), event2Summary.getLongitude())) {
			// location matches
			return true;
		}

		return false;
	}

	/**
	 * Build a ProductIndexQuery that searches based on event id.
	 * 
	 * @param eventSource
	 *            the eventSource to search
	 * @param eventCode
	 *            the eventCode to search
	 * @return null if eventSource or eventCode are null, otherwise a
	 *         ProductIndexQuery. A returned ProductIndexQuery will have
	 *         EventSearchType SEARCH_EVENT_PREFERRED and ResultType
	 *         RESULT_TYPE_ALL.
	 */
	@Override
	public ProductIndexQuery getEventIdQuery(final String eventSource,
			final String eventCode) {
		ProductIndexQuery query = null;

		if (eventSource != null && eventCode != null) {
			query = new ProductIndexQuery();
			// search all products, not just preferred (in case the preferred is
			// a delete)
			query.setEventSearchType(ProductIndexQuery.SEARCH_EVENT_PRODUCTS);
			query.setResultType(ProductIndexQuery.RESULT_TYPE_ALL);

			query.setEventSource(eventSource);
			query.setEventSourceCode(eventCode);

			query.log(LOGGER);
		}

		return query;
	}

	/**
	 * Build a ProductIndexQuery that searches based on location.
	 * 
	 * 
	 * @param time
	 *            the time to search around.
	 * @param latitude
	 *            the latitude to search around.
	 * @param longitude
	 *            the longitude to search around.
	 * @return null if time, latitude, or longitude are null, otherwise a
	 *         ProductIndexQuery. A returned ProductIndexQuery will have
	 *         EventSearchType SEARCH_EVENT_PREFERRED and ResultType
	 *         RESULT_TYPE_ALL.
	 */
	@Override
	public ProductIndexQuery getLocationQuery(final Date time,
			final BigDecimal latitude, final BigDecimal longitude) {
		ProductIndexQuery query = null;
		if (time != null && latitude != null && longitude != null) {
			query = new ProductIndexQuery();

			// search all products, not just preferred (in case the preferred is
			// a delete)
			query.setEventSearchType(ProductIndexQuery.SEARCH_EVENT_PREFERRED);
			query.setResultType(ProductIndexQuery.RESULT_TYPE_ALL);

			// time
			query.setMinEventTime(new Date(time.getTime()
					- TIME_DIFF_MILLISECONDS));
			query.setMaxEventTime(new Date(time.getTime()
					+ TIME_DIFF_MILLISECONDS));

			// latitude
			query.setMinEventLatitude(latitude.subtract(LOCATION_DIFF_DEGREES));
			query.setMaxEventLatitude(latitude.add(LOCATION_DIFF_DEGREES));

			// longitude
			double lat = latitude.abs().doubleValue();
			if (lat < 89.0) {
				// only restrict longitude when not close to a pole...
				BigDecimal adjustedLongitudeDiff = new BigDecimal(
						LOCATION_DIFF_DEGREES.doubleValue()
								/ Math.cos(Math.toRadians(lat)));
				query.setMinEventLongitude(longitude
						.subtract(adjustedLongitudeDiff));
				query.setMaxEventLongitude(longitude.add(adjustedLongitudeDiff));

				/* make sure to compare across date/time line */
				JDBCProductIndex jdbcProductIndex = null;
				try {
					jdbcProductIndex = new JDBCProductIndex();
				} catch (Exception e) {
					e.printStackTrace();
				}

				BigDecimal minLon = query.getMinEventLongitude();
				BigDecimal maxLon = query.getMaxEventLongitude();

				// Normalize the longitudes between -180 and 180
				query.setMinEventLongitude(jdbcProductIndex
						.normalizeLongitude(minLon));
				query.setMaxEventLongitude(jdbcProductIndex
						.normalizeLongitude(maxLon));

			}

			query.log(LOGGER);
		}

		return query;
	}

	/**
	 * Check if a location would be matched by a ProductIndexQuery.
	 * 
	 * @param query
	 *            location query
	 * @param time
	 *            time to check
	 * @param latitude
	 *            latitude to check
	 * @param longitude
	 *            longitude to check
	 * @return false if query, time, latitude, or longitude are null, or if
	 *         min/max time, latitude, longitude are set and do not match time,
	 *         latitude, or longitude. otherwise, true.
	 */
	protected boolean queryContainsLocation(final ProductIndexQuery query,
			final Date time, final BigDecimal latitude,
			final BigDecimal longitude) {

		if (query == null || time == null || latitude == null
				|| longitude == null) {
			// no query or location? no contains
			return false;
		}

		if (query.getMinEventTime() != null
				&& query.getMinEventTime().after(time)) {
			// time too early
			return false;
		}
		if (query.getMaxEventTime() != null
				&& query.getMaxEventTime().before(time)) {
			// time too late
			return false;
		}

		if (query.getMinEventLatitude() != null
				&& query.getMinEventLatitude().compareTo(latitude) > 0) {
			// latitude too small
			return false;
		}
		if (query.getMaxEventLatitude() != null
				&& query.getMaxEventLatitude().compareTo(latitude) < 0) {
			// latitude too large
			return false;
		}

		if (query.getMinEventLongitude() != null
				&& query.getMaxEventLongitude() != null) {

			/*
			 * longitude range check for min & max longitude when the
			 * locationQuery spans the date line
			 */
			if (query.getMinEventLongitude().compareTo(
					query.getMaxEventLongitude()) > 0) {

				boolean inBounds = false;

				// MAX:: getMaxLongitude < longitude <= -180
				if (longitude.compareTo(query.getMaxEventLongitude()) < 0
						&& longitude.compareTo(new BigDecimal("-180")) >= 0) {
					inBounds = true;
				}

				// MIN:: 180 >= longitude > getMinEventLongitude
				if (longitude.compareTo(query.getMinEventLongitude()) > 0
						&& longitude.compareTo(new BigDecimal("180")) <= 0) {
					inBounds = true;
				}

				if (!inBounds) {
					return false;
				}

			} else {

				if (query.getMinEventLongitude().compareTo(longitude) > 0) {
					// longitude too small
					return false;
				}
				if (query.getMaxEventLongitude().compareTo(longitude) < 0) {
					// longitude too large
					return false;
				}
			}
		}

		// must contain location
		return true;
	}
}
