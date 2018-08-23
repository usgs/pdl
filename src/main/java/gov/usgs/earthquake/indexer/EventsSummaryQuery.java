package gov.usgs.earthquake.indexer;

import java.util.Iterator;
import java.util.List;

/**
 * Search for multiple Events.
 */
public class EventsSummaryQuery extends SearchQuery {

	private List<EventSummary> result;

	/**
	 * Construct an EventsSummaryQuery.
	 * 
	 * @param query
	 */
	public EventsSummaryQuery(final ProductIndexQuery query) {
		super(SearchMethod.EVENTS_SUMMARY, query);
	}

	@Override
	public List<EventSummary> getResult() {
		return result;
	}

	public void setResult(List<EventSummary> events) {
		this.result = events;
	}

	@Override
	public int compareTo(SearchQuery that) {
		int r;

		if ((r = super.compareTo(that)) != 0) {
			return r;
		}

		if (this.result != null) {
			List<EventSummary> thatResult = ((EventsSummaryQuery) that).result;
			if ((r = (thatResult.size() - this.result.size())) != 0) {
				return r;
			}

			Iterator<EventSummary> thisIter = this.result.iterator();
			Iterator<EventSummary> thatIter = thatResult.iterator();
			while (thisIter.hasNext() && thatIter.hasNext()) {
				// just compare product ids for now
				r = thisIter.next().compareTo(thatIter.next());
				if (r != 0) {
					return r;
				}
			}
		}

		return 0;
	}

}
