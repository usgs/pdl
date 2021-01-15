package gov.usgs.earthquake.aws;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import gov.usgs.earthquake.distribution.Bootstrappable;
import gov.usgs.earthquake.indexer.Indexer;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.JsonProduct;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

/**
 * Class to index a batch of products that have already been sent to the AWS hub.
 *
 * Reads a list of products to be indexed.
 * Reads indexer from configuration file.
 *
 * For each product, fetch product information from the get_product AWS endpoint
 * and call indexer.onProduct.
 */
public class AwsBatchIndexer implements Bootstrappable {

  /** Logging object. */
  private static final Logger LOGGER = Logger.getLogger(AwsBatchIndexer.class.getName());

  /** Executor where indexing runs. */
  private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

  /** Whether to force indexing. */
  private boolean force = false;

  /** AWS URL for get_product endpoint, with placeholders. */
  private String getProductUrlTemplate = "https://earthquake.usgs.gov/pdl/west"
      + "/get_product?source={source}&type={type}&code={code}&updateTime={updateTime}";

  /** Indexer to process products. */
  private Indexer indexer;


  public static final String FORCE_REINDEX_ARGUMENT = "--force";
  public static final String GET_PRODUCT_URL_ARGUMENT = "--getProductUrl=";
  public static final String INDEXER_CONFIG_NAME_ARGUMENT="--indexerConfigName=";
  public static final String INDEXER_CONFIG_NAME_DEFAULT = "indexer";

  @Override
  public void run(String[] args) throws Exception {
    String indexerConfigName = INDEXER_CONFIG_NAME_DEFAULT;

    // parse arguments
    for (final String arg : args) {
      if (arg.equals(FORCE_REINDEX_ARGUMENT)) {
        force = true;
      } else if (arg.startsWith(GET_PRODUCT_URL_ARGUMENT)) {
        getProductUrlTemplate = arg.replace(GET_PRODUCT_URL_ARGUMENT, "");
      } else if (arg.startsWith(INDEXER_CONFIG_NAME_ARGUMENT)) {
        indexerConfigName = arg.replace(INDEXER_CONFIG_NAME_ARGUMENT, "");
      }
    }

    // load indexer from configuration
    indexer = (Indexer) Config.getConfig().getObject(indexerConfigName);

    // read product ids from stdin
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String line = null;
    while ((line = br.readLine()) != null) {
      if (line.equals("")) {
        continue;
      }

      // parse product id
      ProductId id;
      try {
        id = ProductId.parse(line);
      } catch (Exception e) {
        LOGGER.warning("Error parsing product id '" + line + "'");
        continue;
      }

      // queue for processing
      executor.submit(() -> {
        long start = new Date().getTime();
        Exception e = indexProduct(id);
        long total = new Date().getTime() - start;
        if (e == null) {
          LOGGER.info("Indexed " + id.toString() + " in " + total + " ms");
        } else {
          LOGGER.log(
              Level.WARNING,
              "Error indexing " + id.toString() + " in " + total + "ms",
              e);
        }
      });

      // keep queue size smallish
      if (executor.getQueue().size() > 500) {
        while (executor.getQueue().size() > 100) {
          LOGGER.info("Queue size " + executor.getQueue().size());
          Thread.sleep(5000L);
        }
      }

    }
  }

  /**
   * Use getProductUrl template to generate URL.
   *
   * Replace "{source}", "{type}", "{code}", and "{updateTime}" placeholders.
   *
   * @param id
   *     which product.
   * @return URL with placeholders replaced.
   * @throws Exception
   */
  public URL getProductUrl(final ProductId id) throws Exception {
    String url = getProductUrlTemplate;
    url = url.replace("{source}", id.getSource());
    url = url.replace("{type}", id.getType());
    url = url.replace("{code}", id.getCode());
    url = url.replace("{updateTime}", XmlUtils.formatDate(id.getUpdateTime()));
    return new URL(url);
  }

  /**
   * Get Product from endpoint.
   *
   * @param id
   *     which product.
   * @return Product object.
   * @throws Exception
   */
  public Product getProduct(final ProductId id) throws Exception {
    final URL url = getProductUrl(id);
    byte[] bytes = StreamUtils.readStream(url);
    try (
        final JsonReader reader = Json.createReader(new StringReader(
            new String(bytes, StandardCharsets.UTF_8)))
    ) {
      // parse message
      final JsonObject json = reader.readObject();
      return new JsonProduct().getProduct(json);
    }
  }

  /**
   * Index a product.
   *
   * @param product
   *     which product
   * @throws Exception
   */
  public Exception indexProduct(final ProductId id) {
    try {
      long start = new Date().getTime();
      final Product product = getProduct(id);
      long afterGetProduct = new Date().getTime();
      LOGGER.fine("Loaded product " + id.toString() + " in "
          + (afterGetProduct - start) + " ms");
      indexer.onProduct(product, force);
      return null;
    } catch (Exception e) {
      return e;
    }
  }

}
