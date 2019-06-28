/*
 * Reliable Indexer Listener
 */
package gov.usgs.earthquake.indexer;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.util.Config;

/**
 * ReliableIndexerListener listens for product changes by the indexer, then handles the new products independently in a background thread.
 * 
 * This class does little more than output logs for the products it has seen; it is designed to be extended.
 * 
 * Several useful methods are availble to be overridden or otherwise used:
 * <ul>
 * <li>onBeforeProcessThreadStart</li>
 * <li>onProcessException</li>
 * <li>getNextProducts</li>
 * <li>processProducts</li>
 * </ul>
 * 
 * This class accepts an index for querying in config:
 * 
 * <dl>
 * <dt>index</dt>
 * <dd>(Required) The index to use for product querying.</dd>
 * </dl>
 */

public class ReliableIndexerListener extends DefaultIndexerListener implements IndexerListener, Runnable {

  private static final Logger LOGGER = Logger
  .getLogger(ReliableIndexerListener.class.getName());
  private static int PRODUCTS_PER_QUERY = 10;

  private boolean stopThread = false;
  private long lastIndexId = -1;
  private Object syncObject = new Object();

  private Thread processThread;
  protected ProductIndex productIndex;

  //Constructor
  public ReliableIndexerListener() {
    super();
  }

  /**
   * Sets up an object on start
   * 
   * @param config configuration
   */
  public void configure(Config config) throws Exception {
    super.configure(config);

    //Getting indexer for queries
    String indexName = config.getProperty(Indexer.INDEX_CONFIG_PROPERTY);
		if (indexName != null) {
			LOGGER.config("[" + getName() + "] loading ProductIndex '"
					+ indexName + "'");
			productIndex = (ProductIndex) Config.getConfig().getObject(
					indexName);
    }
    if (productIndex == null) {
      throw new ConfigurationException("[" + getName()
          + "] ProductIndex is required");
    }
  }

  /** 
   * Wakes thread when indexer makes changes
   * 
   * @param delta Indexer Event - not used
   */ 
  public void onIndexerEvent(IndexerEvent delta) throws Exception {
    //Synchronized on the syncObject so we don't miss events
    synchronized (syncObject) {
      syncObject.notify();
    }
  }

  /**
   * Thread main body. Waits until notified, then tries to get the next products and process them.
   */
  @Override
  public void run() {
    //Run until we're told not to
    while (!stopThread) {

      List<ProductSummary> productList = null;

      //Synchronized so we aren't notified of new products right before we wait
      synchronized (syncObject) {
        try {
          productList = getNextProducts();
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "[" + getName() + "] Exception getting next products", e); //Not a phenomenal way to handle this exception
        }
        if (productList == null || productList.size() == 0) {
          try {
            //Wait when there are no mroe products to process
            syncObject.wait();
          } catch (InterruptedException ignore) {
            //Ignore because it's most likely we get interrupted by shutdown
          }
          continue;
        }
      }

      //Process the products we have
      for(ProductSummary summary : productList) {
        //Check for shutdown every iteration so we don't hog shutdown time
        if (stopThread) {
          break;
        }
        try {
          //Process the product types we're told to in configuration
          if (accept(summary.getId())) {
            this.processProduct(summary);
          }
          //Update internal storage so we don't reprocess products
          this.setLastIndexId(summary.getIndexId());
        } catch (Exception e) {
          try {
            //Handle exception if we can
            this.onProcessException(summary,e);
          } catch(Exception e2) {
            //Give up if we can't
            break;
          }
        }
      }

    }
  }

  /**
   * Starts thread
   * 
   * Calls onBeforeProcessThreadStart() in case subclasses want to add functionality
   */
  @Override
  public void startup() throws Exception{
    super.startup();
    this.onBeforeProcessThreadStart();
    this.processThread = new Thread(this);
    this.processThread.start();
  }

  /**
   * Closes thread
   */
  @Override
  public void shutdown() throws Exception{
    stopThread = true;
    //When the thread is ready, tell it to stop
    synchronized (syncObject) {
      this.processThread.interrupt();
    }
    this.processThread.join();

    super.shutdown();
  }


  ////////////////////////
  //Stubs for subclasses//
  ////////////////////////

  /**
   * Gets index ID of last processed product
   */
  public long getLastIndexId() {
    return lastIndexId;
  }

  /**
   * Sets index ID of last processed product
   */
  public void setLastIndexId(final long lastIndexId) {
    this.lastIndexId = lastIndexId;
  } 

  /**
   * Run before thread start.
   */
  protected void onBeforeProcessThreadStart() throws Exception {
    //Do database call to update lastIndexId
  }

  /**
   * Exception handling for product processing. Throws an exception in case we can't handle it.
   */
  protected void onProcessException(ProductSummary product, Exception e) throws Exception {
    LOGGER.log(Level.WARNING, "[" + getName() + "] Exception processing product " + product.getId(), e);
  }

  /**
   * Gets the next products using the index provided in Config
   */
  public List<ProductSummary> getNextProducts() throws Exception{
    ProductIndexQuery query = new ProductIndexQuery();
    query.setLimit(PRODUCTS_PER_QUERY);
    query.setOrderBy(JDBCProductIndex.SUMMARY_PRODUCT_INDEX_ID); //Currently the only public field; should maybe change
    query.setMinProductIndexId(this.getLastIndexId()+1);

    return productIndex.getProducts(query);
  }

  /**
   * Does a task with each product
   */
  public void processProduct(final ProductSummary product) throws Exception {
    //Do stuff
    LOGGER.info("[" + getName() + "] processing product " + product.getId());
  }
   
}
