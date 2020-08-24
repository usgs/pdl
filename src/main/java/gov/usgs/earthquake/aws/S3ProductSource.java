package gov.usgs.earthquake.aws;

import java.util.logging.Logger;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.ProductHandler;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.earthquake.product.io.XmlProductSource;


/**
 * Load a product from an S3 prefix.
 *
 * It should contain a product xml file named "product.xml".
 */
public class S3ProductSource implements ProductSource {

  public static final Logger LOGGER = Logger.getLogger(S3ProductSource.class.getName());

  /** Bucket where product contents are stored. */
  private final String bucketName;
  private final String productPrefix;
  private final S3Client s3Client;

  /**
   * Construct a new S3ProductSource object.
   */
  public S3ProductSource(final String bucketName, final String productPrefix, final S3Client s3Client) {
    this.bucketName = bucketName;
    this.productPrefix = productPrefix;
    this.s3Client = s3Client;
  }

	/**
	 * Load Product from a directory, then send product events to the
	 * ProductOutput.
	 *
	 * @param out
	 *            the ProductOutput that will receive the product.
	 */
	public void streamTo(ProductHandler out) throws Exception {
    final String key = this.productPrefix + "/" + S3ProductHandler.PRODUCT_XML_FILENAME;
    LOGGER.finer("reading product from key " + key);
    try (ResponseInputStream<GetObjectResponse> in =
        this.s3Client.getObject(GetObjectRequest.builder()
            .bucket(this.bucketName)
            .key(key)
            .build())) {
			// load product from xml
      Product product = ObjectProductHandler.getProduct(new XmlProductSource(in));
      LOGGER.finer("read product " + (product != null ? product.getId().toString() : null));
			// use ObjectProductInput to send loaded product
			try (ObjectProductSource source = new ObjectProductSource(product)) {
        source.streamTo(out);
      }
		}
	}

  @Override
  public void close() {
    // nothing to close
  }

}
