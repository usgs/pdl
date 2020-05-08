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

public class GeoservePlacesService implements GeoservePlaces {
  /** Default URL for GeoServe Places service. */
  public static final String DEFAULT_ENDPOINT_URL = "https://earthquake.usgs.gov/ws/geoserve/places.json";
  public static final int DEFAULT_CONNECT_TIMEOUT = 300; // ms
  public static final int DEFAULT_READ_TIMEOUT = 1700; // ms

  /** Configured URL for GeoServe Places service. */
  private String endpointUrl;
  private int connectTimeout;
  private int readTimeout;

  public GeoservePlacesService() {
    this(DEFAULT_ENDPOINT_URL, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  public GeoservePlacesService(final String endpointUrl) {
    this(endpointUrl, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  public GeoservePlacesService(final int connectTimeout, final int readTimeout) {
    this(DEFAULT_ENDPOINT_URL, connectTimeout, readTimeout);
  }

  public GeoservePlacesService(final String endpointUrl, final int connectTimeout, final int readTimeout) {
    this.endpointUrl = endpointUrl;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
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

  public int getConnectTimeout() {
    return this.connectTimeout;
  }

  public String getEndpointURL() {
    return this.endpointUrl;
  }

  public JsonObject getEventPlaces(BigDecimal latitude, BigDecimal longitude)
      throws IOException, MalformedURLException {
    final URL url = new URL(String.format("%s?type=event&latitude=%s&longitude=%s", this.endpointUrl,
        URLEncoder.encode(latitude.toString(), "UTF-8"), URLEncoder.encode(longitude.toString(), "UTF-8")));

    try (InputStream in = StreamUtils.getURLInputStream(url, this.connectTimeout, this.readTimeout)) {
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

    return this.formatEventTitle(feature);
  }

  public int getReadTimeout() {
    return this.readTimeout;
  }

  public void setConnectTimeout(final int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public void setEndpointURL(final String endpointUrl) {
    this.endpointUrl = endpointUrl;
  }

  public void setReadTimeout(final int readTimeout) {
    this.readTimeout = readTimeout;
  }

  // TODO as needed, implement full GeoServe places API options
}