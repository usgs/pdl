/*
 * CLIProductBuilder
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.InputStreamContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.CryptoUtils;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;
import gov.usgs.util.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command Line Interface Product Builder.
 *
 * This class is used to build and send products. It is typically called by
 * using the --build argument with the standard ProductClient.
 *
 * The CLIProductBuilder implements the Configurable interface and uses the
 * following configuration parameters:
 *
 * <dl>
 * <dt>senders</dt>
 * <dd>(Required). A comma separated list of section names that should be loaded
 * as ProductSender objects. Each sender in this list will be used to send any
 * built products. See each type of ProductSender for more configuration
 * details.</dd>
 * </dl>
 */
public class CLIProductBuilder extends DefaultConfigurable {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(CLIProductBuilder.class.getName());

	/** Exit code used when an invalid combination of arguments is used. */
	public static final int EXIT_INVALID_ARGUMENTS = 1;

	/** Exit code used when unable to build a product. */
	public static final int EXIT_UNABLE_TO_BUILD = 2;

	/** Exit code used when errors occur while sending. */
	public static final int EXIT_UNABLE_TO_SEND = 3;

	/** Exit code when errors occur while sending, but not to all senders. */
	public static final int EXIT_PARTIALLY_SENT = 4;

	// product id arguments
	public static final String TYPE_ARGUMENT = "--type=";
	public static final String CODE_ARGUMENT = "--code=";
	public static final String SOURCE_ARGUMENT = "--source=";
	public static final String UPDATE_TIME_ARGUMENT = "--updateTime=";

	// product status arguments
	public static final String STATUS_ARGUMENT = "--status=";
	public static final String DELETE_ARGUMENT = "--delete";

	// tracking url
	public static final String TRACKER_URL_ARGUMENT = "--trackerURL=";

	// product properties
	public static final String PROPERTY_ARGUMENT = "--property-";
	public static final String EVENTID_ARGUMENT = "--eventid=";
	public static final String EVENTSOURCE_ARGUMENT = "--eventsource=";
	public static final String EVENTSOURCECODE_ARGUMENT = "--eventsourcecode=";
	public static final String EVENTCODE_ARGUMENT = "--eventcode=";
	public static final String EVENTTIME_ARGUMENT = "--eventtime=";
	public static final String LATITUDE_ARGUMENT = "--latitude=";
	public static final String LONGITUDE_ARGUMENT = "--longitude=";
	public static final String DEPTH_ARGUMENT = "--depth=";
	public static final String MAGNITUDE_ARGUMENT = "--magnitude=";
	public static final String VERSION_ARGUMENT = "--version=";

	// product links
	public static final String LINK_ARGUMENT = "--link-";

	// product content arguments
	public static final String CONTENT_ARGUMENT = "--content";
	public static final String CONTENT_TYPE_ARGUMENT = "--contentType=";
	public static final String DIRECTORY_ARGUMENT = "--directory=";
	public static final String FILE_ARGUMENT = "--file=";

	// private key used for signature
	public static final String PRIVATE_KEY_ARGUMENT = "--privateKey=";

	/** Property name used for configuring a tracker url. */
	public static final String TRACKER_URL_CONFIG_PROPERTY = "trackerURL";

	/** Property name used for configuring the list of senders. */
	public static final String SENDERS_CONFIG_PROPERTY = "senders";

	/** Arguments for configuring servers and connectTimeouts. */
	public static final String SERVERS_ARGUMENT = "--servers=";
	public static final String CONNECT_TIMEOUT_ARGUMENT = "--connectTimeout=";
	public static final Integer DEFAULT_CONNECT_TIMEOUT = 15000;
	public static final String BINARY_FORMAT_ARGUMENT = "--binaryFormat";
	public static final String DISABLE_DEFLATE = "--disableDeflate";

	/** Tracker URL that is used when not overriden by an argument. */
	private URL defaultTrackerURL;

	/** ProductSenders that send the product after it is built. */
	private List<ProductSender> senders = new LinkedList<ProductSender>();

	/** The command line arguments being parsed. */
	private String[] args;

	private Integer connectTimeout = DEFAULT_CONNECT_TIMEOUT;

	/**
	 * This class is not intended to be instantiated directly. f
	 */
	protected CLIProductBuilder(final String[] args) {
		this.args = args;
	}

	/**
	 * @return the senders
	 */
	public List<ProductSender> getSenders() {
		return senders;
	}

