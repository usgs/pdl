package gov.usgs.earthquake.eids;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.DefaultNotificationListener;
import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.distribution.JDBCNotificationIndex;
import gov.usgs.earthquake.distribution.Notification;
import gov.usgs.earthquake.distribution.NotificationIndex;
import gov.usgs.earthquake.distribution.ProductKey;
import gov.usgs.earthquake.distribution.ProductKeyChain;
import gov.usgs.earthquake.distribution.ProductTracker;
import gov.usgs.earthquake.distribution.SocketProductReceiver;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.CryptoUtils;
import gov.usgs.util.FileUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EIDSInputWedgeTest {

	private File testFile1 = new File("etc/test_products/cube/event_example1");
	private ProductId testFile1Id = ProductId
			.parse("urn:usgs-product:ci:origin:ci09082344:1323449943000");
	private ProductId testFile1InternalId = ProductId
			.parse("urn:usgs-product:ci:internal-origin:ci09082344:1323449943000");
	private ProductId testFile1ScenarioId = ProductId
			.parse("urn:usgs-product:ci:origin-scenario:ci09082344:1323449943000");

	private File testFile2 = new File("etc/test_products/cube/event_example2");
	private ProductId testFile2Id = ProductId
			.parse("urn:usgs-product:us:origin:usmeav:1323449944000");

	private File testFile3 = new File("etc/test_products/cube/event_example3");
	private ProductId testFile3Id = ProductId
			.parse("urn:usgs-product:nc:origin:nc71767785:1334941781000");

	private File publicKey = new File("etc/test_products/test_key/testkey.pub");
	private File privateKey = new File("etc/test_products/test_key/testkey");

	private File storageDirectory = new File("testStorage");
	private File indexFile = new File("testIndex");

	private SocketProductReceiver receiver;
	private FileProductStorage storage;
	private NotificationIndex index;
	private TestNotificationListener listener;

	@Before
	public void setup() throws Exception {
		// turn off tracking during test
		ProductTracker.setTrackerEnabled(false);

		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINE);
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINE);

		// make sure storage is clean
		FileUtils.deleteTree(storageDirectory);
		FileUtils.deleteTree(indexFile);

		storage = new FileProductStorage(storageDirectory);
		storage.setName("test_storage");

		// set up a keychain to verify products are being signed
		ProductKey key = new ProductKey();
		key.setKey(CryptoUtils.readOpenSSHPublicKey(FileUtils
				.readFile(publicKey)));
		key.getTypes().add("origin");
		key.getTypes().add("internal-origin");
		key.getTypes().add("origin-scenario");
		ProductKeyChain keychain = new ProductKeyChain();
		keychain.getKeychain().add(key);
		storage.setKeychain(keychain);

		index = new JDBCNotificationIndex(indexFile.getCanonicalPath());
		index.setName("test_index");

		receiver = new SocketProductReceiver();
		receiver.setProductStorage(storage);
		receiver.setNotificationIndex(index);
		receiver.setName("test_receiver");

		listener = new TestNotificationListener();
		listener.setName("test_listener");
		listener.setIncludeInternals(true);
		listener.setIncludeScenarios(true);
		receiver.addNotificationListener(listener);

		receiver.startup();
	}

	@After
	public void teardown() throws Exception {
		receiver.shutdown();

		FileUtils.deleteTree(storageDirectory);
		FileUtils.deleteTree(indexFile);
	}

	/**
	 * Send one product using the --file=FILE argument, and verify it parsed and
	 * sent.
	 * 
	 * @throws Exception
	 */
	@Test
	public void sendOneProduct() throws Exception {
		new EIDSInputWedge().run(new String[] {
				// file to read as product
				EIDSInputWedge.FILE_ARGUMENT + testFile1.getCanonicalPath(),
				// private key file for signature
				EIDSInputWedge.PRIVATE_KEY_ARGUMENT
						+ privateKey.getCanonicalPath(),
				EIDSInputWedge.PARSER_ARGUMENT
						+ EQMessageProductCreator.class.getName(),
				// server to send products to
				EIDSInputWedge.SERVERS_ARGUMENT + "localhost:11235" });

		Product product = listener.getLastProduct();
		Assert.assertNotNull("product not null", product);

		// must check isSameProduct, because send time is assigned when
		// converted from CUBE
		Assert.assertTrue("is same product",
				product.getId().isSameProduct(testFile1Id));

		Assert.assertTrue("product signature verifies", product
				.verifySignature(new PublicKey[] { CryptoUtils
						.readOpenSSHPublicKey(FileUtils.readFile(publicKey)) }));

		// clear before next test
		storage.removeProduct(product.getId());
		Iterator<Notification> iter = index.findNotifications(product.getId())
				.iterator();
		while (iter.hasNext()) {
			index.removeNotification(iter.next());
		}
	}

	/**
	 * Send one product using the --file=FILE argument, and verify it parsed and
	 * sent.
	 * 
	 * @throws Exception
	 */
	@Test
	public void sendInternalProduct() throws Exception {
		new EIDSInputWedge().run(new String[] {
				// file to read as product
				EIDSInputWedge.FILE_ARGUMENT + testFile1.getCanonicalPath(),
				// private key file for signature
				EIDSInputWedge.PRIVATE_KEY_ARGUMENT
						+ privateKey.getCanonicalPath(),
				EIDSInputWedge.PARSER_ARGUMENT
						+ EQMessageProductCreator.class.getName(),
				// server to send products to
				EIDSInputWedge.SERVERS_ARGUMENT + "localhost:11235",
				// make internal
				EIDSInputWedge.CREATE_INTERNAL_PRODUCTS });

		Product product = listener.getLastProduct();
		Assert.assertNotNull("product not null", product);

		// must check isSameProduct, because send time is assigned when
		// converted from CUBE
		Assert.assertTrue("is same product",
				product.getId().isSameProduct(testFile1InternalId));

		Assert.assertTrue("product signature verifies", product
				.verifySignature(new PublicKey[] { CryptoUtils
						.readOpenSSHPublicKey(FileUtils.readFile(publicKey)) }));

		// clear before next test
		storage.removeProduct(product.getId());
		Iterator<Notification> iter = index.findNotifications(product.getId())
				.iterator();
		while (iter.hasNext()) {
			index.removeNotification(iter.next());
		}
	}

	/**
	 * Send one product using the --file=FILE argument, and verify it parsed and
	 * sent.
	 * 
	 * @throws Exception
	 */
	@Test
	public void sendScenarioProduct() throws Exception {
		listener.setLastProduct(null);

		new EIDSInputWedge().run(new String[] {
				// file to read as product
				EIDSInputWedge.FILE_ARGUMENT + testFile1.getCanonicalPath(),
				// private key file for signature
				EIDSInputWedge.PRIVATE_KEY_ARGUMENT
						+ privateKey.getCanonicalPath(),
				EIDSInputWedge.PARSER_ARGUMENT
						+ EQMessageProductCreator.class.getName(),
				// server to send products to
				EIDSInputWedge.SERVERS_ARGUMENT + "localhost:11235",
				// make internal
				EIDSInputWedge.CREATE_SCENARIO_PRODUCTS });

		Product product = listener.getLastProduct();
		Assert.assertNotNull("product not null", product);

		// must check isSameProduct, because send time is assigned when
		// converted from CUBE
		Assert.assertTrue("is same product",
				product.getId().isSameProduct(testFile1ScenarioId));

		Assert.assertTrue("product signature verifies", product
				.verifySignature(new PublicKey[] { CryptoUtils
						.readOpenSSHPublicKey(FileUtils.readFile(publicKey)) }));

		// clear before next test
		storage.removeProduct(product.getId());
		Iterator<Notification> iter = index.findNotifications(product.getId())
				.iterator();
		while (iter.hasNext()) {
			index.removeNotification(iter.next());
		}
	}

	/**
	 * Set up a poll directory, and move multiple files into the polldirectory.
	 * 
	 * @throws Exception
	 */
	// @Test
	public void pollForThreeProducts() throws Exception {
		File polldir = new File("testPolldir");
		FileUtils.deleteTree(polldir);
		File storagedir = new File("testStoragedir");
		FileUtils.deleteTree(storagedir);

		EIDSInputWedge inputWedge = new EIDSInputWedge();
		inputWedge.run(new String[] {
				// run continuously
				EIDSInputWedge.POLL_ARGUMENT,
				// private key file for signature
				EIDSInputWedge.PRIVATE_KEY_ARGUMENT
						+ privateKey.getCanonicalPath(),
				EIDSInputWedge.PARSER_ARGUMENT
						+ EQMessageProductCreator.class.getName(),
				// server to send products to
				EIDSInputWedge.SERVERS_ARGUMENT + "localhost:11235",
				// poll directory
				EIDSInputWedge.POLLDIR_ARGUMENT + polldir.getCanonicalPath(),
				// old input directory
				EIDSInputWedge.STORAGEDIR_ARGUMENT
						+ storagedir.getCanonicalPath() });

		listener.setLastProduct(null);
		moveToPolldir(polldir, testFile1, new File("testFile1"));
		Product product = listener.getLastProduct();
		Assert.assertTrue("product 1 received",
				product.getId().isSameProduct(testFile1Id));
		Assert.assertTrue("product 1 signature verifies", product
				.verifySignature(new PublicKey[] { CryptoUtils
						.readOpenSSHPublicKey(FileUtils.readFile(publicKey)) }));

		listener.setLastProduct(null);
		moveToPolldir(polldir, testFile2, new File("testFile2"));
		product = listener.getLastProduct();
		Assert.assertTrue("product 2 received",
				product.getId().isSameProduct(testFile2Id));
		Assert.assertTrue("product 2 signature verifies", product
				.verifySignature(new PublicKey[] { CryptoUtils
						.readOpenSSHPublicKey(FileUtils.readFile(publicKey)) }));

		listener.setLastProduct(null);
		moveToPolldir(polldir, testFile3, new File("testFile3"));
		product = listener.getLastProduct();
		Assert.assertTrue("product 3 received",
				product.getId().isSameProduct(testFile3Id));
		Assert.assertTrue("product 3 signature verifies", product
				.verifySignature(new PublicKey[] { CryptoUtils
						.readOpenSSHPublicKey(FileUtils.readFile(publicKey)) }));

		// done, so shutdown persistent wedge
		inputWedge.shutdown();
	}

	public void moveToPolldir(final File polldir, final File source,
			final File tempfile) throws IOException {
		FileUtils.writeFile(tempfile, FileUtils.readFile(source));
		tempfile.renameTo(new File(polldir, tempfile.getName()));
	}

	private class TestNotificationListener extends DefaultNotificationListener {

		private Product lastProduct;
		private Object syncObject = new Object();

		@Override
		public void onProduct(final Product product) throws Exception {
			synchronized (syncObject) {
				this.lastProduct = product;
				System.err.println("listener received "
						+ product.getId().toString());
				syncObject.notify();
			}
		}

		public Product getLastProduct() throws InterruptedException {
			synchronized (syncObject) {
				while (lastProduct == null) {
					syncObject.wait();
				}
			}
			return lastProduct;
		}

		public void setLastProduct(Product lastProduct) {
			this.lastProduct = lastProduct;
		}

		@Override
		public int getMaxTries() {
			return 1;
		}

		@Override
		public long getTimeout() {
			return 0;
		}

	}
}
