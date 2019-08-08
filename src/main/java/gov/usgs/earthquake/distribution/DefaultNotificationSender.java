package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * The base class for all Notification senders.
 *
 * The DefaultNotificationSender references a general need to send notifications. It extends DefaultNotificationListener
 * to allow forwarding of products from any subclass of DefaultNotificationReceiver. *
 */
public class DefaultNotificationSender extends DefaultNotificationListener {

  private static final Logger LOGGER = Logger
          .getLogger(DefaultNotificationSender.class.getName());

  /** Property referencing the server host */
  public static final String SERVER_HOST_PROPERTY = "serverHost";

  /** Property referencing the server port */
  public static final String SERVER_PORT_PROPERTY = "serverPort";

  /** Property referencing product storage object to use */
  public static final String PRODUCT_STORAGE_PROPERTY = "storage";

  /** Property referencing the length of time products should be held in storage*/
  public static final String PRODUCT_STORAGE_MAX_AGE_PROPERTY = "storageage";
  public static final String DEFAULT_PRODUCT_STORAGE_MAX_AGE = "604800000";

  private String serverHost;
  private String serverPort;
  private URLProductStorage productStorage;
  private long productStorageMaxAge;


  /**
   * Configures based on configuration section.
   *
   * @param config
   *            The config
   * @throws Exception
   */
  public void configure(Config config) throws Exception {
    super.configure(config);

    String productStorageName = config.getProperty(PRODUCT_STORAGE_PROPERTY);
    if (productStorageName == null) {
      throw new ConfigurationException("[" + getName() + "] '" + PRODUCT_STORAGE_PROPERTY + "' is a required property.");
    }
    LOGGER.config("[" + getName() + "] loading product storage '" + productStorageName + "'.");
    this.productStorage = (URLProductStorage) Config.getConfig().getObject(productStorageName);
    if (productStorage == null) {
      throw new ConfigurationException("[" + getName() + "] product storage '" + productStorageName + "' improperly configured.");
    }

    productStorageMaxAge = Long.parseLong(config.getProperty(PRODUCT_STORAGE_MAX_AGE_PROPERTY,DEFAULT_PRODUCT_STORAGE_MAX_AGE));
    LOGGER.config("[" + getName() + "] product storage max age: " + productStorageMaxAge + "ms");

    serverHost = config.getProperty(SERVER_HOST_PROPERTY);
    LOGGER.config("[" + getName() + "] messenger server host: " + serverHost);

    serverPort = config.getProperty(SERVER_PORT_PROPERTY);
    LOGGER.config("[" + getName() + "] messenger server port: " + serverPort);
  }

  /**
   * Called on receipt of a new product. Stores this product and calls sendMessage()
   * Most of this logic was lifted from the pre-08/2019 EIDSNotificationSender class.
   *
   * @param product
   *            a product whose notification was accepted.
   * @throws Exception
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
    sendMessage(notificationToString(notification));

    // track that notification was sent
    new ProductTracker(notification.getTrackerURL()).notificationSent(
            this.getName(), notification);
  }

  /**
   * Called when a notification expires
   *
   * @param notification
   *                The expired notification
   * @throws Exception
   */
  @Override
  protected void onExpiredNotification(final Notification notification) throws Exception{
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
   * Utility method to do the actual notification sending. Should be overridden by subclasses.
   *
   * @param message
   *            The text message to send
   * @throws Exception
   */
  protected void sendMessage(final String message) throws Exception {
    LOGGER.info("[" + getName() + "] sent message " + message);
  }

  /**
   * Utility method to convert notifications to message strings. Should be overridden by subclasses to have
   * child-specific message strings
   *
   * @param notification
   *                The notification to be converted
   * @return message
   */
  protected String notificationToString(final Notification notification) throws Exception {
    return notification.toString();
  }


  /**
   * Getters and Setters
   */

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

  public URLProductStorage getProductStorage() {
    return productStorage;
  }

  public void setProductStorage(URLProductStorage productStorage) {
    this.productStorage = productStorage;
  }

  public long getProductStorageMaxAge() {
    return productStorageMaxAge;
  }

  public void setProductStorageMaxAge(long productStorageMaxAge) {
    this.productStorageMaxAge = productStorageMaxAge;
  }
}
