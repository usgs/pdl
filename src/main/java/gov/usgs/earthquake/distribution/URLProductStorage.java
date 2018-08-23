/*
 * URLProductStorage
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;

import gov.usgs.earthquake.product.io.BinaryProductHandler;
import gov.usgs.earthquake.product.io.BinaryProductSource;
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

	private static final Logger LOGGER = Logger
			.getLogger(URLProductStorage.class.getName());

	/** Property name representing base URL. */
	public static final String URL_PROPERTY_NAME = "url";

	/** The URL which corresponds to baseDirectory. */
	private URL baseURL;

	/** Property name to configure binary or xml format. */
	public static final String BINARY_FORMAT_PROPERTY = "binaryFormat";
	/** Default value for whether to use binary format. */
	public static final String BINARY_FORMAT_DEFAULT = "false";

	/** Whether to store in binary format (true), or xml format (false). */
	private boolean binaryFormat = false;

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

		binaryFormat = Boolean.valueOf(config.getProperty(
				BINARY_FORMAT_PROPERTY, BINARY_FORMAT_DEFAULT));
		LOGGER.config("[" + getName() + "] using "
				+ (binaryFormat ? "binary" : "xml") + " format");
	}

	/**
	 * Compute the URL to a product.
	 * 
	 * @param id
	 *            which product.
	 * @return the URL to a product.
	 * @throws Exception
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
		StringBuffer buf = new StringBuffer();
		buf.append(id.getSource());
		buf.append("_");
		buf.append(id.getType());
		buf.append("_");
		buf.append(id.getCode());
		buf.append("_");
		buf.append(id.getUpdateTime().getTime());
		if (binaryFormat) {
			buf.append(".bin");
		} else {
			buf.append(".xml");
		}
		return buf.toString();
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
		if (binaryFormat) {
			return new BinaryProductHandler(out);
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
		if (binaryFormat) {
			return new BinaryProductSource(in);
		} else {
			return new XmlProductSource(in);
		}
	}

}
