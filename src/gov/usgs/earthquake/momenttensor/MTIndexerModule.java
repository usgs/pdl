/**
 * MTIndexerModule
 */

package gov.usgs.earthquake.momenttensor;

import java.math.BigDecimal;

import gov.usgs.earthquake.indexer.DefaultIndexerModule;
import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;

public class MTIndexerModule extends DefaultIndexerModule {

	private static final String TYPE_MWW = "Mww";
	private static final long TYPE_MWW_BONUS = 60L;
	private static final String TYPE_MWC = "Mwc";
	private static final long TYPE_MWC_BONUS = 2L;
	private static final String TYPE_MWB = "Mwb";
	private static final long TYPE_MWB_BONUS = 1L;
	private static final long TYPE_OTHER_BONUS = 0L;
	private static final String EVENT_SOURCE_GCMT = "gcmt";
	private static final long EVENT_SOURCE_GCMT_BONUS = 56L;
	private static final long MAG_OUTSIDE_RANGE_PENALTY = -100L;
	private static final BigDecimal MAG_RANGE_MIN = new BigDecimal(5.5);
	private static final BigDecimal MAG_RANGE_MAX = new BigDecimal(7.0);

	@Override
	public int getSupportLevel(Product product) {
		int supportLevel = IndexerModule.LEVEL_UNSUPPORTED;
		String type = getBaseProductType(product.getId().getType());
		// Support only moment tensor products
		if (type.equals("moment-tensor")) {
			supportLevel = IndexerModule.LEVEL_SUPPORTED;
		}

		return supportLevel;
	}

	@Override
	protected long getPreferredWeight(ProductSummary summary) {
		// Get the default preferred weight value from the parent class
		long weight = super.getPreferredWeight(summary);

		// points by type
		String tensorType = summary.getProperties().get("derived-magnitude-type");
		String eventSource = summary.getEventSource();
		String derivedMagnitude = summary.getProperties().get("derived-magnitude");
		BigDecimal magRange = derivedMagnitude == null ? null : new BigDecimal(derivedMagnitude);

		if (tensorType == null) {
			tensorType = summary.getProperties().get("beachball-type");
		}

		if (tensorType != null) {
			// Add bonus
			if (tensorType.equalsIgnoreCase(TYPE_MWW)) {
				weight += TYPE_MWW_BONUS;
			} else if (tensorType.equalsIgnoreCase(TYPE_MWC)) {
				weight += TYPE_MWC_BONUS;
			} else if (tensorType.equalsIgnoreCase(TYPE_MWB)) {
				weight += TYPE_MWB_BONUS;
			} else {
				weight += TYPE_OTHER_BONUS;
			}

			// Subtract penalty
			if (magRange != null) {
				if (tensorType.equalsIgnoreCase(TYPE_MWB)
						&& (magRange.compareTo(MAG_RANGE_MIN) == -1 || magRange.compareTo(MAG_RANGE_MAX) == 1)) {
					weight += MAG_OUTSIDE_RANGE_PENALTY;
				}
			}
		}

		// Add gcmt bonus if required
		if (eventSource.equalsIgnoreCase(EVENT_SOURCE_GCMT)) {
			weight += EVENT_SOURCE_GCMT_BONUS;
		}

		return weight;
	}
}
