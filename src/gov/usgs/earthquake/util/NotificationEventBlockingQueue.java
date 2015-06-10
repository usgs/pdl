package gov.usgs.earthquake.util;

import gov.usgs.earthquake.distribution.NotificationEvent;
import gov.usgs.earthquake.product.ProductId;

public class NotificationEventBlockingQueue extends
		RoundRobinBlockingQueue<NotificationEvent> {

	/**
	 * Round robin per source + type.
	 */
	@Override
	protected String getQueueId(final NotificationEvent event) {
		ProductId id = event.getNotification().getProductId();
		return (id.getSource() + "_" + id.getType()).toLowerCase();
	}

}
