package gov.usgs.earthquake.origin;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonObject;

import gov.usgs.earthquake.geoserve.GeoservePlacesService;
import gov.usgs.earthquake.geoserve.GeoserveRegionsService;
import gov.usgs.earthquake.indexer.DefaultIndexerModule;
import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;

import gov.usgs.util.Config;
import gov.usgs.util.StringUtils;

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

  private GeoservePlacesService geoservePlaces;
  private GeoserveRegionsService geoserveRegions;


  /** Property for places endpoint url */
  public static final String PLACES_ENDPOINT_URL_PROPERTY = "placesEndpointUrl";
  /** property for regions endpoint url */
  public static final String REGIONS_ENDPOINT_URL_PROPERTY = "regionsEndpointUrl";
  /** property for connectTimeout */
  public static final String CONNECT_TIMEOUT_PROPERTY = "connectTimeout";
  /** Properties for readTimeout */
  public static final String READ_TIMEOUT_PROPERTY = "readTimeout";

  /** Property for Geoserve distance threshold */
  public static final String GEOSERVE_DISTANCE_THRESHOLD_PROPERTY = "geoserveDistanceThreshold";

  /**
   * Distance threshold (in km), determines whether to use fe region
   * or nearest place in the event title
   */
  public static final int DEFAULT_GEOSERVE_DISTANCE_THRESHOLD = 300;

  private int distanceThreshold;

  /**
   * Empty constructor
   * Do nothing, must be configured through bootstrapping before use
   */
  public OriginIndexerModule() {
  }

  /**
   * Constructor
   * @param geoservePlaces GeoservePlacesService
   * @param geoserveRegions GeoserveRegionsService
   */
  public OriginIndexerModule(
      final GeoservePlacesService geoservePlaces,
      final GeoserveRegionsService geoserveRegions
  ) {
    this.setPlacesService(geoservePlaces);
    this.setRegionsService(geoserveRegions);
  }

  /**
   * @return The places service currently being used to return nearby places
   */
  public GeoservePlacesService getPlacesService() {
    return this.geoservePlaces;
  }

  /**
   * @return The regions service currently being used to return fe regions
   */
  public GeoserveRegionsService getRegionsService() {
    return this.geoserveRegions;
  }

  /**
   * @return The distance threshold currently being used to default to FE region
   */
  public int getDistanceThreshold() {
    return this.distanceThreshold;
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
        title = this.getEventTitle(latitude, longitude);
        summaryProperties.put("title", StringUtils.encodeAsUtf8(title));
      } catch (Exception ex) {
        LOGGER
            .warning(String.format("[%s] %s for product %s", this.getName(), ex.getMessage(), product.getId().toString()));
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
   * Set the GeoservePlacesService to be used for subsequent calls to GeoServe places
   * endpoint.
   *
   * @param geoservePlaces The GeoservePlacesService to use
   */
  public void setPlacesService(GeoservePlacesService geoservePlaces) {
    this.geoservePlaces = geoservePlaces;
  }

  /**
   * Set the geoserveRegions to be used for subsequent calls to GeoServe regions
   * endpoint.
   *
   * @param geoserveRegions The GeoserveRegions to use
   */
  public void setRegionsService(GeoserveRegionsService geoserveRegions) {
    this.geoserveRegions = geoserveRegions;
  }

  /**
   * Set the distance threshold to prefer fe region over nearst place
   * in the event title
   *
   * @param threshold The distance threshold to use
   */
  public void setDistanceThreshold(int threshold) {
    this.distanceThreshold = threshold;
  }

  @Override
  public void configure(Config config) throws Exception {
    // Distance threshold (in km)
    this.distanceThreshold = Integer.parseInt(
        config.getProperty(
            GEOSERVE_DISTANCE_THRESHOLD_PROPERTY,
            Integer.toString(DEFAULT_GEOSERVE_DISTANCE_THRESHOLD)
        )
    );

    // Geoserve Places Endpoint configuration
    String placesEndpointUrl = config.getProperty(
        PLACES_ENDPOINT_URL_PROPERTY,
        GeoservePlacesService.DEFAULT_ENDPOINT_URL
    );
    int placesEndpointConnectTimeout = Integer.parseInt(
        config.getProperty(
            CONNECT_TIMEOUT_PROPERTY,
            Integer.toString(GeoservePlacesService.DEFAULT_CONNECT_TIMEOUT)
        )
    );
    int placesEndpointReadTimeout = Integer.parseInt(
        config.getProperty(
            READ_TIMEOUT_PROPERTY,
            Integer.toString(GeoservePlacesService.DEFAULT_READ_TIMEOUT)
        )
    );
    LOGGER.config(
        String.format("[%s] GeoservePlacesService(%s, %d, %d)",
          this.getName(),
          placesEndpointUrl,
          placesEndpointConnectTimeout,
          placesEndpointReadTimeout
        )
    );
    this.setPlacesService(
        new GeoservePlacesService(
          placesEndpointUrl,
          placesEndpointConnectTimeout,
          placesEndpointReadTimeout
        )
    );

    // Geoserve Regions Endpoint configuration
    String regionsEndpointUrl = config.getProperty(
        REGIONS_ENDPOINT_URL_PROPERTY,
        GeoserveRegionsService.DEFAULT_ENDPOINT_URL
    );
    int regionsEndpointConnectTimeout = Integer.parseInt(
        config.getProperty(
            CONNECT_TIMEOUT_PROPERTY,
            Integer.toString(GeoserveRegionsService.DEFAULT_CONNECT_TIMEOUT)
        )
    );
    int regionsEndpointReadTimeout = Integer.parseInt(
        config.getProperty(
            READ_TIMEOUT_PROPERTY,
            Integer.toString(GeoserveRegionsService.DEFAULT_READ_TIMEOUT)
        )
    );
    LOGGER.config(
        String.format("[%s] GeoserveRegionsService(%s, %d, %d)",
            this.getName(),
            regionsEndpointUrl,
            regionsEndpointConnectTimeout,
            regionsEndpointReadTimeout
        )
    );
    this.setRegionsService(
        new GeoserveRegionsService(
            regionsEndpointUrl,
            regionsEndpointConnectTimeout,
            regionsEndpointReadTimeout
        )
    );
  }

  /**
   * Get the event title based on the name and location of the nearest
   * place, or if the nearest place is outside of the distance threshold
   * return the fe region name
   *
   * @param latitude event latitude in degrees
   * @param longitude event longitude in degrees
   *
   * @return {String} event name
   *
   * @throws IOException if IO error occurs
   */
  public String getEventTitle(BigDecimal latitude, BigDecimal longitude) throws Exception, IOException {
    StringBuffer messages = new StringBuffer();
    String message = null;

    try {
      final JsonObject feature = this.geoservePlaces.getNearestPlace(
          latitude,
          longitude,
          new BigInteger(String.valueOf(this.distanceThreshold))
        );

      return this.formatEventTitle(feature);
    } catch (IndexOutOfBoundsException ibx) {
      message = "Places service returned no places within distance threshold";
      messages.append(message + ". ");
      LOGGER.log(Level.INFO, "[" + this.getName() + "] " + message);
    } catch (Exception e) {
      message = "Failed to get nearest place from geoserve places service";
      messages.append(message + ". ");
      messages.append(e.getMessage() + ". ");
      LOGGER.log(Level.INFO, "[" + this.getName() + "] " + message);
    }

    try {
      return this.geoserveRegions.getFeRegionName(latitude, longitude);
    } catch (Exception e) {
      message = "Failed to get FE region name";
      messages.append(message + ". ");
      messages.append(e.getMessage() + ". ");
      LOGGER.log(Level.INFO, "[" + this.getName() + "] .");
    }

    // If we get this far, things failed spectacularly, report the error
    Exception e = new Exception(messages.toString());
    e.fillInStackTrace();
    throw e;
  }

  /**
   * Takes properties from feature and formats them into a string
   * @param feature feature to format
   * @return string with distance, direction, name, and admin
   */
  public String formatEventTitle(JsonObject feature) {
    JsonObject properties = feature.getJsonObject("properties");

    String name = properties.getString("name");
    String country = properties.getString("country_code").toLowerCase();
    String admin = properties.getString("country_name");
    int distance = properties.getInt("distance");
    double azimuth = properties.getJsonNumber("azimuth").doubleValue();
    String direction = azimuthToDirection(azimuth);

    if ("us".equals(country)) {
      admin = properties.getString("admin1_name");
    }

    return String.format("%d km %s of %s, %s", distance, direction, name, admin);
  }

  /**
   * Converts a decimal degree azimuth to a canonical compass direction
   *
   * @param azimuth The degrees azimuth to be converted
   *
   * @return {String} The canonical compass direction for the given input azimuth
   */
  public String azimuthToDirection(double azimuth) {
    double fullwind = 22.5;
    String[] directions = { "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW",
        "NNW", "N" };

    // Invert azimuth for proper directivity
    // Maybe not needed in the future.
    azimuth += 180.0;

    // adjust azimuth if negative
    while (azimuth < 0.0) {
      azimuth = azimuth + 360.0;
    }

    return directions[(int) Math.round((azimuth % 360.0) / fullwind)];
  }

}
