/*
 * CLIProductBuilderTest
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StreamUtils;

import java.io.InputStream;

import org.junit.Test;
import org.junit.Assert;

/**
 * Test CLIProductBuilder argument parsing.f
 */
public class CLIProductBuilderTest {

	public static String[] BUILDER_ARGS = new String[] { "--type=testtype",
			"--code=testcode", "--source=testsource", "--status=HELLO",
			"--trackerURL=http://earthquake.usgs.gov/",
			"--property-test=testvalue", "--latitude=34.123",
			"--longitude=-118.456",
			"--link-related=urn:usgs-product:us:shakemap:us2009abcd",
			"--content", "--contentType=text/fancy" };

	public static String[] INCOMPLETE_BUILDER_ARGS = new String[] {
			"--type=testtype", "--code=testcode", "--delete"
	// missing source
	};

	public static String TEST_CONTENT = "I am some test content\nwith newlines\nand other stuff.";

	/**
	 * Build a product using valid arguments.
	 * 
	 * @throws Exception
	 */
	@Test
	public void buildProduct() throws Exception {
		// save system.in for later
		InputStream oldSystemIn = System.in;
		System.setIn(StreamUtils.getInputStream(TEST_CONTENT));

		// build and send product
		CLIProductBuilder builder = new CLIProductBuilder(BUILDER_ARGS);
		TestProductSender sender = new TestProductSender();
		builder.getSenders().add(sender);
		builder.sendProduct(builder.buildProduct());

		// restore system.in
		System.setIn(oldSystemIn);

		// verify the build product matches expected results
		Product product = sender.getSentProduct();
		Assert.assertNotNull("Built product is not null", product);

		ProductId id = product.getId();
		Assert.assertEquals("type equals", "testtype", id.getType());
		Assert.assertEquals("code equals", "testcode", id.getCode());
		Assert.assertEquals("source equals", "testsource", id.getSource());
		Assert.assertEquals("status equals", "HELLO", product.getStatus());
		Assert.assertEquals("property equals", "testvalue", product
				.getProperties().get("test"));
		Assert.assertEquals("latitude equals", "34.123", product
				.getProperties().get("latitude"));
		Assert.assertEquals("longitude equals", "-118.456", product
				.getProperties().get("longitude"));
		Assert.assertArrayEquals(
				"content equals",
				TEST_CONTENT.getBytes(),
				((ByteContent) product.getContents().values().iterator().next())
						.getByteArray());

	}

	/**
	 * Unable to build product using invalid arguments.
	 */
	@Test
	public void buildIncompleteProduct() {
		try {
			CLIProductBuilder builder = new CLIProductBuilder(
					INCOMPLETE_BUILDER_ARGS);
			Product product = builder.buildProduct();
			Assert.fail("Incomplete arguments should not build a product ("
					+ product.getId().toString() + ")");
		} catch (Exception e) {
			Assert.assertTrue(
					"Incomplete arguments throw IllegalArgumentException",
					e instanceof IllegalArgumentException);
		}
	}

	private static class TestProductSender extends DefaultConfigurable
			implements ProductSender {

		/** The product that was sent. */
		private Product sentProduct;

		/**
		 * Create a new TestProductSender object.
		 */
		public TestProductSender() {
		}

		/**
		 * Receive the actual product being sent.
		 * 
		 * The product is compared to the expected product using the ProductTest
		 * class.
		 */
		public void sendProduct(Product product) throws Exception {
			this.sentProduct = product;
		}

		/**
		 * @return the last product that was sent.
		 */
		public Product getSentProduct() {
			return this.sentProduct;
		}

		public void configure(Config config) throws Exception {
		}

		public void shutdown() throws Exception {
		}

		public void startup() throws Exception {
		}

	}

}
