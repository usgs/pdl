package gov.usgs.earthquake.origin;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.earthquake.geoserve.GeoservePlaces;
import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;

public class OriginIndexerModuleTest {

  private OriginIndexerModule module = null;

  @Before
  public void setUpTestEnvironment() throws Exception {
    module = new OriginIndexerModule();
  }

  @Test
  public void getSupportLevel() throws Exception {
    ProductId id = new ProductId("unit", "origin", "test");

    // Does support origin products with non-delete status
    Product product = new Product(id, "UPDATE");
    int supportLevel = module.getSupportLevel(product);
    Assert.assertEquals(IndexerModule.LEVEL_SUPPORTED, supportLevel);

    product = new Product(id, "DELETE");
    supportLevel = module.getSupportLevel(product);
    Assert.assertEquals(IndexerModule.LEVEL_UNSUPPORTED, supportLevel);

    product = new Product(id, "SOME_OTHER_STATUS");
    supportLevel = module.getSupportLevel(product);
    Assert.assertEquals(IndexerModule.LEVEL_SUPPORTED, supportLevel);

    // Does not support non-origin products
    id = new ProductId("unit", "NotAnOrigin", "test");
    product = new Product(id, "UPDATE");
    supportLevel = module.getSupportLevel(product);
    Assert.assertEquals(IndexerModule.LEVEL_UNSUPPORTED, supportLevel);

    product = new Product(id, "DELETE");
    supportLevel = module.getSupportLevel(product);
    Assert.assertEquals(IndexerModule.LEVEL_UNSUPPORTED, supportLevel);

    product = new Product(id, "SOME_OTHER_STATUS");
    supportLevel = module.getSupportLevel(product);
    Assert.assertEquals(IndexerModule.LEVEL_UNSUPPORTED, supportLevel);
  }

  @Test
  public void getProductSummary() throws Exception {
    GeoservePlaces service = new DummyPlacesService();
    ProductId id = new ProductId("unit", "origin", "test");
    String defaultTitle = "event title";
    Product product = null;
    ProductSummary summary = null;
    BigDecimal latitude = new BigDecimal("0.0");
    BigDecimal longitude = new BigDecimal("0.0");

    module.setPlacesService(service);

    // Product has no title and no coordinates, do not generate title
    product = new Product(id);
    summary = module.getProductSummary(product);
    Assert.assertNull(summary.getProperties().get("title"));

    // Product has title, but no coordinates, do not generate title
    product = new Product(id);
    product.getProperties().put("title", defaultTitle);
    summary = module.getProductSummary(product);
    Assert.assertEquals(defaultTitle, summary.getProperties().get("title"));

    // Product has title and coordinates, do not generate title
    product = new Product(id);
    product.getProperties().put("title", defaultTitle);
    summary = module.getProductSummary(product);
    Assert.assertEquals(defaultTitle, summary.getProperties().get("title"));

    // Product has no title, but does have coordinates, generate title
    product = new Product(id);
    product.setLatitude(latitude);
    product.setLongitude(longitude);
    summary = module.getProductSummary(product);
    Assert.assertEquals(service.getEventTitle(latitude, longitude), summary.getProperties().get("title"));
  }

  protected class DummyPlacesService implements GeoservePlaces {
    @Override
    public String getEventTitle(BigDecimal latitude, BigDecimal longitude) throws IOException, MalformedURLException {
      return String.format("Dummy Response: %f, %f", latitude.doubleValue(), longitude.doubleValue());
    }
  }
}
