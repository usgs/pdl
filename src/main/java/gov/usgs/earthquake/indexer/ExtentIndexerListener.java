/**
 * Extent Indexer Listener
 */

package gov.usgs.earthquake.indexer;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import gov.usgs.util.Config;

public class ExtentIndexerListener extends DefaultIndexerListener implements IndexerListener, Runnable {

  private static final Logger LOGGER = Logger
  .getLogger(ExtentIndexerListener.class.getName());

  private boolean stopThread;

  private long lastIndexId;

  private Object syncObject;
  private Thread processThread;

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

      List<ProductSummary> productList;

      synchronized (syncObject) {
        productList = getNextProducts();

        if (productList.size() == 0) {
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
        this.processProduct(summary);
        this.setLastIndexId(summary.getIndexId());
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

  protected void onBeforeProcessThreadStart() throws Exception{
    //Do database call to update lastIndexId
  }

  public long getLastIndexId() {
    return lastIndexId;
  }

  public void setLastIndexId(final long lastIndexId) {
    this.lastIndexId = lastIndexId;
  } 

  public List<ProductSummary> getNextProducts() {
    return new LinkedList<ProductSummary>();
  }

  public void processProduct(final ProductSummary product) {
    //Do stuff
    LOGGER.info("[" + getName() + "] processing product " + product.getId());
  }
   
}
