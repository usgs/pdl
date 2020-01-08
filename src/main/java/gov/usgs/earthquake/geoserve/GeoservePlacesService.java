package gov.usgs.earthquake.geoserve;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.json.Json;
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

    try (InputStream in = StreamUtils.getInputStream(url)) {
      JsonReader reader = Json.createReader(in);
      JsonObject json = reader.readObject();
      reader.close();
      return json.getJsonObject("event");
    }
  }

  // TODO as needed, implement full GeoServe places API options
}