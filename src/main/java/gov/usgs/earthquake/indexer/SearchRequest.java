package gov.usgs.earthquake.indexer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A search request, which is one or more {@link SearchQuery}s.
 */
public class SearchRequest implements Comparable<SearchRequest> {

	private final List<SearchQuery> queries = new LinkedList<SearchQuery>();

	/** Construct a new SearchRequest. */
	public SearchRequest() {
	}

	/**
	 * Add another query to this request.
	 * 
	 * @param query
	 *            the query to add
	 */
	public void addQuery(final SearchQuery query) {
		this.queries.add(query);
	}

	/**
	 * @return The list of queries that are part of this request.
	 */
	public List<SearchQuery> getQueries() {
		return this.queries;
	}

	@Override
	public boolean equals(Object that) {
		return (this.compareTo((SearchRequest) that)) == 0;
	}

	@Override
	public int compareTo(SearchRequest that) {
		int r;

		// test list size first
		if ((r = (this.queries.size() - that.queries.size())) != 0) {
			return r;
		}

		Iterator<SearchQuery> thisIter = this.queries.iterator();
		Iterator<SearchQuery> thatIter = that.queries.iterator();

		while (thisIter.hasNext() && thatIter.hasNext()) {
			r = thisIter.next().compareTo(thatIter.next());
			if (r != 0) {
				return r;
			}
		}

		return 0;
	}

}
