package gov.usgs.earthquake.aws;

import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.indexer.DefaultIndexerListener;
import gov.usgs.earthquake.indexer.IndexerEvent;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;


public class S3IndexerListener extends DefaultIndexerListener {

  private static final Logger LOGGER = Logger.getLogger(S3IndexerListener.class.getName());

  public static final String AWS_REGION_PROPERTY = "awsRegion";
  public static final String BUCKET_NAME_PROPERTY = "bucketName";
  public static final String BUCKET_PREFIX_PROPERTY = "bucketPrefix";

  public static final String BUCKET_PREFIX_DEFAULT = "pdl/indexer/actions";

  private Region awsRegion;
  private String bucketName;
  private String bucketPrefix = BUCKET_PREFIX_DEFAULT;
  private S3Client s3Client;

  public S3IndexerListener() {
  }

  public S3IndexerListener(final Region awsRegion, final String bucketName, final String bucketPrefix) {
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
    this.s3Client = S3Client.builder().region(this.awsRegion).build();
  }

  @Override
  public void shutdown() throws Exception {
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
  public String getProductPath(final ProductId id) {
    return this.bucketPrefix
        + "/" + new Date().getTime()
        + "_" + id.getType()
        + "_" + id.getCode()
        + "_" + id.getSource()
        + "_" + id.getUpdateTime().getTime();
  }

  @Override
  public void onIndexerEvent(IndexerEvent event) throws Exception {
    ProductId id = event.getProduct().getId();
    final byte[] jsonEvent = new IndexerEventJson().getJsonObject(event).toString().getBytes();
    // upload content to s3
    final String key = this.getProductPath(id);
    LOGGER.finer("Uploading content to key " + key);
    final long start = new Date().getTime();
    final PutObjectResponse response = s3Client.putObject(
        PutObjectRequest.builder().bucket(this.bucketName).key(key).build(),
        RequestBody.fromBytes(jsonEvent));
    final SdkHttpResponse httpResponse = response.sdkHttpResponse();
    if (!httpResponse.isSuccessful()) {
      throw new Exception("Error uploading " + key + ": "
          + httpResponse.statusCode() + " " + httpResponse.statusText());
    }
    final long end = new Date().getTime();
    LOGGER.fine("Uploaded key " + key
        + " (time = " + (end - start) + "ms)"
        + " (size = " + jsonEvent.length + " bytes)");
    // log uploaded url for now
    final URL url = this.s3Client.utilities().getUrl(
        GetUrlRequest.builder().bucket(this.bucketName).key(key).build());
    LOGGER.finer("Uploaded content url is " + (url != null ? url.toString() : null));
  }

}
