package gov.usgs.earthquake.indexer;

import java.util.Iterator;
import java.util.List;

import gov.usgs.earthquake.product.Product;

/**
 * Search for one product.
 */
public class ProductDetailQuery extends SearchQuery {

	private List<Product> result;

	public ProductDetailQuery(final ProductIndexQuery query) {
		super(SearchMethod.PRODUCT_DETAIL, query);
	}

	@Override
	public List<Product> getResult() {
		return result;
	}

	public void setResult(final List<Product> product) {
		this.result = product;
	}

	@Override
	public int compareTo(SearchQuery that) {
		int r;

		if ((r = super.compareTo(that)) != 0) {
			return r;
		}

		if (this.result != null) {
			List<Product> thatResult = ((ProductDetailQuery) that).result;
			if ((r = (thatResult.size() - this.result.size())) != 0) {
				return r;
			}

			Iterator<Product> thisIter = this.result.iterator();
			Iterator<Product> thatIter = thatResult.iterator();
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
