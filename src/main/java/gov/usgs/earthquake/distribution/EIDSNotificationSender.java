/*
 *
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.eidsutil.CorbaSender;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EIDSNotificationSender extends DefaultNotificationListener {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(EIDSNotificationSender.class.getName());

	/** Property referencing a product storage config section. */
	public static final String PRODUCT_STORAGE_PROPERTY = "storage";

	/** Property referencing how long to store products in milliseconds. */
	public static final String PRODUCT_STORAGE_MAX_AGE_PROPERTY = "storageage";

	/** Default max age to store products, 604800000 milliseconds = 1 week. */
	public static final String DEFAULT_PRODUCT_STORAGE_MAX_AGE = "604800000";

	/** Property referencing eids server host. */
	public static final String EIDS_SERVER_HOST_PROPERTY = "serverHost";

	/** Property referencing eids server port. */
	public static final String EIDS_SERVER_PORT_PROPERTY = "serverPort";

	/** Property referencing directory where notifications are "sent". */
	public static final String EIDS_POLLDIR_PROPERTY = "serverPolldir";

	/** Default directory where notifications are sent. */
	public static final String EIDS_DEFAULT_POLLDIR = "polldir";

	/** The product storage where retrieved products are stored. */
	private URLProductStorage productStorage;

	/** How long to store retrieved product, in milliseconds. */
	private Long productStorageMaxAge;

	/** How long to wait until checking for expired notifications/products. */
	private Long senderCleanupInterval;

	/** Host for CORBA send. */
	private String serverHost = null;
	/** Port for CORBA send. */
	private String serverPort = null;
	/** Directory for Polldir send, in case CORBA send fails. */
	private File serverPolldir = null;
	/** CORBA sending object. */
	private CorbaSender corbaSender = null;

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
		if (!isProcessDuplicates()) {
			// only check if we care
			List<Notification> notifications = getNotificationIndex()
					.findNotifications(notification.getProductId());
			if (notifications.size() > 0) {
				if (productStorage.hasProduct(notification.getProductId())) {
					LOGGER.finer("[" + getName()
							+ "] skipping existing product "
							+ notification.getProductId().toString());
					return false;
				} else {
					LOGGER.finer("["
							+ getName()
							+ "] found notifications, but product missing from storage "
							+ notification.getProductId().toString());
				}
			}
		}

		return true;
	}

	@Override
	protected void onAfterProcessNotification(final Notification notification)
			throws Exception {
		// replace this function, so the notification is not added to the index
		// this classes adds the notification it sends to the index, instead of
		// the notification it received from the receiver.
	}

	@Override
	public void onExpiredNotification(final Notification notification)
			throws Exception {
		List<Notification> notifications = getNotificationIndex()
				.findNotifications(notification.getProductId());
		if (notifications.size() <= 1) {
			// this is called before removing notification from index.
			productStorage.removeProduct(notification.getProductId());
			LOGGER.finer("[" + getName()
					+ "] removed expired product from sender storage "
					+ notification.getProductId().toString());
		} else {
			// still have notifications left for product, don't remove
		}
	}

	/**
	 * Store product and send a notification via eids.
	 */
	public void onProduct(final Product product) throws Exception {
		ProductId id = product.getId();

		// store product
		try {
			productStorage.storeProduct(product);
		} catch (ProductAlreadyInStorageException e) {
			// ignore
		}

		// create notification
		// make expiration relative to now
		Date expirationDate = new Date(new Date().getTime()
				+ productStorageMaxAge);
		URLNotification notification = new URLNotification(id, expirationDate,
				product.getTrackerURL(), productStorage.getProductURL(id));

		// remove any existing notifications, generally there won't be any
		Iterator<Notification> existing = getNotificationIndex()
				.findNotifications(id).iterator();
		while (existing.hasNext()) {
			getNotificationIndex().removeNotification(existing.next());
		}

		// add created notification to index. Used to track which products
		// have been processed, and to delete after expirationDate
		getNotificationIndex().addNotification(notification);

		// send notification
		sendEIDSMessage(serverHost, serverPort, notification.toXML());

		// track that notification was sent
		new ProductTracker(notification.getTrackerURL()).notificationSent(
				this.getName(), notification);
	}

	protected void sendEIDSMessage(final String server, final String port,
			final String message) throws Exception {
		boolean sent = false;

		if (serverHost != null && serverPort != null) {
			try {
				if (corbaSender == null) {
					// try to establish a connection
					corbaSender = new CorbaSender(serverHost, serverPort);
				}
				sent = corbaSender.sendMessage(message);
				if (sent) {
					LOGGER.fine("[" + getName()
							+ "] sent notification to EIDS via CORBA");
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] unable to send notification using CORBA", e);
			}
		}

		if (!sent) {
			// when unable to send directly to corba service
			// send notification via EIDS polldir

			// create a uniqueish filename
			String filename = "out_" + new Date().getTime();
			File outFile = new File(serverPolldir, filename + ".xml");
			while (outFile.exists()) {
				filename = filename += "_1";
				outFile = new File(serverPolldir, filename + ".xml");
			}

			// is this atomic enough?, write then move may be better
			FileUtils.writeFile(outFile, message.getBytes());
			LOGGER.log(Level.INFO, "[" + getName()
					+ "] sent notification to EIDS via " + outFile.getPath());
		}
	}

	public void configure(Config config) throws Exception {
		// let default notification listener configure itself
		super.configure(config);

		if (getNotificationIndex() == null) {
			throw new ConfigurationException("[" + getName()
					+ "] 'index' is a required configuration property");
		}

		String productStorageName = config
				.getProperty(PRODUCT_STORAGE_PROPERTY);
		if (productStorageName == null) {
			throw new ConfigurationException("[" + getName()
					+ "] 'storage' is a required configuration property");
		}
		LOGGER.config("[" + getName() + "] loading product storage '"
				+ productStorageName + "'");
		productStorage = (URLProductStorage) Config.getConfig().getObject(
				productStorageName);
		if (productStorage == null) {
			throw new ConfigurationException("[" + getName() + "] storage '"
					+ productStorageName + "' is not properly configured");
		}

		productStorageMaxAge = Long.parseLong(config.getProperty(
				PRODUCT_STORAGE_MAX_AGE_PROPERTY,
				DEFAULT_PRODUCT_STORAGE_MAX_AGE));
		LOGGER.config("[" + getName() + "] storage max age "
				+ productStorageMaxAge + " ms");

		serverHost = config.getProperty(EIDS_SERVER_HOST_PROPERTY);
		LOGGER.config("[" + getName() + "] EIDS CORBA server host '"
				+ serverHost + "'");

		serverPort = config.getProperty(EIDS_SERVER_PORT_PROPERTY);
		LOGGER.config("[" + getName() + "] EIDS CORBA server port '"
				+ serverPort + "'");

		serverPolldir = new File(config.getProperty(EIDS_POLLDIR_PROPERTY,
				EIDS_DEFAULT_POLLDIR));
		LOGGER.config("[" + getName() + "] EIDS server polldir '"
				+ serverPolldir + "'");

	}

	public void shutdown() throws Exception {
		super.shutdown();

		if (corbaSender != null) {
			try {
				corbaSender.destroy();
			} catch (Exception e) {
				// ignore
			}
			corbaSender = null;
		}

		try {
			productStorage.shutdown();
		} catch (Exception e) {
			// ignore
		}
	}

	public void startup() throws Exception {
		productStorage.startup();

		if (serverHost != null && serverPort != null) {
			try {
				corbaSender = new CorbaSender(serverHost, serverPort);
			} catch (org.omg.CORBA.COMM_FAILURE e) {
				LOGGER.warning("[" + getName()
						+ "] unable to connect to EIDS using CORBA");
				corbaSender = null;
			}
		}

		super.startup();
	}

	public URLProductStorage getProductStorage() {
		return productStorage;
	}

	public void setProductStorage(URLProductStorage productStorage) {
		this.productStorage = productStorage;
	}

	public Long getProductStorageMaxAge() {
		return productStorageMaxAge;
	}

	public void setProductStorageMaxAge(Long productStorageMaxAge) {
		this.productStorageMaxAge = productStorageMaxAge;
	}

	public Long getSenderCleanupInterval() {
		return senderCleanupInterval;
	}

	public void setSenderCleanupInterval(Long senderCleanupInterval) {
		this.senderCleanupInterval = senderCleanupInterval;
	}

	public String getServerHost() {
		return serverHost;
	}

	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	public String getServerPort() {
		return serverPort;
	}

	public void setServerPort(String serverPort) {
		this.serverPort = serverPort;
	}

	public File getServerPolldir() {
		return serverPolldir;
	}

	public void setServerPolldir(File serverPolldir) {
		this.serverPolldir = serverPolldir;
	}

}
