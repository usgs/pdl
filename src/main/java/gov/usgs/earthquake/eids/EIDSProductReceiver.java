/*
 * EIDSProductReceiver
 */
package gov.usgs.earthquake.eids;

import gov.usgs.earthquake.distribution.EIDSNotificationReceiver;
import gov.usgs.earthquake.distribution.Notification;
import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.eidsutil.EIDSMessageEvent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.util.Config;

/**
 * Wrapper around EIDSProductBuilder so it acts as a Receiver.
 */
public class EIDSProductReceiver extends EIDSNotificationReceiver {

	/** The EIDS builder. */
	private EIDSProductBuilder builder;

	/**
	 * Construct a new EIDSProductReceiver.
	 * 
	 * Uses the Configurable interface, see EIDSNotificationReceiver.
	 */
	public EIDSProductReceiver() {
		builder = new EIDSProductBuilder();

		// add a product sender that sends to this receiver
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
				// forward built products to receiver
				Notification notification = storeProductSource(new ObjectProductSource(
						product));
				receiveNotification(notification);
			}

			@Override
			public String getName() {
				return null;
			}

			@Override
			public void setName(String arg0) {
			}
		});
	}

	/**
	 * Receive messages from the EIDSNotificationReceiver EIDS Client.s
	 * 
	 * Forwards messages to builder, which sends built products to
	 * receiveNotification method.
	 */
	public void onEIDSMessage(EIDSMessageEvent event) {
		// forward eids messages to builder
		builder.onEIDSMessage(event);
	}

	@Override
	public void configure(final Config config) throws Exception {
		super.configure(config);

		// load builder configuration properties too
		builder.configure(config);
	}
}
