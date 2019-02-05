package gov.usgs.earthquake.distribution;

import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultStorageListener extends DefaultConfigurable implements
		StorageListener {

	private static final Logger LOGGER = Logger
			.getLogger(DefaultStorageListener.class.getName());

	@Override
	public void configure(Config arg0) throws Exception {
		// Nothing to do for default configure
	}

	@Override
	public void shutdown() throws Exception {
		// Nothing to do for default shutdown
	}

	@Override
	public void startup() throws Exception {
		// Nothing to do for default startup
	}

	/**
	 * Simple dispatch method for listeners who are only interested in certain
	 * types of <code>StorageEvent</code>s.
	 * 
	 * @param event
	 *            The event that triggered the call
	 */
	@Override
	public void onStorageEvent(StorageEvent event) {
		StorageEvent.StorageEventType type = event.getType();
		try {
			if (type == StorageEvent.PRODUCT_STORED) {
				onProductStored(event);
			} else if (type == StorageEvent.PRODUCT_REMOVED) {
				onProductRemoved(event);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "[" + getName() + "] exception processing storage event", e);
		}
	}

	/**
	 * Dispatched method called when the type of event is
	 * <code>StorageEvent.StorageEventType.PRODUCT_STORED</code>.
	 * 
	 * @param event
	 */
	public void onProductStored(StorageEvent event) throws Exception {
		LOGGER.info("onProductStored::" + event.getProductId().toString());
	}

	/**
	 * Dispatched method called when the type of event is
	 * <code>StorageEvent.StorageEventType.PRODUCT_REMOVED</code>.
	 * 
	 * @param event
	 */
	public void onProductRemoved(StorageEvent event) throws Exception {
		LOGGER.info("onProductRemoved::" + event.getProductId().toString());
	}

}
