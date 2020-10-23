/*
 * JsonProductHandler
 */
package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.StreamUtils;

import java.io.OutputStream;

/**
 * Store a product as Json.
 */
public class JsonProductHandler extends ObjectProductHandler {

  /** The output stream where zip content is written. */
  private OutputStream out;

  /**
   * Construct a new ZipProductHandler object.
   *
   * @param out
   *            the output stream where zip content is written.
   */
  public JsonProductHandler(final OutputStream out) {
    this.out = out;
  }

  /**
   * Creates and outputs the zip stream.
   */
  public void onEndProduct(ProductId id) throws Exception {
    super.onEndProduct(id);

    // write json format
    Product product = getProduct();
    byte[] json = new JsonProduct().getJsonObject(product).toString().getBytes("UTF8");
    out.write(json);
  }


  /**
   * Free any resources associated with this handler.
   */
  @Override
  public void close() {
    StreamUtils.closeStream(out);
  }

}
