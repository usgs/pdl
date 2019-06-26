/**
 * Extent Indexer Listener
 */

package gov.usgs.earthquake.indexer;

import gov.usgs.util.Config;
import gov.usgs.earthquake.indexer.ProductSummary;

public class ExtentIndexerListener extends ReliableIndexerListener {
  
  public ExtentIndexerListener() {
    super();
  }

  public void configure(Config config) throws Exception{
    
  }

  @Override
  protected void onBeforeProcessThreadStart() throws Exception {

  }

  @Override
  protected void onProcessException(ProductSummary product, Exception e) throws Exception{

  }

  @Override
  public void processProduct(ProductSummary product) throws Exception{

  }

  
}