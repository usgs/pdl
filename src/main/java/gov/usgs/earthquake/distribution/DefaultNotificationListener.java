/*
 * DefaultNotificationListener
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.AbstractListener;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A base implementation of a notification listener. Implements functionality
 * that is useful for most notification listeners.
 * 
 * Sub classes should override the onProduct(Product) method to add custom
 * processing.
 * 
 * The DefaultNotificationListener extends the AbstractListener and can use any
 * of those configuration parameters.
 * 
 * @see gov.usgs.earthquake.product.AbstractListener
 */
public class DefaultNotificationListener extends AbstractListener implements
		NotificationListener {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(DefaultNotificationListener.class.getName());

	/** Property referencing a notification index config section. */
	public static final String NOTIFICATION_INDEX_PROPERTY = "listenerIndex";
	public static final String INDEX_FILE_PROPERTY = "listenerIndexFile";

	/** How long to wait until checking for expired notifications/products. */
	public static final String CLEANUP_INTERVAL_PROPERTY = "cleanupInterval";
	public static final String DEFAULT_CLEANUP_INTERVAL = "3600000";

	/** Whether or not to process products more than once. */
	public static final String PROCESS_DUPLICATES = "processDuplicates";
	public static final String DEFAULT_PROCESS_DUPLICATES = "false";

	/** Filter products based on content paths they contain. */
	public static final String INCLUDE_PATHS_PROPERTY = "includePaths";
	public static final String EXCLUDE_PATHS_PROPERTY = "excludePaths";

	/** Optional notification index. */
	private NotificationIndex notificationIndex = null;

	/** How often to run cleanup task, in ms, <=0 = off. */
	private Long cleanupInterval = 0L;

	/** Timer that schedules sender cleanup task. */
	private Timer cleanupTimer = null;

	/** Whether or not to process products that have already been processed. */
	private boolean processDuplicates = false;

	/** Array of content paths to search. */
	private final ArrayList<String> includePaths = new ArrayList<String>();

	/** Array of content paths to search. */
	private final ArrayList<String> excludePaths = new ArrayList<String>();

	/**
	 * Implement the NotificationListener interface.
	 * 
	 * This method calls accept, and if accept returns true, retrieves the
	 * product and calls onProduct.
	 */
	public void onNotification(final NotificationEvent event) throws Exception {
		Notification notification = event.getNotification();
		ProductId id = notification.getProductId();
		String productId = id.toString();

		LOGGER.finest("[" + getName() + "] received notification for id="
				+ productId);

		if (!accept(id)) {
			LOGGER.finest("[" + getName() + "] rejected notification for id="
					+ productId);
			return;
		}

		if (!onBeforeProcessNotification(notification)) {
			return;
		}

		LOGGER.finer("[" + getName() + "] processing notification for id="
				+ productId);

		Product product = event.getProduct();
		if (product == null) {
			throw new ContinuableListenerException("retrieved product null,"
					+ " notification id=" + productId);
		} else {
			if (!onBeforeProcessProduct(product)) {
				return;
			}
			LOGGER.finer("[" + getName() + "] processing product for id="
					+ productId);
			onProduct(product);

			onAfterProcessNotification(notification);
		}
	}

	/**
	 * Called by onNotification when a product is retrieved.
	 * 
	 * @param product
	 *            a product whose notification was accepted.
	 * @throws Exception
	 */
	public void onProduct(final Product product) throws Exception {
		// subclasses do stuff here
		ProductId id = product.getId();
		StringBuffer b = new StringBuffer("[" + getName()
				+ "] product processed source=" + id.getSource() + ", type="
				+ id.getType() + ", code=" + id.getCode() + ", updateTime="
				+ id.getUpdateTime().toString());

		Map<String, String> properties = product.getProperties();
		Iterator<String> iter = properties.keySet().iterator();
		while (iter.hasNext()) {
			String name = iter.next();
			b.append(", ").append(name).append("=")
					.append(properties.get(name));
		}

		LOGGER.info(b.toString());
		System.out.println(b.toString());
	}

	/**
	 * Called just before this listener processes a notification.
	 * 
	 * @param notification
	 *            notification about to be processed.
	 * @return true to process the notification, false to skip
	 * @throws Exception
	 */
	protected boolean onBeforeProcessNotification(
			final Notification notification) throws Exception {
		if (!processDuplicates) {
			// only check if we care
			if (this.notificationIndex != null) {
				List<Notification> notifications = this.notificationIndex
						.findNotifications(notification.getProductId());
				if (notifications.size() > 0) {
					LOGGER.finer("[" + getName()
							+ "] skipping existing product "
							+ notification.getProductId().toString());
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Called after a product has been downloaded, but before onProduct is
	 * called.
	 * 
	 * Sometimes a listener cannot tell whether it should process a product
	 * until its contents are available. This is where the "includePaths" and
	 * "excludePaths" are evaluated.
	 * 
	 * @param product
	 *            product about to be processed.
	 * @return true to process the product, false to skip
	 * @throws Exception
	 */
	protected boolean onBeforeProcessProduct(final Product product)
			throws Exception {
		if (excludePaths.size() != 0) {
			Map<String, Content> contents = product.getContents();
			Iterator<String> pathIter = excludePaths.iterator();
			while (pathIter.hasNext()) {
				String path = pathIter.next();
				if (contents.containsKey(path)) {
					// contains at least one matching include path
					LOGGER.fine("[" + getName() + "] skipping product "
							+ product.getId().toString() + ", '" + path
							+ "' matches excludePaths");
					return false;
				}
			}
		}

		if (includePaths.size() != 0) {
			boolean containsPath = false;
			Map<String, Content> contents = product.getContents();
			Iterator<String> pathIter = includePaths.iterator();
			while (pathIter.hasNext()) {
				String path = pathIter.next();
				if (contents.containsKey(path)) {
					// contains at least one matching include path
					containsPath = true;
					break;
				}
			}
			if (!containsPath) {
				LOGGER.fine("[" + getName() + "] skipping product "
						+ product.getId().toString()
						+ ", does not match includePaths");
				return false;
			}
		}

		return true;
	}

	/**
	 * Called when this listener has successfully processed a notification.
	 * 
	 * @param notification
	 *            notification that was processed.
	 * @throws Exception
	 */
	protected void onAfterProcessNotification(final Notification notification)
			throws Exception {
		if (this.notificationIndex != null) {
			this.notificationIndex.addNotification(notification);
		}
	}

	/**
	 * Called when an expired notification is being removed from the index.
	 * 
	 * @param notification
	 * @throws Exception
	 */
	protected void onExpiredNotification(final Notification notification)
			throws Exception {
		// nothing to do
	}

	/**
	 * Periodic cleanup task.
	 * 
	 * Called every cleanupInterval milliseconds.
	 */
	public void cleanup() {
		LOGGER.finer("[" + getName() + "] running listener cleanup");
		try {
			if (notificationIndex != null) {
				Iterator<Notification> iter = notificationIndex
						.findExpiredNotifications().iterator();
				while (iter.hasNext()) {
					Notification notification = iter.next();

					// let subclasses remove other stuff first
					onExpiredNotification(notification);

					// remove expired notification from index
					notificationIndex.removeNotification(notification);

					if (LOGGER.isLoggable(Level.FINEST)) {
						LOGGER.finest("["
								+ getName()
								+ "] removed expired notification from sender index "
								+ notification.getProductId().toString());
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception during listener cleanup", e);
		}
	};

	@Override
	public void startup() throws Exception {
		super.startup();
		if (this.notificationIndex != null) {
			this.notificationIndex.startup();
		}

		// only schedule cleanup if interval is non-zero
		if (cleanupInterval > 0) {
			cleanupTimer = new Timer();
			cleanupTimer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					try {
						cleanup();
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "[" + getName()
								+ "] exception during sender cleanup", e);
					}
				}
			}, 0, cleanupInterval);
		}
	}

	@Override
	public void shutdown() throws Exception {
		super.shutdown();
		try {
			this.notificationIndex.shutdown();
		} catch (Exception e) {
			// ignore
		}
		try {
			this.cleanupTimer.cancel();
		} catch (Exception e) {
			// ignore
		}
	}

	@Override
	public void configure(final Config config) throws Exception {
		super.configure(config);

		String notificationIndexName = config
				.getProperty(NOTIFICATION_INDEX_PROPERTY);
		String notificationIndexFile = config.getProperty(INDEX_FILE_PROPERTY);
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
		} else if (notificationIndexFile != null) {
			LOGGER.config("[" + getName() + "] using notification index '"
					+ notificationIndexFile + "'");
			notificationIndex = new JDBCNotificationIndex(notificationIndexFile);
		}

		cleanupInterval = Long.parseLong(config.getProperty(
				CLEANUP_INTERVAL_PROPERTY, DEFAULT_CLEANUP_INTERVAL));

		processDuplicates = Boolean.valueOf(config.getProperty(
				PROCESS_DUPLICATES, DEFAULT_PROCESS_DUPLICATES));
		LOGGER.config("[" + getName() + "] process duplicates = "
				+ processDuplicates);

		includePaths.addAll(StringUtils.split(
				config.getProperty(INCLUDE_PATHS_PROPERTY), ","));
		LOGGER.config("[" + getName() + "] include paths = " + includePaths);

		excludePaths.addAll(StringUtils.split(
				config.getProperty(EXCLUDE_PATHS_PROPERTY), ","));
		LOGGER.config("[" + getName() + "] exclude paths = " + excludePaths);
	}

	public NotificationIndex getNotificationIndex() {
		return notificationIndex;
	}

	public void setNotificationIndex(NotificationIndex notificationIndex) {
		this.notificationIndex = notificationIndex;
	}

	public Long getCleanupInterval() {
		return cleanupInterval;
	}

	public void setCleanupInterval(Long cleanupInterval) {
		this.cleanupInterval = cleanupInterval;
	}

	public boolean isProcessDuplicates() {
		return processDuplicates;
	}

	public void setProcessDuplicates(boolean processDuplicates) {
		this.processDuplicates = processDuplicates;
	}

	/**
	 * @return the includePaths
	 */
	public ArrayList<String> getIncludePaths() {
		return includePaths;
	}

	/**
	 * @return the excludePaths
	 */
	public ArrayList<String> getExcludePaths() {
		return excludePaths;
	}

}
