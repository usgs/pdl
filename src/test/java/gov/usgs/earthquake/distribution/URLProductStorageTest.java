package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;

import java.net.URL;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;


public class URLProductStorageTest {

  /**
   * Test that storage path is configured properly by default
   * and includes all product id components (so url is unique per product).
   */
  @Test
  public void testDefaultStoragePath() throws Exception {
    URLProductStorage storage = new URLProductStorage();
    Config config = new Config();
    config.setProperty(URLProductStorage.URL_PROPERTY_NAME, "http://testserver/product/");
    storage.configure(config);

    ProductId id = new ProductId("example_source", "example_type", "example_code", new Date());
    String path = storage.getProductPath(id);
    Assert.assertTrue("contains source", path.contains("example_source"));
    Assert.assertTrue("contains type", path.contains("example_type"));
    Assert.assertTrue("contains code", path.contains("example_code"));
    Assert.assertTrue(
        "contains updateTime",
        path.contains("" + id.getUpdateTime().getTime()));
  }

  /**
   * Test that url is constructed by combining base url and product path.
   */
  @Test
  public void testGetProductUrl() throws Exception {
    URLProductStorage storage = new URLProductStorage();
    Config config = new Config();
    config.setProperty(URLProductStorage.URL_PROPERTY_NAME, "http://testserver/product/");
    storage.configure(config);

    ProductId id = new ProductId("source", "type", "code", new Date());
    URL url = storage.getProductURL(id);
    Assert.assertEquals("Computes URL",
        url.toExternalForm(),
        "http://testserver/product/" + storage.getProductPath(id));
  }

}