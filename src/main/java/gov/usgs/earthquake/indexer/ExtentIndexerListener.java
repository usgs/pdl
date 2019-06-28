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

  @Override
  protected void onBeforeProcessThreadStart() throws Exception {
    setLastIndexId(((ExtentIndex)productIndex).getLastExtentIndexId());
  }

  @Override
  protected void onProcessException(ProductSummary product, Exception e) throws Exception{
    super.onProcessException(product,e);
  }

  @Override
  public void processProduct(ProductSummary product) throws Exception{
    ExtentSummary extent = new ExtentSummary(product);

    ((ExtentIndex)productIndex).addExtentSummary(extent);
    setLastIndexId(product.getIndexId());
  }

  
}