	/**
	 * @return the defaultTrackerURL
	 */
	public URL getDefaultTrackerURL() {
		return defaultTrackerURL;
	}

	/**
	 * @param defaultTrackerURL
	 *            the defaultTrackerURL to set
	 */
	public void setDefaultTrackerURL(URL defaultTrackerURL) {
		this.defaultTrackerURL = defaultTrackerURL;
	}

	/**
	 * Load ProductSenders that will send any built Products.
	 *
	 * There should be a property "senders" containing a comma delimited list of
	 * sender names to be loaded.
	 *
	 * @param config
	 *            the Config to load.
	 */
	public void configure(final Config config) throws Exception {
		Iterator<String> iter = StringUtils.split(
				config.getProperty(SENDERS_CONFIG_PROPERTY), ",").iterator();
		while (iter.hasNext()) {
			String senderName = iter.next();

			LOGGER.config("Loading sender '" + senderName + "'");
			// names reference global configuration objects.

			ProductSender sender = (ProductSender) Config.getConfig()
					.getObject(senderName);

			if (sender == null) {
				throw new ConfigurationException("Sender '" + senderName
						+ "' is not properly configured");
			}

			senders.add(sender);
		}

		String trackerURLProperty = config
				.getProperty(TRACKER_URL_CONFIG_PROPERTY);
		if (trackerURLProperty != null) {
			defaultTrackerURL = new URL(trackerURLProperty);
		}
	}

	/**
	 * Called when the client is shutting down.
	 */
	public void shutdown() throws Exception {
		Iterator<ProductSender> iter = senders.iterator();
		while (iter.hasNext()) {
			try {
				iter.next().shutdown();
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Exception shutting down sender", e);
			}
		}
	}

	/**
	 * Called when the client is done configuring.
	 */
	public void startup() throws Exception {
		Iterator<ProductSender> iter = senders.iterator();
		while (iter.hasNext()) {
			iter.next().startup();
		}
	}

	/**
	 * Send a product to all configured ProductSenders.
	 *
	 * @param product
	 *            the product to send.
	 * @return exceptions that occured while sending. If map is empty, there
	 *         were no exceptions.
	 */
	public Map<ProductSender, Exception> sendProduct(final Product product) {
		Map<ProductSender, Exception> sendExceptions = new HashMap<ProductSender, Exception>();

		Iterator<ProductSender> iter = senders.iterator();
		while (iter.hasNext()) {
			ProductSender sender = iter.next();
			try {
				sender.sendProduct(product);
			} catch (Exception e) {
				sendExceptions.put(sender, e);
			}
		}

		return sendExceptions;
	}

