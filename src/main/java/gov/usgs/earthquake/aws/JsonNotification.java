package gov.usgs.earthquake.aws;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.json.JsonObject;

import gov.usgs.earthquake.distribution.URLNotification;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.JsonProduct;
import gov.usgs.util.protocolhandlers.data.Handler;

/**
 * Json formatted notification.
 *
 * Stores stores product content in URLNotification as a data URL.
 */
public class JsonNotification extends URLNotification {

  /** Empty URL for product tracker. */
  public static final URL EMPTY_URL;
  static {
    try {
      // make sure data protocol handler is registered
      Handler.register();
      EMPTY_URL = new URL("data:,");
    } catch (MalformedURLException mue) {
      throw new RuntimeException("failed to parse empty url");
    }
  }

  /** When notification was created. */
  public final Instant created;
  /** Product attached to notification. */
  public final Product product;

  /**
   * Parse a Json formatted Notification.
   *
   * @param json
   * @throws Exception
   */
  JsonNotification(final JsonObject json) throws Exception {
    this(
        Instant.parse(json.getString("created")),
        new JsonProduct().getProduct(json.getJsonObject("product")));
  }

  /**
   * Create a JsonNotification from an existing Product.
   */
  JsonNotification(final Instant created, final Product product) throws Exception {
    super(
        product.getId(),
        // expiration date
        new Date(created.plusSeconds(30 * 86400).toEpochMilli()),
        // no tracker
        EMPTY_URL,
        // store product as data url
        new URL("data:;base64," +
            new String(Base64.getEncoder().encode(
                new JsonProduct().getJsonObject((product))
                    .toString().getBytes(StandardCharsets.UTF_8)))));
    this.created = created;
    this.product = product;
  }
}
