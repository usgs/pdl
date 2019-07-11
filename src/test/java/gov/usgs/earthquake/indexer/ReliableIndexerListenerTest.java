/*
 * Reliable Indexer Listener Test
 */

package gov.usgs.earthquake.indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.earthquake.product.ProductId;

public class ReliableIndexerListenerTest {

  private TestIndexerListener synchronizeListener = new TestIndexerListener();
  private ArrayList<ProductSummary> products = new ArrayList<ProductSummary>();
  private long currentIndex = 0;
  private long lastQueryIndexId;
  private Object nextProducts = new Object();
  private Object productProcessed = new Object();
  private boolean waitForProducts = false;


  @Before
  public void prepareTests() {
    synchronizeListener = new TestIndexerListener();
    products.clear();
  }

  @Test
  public void synchronizeTest() throws Exception {
    long testIndex = 7;

    //make wait flag true so we block
    waitForProducts = true;

    //start up synchronized listener
    synchronizeListener.startup();

    //in synchronized, trigger indexer event
    Thread tr = new Thread(new IndexerEventThread());
    synchronized (nextProducts) {
      //start indexer event thread (basically queue new event)
      tr.start();
    }

    //in synchronized:
    //  notify getNextProducts -> returns empty list
    synchronized(nextProducts) {
      nextProducts.notify();
    }
    //because of queued indexerevent, getNextProducts blocks again

    //sync on process
    //  sync on next
    //    update next product list
    //    notify so it returns
    //  wait on process (until list is processed)
    synchronized (productProcessed) {
      synchronized (nextProducts) {
        ProductSummary product = new ProductSummary();
        product.setIndexId(testIndex);
        product.setId(new ProductId("test","test","test"));
        products.add(product);
        waitForProducts = false; //Turn off wait so we don't block anymore
        nextProducts.notify();
      }
      productProcessed.wait();
    }
    
    //confirm last index is the one we handed
    Assert.assertEquals(testIndex,synchronizeListener.getLastIndexId());

    tr.join(); //join indexer event thread
    synchronizeListener.shutdown();
  }

  @Test
  public void indexTest() throws Exception {
    long testIndex = 8;

    //start up listener
    synchronizeListener.startup();

    //update product list
    synchronized (nextProducts) {
      ProductSummary product = new ProductSummary();
      product.setIndexId(testIndex);
      product.setId(new ProductId("test","test","test"));
      products.add(product);
    }

    //hand in new event, wait until processed
    synchronized (productProcessed) {
      synchronizeListener.onIndexerEvent(new IndexerEvent(new Indexer()));
      productProcessed.wait();
    }

    //shutdown synchronizelistener
    synchronizeListener.shutdown();

    //start up listener
    synchronizeListener.startup();

    //confirm that it starts where it left off
    Assert.assertEquals(testIndex,synchronizeListener.getLastIndexId());

    synchronizeListener.shutdown();

  }

  //TODO: Fix me, still broken
  @Test
  public void queryTest() throws Exception {
    long testIndex = 9;

    //start new reliablelistener, hand index
    ReliableIndexerListener listener = new ReliableIndexerListener();
    listener.setProductIndex(new TestIndex());
    listener.startup();

    //create new product
    ProductSummary product = new ProductSummary();
    product.setIndexId(testIndex);
    product.setId(new ProductId("test","test","test"));
    products.add(product);
    
    //notify of new product (with index)
    synchronized (nextProducts) {
      listener.onIndexerEvent(new IndexerEvent(new Indexer()));
      nextProducts.wait(); //wait until listener has the product
    }

    //wait to ask for another product
    synchronized(nextProducts) {
      nextProducts.wait();
    }

    //confirm correct query for product
    Assert.assertEquals(testIndex+1,lastQueryIndexId);

    listener.shutdown();
  }

  public class TestIndexerListener extends ReliableIndexerListener {
    //Grabs up to 10 products from product list
    @Override
    public List<ProductSummary> getNextProducts() throws InterruptedException{
      synchronized (nextProducts) {
        if (waitForProducts) {
          LOGGER.log(Level.FINEST,"[" + getName() + "] waiting for products...");
          nextProducts.wait();
        }
        LOGGER.log(Level.FINEST,"[" + getName() + "] done waiting for products.");
      }
      return new ArrayList<>(products);
    }

    @Override
    public void processProduct(ProductSummary product) throws Exception{
      super.processProduct(product);
      synchronized (productProcessed) {
        products.clear();
        currentIndex = product.getIndexId();
        setLastIndexId(product.getIndexId());
        productProcessed.notify();
      }
    }

    //Updates the index on start like the extent should
    @Override
    public void onBeforeProcessThreadStart() {
      setLastIndexId(currentIndex);
    }

  }

  public class IndexerEventThread implements Runnable {

    @Override
    public void run(){
      try {
        synchronizeListener.onIndexerEvent(new IndexerEvent(new Indexer()));
      } catch (Exception e) {
        //Exception happened
      }
    }
    
  }

  public class TestIndex extends JDBCProductIndex {

    public TestIndex() throws Exception {
      super();
    }

    @Override
    public List<ProductSummary> getProducts(ProductIndexQuery query) {
      synchronized(nextProducts) {
        lastQueryIndexId = query.getMinProductIndexId();
        List<ProductSummary> ret = new ArrayList<>(products); //Get copy of products
        products.clear(); //Clear products so we don't loop infinitely
        nextProducts.notify();
        return ret;
      }
    }
    
  }
}