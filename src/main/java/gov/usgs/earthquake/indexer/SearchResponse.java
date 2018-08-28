package gov.usgs.earthquake.indexer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Results from a SearchRequest.
 */
public class SearchResponse {

	/** The queries with results. */
	private final List<SearchQuery> results = new LinkedList<SearchQuery>();

	/**
	 * Construct a new Search response.
	 */
	public SearchResponse() {
	}

	/**
	 * Add a search result to this response.
	 * 
	 * @param result
	 */
	public void addResult(final SearchQuery result) {
		results.add(result);
	}

	/**
	 * @return The search results.
	 */
	public List<SearchQuery> getResults() {
		return results;
	}

	/**
	 * Test by comparing each result.
	 */
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SearchResponse)) {
			return false;
		}

		SearchResponse that = (SearchResponse) obj;
		if (this.getResults().size() != that.getResults().size()) {
			return false;
		}

		Iterator<SearchQuery> thisIter = this.getResults().iterator();
		Iterator<SearchQuery> thatIter = that.getResults().iterator();
		while (thisIter.hasNext() && thatIter.hasNext()) {
			if (!thisIter.next().equals(thatIter.next())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Also override hashCode, using hash of result objects.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.getResults().toArray());
	}

	/**
	 * Get a distinct list of events from EventDetailQuery results.
	 * 
	 * @return List of found events. List will be empty if there were no
	 *         EventDetailQueries, or no matching events were found.
	 */
	public List<Event> getEvents() {
		List<Event> events = new LinkedList<Event>();

		// Get the results out of the SearchRequest and into a list
		Iterator<SearchQuery> iter = getResults().iterator();
		while (iter.hasNext()) {
			SearchQuery query = iter.next();

			if (query instanceof EventDetailQuery) {
				List<Event> queryEvents = ((EventDetailQuery) query)
						.getResult();
				Iterator<Event> queryIter = queryEvents.iterator();
				while (queryIter.hasNext()) {
					Event event = queryIter.next();
					if (!events.contains(event)) {
						// since events may be returned multiple times by
						// search query (one per query).
						events.add(event);
					}
				}
			}
		}

		return events;
	}

}
