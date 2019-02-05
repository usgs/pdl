/*
 * SearchQuery
 */
package gov.usgs.earthquake.indexer;

/**
 * A search to execute against a ProductIndex, or ProductStorage.
 */
public abstract class SearchQuery implements Comparable<SearchQuery> {

	/** The type of response that is expected. */
	private final SearchMethod type;

	/** The search parameters. */
	private final ProductIndexQuery query;
	
	/** Contains an error returned in a SearchResult if one occurred **/
	private String error;

	/**
	 * Construct a new SearchQuery object.
	 * 
	 * @param type
	 *            the type of search.
	 * @param query
	 *            the query parameters.
	 */
	protected SearchQuery(final SearchMethod type, final ProductIndexQuery query) {
		this.type = type;
		this.query = query;
		this.error = null;
	}

	public SearchMethod getType() {
		return this.type;
	}

	public ProductIndexQuery getProductIndexQuery() {
		return this.query;
	}

	/**
	 * Get the result associated with a specific query type.
	 * 
	 * @return the result, or null if the search has not yet executed.
	 */
	public abstract Object getResult();

	/**
	 * Create a SearchQuery object based on a SearchType.
	 * 
	 * @param type
	 *            the search type to create
	 * @param query
	 *            the associated query
	 * @return a SearchQuery, or null if type is unknown.
	 */
	public static SearchQuery getSearchQuery(final SearchMethod type,
			final ProductIndexQuery query) {
		if (type == null) {
			return null;
		} else if (type.equals(SearchMethod.EVENTS_SUMMARY)) {
			return new EventsSummaryQuery(query);
		} else if (type.equals(SearchMethod.EVENT_DETAIL)) {
			return new EventDetailQuery(query);
		} else if (type.equals(SearchMethod.PRODUCTS_SUMMARY)) {
			return new ProductsSummaryQuery(query);
		} else if (type.equals(SearchMethod.PRODUCT_DETAIL)) {
			return new ProductDetailQuery(query);
		} else {
			return null;
		}
	}

	@Override
	public boolean equals(Object that) {
		return (this.compareTo((SearchQuery) that)) == 0;
	}

	@Override
	public int compareTo(SearchQuery that) {
		int r;

		if ((r = this.type.compareTo(that.type)) != 0) {
			return r;
		}

		if ((r = this.query.compareTo(that.query)) != 0) {
			return r;
		}

		//both have results
		Object thatResult = that.getResult();
		Object thisResult = this.getResult();
		if (thisResult == null && thatResult == null) {
			return 0;
		} else if (thisResult != null && thatResult == null) {
			return -1;
		} else if (thisResult == null && thatResult != null) {
			return 1;
		}

		return 0;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * @return the error or null if no error occurred.
	 */
	public String getError() {
		return error;
	}

}
