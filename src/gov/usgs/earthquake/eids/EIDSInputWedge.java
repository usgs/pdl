package gov.usgs.earthquake.eids;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.xml.sax.SAXParseException;

import gov.usgs.earthquake.distribution.Bootstrappable;
import gov.usgs.earthquake.distribution.CLIProductBuilder;
import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.distribution.ProductBuilder;
import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.distribution.SocketProductSender;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.quakeml.FileToQuakemlConverter;
import gov.usgs.util.Config;
import gov.usgs.util.CryptoUtils;
import gov.usgs.util.DirectoryPoller;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StringUtils;

/**
 * Read messages from a poll directory, and then push products into PDL. Also
 * supports push.
 * 
 * This is supports EIDS/QDDS style polling. The input messages are converted to
 * Quakeml using the FileToQuakemlConverter interface, then sent as Quakeml
 * based products.
 * 
 * Much of the configuration can be supplied using either a configuration file,
 * or command line arguments.
 */
public class EIDSInputWedge extends ProductBuilder implements Runnable,
		Bootstrappable {

	/**
	 * Global reference to arguments array, when EIDSInputWedge is run via
	 * Bootstrap.
	 */
	public static String[] ARGS = null;

	private static final Logger LOGGER = Logger.getLogger(EIDSInputWedge.class
			.getName());

	public static final String PARSER_CLASS_PROPERTY = "parserClass";
	public static final String DEFAULT_PARSER_CLASS = "gov.usgs.earthquake.event.QuakemlToQuakemlConverter";

	public static final String POLLDIR_PROPERTY = "directory";
	public static final String DEFAULT_POLLDIR = "polldir";
	private File polldir = new File(DEFAULT_POLLDIR);

	public static final String STORAGEDIR_PROPERTY = "oldinputdir";
	public static final String DEFAULT_STORAGEDIR = "oldinput";
	private File storagedir = new File(DEFAULT_STORAGEDIR);

	public static final String ERRORDIR_PROPERTY = "errordir";
	public static final String DEFAULT_ERRORDIR = "errordir";
	private File errordir = new File(DEFAULT_ERRORDIR);

	public static final String VALIDATE_PROPERTY = "validate";
	public static final String DEFAULT_VALIDATE = "false";

	public static final String SEND_ORIGIN_WHEN_PHASES_EXIST_PROPERTY = "sendOriginWhenPhasesExist";
	public static final String DEFAULT_SEND_ORIGIN_WHEN_PHASES_EXIST = "false";

	public static final String SEND_MECHANISM_WHEN_PHASES_EXIST_PROPERTY = "sendMechanismWhenPhasesExist";
	public static final String DEFAULT_SEND_MECHANISM_WHEN_PHASES_EXIST = "false";

	/** Convert parsed quakeml to a product. */
	private ProductCreator productCreator = new QuakemlProductCreator();

	/** Whether created products should be converted to internal types. */
	public static final String CREATE_INTERNAL_PRODUCTS_PROPERTY = "createInternalProducts";
	public static final String DEFAULT_CREATE_INTERNAL_PRODUCTS = "false";
	private boolean createInternalProducts = false;

	/** Whether created products should be converted to scenario types. */
	public static final String CREATE_SCENARIO_PRODUCTS_PROPERTY = "createScenarioProducts";
	public static final String DEFAULT_CREATE_SCENARIO_PRODUCTS = "false";
	private boolean createScenarioProducts = false;

	/** Directory polling object. */
	private DirectoryPoller directoryPoller;

	public static final String POLLINTERVAL_PROPERTY = "interval";
	public static final String DEFAULT_POLLINTERVAL = "1000";
	private long pollInterval = 1000L;

	public static final String POLL_CAREFULLY_PROPERTY = "pollCarefully";
	public static final String DEFAULT_POLL_CAREFULLY = "false";
	private boolean pollCarefully = false;

	private Thread pollThread = null;

	public EIDSInputWedge() throws Exception {
	}

	public Map<ProductId, Map<ProductSender, Exception>> parseAndSend(
			final File file, final Map<String, Content> attachContent)
			throws Exception {
		Map<ProductId, Map<ProductSender, Exception>> sendProductResults = new HashMap<ProductId, Map<ProductSender, Exception>>();

		List<Product> products = productCreator.getProducts(file);

		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			Product product = iter.next();
			ProductId id = product.getId();

			if (createInternalProducts) {
				id.setType("internal-" + id.getType());
			}
			if (createScenarioProducts) {
				id.setType(id.getType() + "-scenario");
			}

			// attach files to generated product
			if (attachContent != null && attachContent.size() > 0) {
				if (products.size() > 1) {
					throw new Exception("Trying to attach files,"
							+ " generated more than 1 product");
				}
				product.getContents().putAll(attachContent);
			}

			// send product, save any exceptions
			sendProductResults.put(product.getId(), sendProduct(product));
		}

		return sendProductResults;
	}

	public void onFile(File file) {
		Date inputtime = new Date();
		LOGGER.info("Reading file " + file.getName());

		try {
			Map<ProductId, Map<ProductSender, Exception>> sendExceptions = parseAndSend(
					file, null);

			// check how send went
			int numSenders = getProductSenders().size();
			int total = sendExceptions.size();
			int successful = 0;
			int partialFailures = 0;
			int totalFailures = 0;

			Iterator<ProductId> sentIds = sendExceptions.keySet().iterator();
			while (sentIds.hasNext()) {
				ProductId sentId = sentIds.next();
				if (sendExceptions.get(sentId).size() == numSenders) {
					totalFailures++;
					LOGGER.severe("Total failure sending product "
							+ sentId.toString());
				} else {
					// output built product id because it was sent at least once
					System.out.println(sentId.toString());

					if (sendExceptions.get(sentId).size() == 0) {
						successful++;
					} else {
						partialFailures++;
						LOGGER.warning("Partial failure sending product "
								+ sentId.toString());
					}
				}
			}

			LOGGER.info("generated " + total + " products: " + successful
					+ " sent, " + partialFailures + " partially sent, "
					+ totalFailures + " failed to send");

			// notify of failures using exit code
			if (totalFailures > 0) {
				// consider this failure, event if some products sent
				throw new Exception();
			}

			// succeeded, at least somewhat
			// move to oldinput directory
			file.renameTo(new File(storagedir, file.getName() + "_"
					+ inputtime.getTime()));
		} catch (Exception e) {
			if (e instanceof JAXBException
					&& ((JAXBException) e).getLinkedException() instanceof SAXParseException) {
				SAXParseException spe = (SAXParseException) ((JAXBException) e)
						.getLinkedException();
				LOGGER.warning("Parse error: " + spe.getMessage() + "; line="
						+ spe.getLineNumber() + ", column="
						+ spe.getColumnNumber());
			}

			if (errordir != null) {
				if (!errordir.exists()) {
					errordir.mkdirs();
				}
				File errorfile = new File(errordir, file.getName() + "_"
						+ new Date().getTime());

				// move to error directory
				try {
				LOGGER.log(Level.WARNING, "Errors processing file, moving to "
						+ errorfile.getCanonicalPath(), e);
				} catch (Exception ignore) {
					//ignore
				}
				file.renameTo(errorfile);
			} else {
				LOGGER.warning("Error processing file " + file.getName()
						+ ", and no error directory configured");
				FileUtils.deleteTree(file);
			}
		}
	}

	@Override
	public void configure(Config config) throws Exception {
		super.configure(config);

		String parserClassName = config.getProperty(PARSER_CLASS_PROPERTY);
		if (parserClassName == null) {
			LOGGER.config("Using QuakemlToProductConverter");
		} else {
			Object parserObj = Class.forName(parserClassName)
					.getConstructor().newInstance();
			if (parserObj instanceof ProductCreator) {
				productCreator = (ProductCreator) parserObj;
			} else if (parserObj instanceof FileToQuakemlConverter) {
				QuakemlProductCreator quakemlCreator = new QuakemlProductCreator();
				quakemlCreator.setConverter((FileToQuakemlConverter) parserObj);
				productCreator = quakemlCreator;
			} else {
				throw new ConfigurationException("configured parser class "
						+ parserClassName + " does not implement "
						+ FileToQuakemlConverter.class.getName());
			}
			LOGGER.config("Using parser class " + parserClassName);
		}

		boolean validate = Boolean.getBoolean(config.getProperty(
				VALIDATE_PROPERTY, DEFAULT_VALIDATE));
		productCreator.setValidate(validate);
		LOGGER.config("Validation " + (validate ? "enabled" : "disabled"));

		boolean sendOriginWhenPhasesExist = Boolean.valueOf(config
				.getProperty(SEND_ORIGIN_WHEN_PHASES_EXIST_PROPERTY,
						DEFAULT_SEND_ORIGIN_WHEN_PHASES_EXIST));
		if (productCreator instanceof EQMessageProductCreator) {
			((EQMessageProductCreator) productCreator)
					.setSendOriginWhenPhasesExist(sendOriginWhenPhasesExist);
		} else {
			((QuakemlProductCreator) productCreator)
					.setSendOriginWhenPhasesExist(sendOriginWhenPhasesExist);
		}
		LOGGER.config("sendOriginWhenPhasesExist = "
				+ sendOriginWhenPhasesExist);

		boolean sendMechanismWhenPhasesExist = Boolean.valueOf(config
				.getProperty(SEND_MECHANISM_WHEN_PHASES_EXIST_PROPERTY,
						DEFAULT_SEND_MECHANISM_WHEN_PHASES_EXIST));
		if (productCreator instanceof EQMessageProductCreator) {
			if (sendMechanismWhenPhasesExist) {
				LOGGER.warning(SEND_MECHANISM_WHEN_PHASES_EXIST_PROPERTY
						+ " is not supported for EQMessageProductCreator");
			}
		} else {
			((QuakemlProductCreator) productCreator)
					.setSendMechanismWhenPhasesExist(sendMechanismWhenPhasesExist);
		}
		LOGGER.config("sendMechanismWhenPhasesExist = "
				+ sendMechanismWhenPhasesExist);

		polldir = new File(
				config.getProperty(POLLDIR_PROPERTY, DEFAULT_POLLDIR));
		LOGGER.config("Using poll directory " + polldir.getCanonicalPath());

		pollInterval = Long.valueOf(config.getProperty(POLLINTERVAL_PROPERTY,
				DEFAULT_POLLINTERVAL));
		LOGGER.config("Using poll interval " + pollInterval + "ms");

		pollCarefully = Boolean.valueOf(config.getProperty(POLL_CAREFULLY_PROPERTY,
				DEFAULT_POLL_CAREFULLY));
		LOGGER.config("Poll carefully = " + pollCarefully);

		storagedir = new File(config.getProperty(STORAGEDIR_PROPERTY,
				DEFAULT_STORAGEDIR));
		LOGGER.config("Using oldinput directory "
				+ storagedir.getCanonicalPath());

		errordir = new File(config.getProperty(ERRORDIR_PROPERTY,
				DEFAULT_ERRORDIR));
		LOGGER.config("Using error directory " + errordir.getCanonicalPath());

		createInternalProducts = Boolean.valueOf(config
				.getProperty(CREATE_INTERNAL_PRODUCTS_PROPERTY,
						DEFAULT_CREATE_INTERNAL_PRODUCTS));
		LOGGER.config("createInternalProducts = " + createInternalProducts);

		createScenarioProducts = Boolean.valueOf(config
				.getProperty(CREATE_SCENARIO_PRODUCTS_PROPERTY,
						DEFAULT_CREATE_SCENARIO_PRODUCTS));
		LOGGER.config("createScenarioProducts = " + createScenarioProducts);
	}

	@Override
	public void shutdown() throws Exception {
		if (pollThread != null) {
			pollThread.interrupt();
			pollThread = null;
		}

		super.shutdown();
	}

	@Override
	public void startup() throws Exception {
		super.startup();

		if (pollThread == null) {
			pollThread = new Thread(this);
			pollThread.setName("poll thread");
			pollThread.start();
		}
	}

	public File getPolldir() {
		return polldir;
	}

	public void setPolldir(File polldir) {
		this.polldir = polldir;
	}

	public File getStoragedir() {
		return storagedir;
	}

	public void setStoragedir(File storagedir) {
		this.storagedir = storagedir;
	}

	public File getErrordir() {
		return errordir;
	}

	public void setErrordir(File errordir) {
		this.errordir = errordir;
	}

	public ProductCreator getProductCreator() {
		return productCreator;
	}

	public void setProductCreator(ProductCreator productCreator) {
		this.productCreator = productCreator;
	}

	public DirectoryPoller getDirectoryPoller() {
		return directoryPoller;
	}

	public void setDirectoryPoller(DirectoryPoller directoryPoller) {
		this.directoryPoller = directoryPoller;
	}

	public long getPollInterval() {
		return pollInterval;
	}

	public void setPollInterval(long pollInterval) {
		this.pollInterval = pollInterval;
	}

	public boolean isPollCarefully() {
		return pollCarefully;
	}

	public void setPollCarefully(boolean pollCarefully) {
		this.pollCarefully = pollCarefully;
	}

	/**
	 * @return the createInternalProducts
	 */
	public boolean isCreateInternalProducts() {
		return createInternalProducts;
	}

	/**
	 * @param createInternalProducts
	 *            the createInternalProducts to set
	 */
	public void setCreateInternalProducts(boolean createInternalProducts) {
		this.createInternalProducts = createInternalProducts;
	}

	/**
	 * @return the createScenarioProducts
	 */
	public boolean isCreateScenarioProducts() {
		return createScenarioProducts;
	}

	/**
	 * @param createScenarioProducts
	 *            the createScenarioProducts to set
	 */
	public void setCreateScenarioProducts(boolean createScenarioProducts) {
		this.createScenarioProducts = createScenarioProducts;
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

	public static final String HELP_ARGUMENT = "--help";
	public static final String POLL_ARGUMENT = "--poll";

	public static final String POLL_CAREFULLY_ARGUMENT = "--pollCarefully";
	public static final String POLLDIR_ARGUMENT = "--polldir=";
	public static final String ERRORDIR_ARGUMENT = "--errordir=";
	public static final String STORAGEDIR_ARGUMENT = "--oldinputdir=";
	public static final String POLL_INTERVAL_ARGUMENT = "--pollInterval=";
	public static final String TRACKER_URL_ARGUMENT = "--trackerURL=";

	public static final String FILE_ARGUMENT = "--file=";

	public static final String PARSER_ARGUMENT = "--parser=";
	public static final String VALIDATE_ARGUMENT = "--validate";
	public static final String PRIVATE_KEY_ARGUMENT = "--privateKey=";
	public static final String SERVERS_ARGUMENT = "--servers=";
	public static final String SERVERS_DEFAULT = "prod01-pdl01.cr.usgs.gov:11235,prod02-pdl01.cr.usgs.gov:11235";
	public static final String CONNECT_TIMEOUT_ARGUMENT = "--connectTimeout=";
	public static final Integer DEFAULT_CONNECT_TIMEOUT = 15000;
	public static final String BINARY_FORMAT_ARGUMENT = "--binaryFormat";
	public static final String DISABLE_DEFLATE_ARGUMENT = "--disableDeflate";
	public static final String ATTACH_ARGUMENT = "--attach=";
	public static final String SEND_ORIGINS_WITH_PHASES = "--sendOriginWhenPhasesExist";
	public static final String SEND_MECHANISMS_WITH_PHASES = "--sendMechanismWhenPhasesExist";

	public static final String CREATE_INTERNAL_PRODUCTS = "--internal";
	public static final String CREATE_SCENARIO_PRODUCTS = "--scenario";

	public static final String TEST_ARGUMENT = "--test";

	/**
	 * Bootstrappable interface.
	 */
	@Override
	public void run(final String[] args) throws Exception {
		// save arguments in global for access by FileToQuakemlParser objects.
		EIDSInputWedge.ARGS = args;

		boolean test = false;
		boolean help = false;
		boolean poll = false;
		boolean validate = this.productCreator.isValidate();
		boolean sendOriginWhenPhasesExist = false;
		boolean sendMechanismWhenPhasesExist = false;

		// preserve any existing settings from config file
		if (productCreator instanceof QuakemlProductCreator) {
			sendOriginWhenPhasesExist = ((QuakemlProductCreator) productCreator)
					.isSendOriginWhenPhasesExist();
			sendMechanismWhenPhasesExist = ((QuakemlProductCreator) productCreator)
					.isSendMechanismWhenPhasesExist();
		} else if (productCreator instanceof EQMessageProductCreator) {
			sendOriginWhenPhasesExist = ((EQMessageProductCreator) productCreator)
					.isSendOriginWhenPhasesExist();
		}

		File file = null;
		// when sending 1 product, allow extra files to be attached.
		HashMap<String, Content> attachContent = new HashMap<String, Content>();
		Integer connectTimeout = DEFAULT_CONNECT_TIMEOUT;
		boolean binaryFormat = false;
		boolean enableDeflate = true;

		StringBuffer arguments = new StringBuffer();
		for (String arg : args) {
			arguments.append(arg).append(" ");
			if (arg.equals(HELP_ARGUMENT)) {
				help = true;
			} else if (arg.equals(POLL_ARGUMENT)) {
				poll = true;
			} else if (arg.equals(POLL_CAREFULLY_ARGUMENT)) {
				pollCarefully = true;
			} else if (arg.equals(SEND_ORIGINS_WITH_PHASES)) {
				sendOriginWhenPhasesExist = true;
			} else if (arg.equals(SEND_MECHANISMS_WITH_PHASES)) {
				sendMechanismWhenPhasesExist = true;
			} else if (arg.startsWith(POLLDIR_ARGUMENT)) {
				setPolldir(new File(arg.replace(POLLDIR_ARGUMENT, "")));
			} else if (arg.startsWith(ERRORDIR_ARGUMENT)) {
				setErrordir(new File(arg.replace(ERRORDIR_ARGUMENT, "")));
			} else if (arg.startsWith(STORAGEDIR_ARGUMENT)) {
				setStoragedir(new File(arg.replace(STORAGEDIR_ARGUMENT, "")));
			} else if (arg.startsWith(FILE_ARGUMENT)) {
				file = new File(arg.replace(FILE_ARGUMENT, ""));
			} else if (arg.startsWith(ATTACH_ARGUMENT)) {
				File attach = new File(arg.replace(ATTACH_ARGUMENT, ""));
				if (attach.isDirectory()) {
					attachContent.putAll(FileContent
							.getDirectoryContents(attach));
				} else {
					attachContent
							.put(attach.getName(), new FileContent(attach));
				}
			} else if (arg.startsWith(PARSER_ARGUMENT)) {
				Object parser = Class.forName(arg.replace(PARSER_ARGUMENT, ""))
						.getConstructor().newInstance();
				if (parser instanceof ProductCreator) {
					setProductCreator((ProductCreator) parser);
				} else {
					QuakemlProductCreator productCreator = new QuakemlProductCreator();
					productCreator
							.setConverter((FileToQuakemlConverter) parser);
					setProductCreator(productCreator);
				}
			} else if (arg.startsWith(VALIDATE_ARGUMENT)) {
				validate = true;
			} else if (arg.startsWith(SERVERS_ARGUMENT)) {
				// ignore servers argument when in test mode
				if (!test) {
					getProductSenders().clear();
					getProductSenders().addAll(
							parseServers(arg.replace(SERVERS_ARGUMENT, ""),
									connectTimeout, binaryFormat, enableDeflate));
				}
			} else if (arg.startsWith(TEST_ARGUMENT)) {
				test = true;
				getProductSenders().clear();
				getProductSenders().add(new DebugProductSender());
			} else if (arg.startsWith(PRIVATE_KEY_ARGUMENT)) {
				setPrivateKey(CryptoUtils.readOpenSSHPrivateKey(FileUtils
						.readFile(new File(arg
								.replace(PRIVATE_KEY_ARGUMENT, ""))), null));
			} else if (arg.startsWith(CONNECT_TIMEOUT_ARGUMENT)) {
				connectTimeout = Integer.valueOf(arg.replace(
						CONNECT_TIMEOUT_ARGUMENT, ""));
			} else if (arg.equals(BINARY_FORMAT_ARGUMENT)) {
				binaryFormat = true;
			} else if (arg.equals(DISABLE_DEFLATE_ARGUMENT)) {
				enableDeflate = false;
			} else if (arg.startsWith(POLL_INTERVAL_ARGUMENT)) {
				setPollInterval(Long.valueOf(arg.replace(
						POLL_INTERVAL_ARGUMENT, "")));
			} else if (arg.startsWith(TRACKER_URL_ARGUMENT)) {
				this.setTrackerURL(new URL(arg
						.replace(TRACKER_URL_ARGUMENT, "")));
			} else if (arg.equals(CREATE_INTERNAL_PRODUCTS)) {
				createInternalProducts = true;
			} else if (arg.equals(CREATE_SCENARIO_PRODUCTS)) {
				createScenarioProducts = true;
			}
		}

		ProductCreator creator = getProductCreator();
		creator.setValidate(validate);
		if (creator instanceof EQMessageProductCreator) {
			((EQMessageProductCreator) creator)
					.setSendOriginWhenPhasesExist(sendOriginWhenPhasesExist);
		} else if (creator instanceof QuakemlProductCreator) {
			QuakemlProductCreator quakemlCreator = ((QuakemlProductCreator) creator);
			quakemlCreator
					.setSendOriginWhenPhasesExist(sendOriginWhenPhasesExist);
			quakemlCreator
					.setSendMechanismWhenPhasesExist(sendMechanismWhenPhasesExist);
		}

		if (
		// want usage, or didn't provide arguments
		(help || args.length == 0)
		// or didn't provide correct arguments
				|| (!poll && file == null)) {
			printUsage();
		}

		// run continuously
		else if (poll) {
			startup();
		}

		// send, then shutdown
		else {
			// file != null
			try {
				// send
				Map<ProductId, Map<ProductSender, Exception>> sendExceptions = parseAndSend(
						file, attachContent);

				// check how send went
				int numSenders = getProductSenders().size();
				int total = sendExceptions.size();
				int successful = 0;
				int partialFailures = 0;
				int totalFailures = 0;

				Iterator<ProductId> sentIds = sendExceptions.keySet()
						.iterator();
				while (sentIds.hasNext()) {
					ProductId sentId = sentIds.next();
					if (sendExceptions.get(sentId).size() == numSenders) {
						totalFailures++;
						LOGGER.severe("Total failure sending product "
								+ sentId.toString());
					} else {
						// output built product id because it was sent at least
						// once
						System.out.println(sentId.toString());

						if (sendExceptions.get(sentId).size() == 0) {
							successful++;
						} else {
							partialFailures++;
							LOGGER.warning("Partial failure sending product "
									+ sentId.toString());
						}
					}
				}

				LOGGER.info("Generated " + total + " products: " + successful
						+ " sent, " + partialFailures + " partially sent, "
						+ totalFailures + " failed to send");

				// notify of failures using exit code
				if (totalFailures > 0) {
					System.exit(CLIProductBuilder.EXIT_UNABLE_TO_SEND);
				}
				if (partialFailures > 0) {
					System.exit(CLIProductBuilder.EXIT_PARTIALLY_SENT);
				}
			} catch (Exception e) {
				if (e instanceof JAXBException
						&& ((JAXBException) e).getLinkedException() instanceof SAXParseException) {
					SAXParseException spe = (SAXParseException) ((JAXBException) e)
							.getLinkedException();
					LOGGER.severe("Parse error: " + spe.getMessage()
							+ "; line=" + spe.getLineNumber() + ", column="
							+ spe.getColumnNumber());
				} else {
					LOGGER.log(Level.SEVERE, "Exception while sending", e);
				}
				System.exit(CLIProductBuilder.EXIT_UNABLE_TO_SEND);
			}
		}

	}

	public static void printUsage() {
		System.err
				.println("\nUsage:\n\n"
						+ "java -cp ProductClient.jar gov.usgs.earthquake.eids.EIDSInputWedge"
						+ " ("
						+ HELP_ARGUMENT
						+ "|"
						+ POLL_ARGUMENT
						+ "|"
						+ FILE_ARGUMENT
						+ "FILE) ["
						+ PRIVATE_KEY_ARGUMENT
						+ "KEYFILE] ["
						+ SERVERS_ARGUMENT
						+ "SERVERS] ["
						+ TEST_ARGUMENT
						+ "] ["
						+ CONNECT_TIMEOUT_ARGUMENT
						+ "TIMEOUT] ["
						+ PARSER_ARGUMENT
						+ "PARSER] ["
						+ POLLDIR_ARGUMENT
						+ "POLLDIR] ["
						+ POLL_INTERVAL_ARGUMENT
						+ "INTERVAL] ["
						+ STORAGEDIR_ARGUMENT
						+ "STORAGEDIR] ["
						+ ERRORDIR_ARGUMENT
						+ "ERRORDIR] ["
						+ ATTACH_ARGUMENT
						+ "ATTACH] ["
						+ SEND_ORIGINS_WITH_PHASES
						+ "] ["
						+ SEND_MECHANISMS_WITH_PHASES
						+ "] ["
						+ CREATE_INTERNAL_PRODUCTS
						+ "] ["
						+ CREATE_SCENARIO_PRODUCTS
						+ "] ["
						+ BINARY_FORMAT_ARGUMENT
						+ "] ["
						+ DISABLE_DEFLATE_ARGUMENT
						+ "]");

		System.err.println();

		System.err.println("\t" + HELP_ARGUMENT);
		System.err.println("\t\tdisplay this message");
		System.err.println("\t" + FILE_ARGUMENT + "FILE");
		System.err.println("\t\tparse and send one file");
		System.err.println("\t" + POLL_ARGUMENT);
		System.err.println("\t\trun continuously, checking POLLDIR for files");

		System.err.println();

		System.err.println("\t" + PRIVATE_KEY_ARGUMENT + "KEYFILE");
		System.err.println("\t\topenssh private key used to sign products");

		System.err.println("\t" + CONNECT_TIMEOUT_ARGUMENT + "TIMEOUT");
		System.err.println("\t\tmilliseconds before timeout while connecting");
		System.err.println("\t\tdefault is \"" + DEFAULT_CONNECT_TIMEOUT
				+ "\"ms");
		System.err.println("\t\t(must appear before " + SERVERS_ARGUMENT + ")");

		System.err.println("\t" + SERVERS_ARGUMENT + "SERVERS");
		System.err
				.println("\t\tcomma delimited list of servers(host:port) where products are sent");
		System.err.println("\t\tdefault is \"" + SERVERS_DEFAULT + "\"");
		System.err.println("\t" + TEST_ARGUMENT);
		System.err.println("\t\tPrint generated products to console for testing, ignores "
				+ SERVERS_ARGUMENT);
		System.err.println("\t" + PARSER_ARGUMENT + "PARSER");
		System.err.println("\t\tclass that implements "
				+ "gov.usgs.earthquake.quakeml.FileToQuakemlConverter");
		System.err.println("\t\tdefault is \"" + DEFAULT_PARSER_CLASS + "\"");

		System.err.println();

		System.err.println("\t" + POLLDIR_ARGUMENT + "POLLDIR");
		System.err.println("\t\tdirectory to poll for messages");

		System.err.println("\t" + POLL_INTERVAL_ARGUMENT + "INTERVAL");
		System.err.println("\t\tmilliseconds between polling");
		System.err.println("\t\tdefault is \"" + DEFAULT_POLLINTERVAL + "\"ms");

		System.err.println("\t" + STORAGEDIR_ARGUMENT + "STORAGEDIR");
		System.err.println("\t\tdirectory for files that were processed");

		System.err.println("\t" + ERRORDIR_ARGUMENT + "ERRORDIR");
		System.err.println("\t\tdirectory for files that weren't processed");

		System.err.println("\t" + ATTACH_ARGUMENT + "ATTACH");
		System.err
				.println("\t\tattach a file or directory to one generated product, repeatable");
		System.err
				.println("\t\tdirectory trees are preserved, each path must be unique");
		System.err
				.println("\t\tif more than one product is generated, an exception will be thrown");
		System.err.println("\t" + SEND_ORIGINS_WITH_PHASES);
		System.err
				.println("\t\tWhen a phase-data product is generated, also send an origin product without the phase data");
		System.err.println("\t" + SEND_MECHANISMS_WITH_PHASES);
		System.err
				.println("\t\tWhen an phase-data product is generated, also send focal mechanism products without the phase data");
		System.err.println();

		System.err.println("\t" + CREATE_INTERNAL_PRODUCTS);
		System.err
				.println("\t\tuse the product type prefix 'internal-' for all generated products");
		System.err.println("\t" + CREATE_SCENARIO_PRODUCTS);
		System.err
				.println("\t\tuse the product type suffix '-scenario' for all generated products");

		System.err.println("\t" + BINARY_FORMAT_ARGUMENT);
		System.err.println("\t\tsend to hub using binary format");

		System.err.println("\t" + DISABLE_DEFLATE_ARGUMENT);
		System.err.println("\t\tdisable deflate compression when sending to hubs");

		System.exit(1);
	}

	@Override
	public void run() {
		if (!polldir.exists()) {
			polldir.mkdirs();
		}
		while (!Thread.currentThread().isInterrupted()) {
			try {
				Date pollStart = new Date();

				String[] polldirFiles = polldir.list();
				if (polldirFiles.length > 0) {
					LOGGER.fine("Polldir contains " + polldirFiles.length
							+ " files");
				}

				for (int i = 0, len = polldirFiles.length; i < len; i++) {
					File file = new File(polldir, polldirFiles[i]);
					try {
						if (pollCarefully) {
							// wait until file is at least pollInterval ms old,
							// in case it is still being written
							long age = new Date().getTime() - file.lastModified();
							if (age <= pollInterval) {
								continue;
							}
						}

						onFile(file);

						if (storagedir != null) {
							if (!storagedir.exists()) {
								storagedir.mkdirs();
							}
							file.renameTo(new File(storagedir, pollStart
									.getTime() + "_" + file.getName()));
						} else {
							FileUtils.deleteTree(file);
						}
					} catch (Exception e) {
						if (errordir != null) {
							if (!errordir.exists()) {
								errordir.mkdirs();
							}
							file.renameTo(new File(errordir, pollStart
									.getTime() + "_" + file.getName()));
						} else {
							LOGGER.warning("Error processing file "
									+ file.getName()
									+ ", and no error directory configured");
							FileUtils.deleteTree(file);
						}
					}
				}

				Date pollEnd = new Date();
				Long pollTime = pollEnd.getTime() - pollStart.getTime();
				if (pollTime < pollInterval) {
					Thread.sleep(pollInterval - pollTime);
				}
			} catch (InterruptedException ie) {
				// interrupted means shutdown
				return;
			}
		}

	}

}
