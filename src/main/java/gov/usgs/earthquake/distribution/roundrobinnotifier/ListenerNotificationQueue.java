package gov.usgs.earthquake.distribution.roundrobinnotifier;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.util.RoundRobinBlockingQueue;

/**
 * A round-robin blocking queue for ListenerNotification objects.
 */
public class ListenerNotificationQueue extends
		RoundRobinBlockingQueue<ListenerNotification> {

	/**
	 * Round robin per source + type.
	 */
	@Override
	protected String getQueueId(final ListenerNotification notification) {
		ProductId id = notification.getProductId();
		return (id.getSource() + "_" + id.getType()).toLowerCase();
	}

}
