/*
 * FileProductStorageTest
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the FileProductStorage class.
 */
public class FileProductStorageTest {

	/** Directory used to store products for testing. */
	public static File TESTING_DIRECTORY = new File(
			System.getProperty("user.home"), "test" + File.separator
					+ "storage");

	/** ProductTest object used to compare products. */
	private ProductTest productTest;
	/** One testing product. */
	private Product product1;
	/** Another testing product. */
	private Product product2;

	/**
	 * Set up objects for the testing. Creates the productTest and test product
	 * objects.
	 */
	@Before
	public void setupTestEnvironment() {
		productTest = new ProductTest();
		product1 = productTest.getProduct();
		product2 = productTest.getOtherProduct();
	}

	/**
	 * Clean up after testing. Deletes the testing storage directory.
	 */
	@After
	public void cleanupTestEnvironment() {
		FileUtils.deleteTree(TESTING_DIRECTORY);
	}

	/**
	 * Store a test product, make sure it was stored, then load and compare to
	 * original product.
	 * 
	 * @throws Exception
	 */
	@Test
	public void storeProduct() throws Exception {
		// empty the store
		FileUtils.deleteTree(TESTING_DIRECTORY);

		// construct a store
		FileProductStorage storage = new FileProductStorage(TESTING_DIRECTORY);

		// store a product
		ProductId productId = product1.getId();
		// make sure it doesn't already exist
		Assert.assertFalse("Product doesn't exist at start",
				storage.hasProduct(productId));
		ProductId storedId = storage.storeProduct(product1);
		// make sure the returned id matches
		Assert.assertEquals("Product Ids match", productId, storedId);
		// make sure the stored id exists
		Assert.assertTrue(storage.hasProduct(storedId));

		// load the stored product
		Product loadedProduct = storage.getProduct(productId);
		// make sure the same as the original product
		productTest.compareProducts(product1, loadedProduct);
	}

	/**
	 * Test removing a product. Store two products, remove one at a time.
	 * 
	 * @throws Exception
	 */
	@Test
	public void removeProduct() throws Exception {
		// empty the store
		FileUtils.deleteTree(TESTING_DIRECTORY);

		// construct a store
		FileProductStorage storage = new FileProductStorage(TESTING_DIRECTORY);

		// make sure products don't already exist
		ProductId productId1 = product1.getId();
		ProductId productId2 = product2.getId();
		Assert.assertFalse("Product1 doesn't exist",
				storage.hasProduct(productId1));
		Assert.assertFalse("Product2 doesn't exist",
				storage.hasProduct(productId2));

		// store both products
		storage.storeProduct(product1);
		storage.storeProduct(product2);
		Assert.assertTrue("Product1 exists after storing",
				storage.hasProduct(productId1));
		Assert.assertTrue("Product2 exists after storing",
				storage.hasProduct(productId2));

		// store a product that already exists
		try {
			storage.storeProduct(product1);
			Assert.fail("Exception should be thrown when product already in storage.");
		} catch (ProductAlreadyInStorageException e) {
			Assert.assertTrue("ProductAlreadyInStorageException thrown",
					e instanceof ProductAlreadyInStorageException);
		}

		// remove one product
		storage.removeProduct(productId1);
		Assert.assertFalse("Product1 doesn't exist after removing",
				storage.hasProduct(productId1));
		Assert.assertTrue("Product2 still exists after removing Product1",
				storage.hasProduct(productId2));

		// remove the other product
		storage.removeProduct(productId2);
		Assert.assertFalse("Product1 still doesn't exist",
				storage.hasProduct(productId1));
		Assert.assertFalse("Product2 doesn't exist after removing",
				storage.hasProduct(productId2));
	}

