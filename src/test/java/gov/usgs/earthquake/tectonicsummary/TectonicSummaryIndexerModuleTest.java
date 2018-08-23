package gov.usgs.earthquake.tectonicsummary;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.ProductId;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.BinaryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.earthquake.product.io.XmlProductSource;
import gov.usgs.util.StreamUtils;

public class TectonicSummaryIndexerModuleTest {

	public static final long AUTHORITATIVE_WEIGHT = 200;
	private static final String TECTONIC_SUMMARY_XML_TEST_FILE = "etc/test_products/usb000d75b/us_tectonicsummary_usb000d75b_1351097744544.xml";
	private static final String LPAGER_XML_TEST_FILE = "etc/test_products/usa00040xz/us_losspager_usa00040xz_1287260989064.bin";
	
	private IndexerModule module = null;
	private Product product = null;
	private ProductSummary summary = null;
	
	@Test
	public void testTectonicSummaryWeightOver() {
		ProductSummary summary = new ProductSummary();
		Map<String, String> map = new HashMap<String, String>();
		summary.setId(new ProductId("us", "tectonic-summary", "code"));
		summary.setEventSource("us");
		summary.setEventSourceCode("usb000d75b");
		map.put("review-status", "Reviewed");
		summary.setProperties(map);

		TectonicSummaryIndexerModule module = new TectonicSummaryIndexerModule();
		long weight = module.getPreferredWeight(summary);

		Assert.assertTrue("reviewed tectonic summary weight (" + weight + ") should be greater than 200",
				weight > AUTHORITATIVE_WEIGHT);
	}	
	
	@Test
	public void testTectonicSummaryWeightUnder() {
		ProductSummary summary = new ProductSummary();
		Map<String, String> map = new HashMap<String, String>();
		summary.setId(new ProductId("us", "tectonic-summary", "code"));
		summary.setEventSource("us");
		summary.setEventSourceCode("usb000d75b");
		map.put("review-status", "Reviewed");
		summary.setStatus("Automatic");

		TectonicSummaryIndexerModule module = new TectonicSummaryIndexerModule();
		long weight = module.getPreferredWeight(summary);

		Assert.assertTrue("reviewed tectonic summary weight (" + weight + ") should be greater than 200",
				weight <  AUTHORITATIVE_WEIGHT);
	}



	@Before
	public void setUpTestEnvironment() throws Exception {
		module = new TectonicSummaryIndexerModule();
	}

	@Test
	public void getProductSummary() throws Exception {
		product = createProduct(TECTONIC_SUMMARY_XML_TEST_FILE);
		summary = module.getProductSummary(product);

		// Ensure summary exists
		Assert.assertNotNull(summary);

		// Ensure properties are set correctly
		Assert.assertNotNull(summary.getEventId());
		Assert.assertNotNull(summary.getEventSource());
		Assert.assertNotNull(summary.getEventSourceCode());
		Assert.assertEquals(summary.getProperties().get("review-status"), "Reviewed");
		Assert.assertEquals(
				((TectonicSummaryIndexerModule) module).getPreferredWeight(summary),
				summary.getPreferredWeight());
	}

	@Test
	public void getSupportedProductLevel() throws Exception {
		product = createProduct(TECTONIC_SUMMARY_XML_TEST_FILE);

		// This Product should be supported, as it is a ShakeMap product.
		Assert.assertEquals(IndexerModule.LEVEL_SUPPORTED,
				module.getSupportLevel(product));
	}

	@Test
	public void getUnsupportedProductLevel() throws Exception {
		product = createProduct(LPAGER_XML_TEST_FILE);

		// This Product should not be supported, as it is not a ShakeMap
		// product.
		Assert.assertEquals(IndexerModule.LEVEL_UNSUPPORTED,
				module.getSupportLevel(product));
	}

	private Product createProduct(String testFile) throws Exception {
		InputStream in = StreamUtils.getInputStream(new File(testFile));
		ProductSource source;
		if (testFile.endsWith(".xml")) {
			source = new XmlProductSource(in);
		} else {
			source = new BinaryProductSource(in);
		}
		Product p = ObjectProductHandler.getProduct(source);
		return p;
	}

	
}