package gov.usgs.earthquake.shakemap;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.BinaryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.util.StreamUtils;

public class ShakeMapIndexerModuleTest {

	private static final String SHAKEMAP_XML_TEST_FILE = "etc/test_products/usa00040xz/us_shakemap_usa00040xz_1287260900624.bin";
	private static final String LPAGER_XML_TEST_FILE = "etc/test_products/usa00040xz/us_losspager_usa00040xz_1287260989064.bin";

	private static final String EPICENTER_TEST_FILE = "etc/test_products/uw61272661/uw61272661.bin";
	private static final String ZOOM_TEST_FILE = "etc/test_products/uw61272661/uw61272661~SEA.bin";

	private ShakeMapIndexerModule module = null;
	private Product product = null;
	private ProductSummary summary = null;

	@Before
	public void setUpTestEnvironment() throws Exception {
		module = new ShakeMapIndexerModule();
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
		Assert.assertEquals(module.getPreferredWeight(summary), summary.getPreferredWeight());

		// Look for additional properties specific to ShakeMaps
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.EVENT_DESCRIPTION_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.EVENT_TYPE_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.MAP_STATUS_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.MAXIMUM_LATITUDE_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.MAXIMUM_LONGITUDE_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.MINIMUM_LATITUDE_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.MINIMUM_LONGITUDE_PROPERTY));
		Assert.assertNotNull(summary.getProperties().get(ShakeMap.PROCESS_TIMESTAMP_PROPERTY));

		Assert.assertEquals(summary.getProperties().get(ShakeMapIndexerModule.OVERLAY_WIDTH_PROPERTY), "720");
		Assert.assertEquals(summary.getProperties().get(ShakeMapIndexerModule.OVERLAY_HEIGHT_PROPERTY), "716");
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

	@Test
	public void preferShakeMapAtlas() throws Exception {
		ProductSummary atlasSummary = new ProductSummary();
		atlasSummary.setId(new ProductId("atlas", "type", "code"));

		Assert.assertEquals("Atlas summary weight", 201,
				module.getPreferredWeight(atlasSummary));
	}

	private Product createProduct(String testFile) throws Exception {
		Product p = ObjectProductHandler
				.getProduct(new BinaryProductSource(StreamUtils.getInputStream(new File(testFile))));
		return p;
	}

}
