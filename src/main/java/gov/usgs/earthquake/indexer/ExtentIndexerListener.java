/**
 * Extent Indexer Listener
 */

package gov.usgs.earthquake.indexer;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.util.Config;

public class ExtentIndexerListener extends DefaultIndexerListener implements IndexerListener, Runnable {

  private static final Logger LOGGER = Logger
  .getLogger(ExtentIndexerListener.class.getName());

  private boolean stopThread;

  private long lastIndexId;

  private Object syncObject;
  private Thread processThread;

  private ProductIndex productIndex;

  //Constructor
  //Just uses super configure for now
  public ExtentIndexerListener() {
    super();

    stopThread = false;
    lastIndexId = -1; //Replace this with a database call later
    syncObject = new Object();
  }

  //Sets up object on start
  //Will set up thread and then tell it to chill (probably)
  public void configure(Config config) throws Exception {
    super.configure(config);

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

  //Called when indexer does something
  //Will wake our thread up as long as we're not busy processing still
  public void onIndexerEvent(IndexerEvent delta) throws Exception {
    //Maybe check IndexerEvent params to see if we should process it
    //Wake the thread
    synchronized (syncObject) {
      syncObject.notify();
    }
  }

  @Override
  public void run() {
    //Run until we're told not to
    while (!stopThread) {

      List<ProductSummary> productList = null;

      synchronized (syncObject) {
        try {
          productList = getNextProducts();
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "[" + getName() + "] Exception getting next products", e);
        }
        if (productList == null || productList.size() == 0) {
          try {
            syncObject.wait();
          } catch (InterruptedException ignore) {
            //Ignore because it's most likely we get interrupted by shutdown
          }
          continue;
        }
      }

      for(ProductSummary summary : productList) {
        if (stopThread) {
          break;
        }
        try {
          if (accept(summary.getId())) {
            this.processProduct(summary);
          }
          this.setLastIndexId(summary.getIndexId());
        } catch (Exception e) {
          try {
            this.onProcessException(summary,e);
          } catch(Exception e2) {
            break;
          }
        }
      }

    }
  }

  @Override
  public void startup() throws Exception{
    super.startup();
    this.onBeforeProcessThreadStart();
    this.processThread = new Thread(this);
    this.processThread.start();
  }

  @Override
  public void shutdown() throws Exception{
    //Do my stuff first
    stopThread = true;
    synchronized (syncObject) {
      this.processThread.interrupt();
    }
    this.processThread.join();

    super.shutdown();
  }

  //Stubs for subclasses

  protected void onBeforeProcessThreadStart() throws Exception {
    //Do database call to update lastIndexId
  }

  protected void onProcessException(ProductSummary product, Exception e) throws Exception {
    LOGGER.log(Level.WARNING, "[" + getName() + "] Exception processing product " + product.getId(), e);
  }

  public long getLastIndexId() {
    return lastIndexId;
  }

  public void setLastIndexId(final long lastIndexId) {
    this.lastIndexId = lastIndexId;
  } 

  public List<ProductSummary> getNextProducts() throws Exception{
    ProductIndexQuery query = new ProductIndexQuery();
    query.setLimit(10);
    query.setOrderBy(JDBCProductIndex.SUMMARY_PRODUCT_INDEX_ID);
    query.setMinProductIndexId(this.getLastIndexId()+1);

    return productIndex.getProducts(query);
  }

  public void processProduct(final ProductSummary product) throws Exception {
    //Do stuff
    LOGGER.info("[" + getName() + "] processing product " + product.getId());
  }
   
}
