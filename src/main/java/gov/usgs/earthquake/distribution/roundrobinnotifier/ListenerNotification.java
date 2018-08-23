package gov.usgs.earthquake.distribution.roundrobinnotifier;

import java.util.Date;

import gov.usgs.earthquake.distribution.NotificationEvent;
import gov.usgs.earthquake.product.ProductId;

/**
 * Track notification for a specific listener.
 */
public class ListenerNotification {

	/** The notification to deliver. */
	public final NotificationEvent event;
	/** The number of attempts to deliver. */
	public int attempts;
	/** Time of the last attempt. */
	public Date lastAttempt;

	/**
	 * Create a new ListenerNotification.
	 * 
	 * @param event
	 *            the notification to deliver.
	 */
	public ListenerNotification(final NotificationEvent event) {
		this.event = event;
		this.attempts = 0;
		this.lastAttempt = null;
	}

	/**
	 * @return the product id from the notification. 
	 */
	public ProductId getProductId() {
		return this.event.getNotification().getProductId();
	}

}
