/*
 * JsonProductSource
 */
package gov.usgs.earthquake.product.io;

import java.io.InputStream;
import java.io.InputStreamReader;

import javax.json.Json;
import javax.json.JsonReader;

import gov.usgs.util.StreamUtils;

import gov.usgs.earthquake.product.Product;


/**
 * Load a product from an InputStream containing Json.
 */
public class JsonProductSource implements ProductSource {

  /** The input stream where Json is read. */
  private InputStream in;

  /**
   * Create a new JsonProductSource.
   *
   * @param in
   *            the input stream where Json is read.
   */
  public JsonProductSource(final InputStream in) {
    this.in = in;
  }

  /**
   * Begin reading the input stream, sending events to out.
   *
   * @param out
   *            the receiving ProductOutput.
   */
  public synchronized void streamTo(ProductHandler out) throws Exception {
    final Product product;
    try (final JsonReader reader = Json.createReader(new InputStreamReader(in))) {
      product = new JsonProduct().getProduct(reader.readObject());
    }
    final ObjectProductSource source = new ObjectProductSource(product);
    source.streamTo(out);
  }

  /**
   * Free any resources associated with this handler.
   */
  @Override
  public void close() {
    StreamUtils.closeStream(in);
  }

}
