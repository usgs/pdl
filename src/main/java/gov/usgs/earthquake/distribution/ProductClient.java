/*
 * Main
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.indexer.SearchCLI;

import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StringUtils;

import java.lang.management.ManagementFactory;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * The entry point to product distribution.
 *
 * The ProductClient implements the Configurable interface and can use the
 * following configuration parameters:
 *
 * <dl>
 *
 * <dt>receivers</dt>
 * <dd>(Required) A comma separated list of section names that should be loaded
 * as NotificationReceiver objects. Each receiver is a source of notifications
 * for listeners.</dd>
 *
 * <dt>listeners</dt>
 * <dd>(Required) A comma separated list of section names that should be loaded
 * as NotificationListener objects. Each listener receives notifications from
 * receivers.</dd>
 *
 * <dt>logdirectory</dt>
 * <dd>(Optional) Log directory. Default is "log", relative to the current
 * working directory. Log files using a naming convention
 * <code>ProductClient_YYYYMMDD.log</code>.</dd>
 *
 * <dt>loglevel</dt> <dd>(Optional) Default is INFO. One of SEVERE, WARNING,
 * INFO, CONFIG, FINE, FINER, FINEST</dd>
 *
 * <dt>enableTracker</dt> <dd>(Optional) Default is false. Whether or not to
 * send tracker updates to a product tracker. This is generally desirable, but
 * is disabled by default for the paranoid.</dd>
 *
 * <dt>redirectconsole</dt> <dd>(Optional) Default is false. Whether or not to
 * redirect console output to the log file.</dd>
 *
 * </dl>
 *
 * <p>
 * All listeners listen to all receivers for notifications.
 * </p>
 *
 */