	/**
	 * Build a product using command line arguments.
	 *
	 * @throws Exception
	 */
	public Product buildProduct() throws Exception {
		// start product id with null values, and verify they are all set after
		// all arguments are parsed.
		Product product = new Product(new ProductId(null, null, null));
		product.setTrackerURL(defaultTrackerURL);

		// These things are also processed after all arguments are parsed.
		// used with inline content
		boolean hasStdinContent = false;
		String contentType = null;
		// used when signing products
		File privateKey = null;
		boolean binaryFormat = false;
		boolean enableDeflate = true;

		for (String arg : args) {
			if (arg.startsWith(TYPE_ARGUMENT)) {
				product.getId().setType(arg.replace(TYPE_ARGUMENT, ""));
			} else if (arg.startsWith(CODE_ARGUMENT)) {
				product.getId().setCode(arg.replace(CODE_ARGUMENT, ""));
			} else if (arg.startsWith(SOURCE_ARGUMENT)) {
				product.getId().setSource(arg.replace(SOURCE_ARGUMENT, ""));
			} else if (arg.startsWith(UPDATE_TIME_ARGUMENT)) {
				product.getId()
						.setUpdateTime(
								XmlUtils.getDate(arg.replace(
										UPDATE_TIME_ARGUMENT, "")));
			} else if (arg.startsWith(STATUS_ARGUMENT)) {
				product.setStatus(arg.replace(STATUS_ARGUMENT, ""));
			} else if (arg.equals(DELETE_ARGUMENT)) {
				product.setStatus(Product.STATUS_DELETE);
			} else if (arg.startsWith(TRACKER_URL_ARGUMENT)) {
				product.setTrackerURL(new URL(arg.replace(TRACKER_URL_ARGUMENT,
						"")));
			} else if (arg.startsWith(PROPERTY_ARGUMENT)) {
				String[] props = arg.replace(PROPERTY_ARGUMENT, "").split("=",
						2);
				try {
					product.getProperties().put(props[0], props[1]);
				} catch (IndexOutOfBoundsException ioobe) {
					throw new IllegalArgumentException(
							"Invalid property argument, must have value");
				}
			} else if (arg.startsWith(EVENTID_ARGUMENT)) {
				String id = arg.replace(EVENTID_ARGUMENT, "").toLowerCase();
				String eventNetwork = id.substring(0, 2);
				String eventNetworkId = id.substring(2);
				product.setEventId(eventNetwork, eventNetworkId);
			} else if (arg.startsWith(EVENTSOURCE_ARGUMENT)) {
				product.setEventSource(arg.replace(EVENTSOURCE_ARGUMENT, "")
						.toLowerCase());
			} else if (arg.startsWith(EVENTSOURCECODE_ARGUMENT)) {
				product.setEventSourceCode(arg.replace(
						EVENTSOURCECODE_ARGUMENT, "").toLowerCase());
			} else if (arg.startsWith(EVENTCODE_ARGUMENT)) {
				product.setEventSourceCode(arg.replace(EVENTCODE_ARGUMENT, "")
						.toLowerCase());
			} else if (arg.startsWith(EVENTTIME_ARGUMENT)) {
				product.setEventTime(XmlUtils.getDate(arg.replace(
						EVENTTIME_ARGUMENT, "")));
			} else if (arg.startsWith(MAGNITUDE_ARGUMENT)) {
				product.setMagnitude(new BigDecimal(arg.replace(
						MAGNITUDE_ARGUMENT, "")));
			} else if (arg.startsWith(LATITUDE_ARGUMENT)) {
				product.setLatitude(new BigDecimal(arg.replace(
						LATITUDE_ARGUMENT, "")));
			} else if (arg.startsWith(LONGITUDE_ARGUMENT)) {
				product.setLongitude(new BigDecimal(arg.replace(
						LONGITUDE_ARGUMENT, "")));
			} else if (arg.startsWith(DEPTH_ARGUMENT)) {
				product.setDepth(new BigDecimal(arg.replace(DEPTH_ARGUMENT, "")));
			} else if (arg.startsWith(VERSION_ARGUMENT)) {
				product.setVersion(arg.replace(VERSION_ARGUMENT, ""));
			} else if (arg.startsWith(LINK_ARGUMENT)) {
				String[] props = arg.replace(LINK_ARGUMENT, "").split("=", 2);
				try {
					product.addLink(props[0], new URI(props[1]));
				} catch (IndexOutOfBoundsException ioobe) {
					throw new IllegalArgumentException(
							"Invalid link, must have URL as value");
				}
			} else if (arg.equals(CONTENT_ARGUMENT)) {
				hasStdinContent = true;
			} else if (arg.startsWith(CONTENT_TYPE_ARGUMENT)) {
				contentType = arg.replace(CONTENT_TYPE_ARGUMENT, "");
			} else if (arg.startsWith(DIRECTORY_ARGUMENT)) {
				product.getContents().putAll(
						FileContent.getDirectoryContents(new File(arg.replace(
								DIRECTORY_ARGUMENT, ""))));
			} else if (arg.startsWith(FILE_ARGUMENT)) {
				File file = new File(arg.replace(FILE_ARGUMENT, ""));
				product.getContents()
						.put(file.getName(), new FileContent(file));
			} else if (arg.startsWith(PRIVATE_KEY_ARGUMENT)) {
				privateKey = new File(arg.replace(PRIVATE_KEY_ARGUMENT, ""));
			} else if (arg.startsWith(SERVERS_ARGUMENT)) {
				senders.clear();
				senders.addAll(parseServers(arg.replace(SERVERS_ARGUMENT, ""),
						connectTimeout, binaryFormat, enableDeflate));
			} else if (arg.startsWith(CONNECT_TIMEOUT_ARGUMENT)) {
				connectTimeout = Integer.valueOf(arg.replace(
						CONNECT_TIMEOUT_ARGUMENT, ""));
			} else if (arg.equals(BINARY_FORMAT_ARGUMENT)) {
				binaryFormat = true;
			} else if (arg.equals(DISABLE_DEFLATE)) {
				enableDeflate = false;
			} else {
				// not a builder argument
			}
		}

		// validate product
		ProductId id = product.getId();
		if (id.getType() == null || id.getSource() == null
				|| id.getCode() == null || id.getUpdateTime() == null) {
			throw new IllegalArgumentException("Incomplete ProductId: source="
					+ id.getSource() + ", type=" + id.getType() + ", code="
					+ id.getCode() + ", updateTime=" + id.getUpdateTime());
		}

		// tracker url is required
		if (product.getTrackerURL() == null) {
			throw new IllegalArgumentException("Tracker URL is required");
		}

		if (hasStdinContent) {
			LOGGER.info("Reading content on standard input");

			ByteContent stdinContent = new ByteContent(new InputStreamContent(
					System.in));
			if (contentType != null) {
				stdinContent.setContentType(contentType);
			}
			product.getContents().put("", stdinContent);
		}

		// products that aren't being deleted should have content
		if (product.getContents().size() == 0 && !product.isDeleted()) {
			LOGGER.warning("Product has no content, are you sure this is intended?");
		}

		// mark which version of client was used to create product
		product.getProperties().put(ProductClient.PDL_CLIENT_VERSION_PROPERTY,
				ProductClient.RELEASE_VERSION);

		if (privateKey != null) {
			LOGGER.fine("Signing product");
			product.sign(CryptoUtils.readOpenSSHPrivateKey(StreamUtils
					.readStream(StreamUtils.getInputStream(privateKey)), null));
		}

		return product;
	}

