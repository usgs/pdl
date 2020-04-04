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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

	/** Send in parallel. */
	public static final String PARALLEL_SEND_PROPERTY = "parallelSend";
	public static final String DEFAULT_PARALLEL_SEND = "true";

	/** Timeout in seconds for parallel send. */
	public static final String PARALLEL_SEND_TIMEOUT_PROPERTY = "parallelSendTimeout";
	public static final String DEFAULT_PARALLEL_SEND_TIMEOUT = "300";

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

	/** Whether to send in parallel. */
	protected boolean parallelSend = true;

	/** How long to wait before parallel send timeout. */
	protected long parallelSendTimeout = 300L;

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
		if (parallelSend) {
			return parallelSendProduct(senders, product, parallelSendTimeout);
		}

		// send sequentially if not parallel
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

		parallelSend = Boolean.valueOf(config.getProperty(
				PARALLEL_SEND_PROPERTY,
				DEFAULT_PARALLEL_SEND));
		parallelSendTimeout = Long.valueOf(config.getProperty(
				PARALLEL_SEND_TIMEOUT_PROPERTY,
				DEFAULT_PARALLEL_SEND_TIMEOUT));
		LOGGER.config("[" + getName() + "] parallel send enabled="
				+ parallelSend + ", timeout=" + parallelSendTimeout);
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


	/**
	 * Send a product to all ProductSenders concurrently.
	 *
	 * @param senders
	 *        the senders to receive product.
	 * @param product
	 *        the product to send.
	 * @param timeoutSeconds
	 *        number of seconds before timing out,
	 *        interrupting any pending send.
	 * @return exceptions that occured while sending. If map is empty, there were no
	 *         exceptions.
	 */
	public static Map<ProductSender, Exception> parallelSendProduct(
			final List<ProductSender> senders,
			final Product product,
			final long timeoutSeconds) {
		final Map<ProductSender, Boolean> sendComplete = new HashMap<ProductSender, Boolean>();
		final Map<ProductSender, Exception> sendExceptions = new HashMap<ProductSender, Exception>();

		Iterator<ProductSender> iter = senders.iterator();
		List<Callable<Void>> sendTasks = new ArrayList<Callable<Void>>();
		while (iter.hasNext()) {
			final ProductSender sender = iter.next();
			sendComplete.put(sender, false);
			sendTasks.add(() -> {
				try {
					sender.sendProduct(product);
					sendComplete.put(sender, true);
				} catch (Exception e) {
					sendExceptions.put(sender, e);
				}
				return null;
			});
		}
		// run in parallel
		ExecutorService sendExecutor = Executors.newFixedThreadPool(senders.size());
		try {
			sendExecutor.invokeAll(sendTasks, timeoutSeconds, TimeUnit.SECONDS);
		} catch (Exception e) {
			// this may be Interupted, NullPointer, or RejectedExecution
			// in any case, this part is done and move on to checking send status
		}
		sendExecutor.shutdown();
		// check whether send completed or was interrupted
		for (ProductSender sender: sendComplete.keySet()) {
			if (!sendComplete.get(sender) && sendExceptions.get(sender) == null) {
				sendExceptions.put(sender, new InterruptedException());
			}
		}

		return sendExceptions;
	}

}