public class ProductClient extends DefaultConfigurable implements
		ProductClientMBean, Bootstrappable {

	/** The "release" version number. */
	public static final String RELEASE_VERSION = "Version 2.0.0 2019-02-05";

	/** Property name used on products for current RELEASE_VERSION. */
	public static final String PDL_CLIENT_VERSION_PROPERTY = "pdl-client-version";

	/** SVN Id property. */
	public static final String SVN_VERSION = "$Id$";

	/** SVN Revision property. */
	public static final String SVN_REVISION = "$Revision$";

	/** SVN LastChangedDate property. */
	public static final String SVN_LAST_CHANGED_DATE = "$LastChangedDate$";

	/** Logging object. */
	private static final Logger LOGGER = Logger.getLogger(ProductClient.class
			.getName());

	/** Defaults are loaded from inside the jar file. */
	public static final String JAR_CONFIG_FILE = "etc/config/config.ini";

	/** Default location of config file. */
	public static final String DEFAULT_CONFIG_FILE = "config.ini";

	/** Custom config file locations. */
	public static final String CONFIG_FILE_ARGUMENT = "--configFile=";

	/** Run the builder. */
	public static final String BUILD_ARGUMENT = "--build";

	/** Run the tracker. */
	public static final String TRACK_ARGUMENT = "--track";

	/** Run the builder. */
	public static final String SEND_ARGUMENT = "--send";

	/** Run the client. */
	public static final String RECEIVE_ARGUMENT = "--receive";

	/** Run the search. */
	public static final String SEARCH_ARGUMENT = "--search";

	/** Show usage. */
	public static final String USAGE_ARGUMENT = "--help";

	/** Property containing list of receivers. */
	public static final String RECEIVERS_PROPERTY_NAME = "receivers";

	/** Property containing list of senders. */
	public static final String LISTENERS_PROPERTY_NAME = "listeners";

	/** Log level property name. */
	public static final String LOGLEVEL_PROPERTY_NAME = "loglevel";

	/** Default log level is INFO. */
	public static final String DEFAULT_LOGLEVEL = "INFO";

	/** Property with location for log directory. */
	public static final String LOGDIRECTORY_PROPERTY_NAME = "logdirectory";

	/** Default directory for logging. */
	public static final String DEFAULT_LOGDIRECTORY = "log";

	/** Default location for log file. */
	public static final String DEFAULT_LOGFILE = "'ProductClient'_yyyyMMdd'.log'";

	/** Whether or not to redirect stdout and stderr to log file. */
	public static final String CONSOLE_REDIRECT_PROPERTY_NAME = "redirectconsole";

	/** Default console redirect value (don't redirect). */
	public static final String DEFAULT_CONSOLE_REDIRECT = "false";

	/** Property used to disable tracker updates. */
	public static final String ENABLE_TRACKER_PROPERTY_NAME = "enableTracker";

	public static final String ENABLE_ADMIN_SOCKET = "enableAdminSocket";
	public static final String DEFAULT_ENABLE_ADMIN_SOCKET = "false";

	/** List of receivers that generate notifications. */
	private List<NotificationReceiver> receivers = new LinkedList<NotificationReceiver>();

	/** List of listeners that receive notifications. */
	private List<NotificationListener> listeners = new LinkedList<NotificationListener>();

	/** Whether to start a zabbix agent. */
	private boolean enableAdminSocket = false;
	private boolean enableJMX = true;
	private AdminSocketServer adminSocketServer = null;

	public void configure(Config config) throws Exception {
		loadListeners(config);
		loadReceivers(config);

		// connect all listeners to all receivers
		Iterator<NotificationReceiver> iter = receivers.iterator();
		while (iter.hasNext()) {
			NotificationReceiver receiver = iter.next();
			Iterator<NotificationListener> iter2 = listeners.iterator();
			while (iter2.hasNext()) {
				NotificationListener listener = iter2.next();
				receiver.addNotificationListener(listener);
			}
		}

		enableAdminSocket = Boolean.valueOf(config.getProperty(
				ENABLE_ADMIN_SOCKET, DEFAULT_ENABLE_ADMIN_SOCKET));
	}

	/**
	 * Load listeners from a Config object.
	 *
	 * @param config
	 *            the configuration.
	 * @throws Exception
	 */
	public void loadListeners(final Config config) throws Exception {
		Iterator<String> iter = StringUtils.split(
				config.getProperty(LISTENERS_PROPERTY_NAME, ""), ",")
				.iterator();
		while (iter.hasNext()) {
			String listenerName = iter.next();
			LOGGER.config("Loading listener '" + listenerName + "'");

			NotificationListener listener = (NotificationListener) Config
					.getConfig().getObject(listenerName);
			if (listener == null) {
				throw new ConfigurationException("Unable to load listener '"
						+ listenerName
						+ "', make sure it is properly configured.");
			}

			// listenerName references an object in the global configuration
			listeners.add(listener);
		}
	}

	/**
	 * Load NotificationReceivers from a Config object.
	 *
	 * @param config
	 *            the configuration
	 * @throws Exception
	 */
	public void loadReceivers(final Config config) throws Exception {
		Iterator<String> iter = StringUtils.split(
				config.getProperty(RECEIVERS_PROPERTY_NAME), ",").iterator();
		while (iter.hasNext()) {
			String receiverName = iter.next();
			LOGGER.config("Loading receiver '" + receiverName + "'");

			NotificationReceiver receiver = (NotificationReceiver) Config
					.getConfig().getObject(receiverName);
			if (receiver == null) {
				throw new ConfigurationException("Unable to load receiver '"
						+ receiverName
						+ "', make sure it is properly configured.");
			}
			// receiverName references an object in the global configuration
			receivers.add(receiver);
		}
	}

	/**
	 * Start up all listeners and receivers.
	 */
	public void startup() throws Exception {
		Iterator<NotificationListener> iter = listeners.iterator();
		while (iter.hasNext()) {
			iter.next().startup();
		}
		Iterator<NotificationReceiver> iter2 = receivers.iterator();
		while (iter2.hasNext()) {
			iter2.next().startup();
		}

		if (enableAdminSocket) {
			LOGGER.info("Starting AdminSocketServer on port 11111");
			adminSocketServer = new AdminSocketServer();
			adminSocketServer.setClient(this);
			adminSocketServer.startup();
		}

		if (enableJMX) {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName clientName = new ObjectName("ProductClient:name=jmx");
			mbs.registerMBean(this, clientName);
		}
	}

	/**
	 * Shut down all receivers and listeners.
	 */
	public void shutdown() throws Exception {
		if (receivers.size() > 0) {
			Iterator<NotificationReceiver> iter2 = receivers.iterator();
			while (iter2.hasNext()) {
				try {
					iter2.next().shutdown();
				} catch (Exception e) {
					// ignore
				}
			}
		}

		if (listeners.size() > 0) {
			Iterator<NotificationListener> iter = listeners.iterator();
			while (iter.hasNext()) {
				try {
					iter.next().shutdown();
				} catch (Exception e) {
					// ignore
				}
			}
		}

		if (adminSocketServer != null) {
			try {
				adminSocketServer.shutdown();
			} catch (Exception e) {
			}
			adminSocketServer = null;
		}
	}

	/**
	 * Entry point into Product Distribution.
	 *
	 * @param args
	 */
	public void run(final String[] args) throws Exception {
		try {
			// default is show usage
			boolean receiveProducts = false;
			boolean buildProduct = false;
			boolean trackProduct = false;
			boolean searchProduct = false;
			boolean showUsage = false;

			// parse arguments
			for (String arg : args) {
				if (arg.equals(SEND_ARGUMENT) || arg.equals(BUILD_ARGUMENT)) {
					buildProduct = true;
				} else if (arg.equals(RECEIVE_ARGUMENT)) {
					receiveProducts = true;
				} else if (arg.equals(TRACK_ARGUMENT)) {
					trackProduct = true;
				} else if (arg.equals(SEARCH_ARGUMENT)) {
					searchProduct = true;
				} else if (arg.equals(USAGE_ARGUMENT)) {
					showUsage = true;
				}
			}

			// output current version
			System.err.println("Product Distribution Client");
			System.err.println(RELEASE_VERSION);
			System.err.println();

			if (buildProduct) {
				if (showUsage) {
					System.err.println("Usage: ");
					System.err
							.println("    java -jar ProductClient.jar --build [BUILD ARGUMENTS]");
					System.err.println();
					System.err.println(CLIProductBuilder.getUsage());
					System.exit(0);
				}
				LOGGER.info("Running Product Builder");
				// run builder main
				CLIProductBuilder.main(args);
				System.exit(0);
			} else if (trackProduct) {
				if (showUsage) {
					System.err.println("Usage: ");
					System.err
							.println("    java -jar ProductClient.jar --track [TRACK ARGUMENTS]");
					System.err.println();
					System.err.println(ProductTracker.getUsage());
					System.exit(0);
				}
				LOGGER.info("Running Product Tracker");
				ProductTracker.main(args);
				System.exit(0);
			} else if (searchProduct) {
				// search needs to happen after track, since track also uses a
				// --search argument
				if (showUsage) {
					System.err.println("Usage: ");
					System.err
							.println("    java -jar ProductClient.jar --search [SEARCH ARGUMENTS]");
					System.err.println();
					System.err.println(SearchCLI.getUsage());
					System.exit(0);
				}
				LOGGER.info("Running Product Search");
				SearchCLI.main(args);
				System.exit(0);
			} else if (receiveProducts && !showUsage) {
				// start processing
				LOGGER.info("Starting");
				try {
					startup();
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,
							"Exceptions while starting, shutting down", e);
					try {
						// this has been throwing exceptions, move into try
						shutdown();
					} finally {
						// exit no matter what
						System.exit(1);
					}
				}
				LOGGER.info("Started");

				// shutdown threads when control-c is pressed
				// otherwise, would continue running
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						try {
							LOGGER.info("Shutting down");
							shutdown();
							LOGGER.info("Shutdown complete");
						} catch (Exception e) {
							LOGGER.log(Level.WARNING,
									"Exception while shutting down", e);
						}
					}
				});
			} else {
				System.err.println("Usage: ");
				System.err
						.println("    java -jar ProductClient.jar [ARGUMENTS]");
				System.err.println();
				System.err.println(getUsage());
				System.exit(1);
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in main", e);
		}
	}

	/**
	 * @return The list of receivers
	 */
	public List<NotificationReceiver> getReceivers() {
		return receivers;
	}

	/**
	 *
	 * @return The list of listeners
	 */
	public List<NotificationListener> getListeners() {
		return listeners;
	}

	public static String getUsage() {
		StringBuffer buf = new StringBuffer();

		buf.append("[--configFile=FILE]      override the default config file location\n");
		buf.append("                         default is config.ini in CWD\n");
		buf.append("[--help]                 show this message and exit\n");
		buf.append("[--version]              show the version and exit\n");
		buf.append("[--configTest]           load configuration and exit\n");
		buf.append("\n");
		buf.append("[--send]                 create and send a product\n");
		buf.append("                         try --send --help for more information\n");
		buf.append("[--receive]              receive products\n");
		buf.append("[--track]                check or update product status\n");
		buf.append("                         try --track --help for more information\n");
		buf.append("\n");
		buf.append("You must use one of \"--send\", \"--receive\", or \"--track\"\n");
		buf.append("\n");

		return buf.toString();
	}

	@Override
	public String getListenerQueueStatus() {
		StringBuffer buf = new StringBuffer();
		Iterator<NotificationReceiver> iter = receivers.iterator();
		while (iter.hasNext()) {
			NotificationReceiver receiver = iter.next();
			if (receiver instanceof DefaultNotificationReceiver) {
				buf.append(((DefaultNotificationReceiver) receiver)
						.getListenerQueueStatus());
			}
		}
		return buf.toString();
	}

	@Override
	public String getVersion() {
		return RELEASE_VERSION;
	}

	@Override
	public long getMaxMemory() {
		return Runtime.getRuntime().maxMemory();
	}

	@Override
	public long getFreeMemory() {
		return Runtime.getRuntime().freeMemory();
	}

}
