package gov.usgs.earthquake.eids;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.ansseqmsg.EQMessage;
import gov.usgs.earthquake.distribution.ProductBuilder;
import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.eidsutil.EIDSClient;
import gov.usgs.earthquake.eidsutil.EIDSListener;
import gov.usgs.earthquake.eidsutil.EIDSMessageEvent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.util.Config;

/**
 * Convert messages from EIDS into products.
 * 
 * Listens to messages from an EIDSClient. Uses EQXMLProductParser and
 * EventAddonParser to build products. Any built products are sent to all
 * configured productSenders.
 */
public class EIDSProductBuilder extends ProductBuilder implements EIDSListener {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(EIDSProductBuilder.class.getName());

	/** Convert EventAddon to EQMessage objects. */
	private EventAddonParser addonParser = new EventAddonParser();

	/** Convert EQMessages to products. */
	private EQMessageProductCreator eqmessageProductCreator = new EQMessageProductCreator();

	/**
	 * Receive EIDS messages from an EIDSClient.
	 * 
	 * Any received messages are parsed and sent to any ProductSenders. If the
	 * message is not EQXML, this method returns immediately.
	 */
	@Override
	public synchronized void onEIDSMessage(EIDSMessageEvent event) {
		List<Product> products = new LinkedList<Product>();

		// parse eids message into Products
		if (event.getRootElement().equals("EQMessage")) {
			try {
				products.addAll(eqmessageProductCreator
						.getEQMessageProducts(event.getMessage()));
			} catch (Exception e) {
				// unable to parse EQMessage
				LOGGER.log(Level.WARNING, "Error while parsing EQMessage", e);
				LOGGER.info(event.getMessage());
				return;
			}
		} else if (event.getRootElement().equals("eventaddon")) {
			try {
				EQMessage message = addonParser.parseMessage(event);
				if (message != null) {
					products.addAll(eqmessageProductCreator
							.getEQMessageProducts(message));
				} else {
					LOGGER.log(Level.WARNING, "Unable to parse eventaddon\n"
							+ event.getMessage());
				}
			} catch (Exception e) {
				// unable to parse eventaddon
				LOGGER.log(Level.WARNING, "Error while parsing eventaddon", e);
				LOGGER.info(event.getMessage());
				return;
			}
		} else {
			// not parseable
			LOGGER.info("Unexpected EIDS message {" + event.getRootNamespace()
					+ "}" + event.getRootElement());
			return;
		}

		LOGGER.finest("Received EIDS message " + event.getMessage());

		// add eids properties onto products
		String feederSource = event.getMessageSource();
		Long feederSequence = event.getMessageSequence();
		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			Product product = iter.next();
			Map<String, String> properties = product.getProperties();
			if (feederSource != null) {
				properties.put("eids-feeder", feederSource);
			}
			if (feederSequence != null) {
				properties.put("eids-feeder-sequence",
						feederSequence.toString());
			}
		}

		try {
			// send products
			iter = products.iterator();
			while (iter.hasNext()) {
				Product product = iter.next();
				LOGGER.fine("Sending product id=" + product.getId()
						+ ", status=" + product.getStatus());

				Map<ProductSender, Exception> errors = sendProduct(product);
				Iterator<ProductSender> senders = errors.keySet().iterator();
				while (senders.hasNext()) {
					LOGGER.log(Level.WARNING, "Error sending product",
							errors.get(senders.next()));
					LOGGER.info(event.getMessage());
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Error sending products", e);
		}

	}

	/**
	 * Main method to test EQXMLProductBuilder.
	 * 
	 * Connects an eids client to the product builder, and uses a dummy product
	 * sender that outputs to stderr.
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		EIDSProductBuilder builder = new EIDSProductBuilder();
		builder.addProductSender(new ProductSender() {
			@Override
			public void configure(Config arg0) throws Exception {
			}

			@Override
			public void shutdown() throws Exception {
			}

			@Override
			public void startup() throws Exception {
			}

			@Override
			public void sendProduct(Product product) throws Exception {
				System.err.println("product id=" + product.getId().toString());

				Iterator<String> names = product.getProperties().keySet()
						.iterator();
				while (names.hasNext()) {
					String name = names.next();
					System.err.println("\t" + name + "="
							+ product.getProperties().get(name));
				}
			}

			@Override
			public String getName() {
				return null;
			}

			@Override
			public void setName(String arg0) {
			}
		});

		EIDSClient client = new EIDSClient("eids1.cr.usgs.gov", 39977);
		client.addListener(builder);
		client.startup();
	}

}
