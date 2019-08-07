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

  public static final String SERVER_HOST_PROPERTY = "serverHost";
  public static final String SERVER_PORT_PROPERTY = "serverPort";
  public static final String PRODUCT_STORAGE_PROPERTY = "storage";
  public static final String PRODUCT_STORAGE_MAX_AGE_PROPERTY = "storageage";

  public static final String DEFAULT_PRODUCT_STORAGE_MAX_AGE = "604800000";

  private String host;
  private String port;
  private URLProductStorage productStorage;
  private long productStorageMaxAge;


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

    host = config.getProperty(SERVER_HOST_PROPERTY);
    LOGGER.config("[" + getName() + "] messenger server host: " + host);

    port = config.getProperty(SERVER_PORT_PROPERTY);
    LOGGER.config("[" + getName() + "] messenger server port: " + port);
  }

  public void startup() throws Exception{

  }

  public void shutdown() throws Exception{

  }

  public void onProduct(final Product product) throws Exception {

  }

  protected void sendMessage(final String message) throws Exception{

  }
}
