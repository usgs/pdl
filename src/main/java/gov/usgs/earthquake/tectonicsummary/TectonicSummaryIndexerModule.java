package gov.usgs.earthquake.tectonicsummary;

import gov.usgs.earthquake.indexer.DefaultIndexerModule;
import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;

/**
 * A specialized implementation of the IndexerModule interface for Tectonic
 * Summary products.
 *
 * Provides a higher and more specific level of support for tectonic summary
 * products, including checking for "Reviewed" status on the tectonic summary.
 * These "Reviewed tectonic summmaries will always be preferred.
 *
 */

public class TectonicSummaryIndexerModule extends DefaultIndexerModule {

	public static final int REVIEWED_TECTONIC_SUMMARY_WEIGHT = 200;

	@Override
	public int getSupportLevel(Product product) {
		int supportLevel = IndexerModule.LEVEL_UNSUPPORTED;
		String type = getBaseProductType(product.getId().getType());
		// support tectonic summary products
		if (type.equals("tectonic-summary")) {
			supportLevel = IndexerModule.LEVEL_SUPPORTED;
		}
		return supportLevel;
	}

	@Override
	protected long getPreferredWeight(final ProductSummary summary) {
		long preferredWeight = super.getPreferredWeight(summary);
		String reviewStatus = summary.getProperties().get("review-status");

		if ("REVIEWED".equalsIgnoreCase(reviewStatus)) {
			preferredWeight += REVIEWED_TECTONIC_SUMMARY_WEIGHT;
		}

		return preferredWeight;
	}
}
