package gov.usgs.earthquake.dyfi;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.distribution.ProductAlreadyInStorageException;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;

import java.io.File;
import java.util.Date;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DYFILegacyStorageTest {

	private DYFILegacyStorage storage = null;

	private static final String STORAGE_SECTION_NAME = "legacy_storage";
	private static final String STORAGE_DIRECTORY = "/tmp/dyfiLegacy";
	private static final String STORAGE_TYPE = "gov.usgs.earthquake.dyfi.DYFILegacyStorage";

	@Before
	public void before() throws Exception {
		// -- Configure the legacy storage
		Config config = new Config();
		config.setSectionProperty(STORAGE_SECTION_NAME,
				Config.OBJECT_TYPE_PROPERTY, STORAGE_TYPE);
		config.setSectionProperty(STORAGE_SECTION_NAME,
				FileProductStorage.DIRECTORY_PROPERTY_NAME, STORAGE_DIRECTORY);

		Config.setConfig(config);

		storage = (DYFILegacyStorage) Config.getConfig().getObject(
				STORAGE_SECTION_NAME);
	}

	@After
	public void after() throws Exception {
		// Remove the storage directory
		FileUtils.deleteTree(new File(STORAGE_DIRECTORY));
	}

	@Test
	public void testGetPath() throws Exception {
		Product product = DYFIIndexerWedgeTest.createDYFIProduct();

		// Build the directory where we think this product will be stored
		File productDir = new File(storage.getBaseDirectory(),
				storage.getProductPath(product.getId()));

		// Make sure it doesn't already exist
		Assert.assertFalse("Directory already existed.", productDir.exists());
		
		// Store the product, this directory should get created
		storage.storeProduct(product);
		
		// Make sure the directory now exists
		Assert.assertTrue("Directory still doesn't exist.", productDir.exists());
	}

	@Test
	public void testHasProduct() throws Exception {
		Product product = DYFIIndexerWedgeTest.createDYFIProduct();

		// Make sure it isn't already in storage
		Assert.assertFalse("Product storage had product before adding.",
				storage.hasProduct(product.getId()));

		// Add it
		storage.storeProduct(product);

		// Make sure it is in storage after adding
		Assert.assertTrue("Storage didn't have product after storing.",
				storage.hasProduct(product.getId()));

		// Update it to see if we still think we have the **updated** version.
		ProductId newId = product.getId();
		newId.setUpdateTime(new Date());
		product.setId(newId);

		Assert.assertFalse("Storage still thinks we have updated product.",
				storage.hasProduct(product.getId()));

		// Update it
		storage.storeProduct(product);

		// Make sure we now have the updated version
		Assert.assertTrue("Storage doesn't recognize the updated version.",
				storage.hasProduct(product.getId()));

		// Remove it
		storage.removeProduct(product.getId());

		// Make sure it is gone
		Assert.assertFalse("Storage still had product after removing.",
				storage.hasProduct(product.getId()));
	}

	@Test(expected = ProductAlreadyInStorageException.class)
	public void testAddDuplicateProduct() throws Exception {
		Product product = DYFIIndexerWedgeTest.createDYFIProduct();

		// Add the product
		storage.storeProduct(product);

		// Try to store it again (Exception)
		storage.storeProduct(product);
	}

	@Test
	public void testGetFilePath() {

	}
}
