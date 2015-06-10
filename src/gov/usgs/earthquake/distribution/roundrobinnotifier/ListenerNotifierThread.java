package gov.usgs.earthquake.distribution.roundrobinnotifier;

import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.DefaultNotificationListener;
import gov.usgs.earthquake.distribution.NotificationEvent;
import gov.usgs.earthquake.distribution.NotificationListener;
import gov.usgs.earthquake.product.AbstractListener;

/**
 * Thread that delivers notifications to a listener.
 * 
 * Uses interrupt to stop thread, so listeners should be careful when also using
 * interrupts.
 */
public class ListenerNotifierThread implements Runnable {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(ListenerNotifierThread.class.getName());

	/** Listener that receives notifications. */
	private final NotificationListener listener;
	/** Queue of notifications to deliver. */
	private final ListenerNotificationQueue queue;
	/** Queue of notifications that failed, and should be reattempted. */
	private final LinkedBlockingQueue<ListenerNotification> errorQueue;
	/** Thread where "this" is running. */
	private Thread thread;

	/**
	 * Create a new listener notifier thread.
	 * 
	 * @param listener
	 *            listener that receives notifications.
	 */
	public ListenerNotifierThread(final NotificationListener listener) {
		this.listener = listener;
		this.queue = new ListenerNotificationQueue();
		this.errorQueue = new LinkedBlockingQueue<ListenerNotification>();
		this.thread = null;
	}

	/**
	 * Start processing notifications in the queue.
	 */
	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	/**
	 * Stop processing notifications in the queue.
	 */
	public void stop() {
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
	}

	/**
	 * Process notifications in the queue.
	 */
	public void run() {
		ListenerNotification notification = null;
		Date start = null;
		while (!Thread.currentThread().isInterrupted()) {
			try {
				notification = queue.take();
				notification.attempts++;
				start = new Date();
				listener.onNotification(notification.event);
				LOGGER.fine("[" + listener.getName() + "] processed "
						+ notification.getProductId() + " in "
						+ (new Date().getTime() - start.getTime()) + "ms");
			} catch (InterruptedException ie) {
				// thread is stopping
			} catch (Exception e) {
				// requeue notification
				if (notification.attempts < listener.getMaxTries()) {
					// requeue
					notification.lastAttempt = new Date();
					errorQueue.add(notification);
					LOGGER.log(Level.FINE, "[" + listener.getName()
							+ "] exception processing "
							+ notification.getProductId() + " attempt "
							+ notification.attempts + "/"
							+ listener.getMaxTries() + ", requeuing");
				} else {
					// couldn't process
					LOGGER.log(Level.WARNING, "[" + listener.getName()
							+ "] unable to process "
							+ notification.getProductId() + " "
							+ notification.attempts + " attempts", e);
				}
			}
		}
	}

	/**
	 * Add a notification to the queue.
	 * 
	 * Checks if notification is "accept"able before queueing.
	 * 
	 * @param event
	 *            notification to add.
	 */
	public void notify(final NotificationEvent event) {
		if (listener instanceof AbstractListener) {
			if (!((AbstractListener) listener).accept(event.getNotification()
					.getProductId())) {
				return;
			}
		}
		queue.add(new ListenerNotification(event));
	}

	/**
	 * @return the listener.
	 */
	public NotificationListener getListener() {
		return listener;
	}

	/**
	 * @return the queue.
	 */
	public ListenerNotificationQueue getQueue() {
		return queue;
	}

	/**
	 * @return the error queue.
	 */
	public LinkedBlockingQueue<ListenerNotification> getErrorQueue() {
		return errorQueue;
	}

	/**
	 * Move any failed notifications that are ready to be retried from the error
	 * queue into the queue.
	 */
	public void requeueErrors() {
		ListenerNotification notification;
		Date threshold = new Date();
		if (listener instanceof DefaultNotificationListener) {
			threshold = new Date(threshold.getTime()
					- ((DefaultNotificationListener) listener).getRetryDelay());
		}
		while (true) {
			notification = errorQueue.peek();
			if (
			// no more notifications
			notification == null ||
			// after threshold
					notification.lastAttempt.before(threshold)) {
				break;
			}
			// requeue notification
			queue.add(errorQueue.poll());
		}
	}

}
