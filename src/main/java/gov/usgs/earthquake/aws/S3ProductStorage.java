package gov.usgs.earthquake.aws;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

import java.net.URL;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.distribution.URLProductStorage;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.ProductHandler;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.util.Config;

public class S3ProductStorage extends URLProductStorage {

  private static final Logger LOGGER = Logger.getLogger(S3ProductStorage.class.getName());

  private Region awsRegion;
  private String bucketName;
  private String bucketPrefix = "pdl/products";
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
	 * Compute the URL to a product.
	 *
	 * @param id
	 *            which product.
	 * @return the URL to a product.
	 * @throws Exception
	 */
  @Override
	public URL getProductURL(final ProductId id) throws Exception {
    final String s3Key = getProductPath(id) + "/" + S3ProductHandler.PRODUCT_XML_FILENAME;
    final URL url = this.s3Client.utilities().getUrl(
      GetUrlRequest.builder().bucket(this.bucketName).key(s3Key).build());
    return url;
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

  }

}
