/*
 * EIDSClient
 */
package gov.usgs.earthquake.eidsutil;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An EIDS client that is a java event source.
 */
public class EIDSClient implements EIDSListener {

	/** Property name for eids server host address. */
	public static final String EIDS_SERVER_HOST_PROPERTY = "serverHost";

	/** Property name for eids server port. */
	public static final String EIDS_SERVER_PORT = "serverPort";

	/** Property name for eids server alternate servers list. */
	public static final String EIDS_ALTERNATE_SERVERS = "alternateServers";

	/** Property name for objects to load and add as EIDSListeners. */
	public static final String EIDS_LISTENERS = "listeners";

	/** Version string for program. */
	public static final String PROGRAM_VERSION = "0.2";

	/** Name string for program. */
	public static final String PROGRAM_NAME = "EIDSClient";

	/** Default server host. */
	public static final String DEFAULT_SERVER_HOST = "eids1.cr.usgs.gov";

	/** Default server port number. */
	public static final Integer DEFAULT_SERVER_PORT = 39977;

	/** Default maxServerEventAgeDays parameter. */
	public static final Double DEFAULT_MAX_SERVER_EVENT_AGE_DAYS = 3.0;

	/** Default Tracking filename. */
	public static final String DEFAULT_TRACKING_FILE_NAME = "EIDSClient_tracking.dat";

	/** Default client restart interval */
	public static final Long DEFAULT_CLIENT_RESTART_INTERVAL = 86400000L;

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

	/** The actual client this object wraps. */
	private QWEmbeddedClient client;

	/** Listeners to notify when a message is received. */
	private List<EIDSListener> listeners = new LinkedList<EIDSListener>();

	/** A timer used to periodically restart the client. */
	private Timer clientRestartTimer;

	/** How often to periodically restart the client, in milliseconds. */
	private Long clientRestartInterval;

	/** Whether we are debugging or not. Affects log level. */
	private boolean debug;

	public EIDSClient() {
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
	public EIDSClient(final String serverHost, final Integer serverPort) {
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
	public EIDSClient(final String serverHost, final Integer serverPort,
			final String alternateServersList) {
		this(serverHost, serverPort, alternateServersList,
				DEFAULT_MAX_SERVER_EVENT_AGE_DAYS, DEFAULT_TRACKING_FILE_NAME,
				DEFAULT_CLIENT_RESTART_INTERVAL);
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
	public EIDSClient(final String serverHost, final Integer serverPort,
			final String alternateServersList,
			final Double maxServerEventAgeDays, final String trackingFileName,
			final Long clientRestartInterval) {
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		this.alternateServersList = alternateServersList;
		this.maxServerEventAgeDays = maxServerEventAgeDays;
		this.trackingFileName = trackingFileName;
		this.clientRestartInterval = clientRestartInterval;
	}

	/**
	 * Runs the client.
	 * 
	 * Any listeners should be added before calling this method.
	 */
	public void startup() {
		// were we already running?
		if (client != null) {
			client.shutdown();
			client = null;
		}

		// start the client
		client = new QWEmbeddedClient(serverHost, serverPort,
				alternateServersList, maxServerEventAgeDays, trackingFileName);
		client.addListener(this);

		if (debug) {
			client.setConsoleLogLevel("Debug5");
		}

		client.startup();

		// if this is the first time through
		if (clientRestartTimer == null) {
			if (clientRestartInterval > 0) {
				clientRestartTimer = new Timer();
				clientRestartTimer.schedule(new TimerTask() {
					public void run() {
						reinitConnection();
					}
				}, clientRestartInterval, // before first execution
						clientRestartInterval // between subsequent executions
						);
			}
		}
	}

	/**
	 * Shuts down a running client.
	 * 
	 * Does not call system.exit.
	 */
	public void shutdown() {
		if (clientRestartTimer != null) {
			clientRestartTimer.cancel();
		}
		if (client != null) {
			client.shutdown();
			client = null;
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

	public Long getClientRestartInterval() {
		return clientRestartInterval;
	}

	public void setClientRestartInterval(Long clientRestartInterval) {
		this.clientRestartInterval = clientRestartInterval;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isDebug() {
		return debug;
	}

	public boolean reinitConnection() {
		if (client != null) {
			return client.getConnManagerObj().reinitConnection();
		}
		return false;
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
				System.err.println(event.getMessage());
			}
		};

		String host = "eids1.cr.usgs.gov";
		int port = 39977;
		boolean debug = false;

		for (String arg : args) {
			if (arg.startsWith("--host=")) {
				host = arg.replace("--host=", "");
			} else if (arg.startsWith("--port=")) {
				port = Integer.parseInt(arg.replace("--port=", ""));
			} else if (arg.equals("--debug")) {
				debug = true;
			}
		}

		EIDSClient client = new EIDSClient(host, port);
		client.setClientRestartInterval(10000L);
		client.setDebug(debug);
		client.addListener(listener);
		client.startup();
	}

}