	/**
	 * Configure a FileProductStorage object using the Configurable interface.
	 * Then verify product content is stored within the configured directory.
	 * 
	 * @throws Exception
	 */
	@Test
	public void configureStore() throws Exception {

		// empty the store
		FileUtils.deleteTree(TESTING_DIRECTORY);

		// construct the product store using the configurable interface
		Config config = new Config();
		config.setProperty(Config.OBJECT_TYPE_PROPERTY,
				"gov.usgs.earthquake.distribution.FileProductStorage");
		config.setProperty(FileProductStorage.DIRECTORY_PROPERTY_NAME,
				TESTING_DIRECTORY.getCanonicalPath());
		config.store(System.out, "test");
		FileProductStorage storage = (FileProductStorage) config.getObject();

		// store a product
		storage.storeProduct(product1);
		Product loadedProduct = storage.getProduct(product1.getId());

		// load the content back, and make sure it is in storage directory.
		Content content = loadedProduct.getContents().get("test.txt");
		Assert.assertTrue("Content is an instance of FileContent",
				content instanceof FileContent);
		Assert.assertTrue(((FileContent) content).getFile().getCanonicalPath()
				.startsWith(TESTING_DIRECTORY.getCanonicalPath()));

	}

	/**
	 * Checks that a ProductAlreadyInStorageException exception is thrown when
	 * product already exists in storage.
	 * 
	 * @throws Exception
	 */
	@Test
	public void addDuplicateProduct() throws Exception {
		// empty the store
		FileUtils.deleteTree(TESTING_DIRECTORY);

		// construct a store
		FileProductStorage storage = new FileProductStorage(TESTING_DIRECTORY);

		storage.storeProduct(product1);
		try {
			storage.storeProduct(product1);
			Assert.fail("Product already in storage. Exception not thrown");
		} catch (ProductAlreadyInStorageException e) {
			// Test passed
		}
	}

	@Test
	public void verifyProductSignature() throws Exception {
		// empty the store
		FileUtils.deleteTree(TESTING_DIRECTORY);

		// construct a store
		FileProductStorage storage = new FileProductStorage(TESTING_DIRECTORY);
		product1.sign(ProductTest.SIGNATURE_KEY_PAIR.getPrivate());
		Logger.getLogger("").info("Signature=" + product1.getSignature());
		ProductKeyChain chain = new ProductKeyChain();
		ProductKey key = new ProductKey(
				ProductTest.SIGNATURE_KEY_PAIR.getPublic(), null, null);
		// key.getSources().add(product1.getId().getSource());
		// key.getTypes().add(product1.getId().getType());
		key.setName("testKey");
		chain.getKeychain().add(key);

		storage.setKeychain(chain);
		storage.setRejectInvalidSignatures(true);

		// Store a product signed with a key on the keychain
		storage.storeProduct(product1);

		try {
			// Store a product without a signature
			storage.storeProduct(product2);
			Assert.fail();
		} catch (InvalidSignatureException e) {
			// Test passed
		}

	}

	@Test
	public void notifyListeners() {
		Assert.assertTrue("TODO :: Implement notifyListener test.", true);
	}

	@Test
	public void legacyStorageTest() throws Exception {
		// empty the store
		FileUtils.deleteTree(TESTING_DIRECTORY);

		// construct a store
		FileProductStorage primary = new FileProductStorage(new File(
				TESTING_DIRECTORY, "primary"));
		primary.setName("primary");
		primary.setRejectInvalidSignatures(false);
		FileProductStorage legacy = new FileProductStorage(new File(
				TESTING_DIRECTORY, "legacy"));
		legacy.setName("legacy");
		legacy.setRejectInvalidSignatures(false);
		primary.getLegacyStorages().add(legacy);

		legacy.storeProduct(product1);
		Assert.assertTrue("primary finds legacy products",
				primary.hasProduct(product1.getId()));

		primary.storeProduct(product2);
		Assert.assertFalse("primary stores new products",
				legacy.hasProduct(product2.getId()));

		primary.removeProduct(product1.getId());
		Assert.assertFalse("primary removes products from legacy",
				legacy.hasProduct(product1.getId()));
	}
}