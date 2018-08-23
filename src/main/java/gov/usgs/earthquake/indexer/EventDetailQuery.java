package gov.usgs.earthquake.indexer;

import java.util.Iterator;
import java.util.List;

/**
 * Search for one event.
 */
public class EventDetailQuery extends SearchQuery {

	private List<Event> result;

	public EventDetailQuery(final ProductIndexQuery query) {
		super(SearchMethod.EVENT_DETAIL, query);
	}

	@Override
	public List<Event> getResult() {
		return result;
	}

	public void setResult(final List<Event> event) {
		this.result = event;
	}

	@Override
	public int compareTo(SearchQuery that) {
		int r;

		if ((r = super.compareTo(that)) != 0) {
			return r;
		}

		if (this.result != null) {
			List<Event> thatResult = ((EventDetailQuery) that).result;
			if ((r = (thatResult.size() - this.result.size())) != 0) {
				return r;
			}

			Iterator<Event> thisIter = this.result.iterator();
			Iterator<Event> thatIter = thatResult.iterator();
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
