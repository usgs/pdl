package gov.usgs.earthquake.origin;

import java.math.BigDecimal;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;

import gov.usgs.earthquake.geoserve.GeoservePlacesService;
import gov.usgs.earthquake.indexer.DefaultIndexerModule;
import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;

public class OriginIndexerModule extends DefaultIndexerModule {
  private static final Logger LOGGER = Logger.getLogger(OriginIndexerModule.class.getName());

  private GeoservePlacesService geoservePlaces;

  public OriginIndexerModule() {
    this.geoservePlaces = new GeoservePlacesService();
  }

  @Override
  public int getSupportLevel(Product product) {
    int supportLevel = IndexerModule.LEVEL_UNSUPPORTED;
    String type = getBaseProductType(product.getId().getType());

    if ("origin".equals(type) && "update".equalsIgnoreCase(product.getStatus())) {
      supportLevel = IndexerModule.LEVEL_SUPPORTED;
    }

    return supportLevel;
  }

  // @Override
  public ProductSummary getProductSummary(Product product) throws Exception {
    ProductSummary summary = super.getProductSummary(product);

    // Defer to existing title property if set...
    Map<String, String> summaryProperties = summary.getProperties();
    String title = summaryProperties.get("title");

    if (title == null) {
      BigDecimal latitude = summary.getEventLatitude();
      BigDecimal longitude = summary.getEventLongitude();

      JsonObject places = this.geoservePlaces.getEventPlaces(latitude, longitude);
      JsonArray features = places.getJsonArray("features");
      JsonObject feature = features.get(0).asJsonObject();
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

      title = String.format("%d km %s of %s, %s", distance, direction, name, admin);
      summaryProperties.put("title", title);
      // summaryProperties.put("region", title);
    }

    return summary;
  }

  /**
   * Converts a decimal degree azimuth to a canonical compass direction
   *
   * @param {Number} azimuth The degrees azimuth to be converted
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
    azimuth = Math.abs(azimuth);
    while (azimuth < 0.0) {
      azimuth = azimuth + 360.0;
    }

    return directions[(int) Math.round((azimuth % 360.0) / fullwind)];
  }

}