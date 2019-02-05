package gov.usgs.earthquake.dyfi;

import gov.usgs.earthquake.indexer.DefaultIndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.ProductTest;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DYFIIndexerModuleTest {

	private DYFIProduct dyfi = null;
	private DYFIIndexerModule module = null;

	@Before
	public void before() throws Exception {
		dyfi = DYFIProductTest.getDYFIProduct();
		module = new DYFIIndexerModule();
	}

	@Test
	public void testSupportLevel() {
		Assert.assertEquals("Support level was incorrect for DYFI product.",
				DefaultIndexerModule.LEVEL_SUPPORTED,
				module.getSupportLevel(dyfi));

		Assert.assertEquals("Support level was incorrect for generic product.",
				DefaultIndexerModule.LEVEL_UNSUPPORTED,
				module.getSupportLevel((new ProductTest()).getProduct()));
	}

	@Test
	public void testSummary() throws Exception {
		ProductSummary summary = module.getProductSummary(dyfi);

		// Check the max intensity
		Assert.assertEquals(
				"Product and summary disagreed about intensity.",
				dyfi.getMaxMMI(),
				new BigDecimal(summary.getProperties().get(
						DYFIProduct.DYFI_MAX_MMI_PROPERTY)));

		// Check the number of responses
		Assert.assertEquals(
				"Product and summary disagreed about number of responses.",
				dyfi.getNumResponses(),
				Integer.parseInt(summary.getProperties().get(
						DYFIProduct.DYFI_NUM_RESP_PROPERTY)));
	}
}
