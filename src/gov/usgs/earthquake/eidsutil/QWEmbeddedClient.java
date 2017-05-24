/*
 * QWEmbeddedClient
 */
package gov.usgs.earthquake.eidsutil;

import com.isti.quakewatch.clientbase.QWTrackingClient;
import com.isti.quakewatch.util.QWConnectionMgr;
import com.isti.util.CfgProperties;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

/**
 * An EIDS client that is a java event source.
 */
public class QWEmbeddedClient extends QWTrackingClient implements EIDSListener {

	/** Version string for program. */
	public static final String PROGRAM_VERSION = "0.2";

	/** Name string for program. */
	public static final String PROGRAM_NAME = "QWEmbeddedClient";

	/** Default server host. */
	public static final String DEFAULT_SERVER_HOST = "eids1.cr.usgs.gov";

	/** Default server port number. */
	public static final Integer DEFAULT_SERVER_PORT = 39977;

	/** Default maxServerEventAgeDays parameter. */
	public static final Double DEFAULT_MAX_SERVER_EVENT_AGE_DAYS = 3.0;

	/** Default Tracking filename. */
	public static final String DEFAULT_TRACKING_FILE_NAME = "EIDSClient_tracking.dat";

	/** Server host name. */
	private String serverHost;

	/** Server port number. */
	private Integer serverPort;

	/** Comma delimited list of host:port s. */
	private String alternateServersList;

	/** The decimal age in days. */
	private Double maxServerEventAgeDays;

	/** Tracking file for EIDS client. */
	private String trackingFileName;

	/** An object that "processes" messages, by passing them up to this object. */
	private QWEmbeddedMsgProcessor processor = new QWEmbeddedMsgProcessor(this);

	/** Listeners to notify when a message is received. */
	private List<EIDSListener> listeners = new LinkedList<EIDSListener>();

	/** Whether this has been shutdown already. */
	private boolean isShutdown = false;

	/** Console log level. */
	private String consoleLogLevel = "Info";

	public QWEmbeddedClient() {
		this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
	}

	/**
	 * Construct an EIDSClient using only server host and port.
	 * 
	 * Calls other constructor with null values for other parameters.
	 * 
	 * @param serverHost
	 * @param serverPort
	 */
	public QWEmbeddedClient(final String serverHost, final Integer serverPort) {
		this(serverHost, serverPort, "");
	}

	/**
	 * Construct an EIDSClient using serverHost, serverPort, and
	 * alternateServersList.
	 * 
	 * @param serverHost
	 * @param serverPort
	 * @param alternateServersList
	 */
	public QWEmbeddedClient(final String serverHost, final Integer serverPort,
			final String alternateServersList) {
		this(serverHost, serverPort, alternateServersList,
				DEFAULT_MAX_SERVER_EVENT_AGE_DAYS, DEFAULT_TRACKING_FILE_NAME);
	}

	/**
	 * Constructor with all options.
	 * 
	 * @param serverHost
	 *            the eids server host or ip address.
	 * @param serverPort
	 *            the eids server port.
	 * @param alternateServersList
	 *            a comma delimited list of host:port that are used when unable
	 *            to connect to the primary serverHost and serverPort.
	 * @param maxServerEventAgeDays
	 *            number of days worth of messages to retrieve on first connect.
	 * @param trackingFileName
	 *            location where tracking file is stored. This file is used to
	 *            track which messages have been received.
	 */
	public QWEmbeddedClient(final String serverHost, final Integer serverPort,
			final String alternateServersList,
			final Double maxServerEventAgeDays, final String trackingFileName) {
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		this.alternateServersList = alternateServersList;
		this.maxServerEventAgeDays = maxServerEventAgeDays;
		this.trackingFileName = trackingFileName;
	}

	/**
	 * 
	 */
	public void setupConfiguration(CfgProperties userPropsObj,
			Object connGroupSelObj, Object logGroupSelObj,
			boolean addPrependDeclFlag) {
		super.setupConfiguration(userPropsObj, connGroupSelObj, logGroupSelObj,
				addPrependDeclFlag);
		CfgProperties props = getConnPropsObj().getConfigProps();
		props.get("serverHostAddress").setValue(serverHost);
		props.get("serverPortNumber").setValue(serverPort);
		props.get("maxServerEventAgeDays").setValue(maxServerEventAgeDays);

		if (alternateServersList != null) {
			props.get("alternateServersList").setValue(alternateServersList);
			props.get("altServersEnabledFlag").setValue(true);
			props.get("keepDefaultAltServersFlag").setValue(true);
		} else {
			props.get("alternateServersList").setValue("");
			props.get("altServersEnabledFlag").setValue(false);
			props.get("keepDefaultAltServersFlag").setValue(true);
		}

		props.get("trackingFileName").setValue(trackingFileName);
		props.get("clientConsoleLevel").setValue(consoleLogLevel);

		// disable separate log file
		props.get("clientLogFileName").setValue("");
		props.get("clientLogFileLevel").setValue("Debug");
	}

