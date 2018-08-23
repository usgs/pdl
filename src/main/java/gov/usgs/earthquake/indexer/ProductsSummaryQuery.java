package gov.usgs.earthquake.indexer;

import java.util.Iterator;
import java.util.List;

/**
 * Search for multiple products.
 */
public class ProductsSummaryQuery extends SearchQuery {

	private List<ProductSummary> result;

	public ProductsSummaryQuery(final ProductIndexQuery query) {
		super(SearchMethod.PRODUCTS_SUMMARY, query);
	}

	@Override
	public List<ProductSummary> getResult() {
		return result;
	}

	public void setResult(final List<ProductSummary> products) {
		this.result = products;
	}

	@Override
	public int compareTo(SearchQuery that) {
		int r;

		if ((r = super.compareTo(that)) != 0) {
			return r;
		}

		if (this.result != null) {
			List<ProductSummary> thatResult = ((ProductsSummaryQuery) that).result;
			if ((r = (thatResult.size() - this.result.size())) != 0) {
				return r;
			}

			Iterator<ProductSummary> thisIter = this.result.iterator();
			Iterator<ProductSummary> thatIter = thatResult.iterator();
			while (thisIter.hasNext() && thatIter.hasNext()) {
				// just compare product ids for now
				r = thisIter.next().getId().compareTo(thatIter.next().getId());
				if (r != 0) {
					return r;
				}
			}
		}

		return 0;
	}

}