	public static List<ProductSender> parseServers(final String servers,
			final Integer connectTimeout, final boolean binaryFormat,
			final boolean enableDeflate) {
		List<ProductSender> senders = new ArrayList<ProductSender>();

		Iterator<String> iter = StringUtils.split(servers, ",").iterator();
		while (iter.hasNext()) {
			String server = iter.next();
			String[] parts = server.split(":");
			SocketProductSender sender = new SocketProductSender(parts[0],
					Integer.parseInt(parts[1]), connectTimeout);
			sender.setBinaryFormat(binaryFormat);
			sender.setEnableDeflate(enableDeflate);
			senders.add(sender);
		}

		return senders;
	}

	/**
	 * Entry point into CLIProductBuilder.
	 *
	 * Called by Main if the --build argument is present.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		CLIProductBuilder builder = new CLIProductBuilder(args);
		builder.configure(Config.getConfig());
		builder.startup();

		Product product = null;
		try {
			product = builder.buildProduct();
		} catch (Exception e) {
			if (e.getMessage() == null) {
				LOGGER.log(Level.SEVERE, "Error building product", e);
			} else {
				LOGGER.severe("Invalid arguments: " + e.getMessage());
			}
			System.exit(EXIT_INVALID_ARGUMENTS);
		}

		if (product == null) {
			LOGGER.severe("Unable to build product");
			System.exit(EXIT_UNABLE_TO_BUILD);
		}

		// send tracker update
		new ProductTracker(product.getTrackerURL()).productCreated(
				SocketProductSender.class.getName(), product.getId());

		// send the product
		Map<ProductSender, Exception> sendExceptions = builder
				.sendProduct(product);

		// handle any send exceptions
		if (sendExceptions.size() != 0) {
			Iterator<ProductSender> senders = sendExceptions.keySet()
					.iterator();
			// log the exceptions
			while (senders.hasNext()) {
				ProductSender sender = senders.next();
				if (sender instanceof SocketProductSender) {
					// put more specific information about socket senders
					SocketProductSender socketSender = (SocketProductSender) sender;
					LOGGER.log(
							Level.WARNING,
							"Exception sending product to "
									+ socketSender.getHost() + ":"
									+ socketSender.getPort(),
							sendExceptions.get(sender));
				} else {
					LOGGER.log(Level.WARNING, "Exception sending product "
							+ sendExceptions.get(sender));
				}
			}

			if (sendExceptions.size() < builder.getSenders().size()) {
				LOGGER.warning("Partial failure sending product,"
						+ " at least one sender accepted product."
						+ " Check the tracker for more information.");
				// still output built product id
				System.out.println(product.getId().toString());
				// but exit with partial failure
				System.exit(EXIT_PARTIALLY_SENT);
			} else {
				LOGGER.severe("Total failure sending product");
				System.exit(EXIT_UNABLE_TO_SEND);
			}

		}

		// otherwise output built product id
		System.out.println(product.getId().toString());

		// normal exit
		builder.shutdown();
		System.exit(0);
	}

	public static String getUsage() {
		StringBuffer buf = new StringBuffer();

		buf.append("Product identification\n");
		buf.append("--source=SOURCE          product source, e.g. us, nc\n");
		buf.append("--type=TYPE              product type, e.g. shakemap, pager\n");
		buf.append("--code=CODE              product code, e.g. us2009abcd, nc12345678\n");
		buf.append("[--updateTime=TIME]      when the product was updated\n");
		buf.append("                         e.g. 2010-02-11T15:16:17+0000\n");
		buf.append("                         default is now\n");
		buf.append("[--status=STATUS]        product status\n");
		buf.append("                         default is UPDATE\n");
		buf.append("[--delete]               same as --status=DELETE\n");
		buf.append("\n");

		buf.append("Product contents\n");
		buf.append("[--directory=DIR]        read content from a directory, preserves hierarchy\n");
		buf.append("[--file=FILE]            read content from a file, added at top level of product\n");
		buf.append("[--content]              read content from STDIN\n");
		buf.append("[--contentType=MIMETYPE] used with --content to specify STDIN mime type\n");
		buf.append("\n");

		buf.append("Product metadata\n");
		buf.append("[--link-RELATION=URI]    link to another product or resource\n");
		buf.append("[--property-NAME=VALUE]  attributes of this product\n");
		buf.append("[--latitude=LAT]         Latitude of associated event.\n");
		buf.append("                             Decimal degrees\n");
		buf.append("                             Same as --property-latitude=LAT\n");
		buf.append("[--longitude=LNG]        Longitude of associated event.\n");
		buf.append("                             Decimal degrees\n");
		buf.append("                             Same as --property-longitude=LNG\n");
		buf.append("[--eventtime=TIME]       Time of associated event.\n");
		buf.append("                             Example: 2010-02-11T15:16:17+0000\n");
		buf.append("                             Same as --property-eventtime=TIME\n");
		buf.append("[--magnitude=MAG]        Magnitude of associated event.\n");
		buf.append("                             Same as --property-magnitude=MAG\n");
		buf.append("[--depth=DEPTH]          Depth of associated event.\n");
		buf.append("                             Kilometers.\n");
		buf.append("                             Same as --property-depth=DEPTH\n");
		buf.append("[--eventsource=SOURCE]   Network of associated event.\n");
		buf.append("                             Examples: us, nc, ci\n");
		buf.append("                             Same as --property-eventsource=SOURCE\n");
		buf.append("[--eventcode=CODE]       NetworkID of associated event.\n");
		buf.append("                             Examples: 2010abcd, 12345678\n");
		buf.append("                             Same as --property-eventsourcecode=CODE\n");
		buf.append("[--eventid=EVENTID]      Deprecated, use --eventsource and --eventsourcecode.\n");
		buf.append("                             Assumes a 10 character eventid: \n");
		buf.append("                                 assigns first 2 characters as 'eventsource',\n");
		buf.append("                                 rest as 'eventsourcecode'\n");
		buf.append("[--version=VERSION]      Internal product version.\n");
		buf.append("                             Same as --property-version=VERSION\n");
		buf.append("\n");

		buf.append("[--trackerURL=URL]       tracker url\n");
		buf.append("                         Override a configured default trackerURL\n");
		buf.append("[--privateKey=FILE]      OpenSSH DSA private key used to sign products\n");
		buf.append("                         A product signature may or may not be optional\n");
		buf.append("\n");

		buf.append("Where product is sent\n");
		buf.append("[--connectTimeout=15000] Connect timeout in milliseconds\n");
		buf.append("                         Only used with --servers argument\n");
		buf.append("                         Must appear before --servers argument.\n");
		buf.append("[--binaryFormat]         Send to hub using binary format.\n");
		buf.append("                         Only used with --servers argument\n");
		buf.append("                         Must appear before --servers argument.\n");
		buf.append("[--disableDeflate]       Send to hub without using deflate compression.\n");
		buf.append("                         Only used with --servers argument\n");
		buf.append("                         Must appear before --servers argument.\n");
		buf.append("[--servers=SERVERLIST]   server:port[,server:port]\n");
		buf.append("                         Overrides any configured senders\n");
		buf.append("                         Example: pdldevel.cr.usgs.gov:11235\n");
		buf.append("\n");

		return buf.toString();
	}
}
