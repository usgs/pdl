/*
 * NotificationReceiver
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Configurable;

import java.util.List;

/**
 * Receives and processes notifications.
 * 
 * A NotificationReceiver receives and processes notifications, alerting
 * NotificationListeners after they are processed.
 * 
 */
public interface NotificationReceiver extends Configurable {

	/**
	 * Receive and process a notification.
	 * 
	 * @param notification
	 *            the notification being received.
	 * @throws Exception
	 *             if errors occur.
	 */
	public void receiveNotification(final Notification notification)
			throws Exception;

	/**
	 * If a NotificationReceiver stores notifications, all expired notifications
	 * and products should be removed when this method is called.
	 * 
	 * @throws Exception
	 *             if errors occur while removing expired notifications.
	 */
	public void removeExpiredNotifications() throws Exception;

	/**
	 * NotificationListeners use this method to request a product.
	 * 
	 * A NotificationReceiver may have many listeners, and should try to
	 * retrieve products once for each product id. This will typically generate
	 * a "local" notification, that, when expiring, signals the product may be
	 * removed.
	 * 
	 * @param id
	 *            the product to retrieve.
	 * @return the retrieved product, or null if not available.
	 * @throws Exception
	 *             if an error occurs while retrieving the product.
	 */
	public Product retrieveProduct(final ProductId id) throws Exception;

	/**
	 * Add a NotificationListener.
	 * 
	 * Notifications processed after this call will be sent to listener.
	 * 
	 * @param listener
	 *            the listener to add.
	 */
	public void addNotificationListener(final NotificationListener listener)
			throws Exception;

	/**
	 * Remove a NotificationListener.
	 * 
	 * Notifications processed after this call will not be sent to listener.
	 * 
	 * @param listener
	 *            the listener to remove.
	 */
	public void removeNotificationListener(final NotificationListener listener)
			throws Exception;

	/**
	 * Send matching notifications to the listener.
	 * 
	 * This method is a way for listeners to search notifications that were
	 * processed while not an active listener. If sources, types, and codes are
	 * all null, a notifications for each known ProductId will be sent to
	 * listener.
	 * 
	 * For example, a new shakemap NotificationListener may wish to know about
	 * all known shakemaps.
	 * 
	 * <pre>
	 * NotificationListener shakemapListener;
	 * // register to receive new notifications as they arrive
	 * receiver.addNotificationListener(shakemapListener);
	 * // check if any notifications haven't already been processed.
	 * List&lt;String&gt; types = new LinkedList&lt;String&gt;();
	 * types.add(&quot;shakemap&quot;);
	 * types.add(&quot;shakemap-scenario&quot;);
	 * receiver.sendNotifications(shakemapListener, null, types, null);
	 * </pre>
	 * 
	 * @param listener
	 *            the listener that will receive any matching notifications.
	 * @param sources
	 *            a list of sources to search, or null for all sources.
	 * @param types
	 *            a list of types to search, or null for all types.
	 * @param codes
	 *            a list of codes to search, or null for all codes.
	 */
	public void sendNotifications(final NotificationListener listener,
			final List<String> sources, final List<String> types,
			final List<String> codes) throws Exception;

}
