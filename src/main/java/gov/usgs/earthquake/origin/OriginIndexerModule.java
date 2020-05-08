package gov.usgs.earthquake.origin;

import java.math.BigDecimal;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.geoserve.GeoservePlaces;
import gov.usgs.earthquake.geoserve.GeoservePlacesService;
import gov.usgs.earthquake.indexer.DefaultIndexerModule;
import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;

import gov.usgs.util.Config;

/**
 * Class for summarizing "origin" type products during the indexing process.
 * Specifically this implementation uses a GeoservePlacesService to augment the
 * properties on the product to include a "title" property if one is not already
 * present.
 *
 * This module may be configured with the following properties: `endpointUrl`
 * `connectTimeout`, and `readTimeout`.
 */
public class OriginIndexerModule extends DefaultIndexerModule {
  private static final Logger LOGGER = Logger.getLogger(OriginIndexerModule.class.getName());

  private GeoservePlaces geoservePlaces;

  public static final String ENDPOINT_URL_PROPERTY = "endpointUrl";
  public static final String CONNECT_TIMEOUT_PROPERTY = "connectTimeout";
  public static final String READ_TIMEOUT_PROPERTY = "readTimeout";

  public OriginIndexerModule() {
    // Do nothing, must be configured through bootstrapping before use
  }

  public OriginIndexerModule(final GeoservePlacesService geoservePlaces) {
    this.setPlacesService(geoservePlaces);
  }

  /**
   * @return The places service currently being used for title generation
   */
  public GeoservePlaces getPlacesService() {
    return this.geoservePlaces;
  }

  @Override
  public ProductSummary getProductSummary(Product product) throws Exception {
    ProductSummary summary = super.getProductSummary(product);
    BigDecimal latitude = summary.getEventLatitude();
    BigDecimal longitude = summary.getEventLongitude();

    // Defer to existing title property if set...
    Map<String, String> summaryProperties = summary.getProperties();
    String title = summaryProperties.get("title");

    if (title == null && latitude != null && longitude != null) {
      try {
        title = this.geoservePlaces.getEventTitle(latitude, longitude);
        summaryProperties.put("title", title);
      } catch (Exception ex) {
        LOGGER
            .fine(String.format("[%s] %s for product %s", this.getName(), ex.getMessage(), product.getId().toString()));
        // Do nothing, value-added failed. Move on.
      }
    }

    return summary;
  }

  @Override
  public int getSupportLevel(Product product) {
    int supportLevel = IndexerModule.LEVEL_UNSUPPORTED;
    String type = getBaseProductType(product.getId().getType());

    if ("origin".equals(type) && !"DELETE".equalsIgnoreCase(product.getStatus())) {
      supportLevel = IndexerModule.LEVEL_SUPPORTED;
    }

    return supportLevel;
  }

  /**
   * Set the geoservePlaces to be used for subsequent calls to GeoServe places
   * endpoint.
   *
   * @param geoservePlaces The GeoservePlaces to use
   */
  public void setPlacesService(GeoservePlaces geoservePlaces) {
    this.geoservePlaces = geoservePlaces;
  }

  @Override
  public void configure(Config config) throws Exception {
    String endpointUrl = config.getProperty(ENDPOINT_URL_PROPERTY, GeoservePlacesService.DEFAULT_ENDPOINT_URL);
    int connectTimeout = Integer.parseInt(
        config.getProperty(CONNECT_TIMEOUT_PROPERTY, Integer.toString(GeoservePlacesService.DEFAULT_CONNECT_TIMEOUT)));
    int readTimeout = Integer.parseInt(
        config.getProperty(READ_TIMEOUT_PROPERTY, Integer.toString(GeoservePlacesService.DEFAULT_READ_TIMEOUT)));

    LOGGER.config(String.format("[%s] GeoservePlacesService(%s, %d, %d)", this.getName(), endpointUrl, connectTimeout,
        readTimeout));
    this.setPlacesService(new GeoservePlacesService(endpointUrl, connectTimeout, readTimeout));
  }
}