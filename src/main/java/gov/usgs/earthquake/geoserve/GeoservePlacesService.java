package gov.usgs.earthquake.geoserve;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import gov.usgs.util.StreamUtils;

public class GeoservePlacesService {
  /** Default URL for GeoServe Places service. */
  public static final String DEFAULT_GEOSERVE_PLACES_URL = "https://earthquake.usgs.gov/ws/geoserve/places.json";

  /** Configured URL for GeoServe Places service. */
  private String endpointUrl;

  public GeoservePlacesService() {
    this(DEFAULT_GEOSERVE_PLACES_URL);
  }

  public GeoservePlacesService(final String endpointUrl) {
    this.endpointUrl = endpointUrl;
  }

  public String getEndpointURL() {
    return this.endpointUrl;
  }

  public void setEndpointURL(final String endpointUrl) {
    this.endpointUrl = endpointUrl;
  }

  public JsonObject getEventPlaces(BigDecimal latitude, BigDecimal longitude)
      throws IOException, MalformedURLException {
    final URL url = new URL(String.format("%s?type=event&latitude=%s&longitude=%s", this.endpointUrl,
        URLEncoder.encode(latitude.toString(), "UTF-8"), URLEncoder.encode(longitude.toString(), "UTF-8")));

    try (InputStream in = StreamUtils.getURLInputStream(url, 250, 250)) {
      JsonReader reader = Json.createReader(in);
      JsonObject json = reader.readObject();
      reader.close();
      return json.getJsonObject("event");
    }
  }

  public String getEventTitle(BigDecimal latitude, BigDecimal longitude) throws IOException, MalformedURLException {
    JsonObject places = this.getEventPlaces(latitude, longitude);
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

    return String.format("%d km %s of %s, %s", distance, direction, name, admin);

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

  // TODO as needed, implement full GeoServe places API options
}