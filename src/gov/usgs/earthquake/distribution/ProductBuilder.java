package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.util.Config;
import gov.usgs.util.CryptoUtils;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Essentials for building/sending products.
 *
 * This is the base class for other builders.
 *
 * Supported configurable properties:
 * <dl>
 * <dt>senders</dt>
 * <dd>A comma delimited list of product senders to use when sending products.</dd>
 * <dt>trackerURL</dt>
 * <dd>Default tracker URL to assign to products that don't already have one.</dd>
 * <dt>privateKeyFile</dt>
 * <dd>Path to a private key that can be used to sign products.</dd>
 * </dl>
 */
public class ProductBuilder extends DefaultConfigurable {

	private static final Logger LOGGER = Logger.getLogger(ProductBuilder.class
			.getSimpleName());

	/** Configurable property for senders. */
	public static final String SENDERS_PROPERTY = "senders";

	/** Property name used for configuring a tracker url. */
	public static final String TRACKER_URL_PROPERTY = "trackerURL";

	/** Private key filename configuration property. */
	public static final String PRIVATE_KEY_PROPERTY = "privateKeyFile";

	/** Default tracker url. */
	public static final URL DEFAULT_TRACKER_URL;
	static {
		URL url = null;
		try {
			url = new URL("http://ehppdl1.cr.usgs.gov/tracker/");
		} catch (MalformedURLException mue) {
			LOGGER.severe("Failed to parse default tracker url.");
			System.exit(1);
		}
		DEFAULT_TRACKER_URL = url;
	}

	/** List of senders where built products are sent. */
	private List<ProductSender> senders = new LinkedList<ProductSender>();

	/** Default trackerURL to set on sent products. */
	private URL trackerURL;

	/** Key used to sign sent products. */
	private PrivateKey privateKey;

	public ProductBuilder() {
		trackerURL = DEFAULT_TRACKER_URL;
	}

	/**
	 * Send a product.
	 *
	 * If the product doesn't yet have a tracker URL, assigns current tracker
	 * URL to product. If the product has not yet been signed, and a privateKey
	 * is configured, signs the product before sending.
	 *
	 * @param product
	 *            the product to send.
	 * @return map of all exceptions thrown, from Sender to corresponding
	 *         Exception.
	 * @throws Exception
	 *             if an error occurs while signing product.
	 */
	public Map<ProductSender, Exception> sendProduct(final Product product)
			throws Exception {

		// doesn't already have a tracker url
		if (product.getTrackerURL() == null) {
			product.setTrackerURL(trackerURL);
		}

		// mark which version of client was used to create product
		product.getProperties().put(ProductClient.PDL_CLIENT_VERSION_PROPERTY,
				ProductClient.RELEASE_VERSION);

		// doesn't already have a signature.
		if (privateKey != null && product.getSignature() == null) {
			product.sign(privateKey);
		}

		// send tracker update
		new ProductTracker(product.getTrackerURL()).productCreated(
				this.getName(), product.getId());

		// send product using all product senders.
		Map<ProductSender, Exception> errors = new HashMap<ProductSender, Exception>();
		Iterator<ProductSender> iter = new LinkedList<ProductSender>(senders)
				.iterator();
		while (iter.hasNext()) {
			ProductSender sender = iter.next();
			try {
				sender.sendProduct(product);
			} catch (Exception e) {
				if (e instanceof ProductAlreadyInStorageException) {
					// condense this message...
					LOGGER.info("Product already in storage, id="
							+ product.getId().toString());
				} else {
					LOGGER.log(Level.WARNING, "[" + sender.getName()
							+ "] error sending product", e);
					errors.put(sender, e);
				}
			}
		}

		return errors;
	}

	/**
	 * @return list of product senders
	 */
	public List<ProductSender> getProductSenders() {
		return senders;
	}

	/**
	 * Add a ProductSender.
	 *
	 * @param sender
	 */
	public void addProductSender(final ProductSender sender) {
		senders.add(sender);
	}

	/**
	 * Remove a previously added ProductSender.
	 *
	 * @param sender
	 */
	public void removeProductSender(final ProductSender sender) {
		senders.remove(sender);
	}

	public URL getTrackerURL() {
		return trackerURL;
	}

	public void setTrackerURL(URL trackerURL) {
		this.trackerURL = trackerURL;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	@Override
	public void configure(final Config config) throws Exception {
		Iterator<String> senderNames = StringUtils.split(
				config.getProperty(SENDERS_PROPERTY), ",").iterator();
		while (senderNames.hasNext()) {
			String name = senderNames.next();
			LOGGER.config("Loading sender " + name);

			ProductSender sender = (ProductSender) Config.getConfig()
					.getObject(name);
			if (sender == null) {
				throw new ConfigurationException("Unable to load sender '"
						+ name + "', make sure it is properly configured.");
			}
			addProductSender(sender);
		}

		String url = config.getProperty(TRACKER_URL_PROPERTY);
		if (url != null) {
			trackerURL = new URL(url);
		}
		LOGGER.config("[" + getName() + "] Using tracker URL '"
				+ trackerURL.toString() + "'");

		String keyFilename = config.getProperty(PRIVATE_KEY_PROPERTY);
		if (keyFilename != null) {
			LOGGER.config("[" + getName() + "] Loading private key file '"
					+ keyFilename + "'");
			privateKey = CryptoUtils.readOpenSSHPrivateKey(
					StreamUtils.readStream(new File(keyFilename)), null);
		}
	}

	@Override
	public void shutdown() throws Exception {
		Iterator<ProductSender> iter = senders.iterator();
		while (iter.hasNext()) {
			iter.next().shutdown();
		}
	}

	@Override
	public void startup() throws Exception {
		Iterator<ProductSender> iter = senders.iterator();
		while (iter.hasNext()) {
			iter.next().startup();
		}
	}

}
