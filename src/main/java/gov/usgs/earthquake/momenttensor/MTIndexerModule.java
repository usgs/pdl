/**
 * MTIndexerModule
 */

package gov.usgs.earthquake.momenttensor;

import java.math.BigDecimal;

import gov.usgs.earthquake.indexer.DefaultIndexerModule;
import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;

/**
 * Moment Tensor Indexer Module.
 * 
 * Implements ANSS business logic for preferred moment tensors.
 *
 * Intended order is:
 * <ol>
 * <li>Mww (W-phase)</li>
 * <li>Mwc from GCMT</li>
 * <li>Mwc</li>
 * <li>Mwb</li>
 * <li>Other</li>
 * <li>Mwb outside magnitude range [5.5, 7.0]</li>
 * </ol>
 *
 * <hr>
 *
 * Uses {@link DefaultIndexerModule#getProductSummary(Product)} defaults,
 * with the following additional weights:
 *
 * <ul>
 * <li>
 *   <code>Event Source</code> comes from the product property
 *   <code>eventsource</code>
 * </li>
 * <li>
 *   <code>Magnitude</code> comes from the product property
 *   <code>derived-magnitude</code>
 * </li>
 * <li>
 *   <code>Type</code> comes from the product property
 *   <code>derived-magnitude-type</code>, or (if not found)
 *   from the product property <code>beachball-type</code>
 * </li>
 * </ul>
 * 
 * <dl>
 * <dt>Type is <code>Mww</code></dt>
 * <dd><code>+60</code></dd>
 * 
 * <dt>Type is <code>Mwc</code></dt>
 * <dd><code>+2</code></dd>
 * 
 * <dt>Type is <code>Mwb</code>
 * <dd><code>+1</code></dd>
 * 
 * <dt>Type is <code>Mwb</code>, and Magnitude outside the
 * 	 range <code>[5.5, 7.0]</code></dt>
 * <dd><code>-100</code></dd>
 *
 * <dt>Event Source is <code>GCMT</code></dt>
 * <dd><code>+56</code></dd>
 * </dl>
 */
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
	private static final BigDecimal MAG_RANGE_MIN = new BigDecimal("5.5");
	private static final BigDecimal MAG_RANGE_MAX = new BigDecimal("7.0");

	/**
	 * Override IndexerModule api method.
	 * 
	 * @return
	 * 		IndexerModule.LEVEL_SUPPORTED when type is <code>moment-tensor</code>;
	 * 		otherwise, IndexerModule.LEVEL_UNSUPPORTED.
	 */
	@Override
	public int getSupportLevel(Product product) {
		int supportLevel = IndexerModule.LEVEL_UNSUPPORTED;
		String type = getBaseProductType(product.getId().getType());
		// Support only moment tensor products
		if ("moment-tensor".equals(type)) {
			supportLevel = IndexerModule.LEVEL_SUPPORTED;
		}

		return supportLevel;
	}

	/**
	 * Calculate preferred weight for <code>moment-tensor</code> type product.
	 * 
	 * @param summary "moment-tensor" type product summary.
	 * @return
	 *      when type is <code>moment-tensor</code>, {@link IndexerModule#LEVEL_SUPPORTED};
	 * 		otherwise, {@link IndexerModule#LEVEL_UNSUPPORTED}
	 */
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
			if (magRange != null
					&& tensorType.equalsIgnoreCase(TYPE_MWB)
					&& (magRange.compareTo(MAG_RANGE_MIN) == -1 || magRange.compareTo(MAG_RANGE_MAX) == 1)) {
				weight += MAG_OUTSIDE_RANGE_PENALTY;
			}
		}

		// Add gcmt bonus if required
		if (eventSource.equalsIgnoreCase(EVENT_SOURCE_GCMT)) {
			weight += EVENT_SOURCE_GCMT_BONUS;
		}

		return weight;
	}
}
