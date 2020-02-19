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

  protected String serverHost;
  protected String serverPort;
  protected URLProductStorage productStorage;
  protected long productStorageMaxAge;


  /**
   * Configures based on configuration section.
   *
   * @param config
   *            The config
   * @throws Exception
   */
  public void configure(Config config) throws Exception {
    // let default notification listener configure itself
    super.configure(config);

    if (getNotificationIndex() == null) {
      throw new ConfigurationException("[" + getName()
              + "] 'index' is a required configuration property");
    }

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

    // send notification
    try {
      sendNotification(notification);
    } catch (Exception e) {
      // if fails, try to remove from storage
      productStorage.removeProduct(id);
      throw e;
    }

    // add created notification to index. Used to track which products
    // have been processed, and to delete after expirationDate
    // done after send in case send fails
    getNotificationIndex().addNotification(notification);

    // track that notification was sent
    new ProductTracker(notification.getTrackerURL()).notificationSent(
            this.getName(), notification);
  }

  /**
   * Called just before this listener processes a notification.
   *
   * @param notification
   *            notification about to be processed.
   * @return true to process the notification, false to skip
   * @throws Exception
   */
  @Override
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
  protected void onAfterProcessNotification(final Notification notification) {
    // function replaced so notifications not added to index
    // this class responds to the index
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
   * @param notification
   *            The notification to send
   * @throws Exception
   */
  protected void sendNotification(final Notification notification) throws Exception {
    LOGGER.info("[" + getName() + "] sent message " + notification.toString());
  }

  /**
   * Start up storage
   *
   * @throws Exception if something goes wrong
   */
  public void startup() throws Exception{
    productStorage.startup();
    super.startup();
  }

  /**
   * Shut down storage
   *
   * @throws Exception if something goes wrong
   */
  public void shutdown() throws Exception{
    super.shutdown();
    try {
      productStorage.shutdown();
    } catch (Exception e) {
      //do nothing
    }
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
