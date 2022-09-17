package gov.usgs.earthquake.origin;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.earthquake.geoserve.GeoserveRegionsService;
import gov.usgs.earthquake.geoserve.GeoservePlacesService;
import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;

public class OriginIndexerModuleTest {

  private OriginIndexerModule module = null;
  private Double distance = 0.0;
  private String feRegionName = "FE Region Name";

  @Before
  public void setUpTestEnvironment() throws Exception {
    GeoservePlacesService geoservePlacesService = new DummyPlacesService();
    GeoserveRegionsService geoserveRegionsService = new DummyRegionsService();
    module = new OriginIndexerModule(geoservePlacesService, geoserveRegionsService);

    // set distance threshold
    module.setDistanceThreshold(300);
  }

  @Test
  public void getSupportLevelTest() throws Exception {
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
  public void getProductSummaryTest() throws Exception {
    GeoservePlacesService service = new DummyPlacesService();
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

    Assert.assertEquals(module.getEventTitle(latitude, longitude), summary.getProperties().get("title"));
  }

  @Test
  public void formatEventTitleTest() {
    int distance = 0;
    double azimuth = 0.0;

    // OUS location
    String expectation = "0 km S of name, country_name";
    JsonObject feature = Json.createObjectBuilder()
        .add("properties",
            Json.createObjectBuilder()
                .add("admin1_name", "admin1_name")
                .add("azimuth", azimuth)
                .add("country_code", "country_code")
                .add("country_name", "country_name")
                .add("distance", distance)
                .add("name", "name"))
        .build();
    Assert.assertEquals(expectation, module.formatEventTitle(feature));

    // US location
    expectation = "0 km S of name, admin1_name";
    feature = Json.createObjectBuilder()
        .add("properties",
            Json.createObjectBuilder()
                .add("admin1_name", "admin1_name")
                .add("azimuth", azimuth)
                .add("country_code", "us")
                .add("country_name", "country_name")
                .add("distance", distance)
                .add("name", "name"))
        .build();
    Assert.assertEquals(expectation, module.formatEventTitle(feature));
  }


  @Test
  public void getEventTitleTest() throws Exception, IOException {
    BigDecimal latitude = new BigDecimal("0.0");
    BigDecimal longitude = new BigDecimal("0.0");
    String title = "";

    // Nearby Places Title
    this.distance = 299.0;
    title = module.getEventTitle(latitude, longitude);
    Assert.assertEquals("299 km WSW of name, country_name", title);

    // FE Region Title
    this.distance = 301.0;
    title = module.getEventTitle(latitude, longitude);
    Assert.assertEquals(feRegionName, title);
  }

  @Test
  public void azimuthToDirectionTest() {
    Assert.assertEquals("S", module.azimuthToDirection(0));
    Assert.assertEquals("W", module.azimuthToDirection(90));
    Assert.assertEquals("N", module.azimuthToDirection(180));
    Assert.assertEquals("E", module.azimuthToDirection(270));

    Assert.assertEquals("S", module.azimuthToDirection(-0));
    Assert.assertEquals("E", module.azimuthToDirection(-90));
    Assert.assertEquals("N", module.azimuthToDirection(-180));
    Assert.assertEquals("W", module.azimuthToDirection(-270));
  }

  protected class DummyPlacesService extends GeoservePlacesService {
    @Override
    public JsonObject getNearestPlace(BigDecimal latitude, BigDecimal longitude, int maxradiuskm)
        throws IOException, MalformedURLException {
      if (distance <= maxradiuskm) {
        return Json.createObjectBuilder().add("properties",
            Json.createObjectBuilder()
                .add("admin1_name", "admin1_name")
                .add("azimuth", 60.1)
                .add("country_code", "country_code")
                .add("country_name", "country_name")
                .add("distance", distance)
                .add("name", "name"))
            .build();
      } else {
        return null;
      }
    }
  }
  protected class DummyRegionsService extends GeoserveRegionsService {
    @Override
    public String getFeRegionName(BigDecimal latitude, BigDecimal longitude) throws IOException, MalformedURLException {
      return feRegionName;
    }
  }
}
