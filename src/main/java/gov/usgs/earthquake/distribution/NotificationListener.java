/*
 * NotificationListener
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.util.Configurable;

/**
 * Process Notifications of Products.
 * 
 * NotificationListeners receive Notifications from NotificationReceivers.
 * 
 * First, a listener registers with a NotificationReceiver using the
 * addNotificationListener method:
 * 
 * <pre>
 * receiver.addNotificationListener(listener);
 * </pre>
 * 
 * A listener may "search" previous notifications using the
 * NotificationReceiver's sendNotifications method.
 */
public interface NotificationListener extends Configurable {

	/**
	 * Receive a Notification that a product is available.
	 * 
	 * If this NotificationListener is interested in the Product, it may use the
	 * convenience method getProduct():
	 * 
	 * <pre>
	 * Product product = event.getProduct();
	 * </pre>
	 * 
	 * When getTimeout() returns a positive (&gt;0) number of milliseconds, the
	 * thread calling onNotification may be interrupted. Listeners should use
	 * care in handling InterruptedExceptions to ensure resources are properly
	 * freed.
	 * 
	 * @param event
	 *            the event corresponding to the notification that is available.
	 * @throws ContinuableListenerException
	 *             if redelivery should be attempted (depending on what
	 *             getAttemptCount() returns).
	 * @see ContinuableListenerException
	 * @see InterruptedException
	 * @see #getMaxTries()
	 */
	public void onNotification(final NotificationEvent event) throws Exception;

	/**
	 * A NotificationReceiver that generates a NotificationEvent will attempt to
	 * deliver the event up to this many times, if the listener throws an
	 * Exception while processing.
	 * 
	 * @return A value of 1 or less means do not attempt more than once.
	 */
	public int getMaxTries();

	/**
	 * A NotificationListener has this many milliseconds to process a
	 * notification before being interrupted.
	 * 
	 * @return number of milliseconds before timing out. A value of 0 or less
	 *         means never time out.
	 */
	public long getTimeout();
}
