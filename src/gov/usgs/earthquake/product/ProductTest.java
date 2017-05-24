/*
 * ProductTest
 */
package gov.usgs.earthquake.product;

import gov.usgs.util.CryptoUtils;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Date;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the product class.
 * 
 * Test signatures and copy constructor. Also provides testing utilities to
 * create a test product, and compare two products to each other.
 */
public class ProductTest {

	public static KeyPair SIGNATURE_KEY_PAIR;
	static {
		try {
			SIGNATURE_KEY_PAIR = CryptoUtils
					.generateDSAKeyPair(CryptoUtils.DSA_1024);
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * Sign product, verify signature, then modify product and check that
	 * signature no longer verifies.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSignature() throws Exception {
		Product product = getProduct();
		KeyPair keyPair = CryptoUtils.generateDSAKeyPair(CryptoUtils.DSA_1024);

		Assert.assertFalse("Unsigned products do not verify signature", product
				.verifySignature(new PublicKey[] { keyPair.getPublic() }));

		product.sign(keyPair.getPrivate());
		Assert.assertTrue("Public key verifies signature correctly", product
				.verifySignature(new PublicKey[] { keyPair.getPublic() }));

		// modify product
		product.getId().setUpdateTime(
				new Date(product.getId().getUpdateTime().getTime() + 1000));
		Assert.assertFalse(
				"Public key doesn't verify after modification",
				product.verifySignature(new PublicKey[] { keyPair.getPublic() }));
	}

	/**
	 * Verify copy constructor works.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCopyConstructor() throws Exception {
		Product product = getProduct();
		Product product2 = new Product(product);
		compareProducts(product, product2);
	}

	/**
	 * Build a product for use during testing.
	 * 
	 * @return product for use during testing.
	 */
	public Product getProduct() {
		// BUILD PRODUCT
		ProductId id = ProductId.parse("urn:usgs-product:us:shakemap:abcd1234:"
				+ new Date().getTime());
		Product product = new Product(id);

		try {
			product.setTrackerURL(new URL("http://localhost/tracker"));
		} catch (Exception e) {
			// ignore
		}

		ByteContent inlineContent = new ByteContent(
				("I am <em>inline</em> html content").getBytes());
		inlineContent.setContentType("text/html");
		product.getContents().put("", inlineContent);

		product.getContents().put(
				"test.txt",
				new ByteContent(("I am a test product content\n"
						+ "that has several lines of text\n"
						+ "blah, blah, blah, content, blah").getBytes()));

		try {
			// URL eidsInstaller = new URL(
			// "http://earthquake.usgs.gov/research/software/eids/EIDSInstaller.jar");
			FileContent file = new FileContent(new File(
					"etc/examples/hub/bin/EIDSInstaller.jar"));
			product.getContents().put("jar/EIDSInstaller.jar", file);
		} catch (Exception e) {
			// ignore
		}

		product.getProperties().put("testprop", "testvalue");
		try {
			product.addLink("testrelation", new URI("http://google.com/"));
		} catch (Exception e) {
			e.printStackTrace();
			// ignore
		}

		try {
			// generate a signature
			product.sign(SIGNATURE_KEY_PAIR.getPrivate());
		} catch (Exception e) {
			System.err.println(e);
		}

		return product;
	}

	/**
	 * Build another product for use during testing.
	 * 
	 * @return product for use during testing.
	 */
	public Product getOtherProduct() {
		// BUILD PRODUCT
		ProductId id = ProductId
				.parse("urn:usgs-product:us:shakemap2:efgh5678:"
						+ new Date().getTime());
		Product product = new Product(id);

		try {
			product.setTrackerURL(new URL("http://localhost/tracker"));
		} catch (Exception e) {
			// ignore
		}

		product.getContents().put(
				"test2.txt",
				new ByteContent(("I am also test product content\n"
						+ "that has several lines of text\n"
						+ "blah, blah, blah, content, blah").getBytes()));

		product.getProperties().put("testprop2", "testvalue2");
		try {
			product.addLink("testrelation2", new URI("http://google.com/"));
		} catch (Exception e) {
			e.printStackTrace();
			// ignore
		}

		return product;
	}

	/**
	 * Comparison method to make sure two products are equivalent.
	 * 
	 * @param expected
	 *            the first product to compare.
	 * @param actual
	 *            the second product to compare.
	 * @throws Exception
	 */
	public void compareProducts(final Product expected, final Product actual)
			throws Exception {

		Assert.assertNotNull("Expected product is null", expected);
		Assert.assertNotNull("Actual product is null", actual);

		Assert.assertEquals("Product IDs are same", expected.getId(),
				actual.getId());

		// make
		Assert.assertEquals("Statuses are same", expected.getStatus(),
				actual.getStatus());

		// make sure both products have same properties
		Assert.assertEquals("Properties are same", expected.getProperties(),
				actual.getProperties());

		// make sure both products have same links
		Assert.assertEquals("Links are same", expected.getLinks(),
				actual.getLinks());

		// make sure both products have same contents
		Assert.assertEquals("Content keys are same", expected.getContents()
				.keySet(), actual.getContents().keySet());

		// compare all product content
		Iterator<String> iter = expected.getContents().keySet().iterator();
		while (iter.hasNext()) {
			String path = iter.next();
			ByteContent bytes = new ByteContent(new InputStreamContent(expected
					.getContents().get(path)));
			ByteContent bytes2 = new ByteContent(new InputStreamContent(actual
					.getContents().get(path)));
			Assert.assertArrayEquals("Content '" + path + "' is same",
					bytes.getByteArray(), bytes2.getByteArray());
		}

		// make sure each product's content's content types are the same,
		for (String key : expected.getContents().keySet()) {
			Assert.assertEquals("Content type for " + key + " are the same",
					expected.getContents().get(key).getContentType(), actual
							.getContents().get(key).getContentType());
		}

		// make sure each product's content's length are the same
		for (String key : expected.getContents().keySet()) {
			Assert.assertEquals("Content length for " + key + " are the same",
					expected.getContents().get(key).getLength(), actual
							.getContents().get(key).getLength());
		}

		// check if signatures match
		Assert.assertEquals("Signatures are same", expected.getSignature(),
				actual.getSignature());
	}

}
