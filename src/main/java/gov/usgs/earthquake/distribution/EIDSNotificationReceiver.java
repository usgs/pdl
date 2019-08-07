/*
 * EIDSNotificationReceiver
 */
package gov.usgs.earthquake.distribution;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.eidsutil.EIDSClient;
import gov.usgs.earthquake.eidsutil.EIDSListener;
import gov.usgs.earthquake.eidsutil.EIDSMessageEvent;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;

/**
 * Receive XML notifications using EIDS.
 * 
 * 
 * This class implements the Configurable interface, and has the following
 * options:
 * <dl>
 * <dt>serverHost</dt>
 * <dd>The EIDS Server hostname or IP address.</dd>
 * <dt>serverPort</dt>
 * <dd>The EIDS Server listen port, usually 39977.</dd>
 * <dt>alternateServers</dt>
 * <dd>A comma separated list of HOST:PORT pairs. The EIDSClient will attempt to
 * connect to these servers when unable to connect to the primary
 * serverHost:serverPort.</dd>
 * <dt>trackingFile</dt>
 * <dd>A file name used for tracking connection state. This is used to receive
 * missed messages when (re)connecting.</dd>
 * </dl>
 */
public class EIDSNotificationReceiver extends DefaultNotificationReceiver
		implements EIDSListener {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(EIDSNotificationReceiver.class.getName());

	/** Property name for eids server host address. */
	public static final String EIDS_SERVER_HOST_PROPERTY = "serverHost";

	/** Property name for eids server port. */
	public static final String EIDS_SERVER_PORT = "serverPort";

	/** Property name for eids server alternate servers list. */
	public static final String EIDS_ALTERNATE_SERVERS = "alternateServers";

	/** Property name for eids client tracking file. */
	public static final String EIDS_TRACKING_FILE = "trackingfile";

	/** Property name for eids client log level. */
	public static final String EIDS_DEBUG = "eidsDebug";

	/** Property name for eids max server event age. */
	public static final String EIDS_MAX_EVENT_AGE = "maxServerEventAgeDays";

	/** EIDSClient that receives notifications. */
	private EIDSClient client;

	/**
	 * Implement the EIDSListener interface to process messages from EIDS.
	 * 
	 * Checks to make sure message has Correct namespace and root element before
	 * parsing.
	 */
	public void onEIDSMessage(EIDSMessageEvent event) {
		if (URLNotificationParser.PRODUCT_XML_NAMESPACE.equals(event
				.getRootNamespace())
				&& URLNotificationParser.NOTIFICATION_ELEMENT.equals(event
						.getRootElement())) {
			InputStream in = null;
			try {
				in = StreamUtils.getInputStream(event.getMessage());
				// this is a notification message
				URLNotification notification = URLNotification.parse(in);
				// process the notification
				receiveNotification(notification);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] exception while parsing URLNotification", e);
			} finally {
				StreamUtils.closeStream(in);
			}
		} else {
			LOGGER.info("[" + getName() + "] ignoring message type "
					+ event.getRootNamespace() + ":" + event.getRootElement());
			LOGGER.info("[" + getName() + "] message content: "
					+ event.getMessage());
		}
	}

	public void configure(Config config) throws Exception {
		super.configure(config);
		// configure eids client
		client = new EIDSClient();
		client.addListener(this);

		String serverHost = config.getProperty(EIDS_SERVER_HOST_PROPERTY,
				EIDSClient.DEFAULT_SERVER_HOST);
		LOGGER.config("[" + getName() + "] EIDS server host is '" + serverHost
				+ "'");
		client.setServerHost(serverHost);

		Integer serverPort = Integer.valueOf(config.getProperty(
				EIDS_SERVER_PORT,
				Integer.toString(EIDSClient.DEFAULT_SERVER_PORT)));
		LOGGER.config("[" + getName() + "] EIDS server port is '" + serverPort
				+ "'");
		client.setServerPort(serverPort);

		String alternateServers = config
				.getProperty(EIDS_ALTERNATE_SERVERS, "");
		LOGGER.config("[" + getName() + "] EIDS alternate servers '"
				+ alternateServers + "'");
		client.setAlternateServersList(alternateServers);

		String trackingFile = config.getProperty(EIDS_TRACKING_FILE);
		if (trackingFile != null) {
			LOGGER.config("[" + getName() + "] EIDS tracking file is '"
					+ trackingFile + "'");
			client.setTrackingFileName(trackingFile);
		}

		String debug = config.getProperty(EIDS_DEBUG);
		if (debug != null) {
			LOGGER.config("[" + getName() + "] EIDS debug mode = '" + debug
					+ "'");
			client.setDebug(Boolean.parseBoolean(debug));
		}

		String maxEventAgeString = config.getProperty(EIDS_MAX_EVENT_AGE);
		if (maxEventAgeString != null) {
			LOGGER.config("[" + getName() + "] EIDS max event age is " + maxEventAgeString + " days");
			client.setMaxServerEventAgeDays(Double.parseDouble(maxEventAgeString));
		}
	}

	public void shutdown() throws Exception {
		client.shutdown();
		super.shutdown();
	}

	public void startup() throws Exception {
		super.startup();
		if (client == null) {
			throw new ConfigurationException("[" + getName()
					+ "] EIDS client is not properly configured");
		}
		client.startup();
	}

	public EIDSClient getClient() {
		return client;
	}

	public void setClient(EIDSClient client) {
		this.client = client;
	}

}
