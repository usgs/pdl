/*
 * URLProductStorage
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;

import gov.usgs.earthquake.product.io.BinaryProductHandler;
import gov.usgs.earthquake.product.io.BinaryProductSource;
import gov.usgs.earthquake.product.io.JsonProductHandler;
import gov.usgs.earthquake.product.io.JsonProductSource;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.earthquake.product.io.ProductHandler;
import gov.usgs.earthquake.product.io.XmlProductSource;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Store products in a file system which is also available at a URL.
 */
public class URLProductStorage extends FileProductStorage {

	/** Different types of formats */
	public enum Format {
		/** Enum for BINARY/bin format */
		BINARY("bin"),
		/** Enum for JSON format */
		JSON("json"),
		/** Enum for XML format */
		XML("xml");

		private String value;

		private Format(final String value) {
			this.value = value;
		}

		public String toString() {
			return this.value;
		}

		/**
		 * Takes a string value and returns ENUM of its format
		 * @param value String
		 * @return Format ENUM
		 */
		public static Format fromString(final String value) {
			if (BINARY.value.equals(value)) {
				return BINARY;
			} else if (JSON.value.equals(value)) {
				return JSON;
			} else if (XML.value.equals(value)) {
				return XML;
			} else {
				throw new IllegalArgumentException("Invalid format");
			}
		}
	};

	private static final Logger LOGGER = Logger
			.getLogger(URLProductStorage.class.getName());

	/** Property name representing base URL. */
	public static final String URL_PROPERTY_NAME = "url";

	/** The URL which corresponds to baseDirectory. */
	private URL baseURL;

	/** Property for storageFormat */
	public static final String STORAGE_FORMAT_PROPERTY = "storageFormat";

	/** Property for storagePath */
	public static final String STORAGE_PATH_PROPERTY = "storagePath";
	/** Sets up default storage path */
	public static final String DEFAULT_STORAGE_PATH = "{source}_{type}_{code}_{updateTime}.{format}";

	/** (Deprecated, use STORAGE_PATH) Property name to configure binary or xml format. */
	public static final String BINARY_FORMAT_PROPERTY = "binaryFormat";
	/** Default value for whether to use binary format. */
	public static final String BINARY_FORMAT_DEFAULT = "false";

	private Format storageFormat = Format.XML;
	private String storagePath = DEFAULT_STORAGE_PATH;

	/**
	 * Constructor for the Configurable interface.
	 */
	public URLProductStorage() {
	}

	/**
	 * Construct a new ProductStorage object
	 *
	 * @param baseDirectory
	 *            the storage directory where products are stored.
	 * @param baseURL
	 *            the url where storage directory is available.
	 */
	public URLProductStorage(final File baseDirectory, final URL baseURL) {
		super(baseDirectory);
		this.baseURL = baseURL;
	}

	/**
	 * Load the baseURL from configuration.
	 *
	 * @param config
	 *            the configuration object.
	 */
	public void configure(final Config config) throws Exception {
		super.configure(config);

		String urlString = config.getProperty(URL_PROPERTY_NAME);
		if (urlString == null) {
			throw new ConfigurationException("[" + getName()
					+ "] 'url' is a required configuration property");
		}
		baseURL = new URL(urlString);
		LOGGER.config("[" + getName() + "] base url is '" + baseURL.toString()
				+ "'");

		String format = config.getProperty(STORAGE_FORMAT_PROPERTY);
		if (format != null) {
			storageFormat = Format.fromString(format);
		} else {
			if (Boolean.valueOf(config.getProperty(
					BINARY_FORMAT_PROPERTY,
					BINARY_FORMAT_DEFAULT))) {
				storageFormat = Format.BINARY;
			} else {
				storageFormat = Format.XML;
			}
		}
		LOGGER.config("[" + getName() + "] using format " + storageFormat);

		storagePath = config.getProperty(STORAGE_PATH_PROPERTY, DEFAULT_STORAGE_PATH);
		LOGGER.config("[" + getName() + "] using path " + storagePath);
	}

	/**
	 * Compute the URL to a product.
	 *
	 * @param id
	 *            which product.
	 * @return the URL to a product.
	 * @throws Exception if error occurs
	 */
	public URL getProductURL(final ProductId id) throws Exception {
		return new URL(baseURL, getProductPath(id));
	}

	/**
	 * A method for subclasses to override the storage path.
	 *
	 * The returned path is appended to the base directory when storing and
	 * retrieving products.
	 *
	 * @param id
	 *            the product id to convert.
	 * @return the directory used to store id.
	 */
	@Override
	public String getProductPath(final ProductId id) {
		String path = storagePath;
		path = path.replace("{source}", id.getSource());
		path = path.replace("{type}", id.getType());
		path = path.replace("{code}", id.getCode());
		path = path.replace("{updateTime}", Long.toString(id.getUpdateTime().getTime()));
		path = path.replace("{format}", storageFormat.toString());
		return path;
	}

	/**
	 * A method for subclasses to override the storage format.
	 *
	 * When overriding this method, the method getProductInputForFile should
	 * also be overridden.
	 *
	 * @param file
	 *            a file that should be converted into a ProductOutput.
	 * @return the ProductOutput.
	 */
	protected ProductHandler getProductHandlerFormat(final File file)
			throws Exception {
		OutputStream out = StreamUtils.getOutputStream(file);
		if (storageFormat == Format.BINARY) {
			return new BinaryProductHandler(out);
		} else if (storageFormat == Format.JSON) {
			return new JsonProductHandler(out);
		} else {
			return new XmlProductHandler(out);
		}
	}

	/**
	 * A method for subclasses to override the storage format.
	 *
	 * When overriding this method, the method getProductOutputForFile should
	 * also be overridden.
	 *
	 * @param file
	 *            a file that should be converted into a ProductInput.
	 * @return the ProductInput.
	 */
	protected ProductSource getProductSourceFormat(final File file)
			throws Exception {
		InputStream in = StreamUtils.getInputStream(file);
		if (storageFormat == Format.BINARY) {
			return new BinaryProductSource(in);
		} else if (storageFormat == Format.JSON) {
			return new JsonProductSource(in);
		} else {
			return new XmlProductSource(in);
		}
	}

	/** @return storageFormat */
	public Format getStorageFormat() {
		return this.storageFormat;
	}

	/** @param format set a storageFormat */
	public void setStorageFormat(final Format format) {
		this.storageFormat = format;
	}

	/** @return storagePath */
	public String getStoragePath() {
		return this.storagePath;
	}

	/** @param path set a string as the storagePath */
	public void setStoragePath(final String path) {
		this.storagePath = path;
	}

}
