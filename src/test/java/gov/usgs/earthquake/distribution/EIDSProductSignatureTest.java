package gov.usgs.earthquake.distribution;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import gov.usgs.ansseqmsg.EQMessage;
import gov.usgs.earthquake.eids.EQMessageProductCreator;
import gov.usgs.earthquake.eqxml.EQMessageParser;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This test demonstrates a bug with setting custom content types on product
 * contents.
 * 
 * The resolution involves updating DirectorySource/Handler to preserve mime
 * type information. For now, not sending custom mime types.
 */
public class EIDSProductSignatureTest {

	@Before
	public void setup() throws Exception {
		// turn off tracking during test
		ProductTracker.setTrackerEnabled(false);

		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINEST);
	}

	@Test
	public void testSignedProduct() throws Exception {
		// create new product based on eqxml.xml
		String eqxml = new String(StreamUtils.readStream(StreamUtils
				.getInputStream(new File(
						"etc/test_products/eqxml/eqxml_event.xml"))));
		EQMessage message = EQMessageParser.parse(eqxml);
		List<Product> products = new EQMessageProductCreator()
				.getEQMessageProducts(message);
		Product product = products.get(0);
		product.setTrackerURL(new URL("http://localhost/"));
		// sign product
		product.sign(ProductTest.SIGNATURE_KEY_PAIR.getPrivate());

		// set up receiver with signature verification, and key to verify
		ProductKey productKey = new ProductKey();
		productKey.setKey(ProductTest.SIGNATURE_KEY_PAIR.getPublic());
		productKey.getTypes().add("origin");
		productKey.getTypes().add("focal-mechanism");

		ProductKeyChain keychain = new ProductKeyChain();
		keychain.getKeychain().add(productKey);

		FileProductStorage storage = new FileProductStorage();
		storage.setRejectInvalidSignatures(true);
		storage.setKeychain(keychain);

		// make sure storage is empty
		FileUtils.deleteTree(storage.getBaseDirectory());
		// same for index
		new File(JDBCNotificationIndex.JDBC_FILE_PROPERTY).delete();

		DefaultNotificationReceiver receiver = new DefaultNotificationReceiver();
		receiver.setProductStorage(storage);
		receiver.setNotificationIndex(new JDBCNotificationIndex());
		receiver.startup();

		try {
			receiver.storeProductSource(new ObjectProductSource(product));
		} catch (InvalidSignatureException ise) {
			Assert.fail("invalid signature");
		}
	}

}
