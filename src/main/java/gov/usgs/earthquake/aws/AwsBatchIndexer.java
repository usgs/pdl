package gov.usgs.earthquake.aws;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import gov.usgs.earthquake.util.JDBCConnection;
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

  public static final String FORCE_REINDEX_ARGUMENT = "--force";
  public static final String GET_PRODUCT_URL_ARGUMENT = "--getProductUrl=";
  public static final String INDEXER_CONFIG_NAME_ARGUMENT="--indexerConfigName=";
  public static final String INDEXER_CONFIG_NAME_DEFAULT = "indexer";

  public static final String DATABASE_DRIVER_ARGUMENT = "--databaseDriver=";
  public static final String DATABASE_URL_ARGUMENT = "--databaseUrl=";
  public static final String INDEXER_DATABASE_ARGUMENT = "--indexerDatabase=";
  public static final String INDEXER_DATABASE_DEFAULT = "indexer";

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


  @Override
  public void run(String[] args) throws Exception {
    // parse arguments
    String databaseDriver = "com.mysql.jdbc.Driver";
    String databaseUrl = null;
    String indexerConfigName = INDEXER_CONFIG_NAME_DEFAULT;
    for (final String arg : args) {
      if (arg.startsWith(DATABASE_DRIVER_ARGUMENT)) {
        databaseDriver = arg.replace(DATABASE_DRIVER_ARGUMENT, "");
      } else if (arg.startsWith(DATABASE_URL_ARGUMENT)) {
        databaseUrl = arg.replace(DATABASE_URL_ARGUMENT, "");
      } else if (arg.equals(FORCE_REINDEX_ARGUMENT)) {
        force = true;
      } else if (arg.startsWith(GET_PRODUCT_URL_ARGUMENT)) {
        getProductUrlTemplate = arg.replace(GET_PRODUCT_URL_ARGUMENT, "");
      } else if (arg.startsWith(INDEXER_CONFIG_NAME_ARGUMENT)) {
        indexerConfigName = arg.replace(INDEXER_CONFIG_NAME_ARGUMENT, "");
      }
    }

    // load indexer from configuration
    indexer = (Indexer) Config.getConfig().getObject(indexerConfigName);
    indexer.startup();

    try {
      if (databaseUrl != null) {
        LOGGER.info("Reading product ids from database");
        readProductIdsFromDatabase(databaseDriver, databaseUrl);
      } else {
        LOGGER.info("Reading product ids from stdin");
        readProductIdsFromStdin();
      }
    } finally {
      indexer.shutdown();
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
      final JsonNotification notification = new JsonNotification(json);
      return notification.product;
    }
  }

  /**
   * Fetch and Index a product.
   *
   * Called from executor service to process product ids.
   *
   * @param id
   *     which product
   */
  public void processProductId(final ProductId id) {
    long start = new Date().getTime();
    try {
      final Product product = getProduct(id);
      long afterGetProduct = new Date().getTime();
      LOGGER.fine("Loaded product " + id.toString() + " in "
          + (afterGetProduct - start) + " ms");

      indexer.onProduct(product, force);
      LOGGER.info("Indexed " + id.toString()
          + " in " + (new Date().getTime() - afterGetProduct) + " ms");
    } catch (Exception e) {
      LOGGER.log(
          Level.WARNING,
          "Error indexing " + id.toString()
              + " in " + (new Date().getTime() - start) + "ms",
          e);
    }
  }

  public void readProductIdsFromDatabase(
      final String driver,
      final String url) throws Exception {
    try (
      final JDBCConnection jdbcConnection = new JDBCConnection()
    ) {
      jdbcConnection.setDriver(driver);
      jdbcConnection.setUrl(url);
      jdbcConnection.startup();

      final String sql = "SELECT id, source, type, code, updatetime"
          + " FROM pdl.product h"
          + " WHERE id > ?"
          + " AND NOT EXISTS ("
          + "  SELECT * FROM indexer.productSummary i"
          + "  WHERE h.source=i.source"
          + "  AND h.type=i.type"
          + "  AND h.code=i.code"
          + "  AND h.updatetime=i.updateTime"
          + " )"
          + " ORDER BY id"
          + " LIMIT 500";

      // start at the beginning
      long lastId = -1;
      while (true) {
        try (
          final Connection conn = jdbcConnection.verifyConnection();
          final PreparedStatement statement = conn.prepareStatement(sql);
        ) {
          // load next batch of products
          statement.setLong(1, lastId);
          try (
            final ResultSet rs = statement.executeQuery();
          ) {
            int count = 0;
            while (rs.next()) {
              lastId = rs.getLong("id");
              final ProductId id = new ProductId(
                  rs.getString("source"),
                  rs.getString("type"),
                  rs.getString("code"),
                  new Date(rs.getLong("updatetime")));
              submitProductId(id);
              count++;
            }

            // exit once all products processed
            if (count == 0) {
              LOGGER.info("No more rows returned, exiting");
              break;
            }
          }
        }
      }
    }
  }

  /**
   * Read product ids (as urns) from stdin and submit to executor for processing.
   *
   * @throws Exception
   */
  public void readProductIdsFromStdin() throws Exception {
    // read product ids from stdin
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String line = null;
    while ((line = br.readLine()) != null) {
      if (line.equals("")) {
        continue;
      }
      // parse product id
      final ProductId id;
      try {
        id = ProductId.parse(line);
      } catch (Exception e) {
        LOGGER.warning("Error parsing product id '" + line + "'");
        continue;
      }
      submitProductId(id);
    }
  }

  /**
   * Submit a product id to the executor service for processing.
   *
   * If queue is too large (500 ids), blocks until queue is smaller (100 ids).
   *
   * @param id
   *     which product
   * @throws InterruptedException
   */
  public void submitProductId(final ProductId id) throws InterruptedException {
    // queue for processing
    executor.submit(() -> processProductId(id));

    // keep queue size smallish
    if (executor.getQueue().size() > 500) {
      while (executor.getQueue().size() > 100) {
        LOGGER.info("Queue size " + executor.getQueue().size());
        Thread.sleep(5000L);
      }
    }
  }
}
