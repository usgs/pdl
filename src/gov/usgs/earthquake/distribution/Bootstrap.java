/*
 * Bootstrap
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.util.Config;
import gov.usgs.util.Configurable;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.logging.LoggingOutputStream;
import gov.usgs.util.logging.SimpleLogFileHandler;
import gov.usgs.util.logging.SimpleLogFormatter;
import gov.usgs.util.logging.StdOutErrLevel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;

/**
 * Bootstrap is a class used to start an application.
 *
 * It loads a configuration file, sets up initial logging, and starts a
 * configurable main method.
 *
 * @author jmfee
 *
 */
public class Bootstrap {

	/**
	 * logging is noisy without this.
	 */
	static {
		Logger.getLogger("com.sun.xml.bind").setLevel(Level.INFO);
		Logger.getLogger("com.sun.activation").setLevel(Level.INFO);
		Logger.getLogger("javax.xml.bind").setLevel(Level.INFO);
		Logger.getLogger("sun.awt.X11.timeoutTask.XToolkit").setLevel(Level.INFO);
		Logger.getLogger("com.sun.xml.bind.v2.runtime.reflect.opt.OptimizedAccessorFactory").setLevel(Level.INFO);
	}

	// public static

	/** Default JAR config path. */
	public static final String JAR_CONFIGFILE = "etc/config/config.ini";

	/** Argument for config file. */
	public static final String CONFIGFILE_ARGUMENT = "--configFile=";
	/** Default config file. */
	public static final String DEFAULT_CONFIGFILE = "config.ini";

	/** Whether to test config only. */
	public static final String CONFIG_TEST_ARGUMENT = "--configTest";

	/** Property for log format. */
	public static final String LOGFORMAT_PROPERTY_NAME = "logformat";
	/** log format value for "pdl" format */
	public static final String LOGFORMAT_PDL = "pdl";
	/** log format value for java "simple" format */
	public static final String LOGFORMAT_SIMPLE = "simple";
	/** log format value for java "xml" format */
	public static final String LOGFORMAT_XML = "xml";
	/** Default log format is "simple". */
	public static final String DEFAULT_LOGFORMAT = LOGFORMAT_PDL;

	/** Property for log level. */
	public static final String LOGLEVEL_PROPERTY_NAME = "loglevel";
	/** Default log level is "INFO". */
	public static final String DEFAULT_LOGLEVEL = "INFO";

	/** Property for log directory. */
	public static final String LOGDIRECTORY_PROPERTY_NAME = "logdirectory";
	/** Default log directory is "log". */
	public static final String DEFAULT_LOGDIRECTORY = "log";

	/** Property for log file pattern. */
	public static final String LOGFILE_PROPERTY_NAME = "logfile";
	/** Default log file pattern is "yyyyMMdd'.log'". */
	public static final String DEFAULT_LOGFILE = "yyyyMMdd'.log'";

	/** Property for console redirect. */
	public static final String CONSOLEREDIRECT_PROPERTY_NAME = "redirectconsole";
	/** Default console redirect value is "false" (don't redirect). */
	public static final String DEFAULT_CONSOLEREDIRECT = "false";

	/** Property used to disable tracker updates. */
	public static final String ENABLE_TRACKER_PROPERTY_NAME = "enableTracker";

	/** Argument for mainclass. */
	public static final String MAINCLASS_ARGUMENT = "--mainclass=";
	/** Property for mainclass. */
	public static final String MAINCLASS_PROPERTY_NAME = "mainclass";
	/** Default mainclass is "gov.usgs.earthquake.distribution.ProductClient. */
	public static final String DEFAULT_MAINCLASS = "gov.usgs.earthquake.distribution.ProductClient";

	public static final String VERSION_ARGUMENT = "--version";

	// private static

	/** Private logging object. */
	private static final Logger LOGGER = Logger.getLogger(Bootstrap.class
			.getName());

	// constructors

	public Bootstrap() {
	}

	// members

	/**
	 * Read configuration from inside jar file, and configFile.
	 *
	 * @param configFile
	 *            config file to load.
	 * @throws IOException
	 */
	public Config loadConfig(final File configFile) throws IOException {
		Config config = new Config();

		// load defaults from jar file
		InputStream in = Bootstrap.class.getClassLoader().getResourceAsStream(
				JAR_CONFIGFILE);
		if (in != null) {
			try {
				config.load(in);
			} finally {
				StreamUtils.closeStream(in);
			}
		} else {
			LOGGER.config("Jar configuration not found");
		}

		// override settings with a config file
		if (configFile.exists()) {
			LOGGER.config("Loading configuration file "
					+ configFile.getCanonicalPath());

			config = new Config(config);
			in = StreamUtils.getInputStream(configFile);
			try {
				config.load(in);
			} finally {
				StreamUtils.closeStream(in);
			}
		}

		return config;
	}

