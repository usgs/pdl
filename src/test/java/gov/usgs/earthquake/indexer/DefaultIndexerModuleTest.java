/*
 * DefaultIndexerModuleTest
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.ProductId;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for DefaultIndexerModule.
 */
public class DefaultIndexerModuleTest {

	/**
	 * The "US" network is authoritative by default.
	 * 
	 * Test in the middle of not-US, where no network should be authoritative
	 * (and hence US is).
	 */
	@Test
	public void testDefaultAuthoritativeSummaryWeight() throws Exception {
		ProductSummary summary = new ProductSummary();
		summary.setId(new ProductId("us", "type", "code"));
		summary.setEventSource("doesn't matter");
		summary.setEventLatitude(new BigDecimal("0.0"));
		summary.setEventLongitude(new BigDecimal("0.0"));

		DefaultIndexerModule module = new DefaultIndexerModule();
		long weight = module.getPreferredWeight(summary);

		Assert.assertTrue("US summary authoritative at (0,0)",
				weight >= DefaultIndexerModule.AUTHORITATIVE_WEIGHT);
	}

	/**
	 * The "CI" network is authoriative in Southern California.
	 * 
	 * Test in the middle of Southern California (LA).
	 */
	@Test
	public void testAuthoritativeSummaryWeight() throws Exception {
		ProductSummary summary = new ProductSummary();
		summary.setId(new ProductId("ci", "type", "code"));
		summary.setEventSource("doesn't matter");
		summary.setEventLatitude(new BigDecimal("34"));
		summary.setEventLongitude(new BigDecimal("-118"));

		DefaultIndexerModule module = new DefaultIndexerModule();
		long weight = module.getPreferredWeight(summary);

		Assert.assertTrue("CI summary authoritative at (34,-118)",
				weight >= DefaultIndexerModule.AUTHORITATIVE_WEIGHT);
	}

	/**
	 * The "NC" network is authoritative in Northern California.
	 * 
	 * Test in the middle of not-Northern California (0,0).
	 */
	@Test
	public void testNonAuthoritativeSummaryWeight() throws Exception {
		ProductSummary summary = new ProductSummary();
		summary.setId(new ProductId("nc", "type", "code"));
		summary.setEventSource("doesn't matter");
		// nc is not authoritative at (0,0)
		summary.setEventLatitude(new BigDecimal("0.0"));
		summary.setEventLongitude(new BigDecimal("0.0"));

		DefaultIndexerModule module = new DefaultIndexerModule();
		long weight = module.getPreferredWeight(summary);

		Assert.assertTrue("NC summary not authoritative at (0,0)",
				weight < DefaultIndexerModule.AUTHORITATIVE_WEIGHT);
	}

	/**
	 * Products receive a higher weight when the product source and event source
	 * match.
	 */
	@Test
	public void testSameSourceWeight() throws Exception {
		ProductSummary summary = new ProductSummary();
		summary.setId(new ProductId("nc", "type", "code"));
		summary.setEventSource("nc");
		// nc is not authoritative at (0,0)
		summary.setEventLatitude(new BigDecimal("0.0"));
		summary.setEventLongitude(new BigDecimal("0.0"));

		DefaultIndexerModule module = new DefaultIndexerModule();
		long weight = module.getPreferredWeight(summary);

		Assert.assertTrue("Event and product source same",
				weight >= DefaultIndexerModule.SAME_SOURCE_WEIGHT);
	}
	
	/**
	 * getBaseProductType should remove "internal-" prefix and "-scenario" suffix
	 * from product type.
	 */
	@Test
	public void testBaseProductType() {
		String typeInternal = "internal-shakemap";
		String typeScenario = "dyfi-scenario";
		String typeInternalScenario = "internal-tectonic-summary-scenario";
		
		DefaultIndexerModule indexer = new DefaultIndexerModule(); 
		
		Assert.assertEquals("shakemap", indexer.getBaseProductType(typeInternal));
		Assert.assertEquals("dyfi", indexer.getBaseProductType(typeScenario));
		Assert.assertEquals("tectonic-summary", indexer.getBaseProductType(typeInternalScenario));
	}
}
