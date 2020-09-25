package gov.usgs.earthquake.aws;

import java.net.URL;
import java.util.logging.Logger;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.distribution.URLProductStorage;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.ProductHandler;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.util.Config;


public class S3ProductStorage extends URLProductStorage {

  private final Logger LOGGER = Logger.getLogger(S3ProductStorage.class.getName());

  public static final String AWS_REGION_PROPERTY = "awsRegion";
  public static final String BUCKET_NAME_PROPERTY = "bucketName";
  public static final String BUCKET_PREFIX_PROPERTY = "bucketPrefix";

  public static final String BUCKET_PREFIX_DEFAULT = "pdl/products";

  private Region awsRegion;
  private String bucketName;
  private String bucketPrefix = BUCKET_PREFIX_DEFAULT;

  private S3Client s3Client;

  public S3ProductStorage() {
  }

  public S3ProductStorage(final Region awsRegion, final String bucketName, final String bucketPrefix) {
    this.awsRegion = awsRegion;
    this.bucketName = bucketName;
    this.bucketPrefix = bucketPrefix;
  }

  @Override
  public void configure(final Config config) throws ConfigurationException {
    this.awsRegion = Region.of(config.getProperty(AWS_REGION_PROPERTY));
    LOGGER.config("[" + getName() + "] awsRegion = " + this.awsRegion);

    this.bucketName = config.getProperty(BUCKET_NAME_PROPERTY);
    LOGGER.config("[" + getName() + "] bucketName = " + this.bucketName);

    this.bucketPrefix = config.getProperty(BUCKET_PREFIX_PROPERTY, BUCKET_PREFIX_DEFAULT);
    LOGGER.config("[" + getName() + "] bucketPrefix = " + this.bucketPrefix);
  }

  @Override
  public void startup() throws Exception {
    super.startup();
    this.s3Client = S3Client.builder().region(this.awsRegion).build();
  }

  @Override
  public void shutdown() throws Exception {
    super.shutdown();
    this.s3Client.close();
    this.s3Client = null;
  }

  /**
   * A method for subclasses to override the storage path.
   *
   * @param id
   *            the product id to convert.
   * @return the prefix used to store id.
   */
  @Override
  public String getProductPath(final ProductId id) {
    return this.bucketPrefix
        + "/" + id.getType()
        + "/" + id.getCode()
        + "/" + id.getSource()
        + "/" + id.getUpdateTime().getTime();
  }

  /**
   * Compute the URL to a product.
   *
   * @param id
   *            which product.
   * @return the URL to a product.
   * @throws Exception
   */
  @Override
  public URL getProductURL(final ProductId id) throws Exception {
    final String key = getProductPath(id) + "/" + S3ProductHandler.PRODUCT_XML_FILENAME;
    LOGGER.finer("[" + getName() + "] getting url for key " + key);
    final URL url = this.s3Client.utilities().getUrl(
      GetUrlRequest.builder().bucket(this.bucketName).key(key).build());
    return url;
  }

  @Override
  protected ProductHandler _getProductHandler(final ProductId id) throws Exception {
    return new S3ProductHandler(this.bucketName, getProductPath(id), this.s3Client);
  }

  @Override
  protected ProductSource _getProductSource(final ProductId id) throws Exception {
    return new S3ProductSource(this.bucketName, getProductPath(id), this.s3Client);
  }

  @Override
  protected boolean _hasProduct(final ProductId id) throws Exception {
    return getProductURL(id) != null;
  }

  @Override
  protected void _removeProduct(final ProductId id) throws Exception {
    ListObjectsV2Response listResponse = this.s3Client.listObjectsV2(
        ListObjectsV2Request.builder()
            .bucket(this.bucketName)
            .prefix(getProductPath(id))
            .build());
    for (final S3Object object : listResponse.contents()) {
      final String key = object.key();
      LOGGER.finer("[" + getName() + "] deleting key " + key);
      DeleteObjectResponse deleteResponse = this.s3Client.deleteObject(
          DeleteObjectRequest.builder()
              .bucket(this.bucketName)
              .key(key)
              .build());
      if (!deleteResponse.sdkHttpResponse().isSuccessful()) {
        LOGGER.info(
            "[" + getName() + "] Error deleting " + key
            + ": " + deleteResponse.sdkHttpResponse().statusText());
      }
    }
  }

}
