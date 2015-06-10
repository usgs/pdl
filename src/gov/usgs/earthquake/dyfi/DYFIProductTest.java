package gov.usgs.earthquake.dyfi;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.earthquake.product.io.DirectoryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductHandler;

import java.io.File;
import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

public class DYFIProductTest {

	// Static convenience strings that are accessed often
	private static final String FS = System.getProperty("file.separator");
	private static final String USER_DIR = System.getProperty("user.dir");

	// This is where the product contents can be found to create a test product
	private static final String TEST_DYFI_PRODUCT_DIR = USER_DIR + FS + "etc"
			+ FS + "test_products" + FS + "usc0001xgp";

	/**
	 * Tests the ability to properly parse and subsequently fetch values for a
	 * DYFI product. The known* values declared at the top of this test method
	 * were read from the event_data.xml file in the product directory (see
	 * above). If this test starts to fail, double-check these values to make
	 * sure they still reflect what is in the event_data.xml file.
	 */
	@Test
	public void testProperties() {
		String knownNumResp = "345";
		String knownIntensity = "7.8";
		int knownNumRespInt = Integer.parseInt(knownNumResp);
		BigDecimal knownIntensityDbl = new BigDecimal(knownIntensity);

		try {
			DYFIProduct product = DYFIProductTest.getDYFIProduct();

			// Test the access methods
			Assert.assertEquals("Number of responses failed.", knownNumRespInt,
					product.getNumResponses());
			Assert.assertEquals("Maximum intensity failed.", knownIntensityDbl,
					product.getMaxMMI());
		} catch (Exception ex) {
			Assert.fail(ex.getMessage());
		}

	}

	/**
	 * Test to make sure we can't force-create a DYFI product if it is not
	 * in-fact a DYFI product.
	 * 
	 * @throws Exception
	 */
	@Test(expected = java.lang.IllegalArgumentException.class)
	public void testBadProduct() throws Exception {
		new DYFIProduct((new ProductTest()).getProduct());
	}

	public static DYFIProduct getDYFIProduct() throws Exception {
		return new DYFIProduct(DYFIProductTest.getGenericDYFIProduct());
	}

	public static Product getGenericDYFIProduct() throws Exception {
		Product p = ObjectProductHandler.getProduct(new DirectoryProductSource(
				new File(DYFIProductTest.TEST_DYFI_PRODUCT_DIR)));

		// Localhost is probably not running a tracker, but who cares.
		p.setTrackerURL(null); // new URL("http://127.0.0.1/tracker/"));

		return p;
	}
}
