package gov.usgs.earthquake.geoserve;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import gov.usgs.util.StreamUtils;

/**
 * Access places from the Geoserve Places service.
 */
public class GeoservePlacesService {
  /** Default URL for GeoServe Places service. */
  public static final String DEFAULT_ENDPOINT_URL = "https://earthquake.usgs.gov/ws/geoserve/places.json";
  /** Default connection timeout */
  public static final int DEFAULT_CONNECT_TIMEOUT = 300; // ms
  /** Default read timeout */
  public static final int DEFAULT_READ_TIMEOUT = 1700; // ms

  /** Configured URL for GeoServe Places service. */
  private String endpointUrl;
  private int connectTimeout;
  private int readTimeout;

  /** Default constructor */
  public GeoservePlacesService() {
    this(DEFAULT_ENDPOINT_URL, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  /**
   * Constructor taking in endpointURL
   * @param endpointUrl for places service
   */
  public GeoservePlacesService(final String endpointUrl) {
    this(endpointUrl, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  /**
   * Constructor taking in timeouts and using default endpoint URL
   * @param connectTimeout in ms
   * @param readTimeout in ms
   */
  public GeoservePlacesService(final int connectTimeout, final int readTimeout) {
    this(DEFAULT_ENDPOINT_URL, connectTimeout, readTimeout);
  }

  /**
   * Custom constructor
   * @param endpointUrl for Places service
   * @param connectTimeout in ms
   * @param readTimeout in ms
   */
  public GeoservePlacesService(final String endpointUrl, final int connectTimeout, final int readTimeout) {
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
   * Find an event in the Places service via a latitude and longitude
   * @param latitude of event
   * @param longitude of event
   * @return JSONObject of event
   * @throws IOException on IO error
   * @throws MalformedURLException or URL error
   */
  public JsonObject getEventPlaces(BigDecimal latitude, BigDecimal longitude)
      throws IOException, MalformedURLException {
    final URL url = new URL(this.endpointUrl +
        "?type=event" +
        "&latitude=" + URLEncoder.encode(latitude.toString(), StandardCharsets.UTF_8.toString()) +
        "&longitude=" + URLEncoder.encode(longitude.toString(), StandardCharsets.UTF_8.toString())
    );

    try (InputStream in = StreamUtils.getURLInputStream(url, this.connectTimeout, this.readTimeout)) {
      JsonReader reader = Json.createReader(in);
      JsonObject json = reader.readObject();
      reader.close();
      return json.getJsonObject("event");
    }
  }


  /**
   * Get nearest place to a latitude and longitude
   * @param latitude of place
   * @param longitude of place
   * @return JSONObject of place
   * @throws IndexOutOfBoundsException on no places returned
   * @throws IOException on IO error
   * @throws MalformedURLException on URL error
   * @deprecated
   */
  public JsonObject getNearestPlace(BigDecimal latitude, BigDecimal longitude)
      throws IndexOutOfBoundsException, IOException, MalformedURLException {
    return this.getNearestPlace(latitude, longitude, null);
  }

  /**
   * Get nearest place to a latitude and longitude
   * @param latitude of place
   * @param longitude of place
   * @param maxradiuskm around place
   * @return JSONObject of place
   * @throws IndexOutOfBoundsException on no places returned
   * @throws IOException on IO error
   * @throws MalformedURLException on URL error
   */
  public JsonObject getNearestPlace(BigDecimal latitude, BigDecimal longitude,
      BigInteger maxradiuskm) throws IndexOutOfBoundsException, IOException, MalformedURLException {
    // JsonObject places = this.getEventPlaces(latitude, longitude);
    // JsonObject feature = places.getJsonArray("features").getJsonObject(0);
    if (maxradiuskm == null) {
      maxradiuskm = new BigInteger("300");
    }

    final URL url = new URL(this.endpointUrl +
       "?type=geonames" +
       "&latitude=" + URLEncoder.encode(latitude.toString(), StandardCharsets.UTF_8.toString()) +
       "&longitude=" + URLEncoder.encode(longitude.toString(), StandardCharsets.UTF_8.toString()) +
       "&maxradiuskm=" + URLEncoder.encode(maxradiuskm.toString(), StandardCharsets.UTF_8.toString()) +
       "&limit=1"
    );

    try (
      InputStream in = StreamUtils.getURLInputStream(url, this.connectTimeout, this.readTimeout);
      JsonReader reader = Json.createReader(in)
    ) {
      JsonObject json = reader.readObject();
      JsonObject places = json.getJsonObject("event");
      return places.getJsonArray("features").getJsonObject(0);
    }
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
