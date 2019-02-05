package gov.usgs.earthquake.momenttensor;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;

/*
 * Test the moment tensor indexer outputs for correct values.
 */
public class MTIndexerModuleTest {
	private IndexerModule module = null;
	private Product product = null;

	@Before
	public void setUpTestEnvironment() throws Exception {
		module = new MTIndexerModule();
	}

	@After
	public void tearDownTestEnvironment() throws Exception {
		
	}
	/*
	 * Checks for MWW and adds the appropriate bonus.
	 */
	@Test
	public void neicMwwInNeicAuthoritativeRegion() throws Exception {
		product = new Product(new ProductId("us", "moment-tensor", "code"));
		product.setEventSource("us");
		product.setEventSourceCode("code");
		product.setLatitude(BigDecimal.ZERO);
		product.setLongitude(BigDecimal.ZERO);
		
		Map<String,String> props = product.getProperties();
		props.put("beachball-type", "MWW");
		
		Assert.assertEquals("MWW from NEIC in NEIC authoritative region.", 216L,
				module.getProductSummary(product).getPreferredWeight());
	}
	
	/*
	 * Checks for MWC and addes the appropriate bonus.
	 */
	@Test
	public void neicMwcInNeicAuthoritativeRegion() throws Exception {
		product = new Product(new ProductId("us", "moment-tensor", "code"));
		product.setEventSource("us");
		product.setEventSourceCode("code");
		product.setLatitude(BigDecimal.ZERO);
		product.setLongitude(BigDecimal.ZERO);
		
		Map<String,String> props = product.getProperties();
		props.put("beachball-type", "MWC");
		
		Assert.assertEquals("MWC from NEIC in NEIC authoritative region.", 158L,
				module.getProductSummary(product).getPreferredWeight());
	}
	
	/*
	 * Checks for gcmt and addes the appropriate bonus.
	 */
	@Test
	public void gcmtMwcInNeicAuthoritativeRegion() throws Exception {
		product = new Product(new ProductId("us", "moment-tensor", "code"));
		product.setEventSource("gcmt");
		product.setEventSourceCode("code");
		product.setLatitude(BigDecimal.ZERO);
		product.setLongitude(BigDecimal.ZERO);
		
		Map<String,String> props = product.getProperties();
		props.put("beachball-type", "MWC");
		
		Assert.assertEquals("MWC from gcmt in NEIC authoritative region.", 159L,
				module.getProductSummary(product).getPreferredWeight());
	}
	
	/*
	 * When type is inside range ( [5.5, 7] ) does nothing..
	 */
	@Test
	public void neicMwbInsideRange() throws Exception {
		product = new Product(new ProductId("us", "moment-tensor", "code"));
		product.setEventSource("us");
		product.setEventSourceCode("code");
		product.setLatitude(BigDecimal.ZERO);
		product.setLongitude(BigDecimal.ZERO);
		
		Map<String,String> props = product.getProperties();
		props.put("beachball-type", "MWB");
		props.put("derived-magnitude", "6");
		
		Assert.assertEquals("MWB from NEIC in NEIC authoritative region.", 157L,
				module.getProductSummary(product).getPreferredWeight());
	}
	
	/*
	 * When type is outside range ( [5.5, 7] ) subtract the appropriate penalty
	 */
	@Test
	public void neicMwbOutsideRange() throws Exception {
		product = new Product(new ProductId("us", "moment-tensor", "code"));
		product.setEventSource("us");
		product.setEventSourceCode("code");
		
		product.setLatitude(BigDecimal.ZERO);
		product.setLongitude(BigDecimal.ZERO);
		
		Map<String,String> props = product.getProperties();
		props.put("beachball-type", "MWB");
		props.put("derived-magnitude", "8");
		
		Assert.assertEquals("MWC from NEIC in NEIC authoritative region.", 57L,
				module.getProductSummary(product).getPreferredWeight());
		
	}
	
}
