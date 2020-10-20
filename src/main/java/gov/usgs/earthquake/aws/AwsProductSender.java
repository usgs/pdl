package gov.usgs.earthquake.aws;

import gov.usgs.earthquake.distribution.ConfigurationException;
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
import java.io.ByteArrayInputStream;
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
	public static final Logger LOGGER = Logger.getLogger(AwsProductSender.class.getName());

	public static final String HUB_URL_PROPERTY = "url";
	public static final String PRIVATE_KEY_PROPERTY = "privateKey";
	public static final String SIGN_PRODUCTS_PROPERTY = "signProducts";

	// url where products are sent
	protected URL hubUrl;
	// signing key
	protected PrivateKey privateKey;
	// whether to sign products
	protected boolean signProducts = false;

	public AwsProductSender() {}

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
				Product uploadProduct = getUploadUrls(json);
				final long afterGetUploadUrls = new Date().getTime();
				LOGGER.fine("[" + getName() + "] get upload urls " + id.toString()
						+ " (" + (afterGetUploadUrls - start) + " ms) ");

				// upload contents
				uploadContents(product, uploadProduct);
				afterUploadContent = new Date().getTime();
				LOGGER.fine("[" + getName() + "] upload contents " + id.toString()
						+ " (" + (afterUploadContent - afterGetUploadUrls) + " ms) ");
			} else {
				afterUploadContent = new Date().getTime();
			}

			// send product
			sendProduct(json);
			final long afterSendProduct = new Date().getTime();
			LOGGER.fine("[" + getName() + "] send product " + id.toString()
					+ " (" + (afterSendProduct - afterUploadContent) + " ms) ");
		} catch (ProductAlreadySentException pase) {
			// hub already has product
			LOGGER.info("[" + getName() + "] hub already has product");
			return;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception sending product " + id.toString(), e);
			throw e;
		} finally {
			final long end = new Date().getTime();
			LOGGER.info("[" + getName() + "] send product total " + id.toString()
					+ " (" + (end - start) + " ms) ");
		}
	}

	protected Product getUploadUrls(final JsonObject json) throws Exception {
		final URL url = new URL(hubUrl, "get_upload_urls");
		final HttpResponse result = postProductJson(url, json);
		final int responseCode = result.connection.getResponseCode();
		if (responseCode != 200) {
			if (responseCode == 401) {
				throw new HttpException(result, "Invalid signature getting upload urls");
			} else {
				throw new HttpException(result, "Error getting upload urls");
			}
		}
		final JsonObject getUploadUrlsResponse = result.getJsonObject();
		final Product product = new JsonProduct().getProduct(
				getUploadUrlsResponse.getJsonObject("product"));
		if (getUploadUrlsResponse.getBoolean("already_exists")) {
			throw new ProductAlreadySentException(product);
		}
		return product;
	}

	protected HttpResponse postProductJson(final URL url, final JsonObject product) throws Exception {
		// send as attribute, for extensibility
		final JsonObject json = Json.createObjectBuilder().add("product", product).build();
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		try (final OutputStream out = connection.getOutputStream()) {
			out.write(json.toString().getBytes());
		}
		return new HttpResponse(connection);
	}

	protected Product sendProduct(final JsonObject json) throws Exception {
		// send request
		final URL url = new URL(hubUrl, "send_product");
		final HttpResponse result = postProductJson(url, json);
		if (result.connection.getResponseCode() != 200) {
			throw new HttpException(result, "Error sending product");
		}
		final JsonObject sendProductResponse = result.getJsonObject();
		// parse response
		final JsonObject notification = sendProductResponse.getJsonObject("notification");
		final Product product = new JsonProduct().getProduct(notification.getJsonObject("product"));
		if (sendProductResponse.getBoolean("already_exists")) {
			throw new ProductAlreadySentException(product);
		}
		String snsMessageId = null;
		if (!sendProductResponse.isNull("sns_message_id")) {
			snsMessageId = sendProductResponse.getString("sns_message_id");
		}
		LOGGER.fine("[" + getName() + "] sns message id "
				+ snsMessageId + " " + product.getId().toString());
		return product;
	}

	protected HttpResponse uploadContent(final String path, final Content content, final URL signedUrl)
			throws Exception {
		final long start = new Date().getTime();
		final HttpURLConnection connection = (HttpURLConnection) signedUrl.openConnection();
		connection.setDoOutput(true);
		// these values are part of signed url
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
		if (connection.getResponseCode() == 422) {
			throw new HttpException(result,
					"Content validation errors: " + result.getJsonObject().toString());
		} else if (connection.getResponseCode() != 200) {
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
								uploadResults.put(
										path,
										uploadContent(
												path,
												product.getContents().get(path),
												((URLContent) uploadProduct.getContents().get(path)).getURL()));
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

	public boolean getSignProducts() {
		return signProducts;
	}

	public void setSignProducts(final boolean sign) {
		this.signProducts = sign;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(final PrivateKey key) {
		this.privateKey = key;
	}

	static class HttpException extends Exception {
		private static final long serialVersionUID = 1L;

		public final HttpResponse response;

		public HttpException(final HttpResponse response, final String cause) {
			super(cause);
			this.response = response;
		}

		public String toString() {
			int code;
			String message;
			try {
				code = this.response.connection.getResponseCode();
				message = this.response.connection.getResponseMessage();
			} catch (Exception e) {
				code = -1;
				message = null;
			}
			return this.getMessage()
					+ ", response " + code + " " + message
					+ " : " + new String(this.response.response);
		}
	}

	/** Class to hold HttpURLConnection and response data. */
	static class HttpResponse {
		public final HttpURLConnection connection;
		public final byte[] response;

		public HttpResponse(final HttpURLConnection connection) throws Exception {
			this.connection = connection;
			try (final InputStream in = connection.getInputStream()) {
				byte[] response = StreamUtils.readStream(in);
				this.response = response;
			}
		}

		public JsonObject getJsonObject() throws Exception {
			return Json.createReader(new ByteArrayInputStream(response)).readObject();
		}
	}
}
