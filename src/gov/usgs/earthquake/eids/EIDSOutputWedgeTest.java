package gov.usgs.earthquake.eids;

import java.io.File;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.ProductTracker;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.BinaryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.XmlProductSource;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EIDSOutputWedgeTest {
	public static final String ORIGIN_PRODUCT_PATH = "etc/test_products/20110725_usc00053hg_nc71606670/nc_origin_nc71606670_1311633433000.xml";
	public static final String PAGER_PRODUCT_PATH = "etc/test_products/usa00040xz/us_losspager_usa00040xz_1287260989064.bin";

	public static final File DIRECTORY = new File(
			System.getProperty("user.home"), "Desktop/productio/");
	public static final File TEMP_DIRECTORY = new File(
			System.getProperty("user.home"), "Desktop/productio2/");

	private Product ORIGIN, PAGER;

	@Before
	public void setupTest() throws Exception {
		// turn off tracking during test
		ProductTracker.setTrackerEnabled(false);

		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINEST);

		ORIGIN = ObjectProductHandler.getProduct(new XmlProductSource(
				StreamUtils.getInputStream(new File(ORIGIN_PRODUCT_PATH))));
		PAGER = ObjectProductHandler.getProduct(new BinaryProductSource(
				StreamUtils.getInputStream(new File(PAGER_PRODUCT_PATH))));
	}

	/**
	 * Tests that given an appropriate origin product with an eqxml file the
	 * program will generate a file for different settings
	 * 
	 * @throws Exception
	 */
	@Test
	public void basicUsageTest() throws Exception {
		EIDSOutputWedge eids = new EIDSOutputWedge();
		Config config = new Config();

		// Test default settings
		File dir = eids.getDirectory();
		clearDirectory(dir);
		eids.onProduct(ORIGIN);
		String[] files = dir.list();
		Assert.assertTrue("No eqxml was generated using all default settings",
				files != null);
		clearDirectory(dir);

		// // Test using custom directory
		// // Test using file aside from eqxml (in this case contents.xml)
		// config.setProperty("directory", DIRECTORY.getPath());
		// config.setProperty("contentFile", "contents.xml");
		// eids = new EIDSOutputWedge();
		// eids.configure(config);
		// dir = eids.getDirectory();
		// clearDirectory(dir);
		// eids.onProduct(ORIGIN);
		// files = dir.list();
		// Assert.assertTrue("No file was generated using custom directory",
		// files != null);
		// clearDirectory(dir);

		// Test using custom temp directory
		config = new Config();
		config.setProperty("tempDirectory", TEMP_DIRECTORY.getPath());
		eids.configure(config);
		dir = eids.getDirectory();
		clearDirectory(dir);
		eids.onProduct(ORIGIN);
		files = dir.list();
		Assert.assertTrue("No eqxml was generated using custom temp directory",
				files != null);
		clearDirectory(dir);
		clearDirectory(TEMP_DIRECTORY);

	}

	/**
	 * Tests that given an appropriate product (in this case a pager product)
	 * without and eqxml file the program will not generate a file at the
	 * specified directory.
	 * 
	 * @throws Exception
	 */
	@Test
	public void noEqxmlTest() throws Exception {
		clearDirectory(DIRECTORY);

		EIDSOutputWedge eids = new EIDSOutputWedge();
		eids.onProduct(PAGER);

		File dir = DIRECTORY;
		String[] files = dir.list();

		Assert.assertTrue("File created for product with no eqxml",
				files == null || files.length == 0);

		clearDirectory(DIRECTORY);
	}

	@Test
	public void conversionTests() throws Exception {
		clearDirectory(DIRECTORY);

		EIDSOutputWedge eids = new EIDSOutputWedge();
		Config config = new Config();
		config.setProperty("directory", DIRECTORY.getPath());
		String[] outputs = { "eqxml.xml", "quakeml.xml", "cube.txt" };
		config.setProperty("outputFormat", "quakeml.xml");
		eids.configure(config);

		QuakemlProductCreatorTest productList = new QuakemlProductCreatorTest();
		List<Product> pList = productList.getTestQuakemlProducts();
		for (int i = 0; i < pList.size(); i++) {
			for (int j = 0; j < outputs.length; j++) {
				config.setProperty("outputFormat", outputs[j]);
				eids.configure(config);
				eids.onProduct(pList.get(i));
			}
		}

		String[] files = DIRECTORY.list();

		Assert.assertTrue("Number of files generated is incorrect",
				files.length == (pList.size() * outputs.length));
		clearDirectory(DIRECTORY);
	}

	/**
	 * Cleans up the directory before/after each test
	 */
	public void clearDirectory(File dir) {
		if (dir != null) {
			FileUtils.deleteTree(dir);
		}
	}
}
