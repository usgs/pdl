package gov.usgs.earthquake.eids;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.eidsutil.EIDSMessageEvent;
import gov.usgs.earthquake.eidsutil.QWEmbeddedClient;
import gov.usgs.earthquake.product.Product;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Test the EIDSProductBuilder by loading known EQXML messages and verifying the
 * generated products.
 */
public class EIDSProductBuilderTest {

	/** File with EQXML event. */
	public static final String EQXML_EVENT_MESSAGE = "etc/test_products/eqxml/eqxml_event.xml";

	/** File with EQXML delete. */
	public static final String EQXML_DELETE_MESSAGE = "etc/test_products/eqxml/eqxml_delete.xml";

	/** File with EQXML product link. */
	public static final String EQXML_PRODUCTLINK_MESSAGE = "etc/test_products/eqxml/eqxml_productlink.xml";

	/** File with eventaddon xml. */
	public static final String EVENTADDON_MESSAGE = "etc/test_products/eqxml/eventaddon.xml";

	private QWEmbeddedClient messageEventSource;
	private TestProductSender sender;
	private EIDSProductBuilder builder;

	@Before
	public void setupTest() {
		messageEventSource = new QWEmbeddedClient();
		builder = new EIDSProductBuilder();
		sender = new TestProductSender();
		builder.addProductSender(sender);
	}

	/**
	 * Builder accepts messages via eids, this creates an object similar to what
	 * eids would send for testing purposes.
	 * 
	 * @param rootNamespace
	 * @param rootElement
	 * @param message
	 * @return
	 */
	public EIDSMessageEvent getEIDSMessageEvent(final String rootNamespace,
			final String rootElement, final String message) {
		EIDSMessageEvent event = new EIDSMessageEvent(messageEventSource, 123L,
				new Date(), "testFeederSourceHost", 123L, rootNamespace,
				rootElement, message);

		return event;
	}

	@Test
	public void testEQXMLEvent() throws IllegalArgumentException, IOException {
		EIDSMessageEvent event = getEIDSMessageEvent(
				null, // namespace
				"EQMessage", // root element
				new String(StreamUtils.readStream(StreamUtils
						.getInputStream(new File(EQXML_EVENT_MESSAGE)))));
		builder.onEIDSMessage(event);
		List<Product> products = sender.getProducts();
		Assert.assertEquals("one product created from event", 1,
				products.size());
		Assert.assertEquals("product is origin", products.get(0).getId()
				.getType(), "origin");
		// Assert.assertEquals("second product is magnitude", products.get(1)
		// .getId().getType(), "magnitude");
		Assert.assertNotNull("origin product has contents.xml", products.get(0)
				.getContents().get(EQMessageProductCreator.CONTENTS_XML_PATH));
	}

	@Test
	public void testEQXMLDelete() throws IllegalArgumentException, IOException {
		EIDSMessageEvent event = getEIDSMessageEvent(
				null, // namespace
				"EQMessage", // root element
				new String(StreamUtils.readStream(StreamUtils
						.getInputStream(new File(EQXML_DELETE_MESSAGE)))));
		builder.onEIDSMessage(event);
		List<Product> products = sender.getProducts();
		Assert.assertEquals("one product created from delete", 1,
				products.size());
		Product delete = products.get(0);
		Assert.assertEquals("delete product type=origin", "origin", delete
				.getId().getType());
		Assert.assertEquals("delete product status=DELETE",
				Product.STATUS_DELETE, delete.getStatus());
		Assert.assertNotNull("delete origin product has contents.xml", products.get(0)
				.getContents().get(EQMessageProductCreator.CONTENTS_XML_PATH));
	}

	private static class TestProductSender implements ProductSender {

		private List<Product> sentProducts = new LinkedList<Product>();

		@Override
		public void configure(Config config) throws Exception {
		}

		@Override
		public void startup() throws Exception {
		}

		@Override
		public void shutdown() throws Exception {
		}

		@Override
		public void sendProduct(Product product) throws Exception {
			sentProducts.add(product);
		}

		public List<Product> getProducts() {
			return sentProducts;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public void setName(String arg0) {
		}
	}

}
