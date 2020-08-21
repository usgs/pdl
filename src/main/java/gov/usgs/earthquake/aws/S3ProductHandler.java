package gov.usgs.earthquake.aws;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.URLContent;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.XmlProductHandler;


/**
 * Store a product to an S3 Bucket.
 *
 * Contents are stored directly in the bucket within the product prefix.
 *
 * All ProductOutput methods are passed to an ObjectProductOutput object,
 * except contents with non-empty paths, and stored in the product prefix
 * in the file named "product.xml".
 */
public class S3ProductHandler extends ObjectProductHandler {

	/** The file where product attributes are stored. */
	public static final String PRODUCT_XML_FILENAME = "product.xml";

	/** Bucket where product contents are stored. */
  private final String bucketName;
  private final String productPath;
  private final S3Client s3Client;

  /**
   * Construct a new S3ProductHandler object.
   */
  public S3ProductHandler(final String bucketName, final String productPath, final S3Client s3Client) {
    this.bucketName = bucketName;
    this.productPath = productPath;
    this.s3Client = s3Client;
  }

  /**
   * Extract content when path isn't empty.
   */
  public void onContent(final ProductId id, final String path, final Content content) throws Exception {
    if ("".equals(path)) {
      super.onContent(id, path, content);
      return;
    }
    // upload content to s3
    final String s3Key = this.productPath + "/" + path;
    final PutObjectResponse response = s3Client.putObject(
        PutObjectRequest.builder().bucket(this.bucketName).key(s3Key).build(),
        RequestBody.fromInputStream(content.getInputStream(), content.getLength()));
    final SdkHttpResponse httpResponse = response.sdkHttpResponse();
    if (!httpResponse.isSuccessful()) {
      throw new Exception("Error uploading " + s3Key + ": "
          + httpResponse.statusCode() + " " + httpResponse.statusText());
    }
    // pass s3 content to parent class
    final URL url = this.s3Client.utilities().getUrl(
        GetUrlRequest.builder().bucket(this.bucketName).key(s3Key).build());
    super.onContent(id, path, new URLContent(content, url));
  }

  /**
   * Store all except product contents to product.xml.
   */
  public void onEndProduct(final ProductId id) throws Exception {
		super.onEndProduct(id);

    // upload content to s3
    final String s3Key = this.productPath + "/" + PRODUCT_XML_FILENAME;
    final PutObjectResponse response = s3Client.putObject(
        PutObjectRequest.builder().bucket(this.bucketName).key(s3Key).build(),
        RequestBody.fromBytes(getProductXml(getProduct())));
    final SdkHttpResponse httpResponse = response.sdkHttpResponse();
    if (!httpResponse.isSuccessful()) {
      throw new Exception("Error uploading " + s3Key + ": "
          + httpResponse.statusCode() + " " + httpResponse.statusText());
    }
	}

  /**
   * Format product metadata.
   *
   * See also {@link #getProductPath()}.
   */
  protected byte[] getProductXml(final Product product) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    // save product attributes as xml
    try (
      final ObjectProductSource source = new ObjectProductSource(product);
      final XmlProductHandler handler = new XmlProductHandler(out);
    ) {
      source.streamTo(handler);
    }
		return out.toByteArray();
  }

}
