package gov.usgs.earthquake.aws;

import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.URLContent;
import gov.usgs.earthquake.product.io.JsonProduct;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

	public static final String URL_PROPERTY = "url";

	protected URL hubUrl;

	public AwsProductSender() {}

	@Override
	public void configure(Config config) throws Exception {
		super.configure(config);

		hubUrl = new URL(config.getProperty(URL_PROPERTY));
	}

	@Override
	public void sendProduct(final Product product) throws Exception {
		final ProductId id = product.getId();
		// convert to json
		JsonObject json = new JsonProduct().getJsonObject(product);

		try {
			LOGGER.fine("Getting upload urls for " + json.toString());
			// get upload urls, response is product with signed content urls for upload
			Product uploadProduct = getUploadUrls(json);

			// upload contents
			uploadContents(product, uploadProduct);

			// send product
			sendProduct(json);
		} catch (ProductAlreadySentException pase) {
			// hub already has product
			LOGGER.info("[" + getName() + "] hub already has product");
			return;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception sending product " + id.toString(), e);
			throw e;
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
		final long start = new Date().getTime();
		final URL url = new URL(hubUrl, "send_product");
		final HttpResponse result = postProductJson(url, json);
		final long elapsed = (new Date().getTime() - start);
		if (result.connection.getResponseCode() != 200) {
			throw new HttpException(result, "Error sending product (" + elapsed + " ms)");
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
		LOGGER.info(
				"Sent product "
						+ product.getId().toString()
						+ " (time= "
						+ elapsed
						+ " ms) (sns id = "
						+ snsMessageId
						+ " )");
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
			throw new HttpException(result, "Error uploading content (" + elapsed + " ms)");
		}
		LOGGER.info(
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
		final long start = new Date().getTime();
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
		final long elapsed = (new Date().getTime() - start);
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
		LOGGER.info(
				"["
						+ getName()
						+ "] uploaded all contents for "
						+ product.getId().toString()
						+ " (time= "
						+ elapsed
						+ " ms)");
		return uploadResults;
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
			try {
				code = this.response.connection.getResponseCode();
			} catch (Exception e) {
				code = -1;
			}
			return this.getMessage()
					+ ", response code="
					+ code
					+ " : "
					+ new String(this.response.response);
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
