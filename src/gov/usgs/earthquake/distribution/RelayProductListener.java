/*
 * RelayProductListener
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.util.Config;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listen for products and use a product sender (SocketProductSender by default)
 * to send products as they are received.
 */
public class RelayProductListener extends DefaultNotificationListener {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(RelayProductListener.class.getName());

	/** Sender used to send products. */
	private ProductSender sender;

	/**
	 * Empty constructor for configurable, will configure as a
	 * SocketProductSender.
	 */
	public RelayProductListener() {
	}

	/**
	 * Construct a RelayProductListener using a custom ProductSender.
	 * 
	 * @param sender
	 *            the sender to use.
	 */
	public RelayProductListener(final ProductSender sender) {
		this.sender = sender;
	}

	/**
	 * Send a product.
	 */
	public void onProduct(final Product product) {
		LOGGER.info("Relaying product " + product.getId().toString() + " "
				+ product.getId().getUpdateTime());

		try {
			sender.sendProduct(product);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error relaying product", e);
		}
	}

	/**
	 * Creates a SocketProductSender and uses its configure method.
	 */
	public void configure(Config config) throws Exception {
		// read DefaultNotificationListener properties
		super.configure(config);

		sender = new SocketProductSender();
		sender.configure(config);
	}

	/**
	 * Call the sender shutdown method.
	 */
	public void shutdown() throws Exception {
		sender.shutdown();
	}

	/**
	 * Call the sender startup method.
	 */
	public void startup() throws Exception {
		sender.startup();
	}

}
