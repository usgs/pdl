package gov.usgs.earthquake.shakemap;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.XmlProductSource;
import gov.usgs.util.StreamUtils;

public class ShakeMapIndexerModuleTest {

	private static final String SHAKEMAP_XML_TEST_FILE = "etc/test_products/usa00040xz/us_shakemap_usa00040xz_1287260900624.xml";
	private static final String LPAGER_XML_TEST_FILE = "etc/test_products/usa00040xz/us_losspager_usa00040xz_1287260989064.xml";

	private static final String EPICENTER_TEST_FILE = "etc/test_products/uw61272661/uw61272661.xml";
	private static final String ZOOM_TEST_FILE = "etc/test_products/uw61272661/uw61272661~SEA.xml";

	private IndexerModule module = null;
	private Product product = null;
	private ProductSummary summary = null;

	@Before
	public void setUpTestEnvironment() throws Exception {
		module = new ShakeMapIndexerModule();
	}

	@After
	public void tearDownTestEnvironment() throws Exception {
		// TODO: What to do?
	}

	@Test
	public void getProductSummary() throws Exception {
		product = createProduct(SHAKEMAP_XML_TEST_FILE);
		summary = module.getProductSummary(product);

		// Ensure summary exists
		Assert.assertNotNull(summary);

		// Ensure properties are set correctly
		Assert.assertNotNull(summary.getEventId());
		Assert.assertNotNull(summary.getEventSource());
		Assert.assertNotNull(summary.getEventSourceCode());
		Assert.assertNotNull(summary.getStatus());
		Assert.assertNotNull(summary.getVersion());
		Assert.assertNotNull(summary.getEventDepth());
		Assert.assertNotNull(summary.getEventLatitude());
		Assert.assertNotNull(summary.getEventLongitude());
		Assert.assertNotNull(summary.getEventMagnitude());
		Assert.assertNotNull(summary.getEventTime());
		Assert.assertEquals(((ShakeMapIndexerModule) module).getPreferredWeight(summary), summary.getPreferredWeight());

		// Look for additional properties specific to ShakeMaps
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.EVENT_DESCRIPTION_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.EVENT_TYPE_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.MAP_STATUS_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.MAXIMUM_LATITUDE_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.MAXIMUM_LONGITUDE_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.MINIMUM_LATITUDE_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.MINIMUM_LONGITUDE_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.PROCESS_TIMESTAMP_PROPERTY));
	}

	@Test
	public void getSupportedProductLevel() throws Exception {
		product = createProduct(SHAKEMAP_XML_TEST_FILE);

		// This Product should be supported, as it is a ShakeMap product.
		Assert.assertEquals(IndexerModule.LEVEL_SUPPORTED, module.getSupportLevel(product));
	}

	@Test
	public void getUnsupportedProductLevel() throws Exception {
		product = createProduct(LPAGER_XML_TEST_FILE);

		// This Product should not be supported, as it is not a ShakeMap
		// product.
		Assert.assertEquals(IndexerModule.LEVEL_UNSUPPORTED, module.getSupportLevel(product));
	}

	@Test
	public void preferMapCenteredOnEpicenter() throws Exception {
		Product epicenter = createProduct(EPICENTER_TEST_FILE);
		ProductSummary epicenterSummary = module.getProductSummary(epicenter);
		Product zoom = createProduct(ZOOM_TEST_FILE);
		ProductSummary zoomSummary = module.getProductSummary(zoom);

		Assert.assertEquals("Epicenter summary weight", epicenterSummary.getPreferredWeight(), 181L);
		Assert.assertEquals("Zoom summary weight", zoomSummary.getPreferredWeight(), 171L);
		Assert.assertTrue("Epicenter map preferred",
				epicenterSummary.getPreferredWeight() > zoomSummary.getPreferredWeight());
	}

	private Product createProduct(String testFile) throws Exception {
		Product p = ObjectProductHandler
				.getProduct(new XmlProductSource(StreamUtils.getInputStream(new File(testFile))));
		return p;
	}

}
