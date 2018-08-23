package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.dyfi.DYFIIndexerWedgeTest;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;

import java.io.File;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReplicationStorageListenerTest {

	private static final String LOCAL_STORAGE_SECTION = "localStorage";
	private static final String REMOTE_STORAGE_SECTION = "remoteStorage";
	private static final String REPLICATION_SECTION = "replicator";

	private static final String scratchStoragePath = System
			.getProperty("user.dir")
			+ System.getProperty("file.separator")
			+ "storage_test";
	private static final String localStoragePath = "localStorage";
	private static final String remoteStoragePath = "remoteStorage";

	private FileProductStorage localStorage = null;
	private FileProductStorage remoteStorage = null;

	@Before
	public void before() throws Exception {
		// Create the config object for the tests
		Config config = new Config();

		// Configure the local storage
		config.setSectionProperty(LOCAL_STORAGE_SECTION,
				Config.OBJECT_TYPE_PROPERTY,
				"gov.usgs.earthquake.distribution.FileProductStorage");
		File localStorageDir = new File(scratchStoragePath, localStoragePath);
		localStorageDir.mkdirs();
		config.setSectionProperty(LOCAL_STORAGE_SECTION,
				FileProductStorage.DIRECTORY_PROPERTY_NAME,
				localStorageDir.getCanonicalPath());
		config.setSectionProperty(LOCAL_STORAGE_SECTION,
				FileProductStorage.STORAGE_LISTENER_PROPERTY,
				REPLICATION_SECTION);

		// Configure the remote storage
		config.setSectionProperty(REMOTE_STORAGE_SECTION,
				Config.OBJECT_TYPE_PROPERTY,
				"gov.usgs.earthquake.distribution.FileProductStorage");
		File remoteStorageDir = new File(scratchStoragePath, remoteStoragePath);
		remoteStorageDir.mkdirs();
		config.setSectionProperty(REMOTE_STORAGE_SECTION,
				FileProductStorage.DIRECTORY_PROPERTY_NAME,
				remoteStorageDir.getCanonicalPath());

		// Configure the replicator
		config.setSectionProperty(REPLICATION_SECTION,
				Config.OBJECT_TYPE_PROPERTY,
				"gov.usgs.earthquake.distribution.ReplicationStorageListener");
		config.setSectionProperty(REPLICATION_SECTION,
				ReplicationStorageListener.ARCHIVE_FLAG_PROPERTY, "true");
		config.setSectionProperty(REPLICATION_SECTION,
				ReplicationStorageListener.REPL_CMD_PROPERTY, "rsync");
		config.setSectionProperty(REPLICATION_SECTION,
				ReplicationStorageListener.REPL_MAX_TRIES_PROPERTY, "1");
		config.setSectionProperty(REPLICATION_SECTION,
				ReplicationStorageListener.REPL_TIMEOUT_PROPERTY, "0");
		config.setSectionProperty(
				REPLICATION_SECTION,
				ReplicationStorageListener.REPL_HOSTS_PROPERTY,
				"127.0.0.1:"
						+ (new File(scratchStoragePath, remoteStoragePath))
								.getCanonicalPath() + "");

		Config.setConfig(config);

		// Create the scratch working directory
		(new File(scratchStoragePath)).mkdirs();

		// Set up working environment
		localStorage = (FileProductStorage) config
				.getObject(LOCAL_STORAGE_SECTION);
		remoteStorage = (FileProductStorage) config
				.getObject(REMOTE_STORAGE_SECTION);

		localStorage.startup();
		remoteStorage.startup();
	}

	@After
	public void after() throws Exception {
		localStorage.shutdown();
		remoteStorage.shutdown();

		// Remote the scratch dir
		FileUtils.deleteTree(new File(scratchStoragePath));
	}

	@Test
	public void testReplicationAddRemove() throws Exception {

		// Store a product into local storage
		Product product = DYFIIndexerWedgeTest.createDYFIProduct();

		// Store another product hehe.
		Product product2 = (new ProductTest()).getProduct();

		System.err.println("Adding first product to local storage.");
		localStorage.storeProduct(product);
		System.err.println("Adding second product to local storage.");
		localStorage.storeProduct(product2);

		// Wait for just a bit to let the product replicate
		Thread.sleep(2500L);

		// Make sure the remote storage has the product
		System.err.println("Checking if remote storage has first product.");
		Assert.assertTrue("Remote storage did not have first product.",
				remoteStorage.hasProduct(product.getId()));

		System.err.println("Checking if remote storage has second product.");
		Assert.assertTrue("Remote storage did not have second product.",
				remoteStorage.hasProduct(product2.getId()));

		// Now remove the product
		System.err.println("Removing first product from local storage.");
		localStorage.removeProduct(product.getId());

		// Wait for just a bit to let the removal replicate
		Thread.sleep(2500L);

		System.err.println("Checking if first product was removed from " +
				"remote storage.");
		Assert.assertFalse("Remote storage still had product after delete.",
				remoteStorage.hasProduct(product.getId()));
	}

}
