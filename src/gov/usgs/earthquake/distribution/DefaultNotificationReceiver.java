/*
 * DefaultNotificationReceiver
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.distribution.roundrobinnotifier.RoundRobinListenerNotifier;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.IOUtil;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.ObjectLock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The core of product distribution.
 * 
 * A DefaultNotificationReceiver receives notifications and notifies listeners
 * of received notifications. NotificationListeners use the NotificationReceiver
 * to retrieve products referenced by notifications.
 * 
 * The NotificationReceiver uses a NotificationIndex to track received
 * notifications, and a ProductStorage to store retrieved products.
 * 
 * The DefaultNotificationReceiver implements the Configurable interface and
 * uses the following configuration parameters:
 * 
 * Each listener has a separate queue of notifications. Each listener is
 * allocated one thread to process notifications from this queue.
 */
public class DefaultNotificationReceiver extends DefaultConfigurable implements
		NotificationReceiver {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(DefaultNotificationReceiver.class.getName());

	/** Property referencing a notification index config section. */
	public static final String NOTIFICATION_INDEX_PROPERTY = "index";

	/** Shortcut to create a SQLite JDBCNotificationIndex. */
	public static final String INDEX_FILE_PROPERTY = "indexFile";

	/** Property referencing a product storage config section. */
	public static final String PRODUCT_STORAGE_PROPERTY = "storage";

	/** Shortcut to create a FileProductStorage. */
	public static final String STORAGE_DIRECTORY_PROPERTY = "storageDirectory";

	/** Property referencing how long to store products in milliseconds. */
	public static final String PRODUCT_STORAGE_MAX_AGE_PROPERTY = "storageAge";

	/** Default max age to store products, 3600000 milliseconds = 1 hour. */
	public static final String DEFAULT_PRODUCT_STORAGE_MAX_AGE = "3600000";

	/**
	 * Property referencing how long to wait until checking for expired
	 * notifications/products.
	 */
	public static final String RECEIVER_CLEANUP_PROPERTY = "cleanupInterval";

	/**
	 * Default time between checking for expired notifications/products, 900000
	 * milliseconds = 15 minutes.
	 */
	public static final String DEFAULT_RECEIVER_CLEANUP = "900000";

	public static final String CONNECT_TIMEOUT_PROPERTY = "connectTimeout";
	public static final String DEFAULT_CONNECT_TIMEOUT = "15000";
	public static final String READ_TIMEOUT_PROPERTY = "readTimeout";
	public static final String DEFAULT_READ_TIMEOUT = "15000";

	public static final String LISTENER_NOTIFIER_PROPERTY = "listenerNotifier";
	public static final String EXECUTOR_LISTENER_NOTIFIER = "executor";
	public static final String ROUNDROBIN_LISTENER_NOTIFIER = "roundrobin";

	/** The notification index where received notifications are stored. */
	private NotificationIndex notificationIndex;

	/** The product storage where retrieved products are stored. */
	private ProductStorage productStorage;

	/** How long to store retrieved product, in milliseconds. */
	private Long productStorageMaxAge = 0L;

	/** How long to wait until checking for expired notifications/products. */
	private Long receiverCleanupInterval = 0L;

	/** Timer that schedules receiver cleanup task. */
	private Timer receiverCleanupTimer = new Timer();

	private int connectTimeout = Integer.parseInt(DEFAULT_CONNECT_TIMEOUT);
	private int readTimeout = Integer.parseInt(DEFAULT_READ_TIMEOUT);

	private ListenerNotifier notifier;

	/** A lock that is acquired when a product is being retrieved. */
	private ObjectLock<ProductId> retrieveLocks = new ObjectLock<ProductId>();

	public DefaultNotificationReceiver() {
		notifier = new ExecutorListenerNotifier(this);
	}

	/**
	 * Add a new notification listener.
	 * 
	 * @param listener
	 *            the listener to add. When notifications are received, this
	 *            listener will be notified.
	 */
	public void addNotificationListener(NotificationListener listener)
			throws Exception {
		notifier.addNotificationListener(listener);
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
	public void removeNotificationListener(NotificationListener listener)
			throws Exception {
		notifier.removeNotificationListener(listener);
	}

	/**
	 * Store a notification and notify listeners.
	 * 
	 * Updates the notification index before notifying listeners of the newly
	 * available product.
	 * 
	 * @param notification
	 *            the notification being received.
	 * @throws Exception
	 *             if the notificationIndex throws an Exception.
	 */
	public void receiveNotification(Notification notification) throws Exception {
		// notification processed
		new ProductTracker(notification.getTrackerURL()).notificationReceived(
				this.getName(), notification);

		if (notification.getExpirationDate().before(new Date())) {
			LOGGER.finer("[" + getName()
					+ "] skipping already expired notification for product id="
					+ notification.getProductId().toString() + ", expiration="
					+ notification.getExpirationDate().toString());
		} else {
			// add notification to index
			notificationIndex.addNotification(notification);

			if (notification instanceof URLNotification) {
				LOGGER.finer("["
						+ getName()
						+ "] notification URL="
						+ ((URLNotification) notification).getProductURL()
								.toString());
			}

			notifyListeners(notification);
		}
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
	 * @param notification
	 *            the notification being sent to listeners.
	 * @throws Exception
	 */
	protected void notifyListeners(final Notification notification)
			throws Exception {

		LOGGER.finest("[" + getName() + "] notifying listeners for product id="
				+ notification.getProductId().toString());

		// queue notification for listeners
		NotificationEvent event = new NotificationEvent(this, notification);
		notifier.notifyListeners(event);
	}

	public String getListenerQueueStatus() {
		return "Using notifier";
	}

	/**
	 * Search the notification index for expired notifications, removing any
	 * that are found. When a notification in the index is not a
	 * URLNotification, it represents a product in storage that will also be
	 * removed.
	 * 
	 * @throws Exception
	 *             if productStorage or notificationIndex throw an Exception.
	 */
	public void removeExpiredNotifications() throws Exception {
		LOGGER.fine("[" + getName() + "] running receiver cleanup");
		Iterator<Notification> iter = notificationIndex
				.findExpiredNotifications().iterator();
		while (iter.hasNext()) {
			Notification notification = iter.next();
			if (!(notification instanceof URLNotification)) {
				// if it isn't a url notification, it's also in storage
				productStorage.removeProduct(notification.getProductId());
				if (LOGGER.isLoggable(Level.FINEST)) {
					LOGGER.finest("[" + getName()
							+ "] removed expired product from receiver cache "
							+ notification.getProductId().toString());
				}
			}

			// remove expired notification from index
			notificationIndex.removeNotification(notification);
			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest("[" + getName()
						+ "] removed expired notification from receiver index "
						+ notification.getProductId().toString());
			}
		}
	}

	/**
	 * Retrieve a product by id.
	 * 
	 * If this product is already in storage, load and return the product.
	 * Otherwise, search notifications for this product, and download the
	 * product into storage.
	 * 
	 * @param id
	 *            the product to retrieve
	 * @return the retrieved product, or null if not available.
	 */
	public Product retrieveProduct(ProductId id) throws Exception {
		Product product = null;
		String productIdString = id.toString();

		LOGGER.finest("[" + getName() + "] acquiring retrieve lock id="
				+ productIdString);
		retrieveLocks.acquireLock(id);
		LOGGER.finest("[" + getName() + "] retrieve lock acquired id="
				+ productIdString);
		try {
			if (productStorage.hasProduct(id)) {
				try {
					LOGGER.finest("[" + getName() + "] storing product id="
							+ productIdString);
					product = productStorage.getProduct(id);
					LOGGER.finest("[" + getName() + "] product stored id="
							+ productIdString);
				} catch (Exception e) {
					LOGGER.log(
							Level.FINE,
							"["
									+ getName()
									+ "] storage claims hasProduct, but threw exception",
							e);
					product = null;
				}
			}

			if (product == null) {
				LOGGER.finer("[" + getName()
						+ "] don't have product yet, searching notifications");
				// don't have product yet, search notifications
				Iterator<Notification> iter = notificationIndex
						.findNotifications(id).iterator();
				while (product == null && iter.hasNext()) {
					Notification notification = iter.next();
					if (!(notification instanceof URLNotification)) {
						// only URL notifications include location info
						continue;
					}

					InputStream in = null;
					try {
						URL productURL = ((URLNotification) notification)
								.getProductURL();
						LOGGER.finer("[" + getName() + "] notification url "
								+ productURL.toString());

						in = StreamUtils.getURLInputStream(productURL,
								connectTimeout, readTimeout);
						ProductSource productSource = IOUtil.autoDetectProductSource(in);

						Notification storedNotification = storeProductSource(productSource);

						LOGGER.finest("[" + getName()
								+ "] after store product, notification="
								+ storedNotification);

						if (productStorage.hasProduct(id)) {
							LOGGER.finer("[" + getName()
									+ "] getting product from storage");
							product = productStorage.getProduct(id);
							LOGGER.finest("[" + getName()
									+ "] after getProduct, product=" + product);

							try {
								new ProductTracker(notification.getTrackerURL())
										.productDownloaded(this.getName(), id);
								LOGGER.fine("[" + getName()
										+ "] product downloaded from "
										+ productURL.toString());
							} catch (Exception e) {
								LOGGER.log(
										Level.WARNING,
										"["
												+ getName()
												+ "] exception notifying tracker about downloaded product",
										e);
							}
						} else {
							LOGGER.finer("[" + getName()
									+ "] product not in storage id="
									+ productIdString);
						}
					} catch (Exception e) {
						if (e instanceof ProductAlreadyInStorageException
								|| e.getCause() instanceof ProductAlreadyInStorageException) {
							LOGGER.finer("[" + getName()
									+ "] product already in storage id="
									+ productIdString);
							product = productStorage.getProduct(id);
							continue;
						}

						// log any exception that happened while retrieving
						// product
						if (e instanceof FileNotFoundException) {
							LOGGER.warning("["
									+ getName()
									+ "] exception while retrieving product, file not found");
						} else {
							LOGGER.log(Level.WARNING, "[" + getName()
									+ "] exception while retrieving product", e);
							new ProductTracker(notification.getTrackerURL())
									.exception(this.getName(), id, e);
						}
					} finally {
						StreamUtils.closeStream(in);
					}
				}
			}
		} finally {
			LOGGER.finest("[" + getName() + "] releasing retrieve lock id="
					+ productIdString);
			retrieveLocks.releaseLock(id);
			LOGGER.finest("[" + getName() + "] retrieve lock released id="
					+ productIdString);
		}

		// return product
		return product;
	}

	/**
	 * Calls the current <code>ProductStorage.storeProductSource</code> method.
	 * 
	 * @param source
	 *            The <code>ProductSource</code> to store.
	 * @return The <code>ProductId</code> of the product referenced by the given
	 *         <code>ProductSource</code>.
	 * @throws Exception
	 * @see gov.usgs.earthquake.distribution.ProductStorage
	 */
	protected Notification storeProductSource(ProductSource source)
			throws Exception {
		Notification notification = null;

		// store product input
		ProductId id = productStorage.storeProductSource(source);

		// check if stored
		if (productStorage.hasProduct(id)) {
			Product product = productStorage.getProduct(id);

			// calculate storage expiration date
			Date expirationDate = new Date(new Date().getTime()
					+ productStorageMaxAge);

			// update notification index
			notification = new DefaultNotification(id, expirationDate,
					product.getTrackerURL());
			notificationIndex.addNotification(notification);
		}

		return notification;
	}

	/**
	 * Send matching notifications to listener.
	 * 
	 * Searches the NotificationIndex for matching notifications, and sends a
	 * NotificationEvent for each notification found.
	 * 
	 * @param listener
	 *            the listener to receive a NotificationEvent for each found
	 *            notification.
	 * @param sources
	 *            sources to include, or null for all.
	 * @param types
	 *            types to include, or null for all.
	 * @param codes
	 *            codes to include, or null for all.
	 * @throws Exception
	 *             if the notification index or notification listener throw an
	 *             exception.
	 */
	public void sendNotifications(NotificationListener listener,
			List<String> sources, List<String> types, List<String> codes)
			throws Exception {
		List<Notification> notifications = notificationIndex.findNotifications(
				sources, types, codes);
		Iterator<Notification> iter = notifications.iterator();
		while (iter.hasNext()) {
			listener.onNotification(new NotificationEvent(this, iter.next()));
		}
	}

	public void configure(Config config) throws Exception {
		String notificationIndexName = config
				.getProperty(NOTIFICATION_INDEX_PROPERTY);
		String notificationIndexFile = config.getProperty(INDEX_FILE_PROPERTY);
		if (notificationIndexName == null && notificationIndexFile == null) {
			throw new ConfigurationException("[" + getName()
					+ "] 'index' is a required configuration property");
		}
		if (notificationIndexName != null) {
			LOGGER.config("[" + getName() + "] loading notification index '"
					+ notificationIndexName + "'");
			notificationIndex = (NotificationIndex) Config.getConfig()
					.getObject(notificationIndexName);
			if (notificationIndex == null) {
				throw new ConfigurationException("[" + getName() + "] index '"
						+ notificationIndexName
						+ "' is not properly configured");
			}
		} else {
			LOGGER.config("[" + getName() + "] using notification index '"
					+ notificationIndexFile + "'");
			notificationIndex = new JDBCNotificationIndex(notificationIndexFile);
		}

		String productStorageName = config
				.getProperty(PRODUCT_STORAGE_PROPERTY);
		String storageDirectory = config
				.getProperty(STORAGE_DIRECTORY_PROPERTY);
		if (productStorageName == null && storageDirectory == null) {
			throw new ConfigurationException("[" + getName()
					+ "] 'storage' is a required configuration property");
		}
		if (productStorageName != null) {
			LOGGER.config("[" + getName() + "] loading product storage '"
					+ productStorageName + "'");
			productStorage = (ProductStorage) Config.getConfig().getObject(
					productStorageName);
			if (productStorage == null) {
				throw new ConfigurationException("[" + getName()
						+ "] storage '" + productStorageName
						+ "' is not properly configured");
			}
		} else {
			LOGGER.config("[" + getName() + "] using storage directory '"
					+ storageDirectory + "'");
			productStorage = new FileProductStorage(new File(storageDirectory));
		}

		productStorageMaxAge = Long.parseLong(config.getProperty(
				PRODUCT_STORAGE_MAX_AGE_PROPERTY,
				// previously all lower-case
				config.getProperty(PRODUCT_STORAGE_MAX_AGE_PROPERTY.toLowerCase(),
						DEFAULT_PRODUCT_STORAGE_MAX_AGE)));
		LOGGER.config("[" + getName() + "] storage max age "
				+ productStorageMaxAge + " ms");

		receiverCleanupInterval = Long.parseLong(config.getProperty(
				RECEIVER_CLEANUP_PROPERTY, DEFAULT_RECEIVER_CLEANUP));
		LOGGER.config("[" + getName() + "] receiver cleanup interval "
				+ receiverCleanupInterval + " ms");

		connectTimeout = Integer.parseInt(config.getProperty(
				CONNECT_TIMEOUT_PROPERTY, DEFAULT_CONNECT_TIMEOUT));
		LOGGER.config("[" + getName() + "] receiver connect timeout "
				+ connectTimeout + " ms");

		readTimeout = Integer.parseInt(config.getProperty(
				READ_TIMEOUT_PROPERTY, DEFAULT_READ_TIMEOUT));
		LOGGER.config("[" + getName() + "] receiver read timeout "
				+ readTimeout + " ms");

		String notifierType = config.getProperty(LISTENER_NOTIFIER_PROPERTY);
		if (notifierType != null) {
			if (notifierType.equals(EXECUTOR_LISTENER_NOTIFIER)) {
				notifier = new ExecutorListenerNotifier(this);
				LOGGER.config("[" + getName()
						+ "] using executor listener notifier");
			} else if (notifierType.equals(ROUNDROBIN_LISTENER_NOTIFIER)) {
				notifier = new RoundRobinListenerNotifier(this);
				LOGGER.config("[" + getName()
						+ "] using round-robin listener notifier");
			} else {
				throw new ConfigurationException("Unknown notifier type "
						+ notifierType);
			}
		}
	}

	public void shutdown() throws Exception {
		receiverCleanupTimer.cancel();

		try {
			notifier.shutdown();
		} catch (Exception ignore) {
		}
		try {
			notificationIndex.shutdown();
		} catch (Exception ignore) {
		}
		try {
			productStorage.shutdown();
		} catch (Exception ignore) {
		}
	}

	public void startup() throws Exception {
		if (productStorage == null) {
			throw new ConfigurationException("[" + getName()
					+ "] storage has not been configured properly");
		}
		if (notificationIndex == null) {
			throw new ConfigurationException("[" + getName()
					+ "] index has not been configured properly");
		}
		productStorage.startup();
		notificationIndex.startup();

		// only schedule cleanup if interval is non-zero
		if (receiverCleanupInterval > 0) {
			receiverCleanupTimer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					try {
						removeExpiredNotifications();
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "[" + getName()
								+ "] exception during receiver cleanup", e);
					}
				}
			}, 0, receiverCleanupInterval);
		}

		// do this last since it may start processing
		notifier.startup();

		// ProductClient already started these listeners...
		// Iterator<NotificationListener> iter = notificationListeners.keySet()
		// .iterator();
		// while (iter.hasNext()) {
		// iter.next().startup();
		// }
	}

	/**
	 * @return the notificationIndex
	 */
	public NotificationIndex getNotificationIndex() {
		return notificationIndex;
	}

	/**
	 * @param notificationIndex
	 *            the notificationIndex to set
	 */
	public void setNotificationIndex(NotificationIndex notificationIndex) {
		this.notificationIndex = notificationIndex;
	}

	/**
	 * @return the productStorage
	 */
	public ProductStorage getProductStorage() {
		return productStorage;
	}

	/**
	 * @param productStorage
	 *            the productStorage to set
	 */
	public void setProductStorage(ProductStorage productStorage) {
		this.productStorage = productStorage;
	}

	/**
	 * @return the productStorageMaxAge
	 */
	public Long getProductStorageMaxAge() {
		return productStorageMaxAge;
	}

	/**
	 * @param productStorageMaxAge
	 *            the productStorageMaxAge to set
	 */
	public void setProductStorageMaxAge(Long productStorageMaxAge) {
		this.productStorageMaxAge = productStorageMaxAge;
	}

	public Map<String, Integer> getQueueStatus() {
		if (notifier instanceof ExecutorListenerNotifier) {
			return ((ExecutorListenerNotifier) notifier).getStatus();
		}
		return null;
	}

	public Long getReceiverCleanupInterval() {
		return receiverCleanupInterval;
	}

	public void setReceiverCleanupInterval(Long receiverCleanupInterval) {
		this.receiverCleanupInterval = receiverCleanupInterval;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

}
