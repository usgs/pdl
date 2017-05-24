/*
 * NotificationEvent
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;

import java.io.IOException;

import java.util.EventObject;

/**
 * An event sent to a NotificationListener.
 * 
 * These events are sent by a NotificationReceiver.
 */
public class NotificationEvent extends EventObject {

	/** For serialization. */
	private static final long serialVersionUID = 1L;

	/** The notification that generated this event. */
	private final Notification notification;

	/**
	 * Construct a new NotificationEvent.
	 * 
	 * @param source
	 *            the source of this event, usually a NotificationReceiver.
	 * @param notification
	 *            the notification that generated this event.
	 */
	public NotificationEvent(final NotificationReceiver source,
			final Notification notification) {
		super(source);
		this.notification = notification;
	}

	/**
	 * Get the notification associated with this NotificationEvent.
	 * 
	 * @return the associated notification.
	 */
	public Notification getNotification() {
		return notification;
	}

	/**
	 * A convenience method that casts event source into a NotificationReceiver.
	 * 
	 * @return source as a NotificationReceiver.
	 */
	public NotificationReceiver getNotificationReceiver() {
		return (NotificationReceiver) getSource();
	}

	/**
	 * A convenience method to request a product.
	 * 
	 * @return the requested product.
	 * @throws IOException
	 *             if any errors occur while retrieving the product.
	 */
	public Product getProduct() throws Exception {
		return getNotificationReceiver().retrieveProduct(
				notification.getProductId());
	}

}
