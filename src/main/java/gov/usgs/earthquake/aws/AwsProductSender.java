package gov.usgs.earthquake.aws;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.distribution.InvalidSignatureException;
import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.URLContent;
import gov.usgs.earthquake.product.io.JsonProduct;
import gov.usgs.util.Config;
import gov.usgs.util.CryptoUtils;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PrivateKey;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;

/** Send using AWS Hub API. */
public class AwsProductSender extends DefaultConfigurable implements ProductSender {

  /** Initialzation of logger. For us later in file. */
  public static final Logger LOGGER = Logger.getLogger(AwsProductSender.class.getName());

  /** Base URL for Hub API. */
  public static final String HUB_URL_PROPERTY = "url";
  /** Private Key to sign products, if signProducts is true. */
  public static final String PRIVATE_KEY_PROPERTY = "privateKey";
  /** Whether to sign products using private key. */
  public static final String SIGN_PRODUCTS_PROPERTY = "signProducts";

  /**url where products are sent */
  protected URL hubUrl;
  /** signing key */
  protected PrivateKey privateKey;
  /** wheter to sign products */
  protected boolean signProducts = false;

  /** Connection timeout. 5s seems excessive, but be cautious for now */
  protected int connectTimeout = 5000;
  /** Server-side timeout. Called at getInputStream().read() */
  protected int readTimeout = 30000;

  /** Empty class constructor */
  public AwsProductSender() {}

  public AwsProductSender(URL url) {
    this.hubUrl = url;
  }

  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    hubUrl = new URL(config.getProperty(HUB_URL_PROPERTY));
    LOGGER.config("[" + getName() + "] url=" + hubUrl.toString());

    final String sign = config.getProperty(SIGN_PRODUCTS_PROPERTY);
    if (sign != null) {
      signProducts = Boolean.valueOf(sign);
    }
    LOGGER.config("[" + getName() + "] sign products=" + signProducts);

    final String key = config.getProperty(PRIVATE_KEY_PROPERTY);
    if (key != null) {
      privateKey = CryptoUtils.readOpenSSHPrivateKey(
          FileUtils.readFile(new File(key)),
          null);
      LOGGER.config("[" + getName() + "] private key=" + key);
    }

