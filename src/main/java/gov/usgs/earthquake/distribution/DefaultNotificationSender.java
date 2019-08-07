package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.util.Config;

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

  //TODO: Implement me
  /**
   * Called on receipt of a new product.
   *
   * @param product
   *            a product whose notification was accepted.
   * @throws Exception
   */
  public void onProduct(final Product product) throws Exception {

  }

  //TODO: Implement me
  /**
   * Called when a notification expires
   *
   * @param notification
   *                The expired notification
   * @throws Exception
   */
  @Override
  protected void onExpiredNotification(final Notification notification) throws Exception{

  }

  /**
   * Utility method to do the actual message sending. Should be overridden by subclasses.
   *
   * @param message
   *            The text message to send
   * @throws Exception
   */
  protected void sendMessage(final String message) throws Exception{
    LOGGER.info("[" + getName() + "] sending notification: " + message);
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
