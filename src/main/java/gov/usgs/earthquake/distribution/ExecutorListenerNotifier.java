package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.aws.JsonNotificationIndex;
import gov.usgs.earthquake.product.AbstractListener;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.ExecutorTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecutorListenerNotifier extends DefaultConfigurable implements
		ListenerNotifier {

	private static final Logger LOGGER = Logger
			.getLogger(ExecutorListenerNotifier.class.getName());

	private static ArrayList<String> AUTOLOADED_INDEXES = new ArrayList<String>();

	private DefaultNotificationReceiver receiver;

	/**
	 * Notification listeners registered to receive notifications, and an
	 * ExecutorService that delivers Notifications to each in a separate thread.
	 */
	protected Map<NotificationListener, ExecutorService> notificationListeners = new HashMap<NotificationListener, ExecutorService>();

	/**
	 * Make sure listener will accept notification before queueing it for
	 * processing.
	 */
	protected boolean acceptBeforeQueuing = true;

	/**
	 * Timer used to retry tasks when they fail and listeners have configured
	 * retryDelay.
	 */
	protected Timer retryTimer = new Timer();

	/** When queue size reaches this level, start throttling */
	protected int throttleStartThreshold = 50000;

	/** When queue size reaches this level, stop throttling */
	protected int throttleStopThreshold = 25000;

	/** When throttling, wait this many milliseconds between queue size checks. */
	protected long throttleWaitInterval = 5000L;

	public ExecutorListenerNotifier(final DefaultNotificationReceiver receiver) {
		this.receiver = receiver;
	}

	/**
	 * Add a new notification listener.
	 *
	 * @param listener
	 *            the listener to add. When notifications are received, this
	 *            listener will be notified.
	 */
	@Override
	public void addNotificationListener(NotificationListener listener)
			throws Exception {
		if (!notificationListeners.containsKey(listener)) {
			// fixed thread pool allows us to inspect the queue length...
			int concurrentProducts = 1;
			if (listener instanceof DefaultNotificationListener) {
				concurrentProducts =
						((DefaultNotificationListener) listener).getConcurrentProducts();
			}
			ExecutorService listenerExecutor = Executors.newFixedThreadPool(
					concurrentProducts);
			notificationListeners.put(listener, listenerExecutor);
		}
	}

	/**
	 * Remove an existing notification listener.
	 *
	 * Any currently queued notifications are processed before shutting down.
	 *
	 * @param listener
	 *            the listener to remove. When notifications are receive, this
	 *            listener will no longer be notified.
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener)
			throws Exception {
		// remove listener from map
		ExecutorService listenerExecutor = notificationListeners
				.remove(listener);

		// shutdown executor thread
		listenerExecutor.shutdown();

		// Could use shutdownNow() instead?
		// however, shutdown() gives all listeners a chance to
		// process all notifications, but may keep client from shutting down
		// quickly. Also, see DefaultNotificationReceiver.shutdown().
	}

	/**
	 * Send a notification to all registered NotificationListeners.
	 *
	 * Creates a NotificationEvent, with a reference to this object and calls
	 * each notificationListeners onNotification method in separate threads.
	 *
	 * This method usually returns before registered NotificationListeners have
	 * completed processing a notification.
	 *
	 * @param event
	 *            the notification being sent to listeners.
	 * @throws Exception
	 */
	@Override
	public void notifyListeners(final NotificationEvent event) throws Exception {
		this.notifyListeners(event, this.notificationListeners.keySet());
	}

	public void notifyListeners(final NotificationEvent event,
			final Collection<NotificationListener> listeners) throws Exception {

		Iterator<NotificationListener> iter = listeners.iterator();
		while (iter.hasNext()) {
			NotificationListener listener = iter.next();
			// only requeue for default notification listeners
			queueNotification(listener, event);
		}
	}

	protected void queueNotification(final NotificationListener listener,
			final NotificationEvent event) {
		if (acceptBeforeQueuing
				&& listener instanceof DefaultNotificationListener) {
			DefaultNotificationListener defaultListener = (DefaultNotificationListener) listener;
			if (!defaultListener.accept(event.getNotification().getProductId())) {
				return;
			}
		}

		// determine retry delay
		long retryDelay = 0L;
		if (listener instanceof AbstractListener) {
			retryDelay = ((AbstractListener) listener).getRetryDelay();
		}

		ExecutorService listenerExecutor = notificationListeners.get(listener);
		ExecutorTask<Void> listenerTask = new ExecutorTask<Void>(
				listenerExecutor, listener.getMaxTries(),
				listener.getTimeout(), new NotificationListenerCallable(
						listener, event), retryTimer, retryDelay);
		listenerExecutor.submit(listenerTask);

		// log how many notifications are pending
		if (listenerExecutor instanceof ThreadPoolExecutor) {
			BlockingQueue<Runnable> pending = ((ThreadPoolExecutor) listenerExecutor)
					.getQueue();
			LOGGER.fine("[" + event.getNotificationReceiver().getName()
					+ "] listener (" + listener.getName() + ") has "
					+ pending.size() + " queued notifications");
		}
	}


	@Override
	public void shutdown() throws Exception {
		// remove all listeners
		Iterator<NotificationListener> iter = new ArrayList<NotificationListener>(
				notificationListeners.keySet()).iterator();
		while (iter.hasNext()) {
			removeNotificationListener(iter.next());
		}
	}

	@Override
	public void startup() throws Exception {
		super.startup();

		NotificationIndex index = receiver.getNotificationIndex();

		// filter down to listeners who can handle requeueing gracefully
		ArrayList<NotificationListener> gracefulListeners = new ArrayList<NotificationListener>();
		Iterator<NotificationListener> iter = this.notificationListeners
				.keySet().iterator();
		while (iter.hasNext()) {
			NotificationListener listener = iter.next();
			// make sure each index only notifies each listener once
			String key = listener.getName() + '|' + index.getName();
			if (AUTOLOADED_INDEXES.contains(key)) {
				// already loaded this notification index for this listener
				// another receiver is sharing this notification index
			} else if (listener instanceof DefaultNotificationListener
					&& ((DefaultNotificationListener) listener)
							.getNotificationIndex() != null) {
				gracefulListeners.add(listener);
				AUTOLOADED_INDEXES.add(key);
			}
		}

		if (gracefulListeners.size() == 0) {
			// don't bother searching if nobody is listening
			return;
		}

		LOGGER.info("[" + receiver.getName()
				+ "] requeueing notification index '" + index.getName() + "'");
		// find all existing notifications
		List<Notification> allNotifications = null;

		// for json index, push intersection into database if only one listener
		if (index instanceof JsonNotificationIndex && gracefulListeners.size() == 1) {
			NotificationIndex listenerIndex =
					((DefaultNotificationListener) gracefulListeners.get(0))
					.getNotificationIndex();
			if (listenerIndex instanceof JsonNotificationIndex) {
				// get intersection
				try {
					allNotifications =
							((JsonNotificationIndex) index).getMissingNotifications(
									((JsonNotificationIndex) listenerIndex).getTable());
				} catch (Exception e) {
					LOGGER.log(Level.INFO, "Exception loading intersection, continuing", e);
				}
			}
		}

		if (allNotifications == null) {
			// fallback to previous behavior
			allNotifications = index.findNotifications(
					(List<String>) null, (List<String>) null, (List<String>) null);
		}
		LOGGER.info("Done finding existing notifications");

		// queue them for processing in case they were previous missed
		Date now = new Date();
		int count = 0;
		for (final Notification n : allNotifications) {
			NotificationEvent event = new NotificationEvent(receiver, n);
			if (event.getNotification().getExpirationDate().after(now)) {
				// still valid
				this.notifyListeners(event, gracefulListeners);
			}
			count += 1;

			// try to keep queue size managable during restart
			throttleQueues(allNotifications.size() - count);
		}
		LOGGER.info("All notifications queued");

		// keep track that we've processed this notification index
		AUTOLOADED_INDEXES.add(index.getName());
	}

	public DefaultNotificationReceiver getReceiver() {
		return receiver;
	}

	public void setReceiver(DefaultNotificationReceiver receiver) {
		this.receiver = receiver;
	}

	public Map<String, Integer> getStatus() {
		HashMap<String, Integer> status = new HashMap<String, Integer>();

		Iterator<NotificationListener> iter = notificationListeners.keySet()
				.iterator();
		while (iter.hasNext()) {
			NotificationListener listener = iter.next();
			ExecutorService listenerExecutor = notificationListeners
					.get(listener);

			if (listenerExecutor instanceof ThreadPoolExecutor) {
				// check how many notifications are pending
				BlockingQueue<Runnable> pending = ((ThreadPoolExecutor) listenerExecutor)
						.getQueue();
				status.put(receiver.getName() + " - " + listener.getName(),
						pending.size());
			}
		}

		return status;
	}

	/**
	 * Check queue status and return length of longest queue.
	 *
	 * @return length of longest queue, or null if no queue lengths.
	 */
	public Integer getMaxQueueSize() {
		final Map<String, Integer> status = getStatus();
		Integer maxQueueSize = null;
		for (Integer queueSize : status.values()) {
			if (queueSize != null && (maxQueueSize == null || queueSize > maxQueueSize)) {
				maxQueueSize = queueSize;
			}
		}
		return maxQueueSize;
	}

	/**
	 * If longest queue has more than 50k notifications,
	 * wait until longest queue has 25k notifications before returning.
	 *
	 * @throws InterruptedException
	 */
	public void throttleQueues() throws InterruptedException {
		throttleQueues(null);
	}

	public void throttleQueues(Integer remaining) throws InterruptedException {
		// try to keep queue size managable during restart
		int maxSize = throttleStartThreshold;
		// track whether any throttles occurred
		boolean throttled = false;

		while (true) {
			final Integer size = getMaxQueueSize();
			if (size == null || size <= maxSize) {
				// within limit
				if (throttled) {
					LOGGER.info("[" + getName() + "] done throttling (size = " + size + ")");
				}
				break;
			}

			throttled = true;
			LOGGER.info("[" + getName() + "]"
					+ " queueing throttled until below "
					+ throttleStopThreshold
					+ " (size = " + size + ", remaining="
							+ (remaining == null ? "?" : remaining)
							+ ")");
			// too many messages queued
			// set maxSize to stop threshold
			maxSize = throttleStopThreshold;
			// wait for listener to do some processing
			// 5s is a little low, but don't want to wait too long
			Thread.sleep(throttleWaitInterval);
		}
	}

	/**
	 * NOTE: messing with the executors map is not a good idea.
	 *
	 * @return the map of listeners and their executors.
	 */
	public Map<NotificationListener, ExecutorService> getExecutors() {
		return notificationListeners;
	}

	public int getThrottleStartThreshold() { return this.throttleStartThreshold; }
	public void setThrottleStartThreshold(final int n) { this.throttleStartThreshold = n; }

	public int getThrottleStopThreshold() { return this.throttleStopThreshold; }
	public void setThrottleStopThreshold(final int n) { this.throttleStopThreshold = n; }

	public long getThrottleWaitInterval() { return this.throttleWaitInterval; }
	public void setThrottleWaitInterval(final long ms) { this.throttleWaitInterval = ms; }

}
