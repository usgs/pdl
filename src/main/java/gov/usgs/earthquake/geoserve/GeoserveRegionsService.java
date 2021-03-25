package gov.usgs.earthquake.geoserve;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import gov.usgs.util.StreamUtils;

/**
 * Access regions from the Geoserve regions service.
 */
public class GeoserveRegionsService {
  /** Default URL for GeoServe Regions service. */
  public static final String DEFAULT_ENDPOINT_URL = "https://earthquake.usgs.gov/ws/geoserve/regions.json";
  /** Default connection timeout */
  public static final int DEFAULT_CONNECT_TIMEOUT = 300; // ms
  /** Default read timeout */
  public static final int DEFAULT_READ_TIMEOUT = 1700; // ms

  /** Configured URL for GeoServe Regions service. */
  private String endpointUrl;
  private int connectTimeout;
  private int readTimeout;

  /** Default constructor */
  public GeoserveRegionsService() {
    this(DEFAULT_ENDPOINT_URL, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  /**
   * Constructor taking in endpointURL
   * @param endpointUrl for places service
   */
  public GeoserveRegionsService(final String endpointUrl) {
    this(endpointUrl, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  /**
   * Constructor taking in timeouts and using default endpoint URL
   * @param connectTimeout in ms
   * @param readTimeout in ms
   */
  public GeoserveRegionsService(final int connectTimeout, final int readTimeout) {
    this(DEFAULT_ENDPOINT_URL, connectTimeout, readTimeout);
  }

  /**
   * Custom constructor
   * @param endpointUrl for Places service
   * @param connectTimeout in ms
   * @param readTimeout in ms
   */
  public GeoserveRegionsService(final String endpointUrl, final int connectTimeout, final int readTimeout) {
    this.setEndpointURL(endpointUrl);
    this.setConnectTimeout(connectTimeout);
    this.setReadTimeout(readTimeout);
  }

  /** @return connectTimemout */
  public int getConnectTimeout() {
    return this.connectTimeout;
  }

  /** @return endpointURL */
  public String getEndpointURL() {
    return this.endpointUrl;
  }

  /**
   * Find an event in the Region service via a latitude and longitude
   * @param latitude of event
   * @param longitude of event
   * @return JSONObject of Fe Region
   * @throws IOException on IO error
   * @throws MalformedURLException or URL error
   */
  public JsonObject getFeRegion(BigDecimal latitude, BigDecimal longitude)
      throws IOException, MalformedURLException {
    final URL url = new URL(this.endpointUrl +
        "?type=fe" +
        "&latitude=" + URLEncoder.encode(latitude.toString(), StandardCharsets.UTF_8.toString()) +
        "&longitude=" + URLEncoder.encode(longitude.toString(), StandardCharsets.UTF_8.toString())
    );

    try (InputStream in = StreamUtils.getURLInputStream(url, this.connectTimeout, this.readTimeout)) {
      JsonReader reader = Json.createReader(in);
      JsonObject json = reader.readObject();
      reader.close();
      return json.getJsonObject("fe");
    }
  }

  /**
   * Get name of FeRegion
   * @param latitude of event
   * @param longitude of event
   * @return string of FeRegion name
   * @throws IOException on IO error
   * @throws MalformedURLException or URL error
   */
  public String getFeRegionName(BigDecimal latitude, BigDecimal longitude) throws IOException, MalformedURLException {
    JsonObject region = this.getFeRegion(latitude, longitude);
    String feRegionName = region.getJsonArray("features")
        .getJsonObject(0)
        .getJsonObject("properties")
        .getJsonString("name")
        .getString();

    return feRegionName;
  }

  /** @return readTimeout */
  public int getReadTimeout() {
    return this.readTimeout;
  }

  /** @param connectTimeout int to set */
  public void setConnectTimeout(final int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  /** @param endpointUrl string to set */
  public void setEndpointURL(final String endpointUrl) {
    this.endpointUrl = endpointUrl;
  }

  /** @param readTimeout int to set */
  public void setReadTimeout(final int readTimeout) {
    this.readTimeout = readTimeout;
  }

  // as needed, implement full GeoServe places API options
}