package gov.usgs.earthquake.aws;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonObject;

import gov.usgs.util.StreamUtils;

/**
 * Utility class to hold HttpURLConnection and parse JSON response data.
 */
public class HttpResponse {
  public final HttpURLConnection connection;
  public final byte[] response;

  /**
   * Reads response from HttpUrlConnection.
   */
  public HttpResponse(final HttpURLConnection connection) throws Exception {
    this.connection = connection;
    try (final InputStream in = connection.getInputStream()) {
      byte[] response = StreamUtils.readStream(in);
      this.response = response;
    }
  }

  /**
   * Parse response into JsonObject.
   *
   * @return parsed JsonObject
   * @throws Exception if unable to parse.
   */
  public JsonObject getJsonObject() throws Exception {
    return Json.createReader(new ByteArrayInputStream(response)).readObject();
  }
}