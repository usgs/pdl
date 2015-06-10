package gov.usgs.earthquake.distribution.roundrobinnotifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.DefaultNotificationListener;
import gov.usgs.earthquake.distribution.ListenerNotifier;
import gov.usgs.earthquake.distribution.Notification;
import gov.usgs.earthquake.distribution.NotificationEvent;
import gov.usgs.earthquake.distribution.NotificationIndex;
import gov.usgs.earthquake.distribution.NotificationListener;
import gov.usgs.earthquake.distribution.DefaultNotificationReceiver;
import gov.usgs.util.DefaultConfigurable;

/**
 * Use round-robin queues to notify listeners.
 * 
 * This attempts to prevent any one product source+type from blocking processing
 * of notifications from other product source+type.
 */
public class RoundRobinListenerNotifier extends DefaultConfigurable implements
		ListenerNotifier, Runnable {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(RoundRobinListenerNotifier.class.getName());

	/** List of indexes that have already been requeued. */
	private static final ArrayList<String> AUTOLOADED_INDEXES = new ArrayList<String>();

	/** The receiver using this notifier. */
	private final DefaultNotificationReceiver receiver;
	/** Registered notification listeners. */
	private final HashMap<NotificationListener, ListenerNotifierThread> listeners;
	/** Status/requeue thread. */
	private Thread thread;
	/** How often to print status and check for notifications to requeue. */
	private long statusInterval = 5000L;

	/**
	 * Create new RoundRobinListenerNotifier.
	 * 
	 * @param receiver
	 *            the receiver using this notifier.
	 */
	public RoundRobinListenerNotifier(final DefaultNotificationReceiver receiver) {
		this.receiver = receiver;
		this.listeners = new HashMap<NotificationListener, ListenerNotifierThread>();
		this.thread = null;
	}

	/**
	 * Start the status/requeue thread.
	 */
	public void startup() throws Exception {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();

			requeue();
		}
	}

	/**
	 * Stop the status/requeue thread.
	 */
	public void shutdown() {
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
	}

	/**
	 * Add a notification listener.
	 */
	@Override
	public void addNotificationListener(NotificationListener listener)
			throws Exception {
		if (!listeners.containsKey(listener)) {
			ListenerNotifierThread notifier = new ListenerNotifierThread(
					listener);
			listeners.put(listener, notifier);
			notifier.start();
		}
	}

	/**
	 * Remove a notification listener.
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener)
			throws Exception {
		if (listeners.containsKey(listener)) {
			ListenerNotifierThread notifier = listeners.remove(listener);
			notifier.stop();
		}
	}

	/**
	 * Notify listeners.
	 */
	@Override
	public void notifyListeners(NotificationEvent event) throws Exception {
		notifyListeners(event, listeners.values());
	}

	/**
	 * Notify a specific list of listeners.
	 * 
	 * Used during renotification to only notify listeners that have an index.
	 * 
	 * @param event
	 *            notification.
	 * @param toNotify
	 *            list of listeners to notify.
	 * @throws Exception
	 */
	protected void notifyListeners(NotificationEvent event,
			final Collection<ListenerNotifierThread> toNotify) throws Exception {
		Iterator<ListenerNotifierThread> iter = toNotify.iterator();
		while (iter.hasNext()) {
			iter.next().notify(event);
		}
	}

	/**
	 * Run status/requeue tasks.
	 */
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				// run every 5 seconds
				Thread.sleep(statusInterval);

				Iterator<ListenerNotifierThread> iter = listeners.values()
						.iterator();
				while (iter.hasNext()) {
					ListenerNotifierThread notifier = iter.next();
					// requeue errors
					notifier.requeueErrors();
					int queued = notifier.getQueue().size();
					int errors = notifier.getErrorQueue().size();
					// print status
					LOGGER.fine("[" + receiver.getName()
							+ "-notifier] listener "
							+ notifier.getListener().getName() + " " + queued
							+ " queued, " + errors + " to retry");
				}
			} catch (InterruptedException ie) {
				// stopping
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "exception running notifier status",
						e);
			}
		}
	}

	/**
	 * Requeue existing notifications at startup.
	 * 
	 * @throws Exception
	 */
	protected void requeue() throws Exception {
		NotificationIndex index = receiver.getNotificationIndex();

		ArrayList<ListenerNotifierThread> toRenotify = new ArrayList<ListenerNotifierThread>();
		Iterator<ListenerNotifierThread> iter = listeners.values().iterator();
		while (iter.hasNext()) {
			ListenerNotifierThread notifier = iter.next();
			NotificationListener listener = notifier.getListener();
			if (listener instanceof DefaultNotificationListener
					&& ((DefaultNotificationListener) listener)
							.getNotificationIndex() != null) {
				// listener that has notification index
				String key = index.getName() + "|" + listener.getName();
				if (!AUTOLOADED_INDEXES.contains(key)) {
					// not already renotified
					toRenotify.add(notifier);
				}
			}
		}
		if (toRenotify.size() == 0) {
			// no listeners to renotify
			return;
		}

		LOGGER.fine("[" + receiver.getName()
				+ "-notifier] requeuing notifications");
		Iterator<Notification> notifications = index.findNotifications(
				(List<String>) null, (List<String>) null, (List<String>) null)
				.iterator();
		while (notifications.hasNext()) {
			Notification notification = notifications.next();
			notifyListeners(new NotificationEvent(receiver, notification),
					toRenotify);
		}
		LOGGER.fine("[" + receiver.getName()
				+ "-notifier] done requeuing notifications");
	}

}
