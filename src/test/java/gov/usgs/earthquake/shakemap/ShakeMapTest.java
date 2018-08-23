package gov.usgs.earthquake.shakemap;

import java.io.File;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.BinaryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.util.StreamUtils;

public class ShakeMapTest {

	@Test
	public void getShakeMap() throws Exception {
		Product product = ObjectProductHandler.getProduct(new BinaryProductSource(
				StreamUtils.getInputStream(new File(
						"etc/test_products/usa00040xz/us_shakemap_usa00040xz_1287260900624.bin"))));

		// Test to make sure the product exists
		Assert.assertNotNull(product);

		// Load product as a shakemap
		ShakeMap shakemap = new ShakeMap(product);

		// Check that Product properties are set
		Assert.assertNotNull(shakemap.getEventId());
		Assert.assertNotNull(shakemap.getEventSource());
		Assert.assertNotNull(shakemap.getEventSourceCode());
		Assert.assertNotNull(shakemap.getMagnitude());
		Assert.assertNotNull(shakemap.getLatitude());
		Assert.assertNotNull(shakemap.getLongitude());
		Assert.assertNotNull(shakemap.getDepth());
		Assert.assertNotNull(shakemap.getVersion());
		Assert.assertNotNull(shakemap.getEventTime());

		// Test that the ShakeMap properties exist
		Assert.assertNotNull(shakemap.getMapStatus());
		Assert.assertNotNull(shakemap.getEventType());
		Assert.assertNotNull(shakemap.getEventDescription());
		Assert.assertNotNull(shakemap.getProcessTimestamp());
		Assert.assertNotNull(shakemap.getMinimumLongitude());
		Assert.assertNotNull(shakemap.getMaximumLongitude());
		Assert.assertNotNull(shakemap.getMinimumLatitude());
		Assert.assertNotNull(shakemap.getMaximumLatitude());

	}

	@Test
	public void testInfoXMLHandler() throws Exception {
		Product product = ObjectProductHandler.getProduct(new BinaryProductSource(
				StreamUtils.getInputStream(new File(
						"etc/test_products/usa00040xz/us_shakemap_usa00040xz_1287260900624.bin"))));

		InfoXMLHandler handler = new InfoXMLHandler();
		HashMap<String, String> info = handler.parse(product.getContents()
				.get("download/info.xml").getInputStream());
		Assert.assertEquals("Incorrect pgv_max", "48.32", info.get("pgv_max"));
		Assert.assertEquals("Should be null", null, info.get("bogus_element"));
	}

}
