package gov.usgs.earthquake.geoserve;

import gov.usgs.util.StreamUtils;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Access places from the Geoserve Layers service.
 * Closely mirrors #{@link GeoserveLayersService}
 */
public class GeoservePlacesService {
  /** Default URL for GeoServe Places service. */
  public static final String DEFAULT_GEOSERVE_PLACES_URL = "https://earthquake.usgs.gov/ws/geoserve/places.json?{parameters}";

  /** Configured URL for GeoServe Places service. */
  private String endpointUrl;

  /**
   * Create a service using the default URL.
   */
  public GeoservePlacesService() {
    this(DEFAULT_GEOSERVE_PLACES_URL);
  }

  /**
   * Create a service using a custom URL.
   *
   * @param endpointUrl places service URL.
   *       Should contain the string <code>{parameters}</code>,
   *       which is replaced during the #{@link #getPlaces(String)}.
   */
  public GeoservePlacesService(final String endpointUrl) {
    this.endpointUrl = endpointUrl;
  }

  /**
   * Fetch and parse a JSON response from the Geoserve Places service.
   */
  public JsonObject getPlaces(String parameters) throws MalformedURLException, IOException {
    final URL url = new URL(endpointUrl.replace("{parameters}", parameters));
    try (InputStream in = StreamUtils.getInputStream(url)) {
      JsonObject json = Json.createReader(in).readObject();
      return json;
    }
  }
}
