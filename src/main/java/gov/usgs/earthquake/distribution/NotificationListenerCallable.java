package gov.usgs.earthquake.distribution;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Callable object for deferred listener notification.
 */
public class NotificationListenerCallable implements Callable<Void> {

	private static final Logger LOGGER = Logger
			.getLogger(NotificationListenerCallable.class.getName());

	private final NotificationListener listener;
	private final NotificationEvent event;

	/**
	 * Create an ExecutorListenerNotifierCallable.
	 * 
	 * @param listener
	 *            the listener to notify
	 * @param event
	 *            the notification to send
	 */
	public NotificationListenerCallable(
			final NotificationListener listener, final NotificationEvent event) {
		this.listener = listener;
		this.event = event;
	}

	public Void call() throws Exception {
		try {
			listener.onNotification(event);
			return null;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "["
					+ event.getNotificationReceiver().getName()
					+ "] listener (" + listener.getName()
					+ ") threw exception, for product id = "
					+ event.getNotification().getProductId(), e);

			// track exception
			Notification notification = event.getNotification();
			new ProductTracker(notification.getTrackerURL()).exception(listener
					.getClass().getCanonicalName(),
					notification.getProductId(), e);

			// but rethrow for outside handling
			throw e;
		}
	}

}