    if (signProducts && privateKey == null) {
      // no key configured
      throw new ConfigurationException("[" + getName() + "] " + SIGN_PRODUCTS_PROPERTY
          + " requires a private key for signing");
    }

  }

  /**
   * Send a product to the hub.
   */
  @Override
  public void sendProduct(final Product product) throws Exception {
    final ProductId id = product.getId();

    // re-sign if configured
    if (signProducts) {
      if (product.getSignature() != null) {
        // preserve original signature
        product.getProperties().put("original-signature", product.getSignature());
        product.getProperties().put("original-signature-version",
            product.getSignatureVersion().toString());
      }
      product.sign(privateKey, CryptoUtils.Version.SIGNATURE_V2);
    }
    // convert to json
    JsonObject json = new JsonProduct().getJsonObject(product);

    final long start = new Date().getTime();
    final long afterUploadContent;
    try {
      // upload contents
      if (
        // has contents
        product.getContents().size() > 0
        // and not only inline content
        && !(product.getContents().size() == 1 && product.getContents().get("") != null)
      ) {
        LOGGER.fine("Getting upload urls for " + json.toString());
        // get upload urls, response is product with signed content urls for upload
        Product uploadProduct;
        try {
          uploadProduct = getUploadUrls(json);
        } catch (HttpException e) {
          HttpURLConnection connection = e.response.connection;
          // check for server error
          if (connection.getResponseCode() >= 500) {
            LOGGER.log(Level.FINE,
                "[" + getName() + "] get upload urls exception, trying again", e);
            // try again after random back off (1-5 s)
            Thread.sleep(1000 + Math.round(4000 * Math.random()));
            uploadProduct = getUploadUrls(json);
          } else {
            // otherwise propagate exception as usual
            throw e;
          }
        }

        final long afterGetUploadUrls = new Date().getTime();
        LOGGER.fine("[" + getName() + "] get upload urls " + id.toString()
            + " (" + (afterGetUploadUrls - start) + " ms) ");

        // upload contents
        try {
          uploadContents(product, uploadProduct);
        } catch (HttpException e) {
          HttpURLConnection connection = e.response.connection;
          // check for S3 "503 Slow Down" error
          if (
            503 == connection.getResponseCode()
            && "Slow Down".equals(connection.getResponseMessage())
          ) {
            LOGGER.fine("[" + getName() + "] 503 slow down exception, trying again");
            // try again after random back off (1-5 s)
            Thread.sleep(1000 + Math.round(4000 * Math.random()));
            uploadContents(product, uploadProduct);
          } else {
            // otherwise propagate exception as usual
            throw e;
          }
        }

        afterUploadContent = new Date().getTime();
        LOGGER.fine("[" + getName() + "] upload contents " + id.toString()
            + " (" + (afterUploadContent - afterGetUploadUrls) + " ms) ");
      } else {
        afterUploadContent = new Date().getTime();
      }

      try {
        // send product
        sendProduct(json);
      } catch (HttpException e) {
        HttpURLConnection connection = e.response.connection;
        // check for server error
        if (connection.getResponseCode() >= 500) {
          LOGGER.log(Level.FINE,
              "[" + getName() + "] send product exception, trying again", e);
          // try again after random back off (1-5 s)
          Thread.sleep(1000 + Math.round(4000 * Math.random()));
          sendProduct(json);
        } else {
          // otherwise propagate exception as usual
          throw e;
        }
      }

      final long afterSendProduct = new Date().getTime();
      LOGGER.fine("[" + getName() + "] send product " + id.toString()
          + " (" + (afterSendProduct - afterUploadContent) + " ms) ");
    } catch (ProductAlreadySentException pase) {
      // hub already has product
      LOGGER.info("[" + getName() + "] hub already has product");
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Exception sending product " + id.toString(), e);
      throw e;
    } finally {
      final long end = new Date().getTime();
      LOGGER.info("[" + getName() + "] send product total " + id.toString()
          + " (" + (end - start) + " ms) ");
    }
  }

  /**
   * Get content upload urls.
   *
   * @param json product in json format.
   * @return product with content urls set to upload URLs.
   * @throws Exception Exception
   */
  protected Product getUploadUrls(final JsonObject json) throws Exception {
    final URL url = new URL(hubUrl, "get_upload_urls");
    final HttpResponse result = postProductJson(url, json);
    final int responseCode = result.connection.getResponseCode();

    // check for errors
    if (responseCode == 401) {
      throw new InvalidSignatureException("Invalid product signature");
    } else if (responseCode == 409) {
      throw new ProductAlreadySentException();
    } else if (responseCode != 200) {
      throw new HttpException(result, "Error getting upload urls");
    }

    // successful response is json object with "products" property
    // that is product with upload urls for contents.
    final JsonObject getUploadUrlsResponse = result.getJsonObject();
    final Product product = new JsonProduct().getProduct(
        getUploadUrlsResponse.getJsonObject("product"));
    return product;
  }

  /**
   * Post product json to a hub url.
   *
   * This is a HTTP POST method,
   * with a JSON content body with a "product" property with the product.
   *
   * @param url url of connection
   * @param product product in json format
   * @return new HTTP POST response
   * @throws Exception Exception
   */
  protected HttpResponse postProductJson(final URL url, final JsonObject product) throws Exception {
    // send as attribute, for extensibility
    final JsonObject json = Json.createObjectBuilder().add("product", product).build();
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout(connectTimeout);
    connection.setReadTimeout(readTimeout);
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    try (final OutputStream out = connection.getOutputStream()) {
      out.write(json.toString().getBytes());
    }
    return new HttpResponse(connection);
  }

  /**
   * Send product after content has been uploaded.
   *
   * @param json product in json format.
   * @return product with content urls pointing to hub.
   * @throws Exception Exception
   */
  protected Product sendProduct(final JsonObject json) throws Exception {
    // send request
    final URL url = new URL(hubUrl, "send_product");
    final HttpResponse result = postProductJson(url, json);
    int responseCode = result.connection.getResponseCode();

    // check for errors
    if (responseCode == 401) {
      throw new InvalidSignatureException("Invalid product signature");
    } else if (responseCode == 409) {
      throw new ProductAlreadySentException();
    } else if (responseCode == 422) {
      throw new HttpException(result,
          "Content validation errors: " + result.getJsonObject().toString());
    } else if (result.connection.getResponseCode() != 200) {
      throw new HttpException(result, "Error sending product");
    }

    // successful response is json object with "notification" property
    // that has "created" and "product" properties with hub urls for contents.
    final JsonObject sendProductResponse = result.getJsonObject();
    final JsonObject notification = sendProductResponse.getJsonObject("notification");
    final Product product = new JsonProduct().getProduct(notification.getJsonObject("product"));
    // json response also has "notification_id" property of broadcast that was sent.
    String notificationId = null;
    if (!sendProductResponse.isNull("notification_id")) {
      notificationId = sendProductResponse.getString("notification_id");
    }
    LOGGER.fine("[" + getName() + "] notification id "
        + notificationId + " " + product.getId().toString());
    return product;
  }

  /**
   * Upload content to a signed url.
   *
   * @param path content path.
   * @param content content to upload.
   * @param signedUrl url where content should be uploaded.
   * @return HTTP result
   * @throws Exception Exception
   */
  protected HttpResponse uploadContent(final String path, final Content content, final URL signedUrl)
      throws Exception {
    final long start = new Date().getTime();
    final HttpURLConnection connection = (HttpURLConnection) signedUrl.openConnection();
    connection.setDoOutput(true);
    connection.setConnectTimeout(connectTimeout);
    connection.setReadTimeout(readTimeout);
    // these values are part of signed url and are required
    connection.setRequestMethod("PUT");
    connection.addRequestProperty("Content-Length", content.getLength().toString());
    connection.addRequestProperty("Content-Type", content.getContentType());
    connection.addRequestProperty(
        "x-amz-meta-modified", XmlUtils.formatDate(content.getLastModified()));
    connection.addRequestProperty("x-amz-meta-sha256", content.getSha256());

    // send content
    try (final InputStream in = content.getInputStream();
        final OutputStream out = connection.getOutputStream()) {
      StreamUtils.transferStream(in, out);
    }
    final HttpResponse result = new HttpResponse(connection);
    final long elapsed = (new Date().getTime() - start);
    if (connection.getResponseCode() != 200) {
      throw new HttpException(result, "Error uploading content "
          + path + " (" + elapsed + " ms)");
    }
    LOGGER.finer(
        "["
            + getName()
            + "] uploaded content " + path + " (size= "
            + content.getLength()
            + " bytes) (time= "
            + elapsed
            + " ms)");
    return result;
  }

  /**
   * Upload product contents.
   *
   * Runs uploads in parallel using a parallel stream.
   *
   * This can be called within a custom ForkJoinPool to use a non-default pool,
   * the default pool is shared by the process and based on number of available
   * cores.
   *
   * @param product product to upload.
   * @param uploadProduct product with signed upload urls.
   * @return upload results
   * @throws Exception if any upload errors occur
   */
  protected Map<String, HttpResponse> uploadContents(
      final Product product, final Product uploadProduct) throws Exception {
    // collect results
    final ConcurrentHashMap<String, HttpResponse> uploadResults =
        new ConcurrentHashMap<String, HttpResponse>();
    final ConcurrentHashMap<String, Exception> uploadExceptions =
        new ConcurrentHashMap<String, Exception>();
    // upload contents in parallel
    uploadProduct.getContents().keySet().parallelStream()
        .filter(path -> !"".equals(path))
        .forEach(
            path -> {
              try {
                Content uploadContent = uploadProduct.getContents().get(path);
                if (!(uploadContent instanceof URLContent)) {
                  throw new IllegalStateException(
                      "Expected URLContent for " + product.getId().toString()
                      + " path '" + path + "' but got " + uploadContent);
                }
                uploadResults.put(
                    path,
                    uploadContent(
                        path,
                        product.getContents().get(path),
                        ((URLContent) uploadContent).getURL()));
              } catch (Exception e) {
                uploadExceptions.put(path, e);
              }
            });
    if (uploadExceptions.size() > 0) {
      Exception e = null;
      // log all
      for (final String path : uploadExceptions.keySet()) {
        e = uploadExceptions.get(path);
        LOGGER.log(Level.WARNING, "Exception uploading content " + path, e);
      }
      // throw last
      throw e;
    }
    return uploadResults;
  }
  /** Getter for signProducts
   * @return boolean
   */
  public boolean getSignProducts() {
    return signProducts;
  }

  /** Setter for signProducts
   * @param sign boolean
   */
  public void setSignProducts(final boolean sign) {
    this.signProducts = sign;
  }

  /** getter for privateKey
   * @return privateKey
   */
  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  /** setting for privateKey
   * @param key PrivateKey
   */
  public void setPrivateKey(final PrivateKey key) {
    this.privateKey = key;
  }

}
