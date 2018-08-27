package gov.usgs.earthquake.dyfi;

import gov.usgs.earthquake.distribution.DefaultNotificationListener;
import gov.usgs.earthquake.distribution.ExternalNotificationListener;
import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.DirectoryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.util.Config;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class DYFIIndexerWedgeTest {

	// Static convenience strings that are accessed often
	private static final String FS = System.getProperty("file.separator");
	private static final String USER_DIR = System.getProperty("user.dir");

	// This is where the product contents can be found to create a test product
	private static final String TEST_DYFI_PRODUCT_DIR = USER_DIR + FS + "etc"
			+ FS + "test_products" + FS + "usc0001xgp";

	public static final String DYFI_SECTION_NAME = "dyfi_section";

	public static final String DYFI_COMMAND_VALUE = USER_DIR + FS
			+ "dyfi.main.sh";

	private static final String DYFI_BASE_DIRECTORY_VALUE = "./";
	private static final String DYFI_ATTEMPT_VALUE = "3";
	private static final String DYFI_TIMEOUT_VALUE = "10000"; // Milliseconds

	// Reference to a configuration section for a DYFILegacyStorage (Good)
	private static final String LEGACY_STORAGE_SECTION_NAME = "legacy_storage";

	// Storage directory property value
	private static final String STORAGE_DIRECTORY_VALUE = "storagedir";

	@Test
	public void testConstruct() {
		DYFIIndexerWedge wedge = new DYFIIndexerWedge();

		Assert.assertTrue(
				"Wedge was not the correct type following construction.",
				wedge instanceof DYFIIndexerWedge);
	}

	@Test
	public void testConfiguration() {
		// Create a configuration object
		setDYFIIndexerWedgeConfig();

		// Now make sure we can properly create a wedge using this config.
		try {
			Object wedge = Config.getConfig().getObject(DYFI_SECTION_NAME);
			Assert.assertTrue(
					"Wedge was not the correct type following configuration.",
					wedge instanceof DYFIIndexerWedge);
		} catch (Exception ex) {
			Assert.fail(ex.getMessage());
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * Checks that the wedge is interested in DYFI and only DYFI products.
	 */
	@Test
	public void testCorrectIncludeTypes() {
		DYFIIndexerWedge wedge = new DYFIIndexerWedge();
		List<String> includeTypes = wedge.getIncludeTypes();

		Assert.assertTrue(includeTypes.size() == 1);
		Assert.assertTrue(includeTypes.contains("dyfi"));
	}

	@Test
	public void testProductCommand() throws Exception {
		setDYFIIndexerWedgeConfig();
		Product product = createDYFIProduct();
		DYFIIndexerWedge wedge = (DYFIIndexerWedge) Config.getConfig()
				.getObject(DYFI_SECTION_NAME);

		String command = wedge.getProductCommand(product);
		Assert.assertTrue(command.startsWith(DYFI_COMMAND_VALUE));
		Assert.assertTrue(command.split(" ").length == 2);
	}

	/**
	 * Simple static factory method for quickly creating a test DYFI product
	 * from the project etc directory.
	 * 
	 * @return The generated product.
	 * @throws Exception
	 *             If the product could not be parsed.
	 */
	public static Product createDYFIProduct() throws Exception {

		Product p = ObjectProductHandler.getProduct(new DirectoryProductSource(
				new File(DYFIIndexerWedgeTest.TEST_DYFI_PRODUCT_DIR)));

		// Localhost is probably not running a tracker, but who cares.
		p.setTrackerURL(null); // new URL("http://127.0.0.1/tracker/"));

		return p;
	}

	/**
	 * Sets a DYFI_SECTION_NAME section on the global config that will not allow
	 * one to properly instantiate a DYFIIndexerWedge.
	 */
	public static void setDYFIIndexerWedgeConfig() {
		// Get a configuration object.
		Config config = Config.getConfig();
		if (config == null) {
			config = new Config();
		}

		// Configure the object. Do no use a DYFILegacyStorage
		config.setSectionProperty(DYFI_SECTION_NAME,
				Config.OBJECT_TYPE_PROPERTY,
				"gov.usgs.earthquake.dyfi.DYFIIndexerWedge");
		config.setSectionProperty(DYFI_SECTION_NAME,
				ExternalNotificationListener.COMMAND_PROPERTY,
				DYFI_COMMAND_VALUE);
		config.setSectionProperty(DYFI_SECTION_NAME,
				DefaultNotificationListener.MAX_TRIES_PROPERTY,
				DYFI_ATTEMPT_VALUE);
		config.setSectionProperty(DYFI_SECTION_NAME,
				DefaultNotificationListener.TIMEOUT_PROPERTY,
				DYFI_TIMEOUT_VALUE);
		config.setSectionProperty(DYFI_SECTION_NAME,
				DYFIIndexerWedge.BASE_DIRECTORY_PROPERTY,
				DYFI_BASE_DIRECTORY_VALUE);

		// Configure the object. Add a DYFILegacy storage.
		config.setSectionProperty(DYFI_SECTION_NAME,
				ExternalNotificationListener.STORAGE_NAME_PROPERTY,
				LEGACY_STORAGE_SECTION_NAME);
		config.setSectionProperty(LEGACY_STORAGE_SECTION_NAME,
				Config.OBJECT_TYPE_PROPERTY,
				"gov.usgs.earthquake.dyfi.DYFILegacyStorage");
		config.setSectionProperty(LEGACY_STORAGE_SECTION_NAME,
				FileProductStorage.DIRECTORY_PROPERTY_NAME,
				STORAGE_DIRECTORY_VALUE);

		// Set the configuration object globally.
		Config.setConfig(config);
	}
}