	public void setupLogging(final Config config) {
		LogManager.getLogManager().reset();

		Level level = Level.parse(config.getProperty(LOGLEVEL_PROPERTY_NAME,
				DEFAULT_LOGLEVEL));
		String logDirectory = config.getProperty(LOGDIRECTORY_PROPERTY_NAME,
				DEFAULT_LOGDIRECTORY);
		LOGGER.config("Logging Level '" + level + "'");
		LOGGER.config("Log directory '" + logDirectory + "'");

		Logger rootLogger = Logger.getLogger("");
		rootLogger.setLevel(level);

		try {
			File logDirectoryFile = new File(logDirectory);
			if (!logDirectoryFile.exists()) {
				LOGGER.fine("Creating log directory");
				if (!logDirectoryFile.mkdirs()) {
					LOGGER.warning("Unable to create log directory");
				}
			}

			// filepattern, maxBytesPerFile, maxFiles, append
			// FileHandler handler = new FileHandler(logFile, 100000, 10, true);
			Handler handler = new SimpleLogFileHandler(logDirectoryFile,
					new SimpleDateFormat(DEFAULT_LOGFILE));
			handler.setLevel(level);
			rootLogger.addHandler(handler);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to create log file handler", e);
		}

		String redirectConsole = config.getProperty(
				CONSOLEREDIRECT_PROPERTY_NAME, DEFAULT_CONSOLEREDIRECT);
		if (!redirectConsole.equals(DEFAULT_CONSOLEREDIRECT)) {
			// default is off, so enable
			System.err.println("Redirecting STDOUT and STDERR to log file");
			System.setOut(new PrintStream(new LoggingOutputStream(Logger
					.getLogger("stdout"), StdOutErrLevel.STDOUT)));
			System.setErr(new PrintStream(new LoggingOutputStream(Logger
					.getLogger("stderr"), StdOutErrLevel.STDERR)));
		} else {
			ConsoleHandler handler = new ConsoleHandler();
			handler.setLevel(level);
			rootLogger.addHandler(handler);
		}

		Formatter formatter;
		String logFormat = config.getProperty(
				LOGFORMAT_PROPERTY_NAME, DEFAULT_LOGFORMAT);
		if (logFormat.equals(LOGFORMAT_SIMPLE)) {
			// built in simple formatter
			formatter = new SimpleFormatter();
		} else if (logFormat.equals(LOGFORMAT_XML)) {
			// built in xml formatter
			formatter = new XMLFormatter();
		} else {
			// pdl style simple formatter
			formatter = new SimpleLogFormatter();
		}
		for (Handler handler : rootLogger.getHandlers()) {
			handler.setFormatter(formatter);
		}
	}

	public static void main(final String[] args) throws Exception {
		StringBuffer argumentList = new StringBuffer();
		boolean configTest = false;

		String className = null;

		// use default config file
		File configFile = new File(DEFAULT_CONFIGFILE);
		for (String arg : args) {
			argumentList.append(arg).append(" ");
			if (arg.startsWith(CONFIGFILE_ARGUMENT)) {
				// unless config file argument provided
				configFile = new File(arg.replace(CONFIGFILE_ARGUMENT, ""));
			} else if (arg.equals(CONFIG_TEST_ARGUMENT)) {
				configTest = true;
			} else if (arg.startsWith(MAINCLASS_ARGUMENT)) {
				className = arg.replace(MAINCLASS_ARGUMENT, "");
			} else if (arg.equals(VERSION_ARGUMENT)) {
				System.err.println("Product Distribution Client");
				System.err.println(ProductClient.RELEASE_VERSION);
				System.exit(0);
			}
		}

		Bootstrap bootstrap = new Bootstrap();

		// load configuration file
		Config config = bootstrap.loadConfig(configFile);

		// set global config object
		Config.setConfig(config);

		// setup logging based on configuration
		bootstrap.setupLogging(config);

		// java and os information
		LOGGER.config("java.vendor = " + System.getProperty("java.vendor"));
		LOGGER.config("java.version = " + System.getProperty("java.version"));
		LOGGER.config("java.home = " + System.getProperty("java.home"));
		LOGGER.config("os.arch = " + System.getProperty("os.arch"));
		LOGGER.config("os.name = " + System.getProperty("os.name"));
		LOGGER.config("os.version = " + System.getProperty("os.version"));
		LOGGER.config("user.dir = " + System.getProperty("user.dir"));
		LOGGER.config("user.name = " + System.getProperty("user.name"));

		// log command line arguments
		LOGGER.fine("Command line arguments: " + argumentList.toString().trim());

		// configure whether tracker updates are sent.
		String enableTrackerProperty = config
				.getProperty(ENABLE_TRACKER_PROPERTY_NAME);
		if (enableTrackerProperty != null) {
			if (enableTrackerProperty.equals("true")) {
				LOGGER.warning("Enabled tracker updates,"
						+ " this is usually not a good idea.");
				ProductTracker.setTrackerEnabled(true);
			}
		}

		// lookup main class
		if (className == null) {
			// no argument specified, check configuration
			className = config.getProperty(MAINCLASS_PROPERTY_NAME,
					DEFAULT_MAINCLASS);
		}

		// invoke main class main(String[] args) method
		LOGGER.config("Loading main class " + className);
		Bootstrappable main = null;
		try {
			main = (Bootstrappable) Class.forName(className)
					.getConstructor().newInstance();
		} catch (ClassCastException cce) {
			LOGGER.log(Level.SEVERE,
					"Main class must implement the Bootstrappable interface",
					cce);
			System.exit(1);
		}

		// use the configurable interface when available
		if (main instanceof Configurable) {
			Configurable configurable = ((Configurable) main);
			configurable.setName("main");
			try {
				configurable.configure(config);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Exception loading configuration ", e);
				System.exit(1);
			}
		}

		// configuration loaded okay
		LOGGER.config("Configuration loaded");
		if (configTest) {
			// exit successfully
			System.exit(0);
		}

		// run main instance
		LOGGER.config("Bootstrap complete, running main class\n");
		try {
			main.run(args);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Main class threw exception, exiting", e);
			System.exit(Bootstrappable.RUN_EXCEPTION_EXIT_CODE);
		}
	}

}
