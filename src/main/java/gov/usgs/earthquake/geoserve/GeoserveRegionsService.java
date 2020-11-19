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

public class GeoserveRegionsService {
  /** Default URL for GeoServe Regions service. */
  public static final String DEFAULT_ENDPOINT_URL = "https://earthquake.usgs.gov/ws/geoserve/regions.json";
  public static final int DEFAULT_CONNECT_TIMEOUT = 300; // ms
  public static final int DEFAULT_READ_TIMEOUT = 1700; // ms

  /** Configured URL for GeoServe Regions service. */
  private String endpointUrl;
  private int connectTimeout;
  private int readTimeout;

  public GeoserveRegionsService() {
    this(DEFAULT_ENDPOINT_URL, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  public GeoserveRegionsService(final String endpointUrl) {
    this(endpointUrl, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  public GeoserveRegionsService(final int connectTimeout, final int readTimeout) {
    this(DEFAULT_ENDPOINT_URL, connectTimeout, readTimeout);
  }

  public GeoserveRegionsService(final String endpointUrl, final int connectTimeout, final int readTimeout) {
    this.setEndpointURL(endpointUrl);
    this.setConnectTimeout(connectTimeout);
    this.setReadTimeout(readTimeout);
  }

  public int getConnectTimeout() {
    return this.connectTimeout;
  }

  public String getEndpointURL() {
    return this.endpointUrl;
  }

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

  public String getFeRegionName(BigDecimal latitude, BigDecimal longitude) throws IOException, MalformedURLException {
    JsonObject region = this.getFeRegion(latitude, longitude);
    String feRegionName = region.getJsonArray("features")
        .getJsonObject(0)
        .getJsonObject("properties")
        .getJsonString("name")
        .getString();

    return feRegionName;
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

  // as needed, implement full GeoServe places API options
}