	/**
	 * Runs the client.
	 * 
	 * Any listeners should be added before calling this method.
	 */
	public void startup() {
		if (isShutdown) {
			throw new IllegalArgumentException(
					"Cannot restart a QWEmbeddedClient after it has shutdown.");
		}
		// initialize client
		setupConfiguration(null, null);
		// set information for this client
		setupClientInfoProps(PROGRAM_NAME, PROGRAM_VERSION);
		// must be called before runClient
		processConfiguration(null, null, null, false, null, false, null);
		runClient(processor);
	}

	/**
	 * Shuts down a running client.
	 * 
	 * Does not call system.exit.
	 */
	public void shutdown() {
		// shutdown the client, but doesn't call System.exit()
		stopClient();
		isShutdown = true;

		// this is supposedly called by stopClient, but adding in because memory
		// not being freed as expected
		try {
			QWConnectionMgr manager = this.getConnManagerObj();
			manager.closeConnection();
			manager.getMsgHandlerObj().clearWaitingMsgsQueueTable();
		} catch (Exception e) {
			System.err.println("Exception shutting down QWEmbeddedClient");
			e.printStackTrace();
		}
	}

	/**
	 * Add a listener.
	 * 
	 * @param listener
	 *            the listener to add.
	 */
	public synchronized void addListener(final EIDSListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Remove a listener.
	 * 
	 * @param listener
	 *            the listener to remove.
	 */
	public synchronized void removeListener(final EIDSListener listener) {
		this.listeners.remove(listener);
	}

	public void onEIDSMessage(EIDSMessageEvent event) {
		// iterate over a copy of the listeners list
		Iterator<EIDSListener> iter = new LinkedList<EIDSListener>(listeners)
				.iterator();
		while (iter.hasNext()) {
			iter.next().onEIDSMessage(event);
		}
	}

	/**
	 * @return the serverHost
	 */
	public String getServerHost() {
		return serverHost;
	}

	/**
	 * @param serverHost
	 *            the serverHost to set
	 */
	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	/**
	 * @return the serverPort
	 */
	public Integer getServerPort() {
		return serverPort;
	}

	/**
	 * @param serverPort
	 *            the serverPort to set
	 */
	public void setServerPort(Integer serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * @return the alternateServersList
	 */
	public String getAlternateServersList() {
		return alternateServersList;
	}

	/**
	 * @param alternateServersList
	 *            the alternateServersList to set
	 */
	public void setAlternateServersList(String alternateServersList) {
		this.alternateServersList = alternateServersList;
	}

	/**
	 * @return the maxServerEventAgeDays
	 */
	public Double getMaxServerEventAgeDays() {
		return maxServerEventAgeDays;
	}

	/**
	 * @param maxServerEventAgeDays
	 *            the maxServerEventAgeDays to set
	 */
	public void setMaxServerEventAgeDays(Double maxServerEventAgeDays) {
		this.maxServerEventAgeDays = maxServerEventAgeDays;
	}

	/**
	 * @return the trackingFileName
	 */
	public String getTrackingFileName() {
		return trackingFileName;
	}

	/**
	 * @param trackingFileName
	 *            the trackingFileName to set
	 */
	public void setTrackingFileName(String trackingFileName) {
		this.trackingFileName = trackingFileName;
	}

	public String getConsoleLogLevel() {
		return consoleLogLevel;
	}

	public void setConsoleLogLevel(String consoleLogLevel) {
		this.consoleLogLevel = consoleLogLevel;
	}

	/**
	 * A method to test the EIDSClient.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		EIDSListener listener = new EIDSListener() {
			public void onEIDSMessage(EIDSMessageEvent event) {
				System.err.println(event.getServerHost() + " "
						+ event.getServerSequence());
				System.err.println("\t" + event.getMessageSource() + " "
						+ event.getMessageSequence());
				System.err.println("\t" + event.getRootNamespace() + ":"
						+ event.getRootElement());
			}
		};

		QWEmbeddedClient client = new QWEmbeddedClient("eids1.cr.usgs.gov",
				39977);
		client.addListener(listener);

		System.err.println("Starting client");
		// run for 2 seconds
		client.startup();
		Thread.sleep(5000);

		System.err.println("Stopping client");
		// stop for 2 seconds
		client.shutdown();

		// start a new client
		client = new QWEmbeddedClient("eids1.cr.usgs.gov", 39977);
		client.addListener(listener);

		System.err.println("Starting client");
		// run for 2 seconds
		client.startup();
		Thread.sleep(5000);

		System.err.println("Stopping client");
		// stop for 2 seconds
		client.shutdown();
	}

}
