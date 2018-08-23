package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.cube.CubeMessage;
import gov.usgs.earthquake.eids.LegacyConverter;
import gov.usgs.earthquake.eidsutil.EIDSClient;
import gov.usgs.earthquake.product.Product;

import java.io.File;

/**
 * An example of an embedded PDL client.
 * 
 * Creates a notification receiver, which store it's information in a specified
 * directory. Listeners can be added to this receiver before its startup()
 * method is called, which starts the distribution process.
 */
public class EmbeddedPDLClient {

	/** name for embedded receiver, appears in log files. */
	public static final String EMBEDDED_NAME = "embeddedPDL";
	/** name for eids tracking file, in data directory. */
	public static final String EMBEDDED_TRACKING_FILE = "receiver_tracking.dat";
	/** name for notification index file, in data directory. */
	public static final String EMBEDDED_INDEX_FILE = "receiver_index.db";
	/** name for receiver storage directory, in data directory. */
	public static final String EMBEDDED_STORAGE_DIRECTORY = "receiver_storage";

	/** The notification receiver. */
	private EIDSNotificationReceiver eidsReceiver;

	/**
	 * Construct an embedded PDL client.
	 * 
	 * @param dataDirectory
	 *            directory where receiver files are stored.
	 * @param serverHost
	 *            PDL hub hostname.
	 * @param serverPort
	 *            PDL hub port.
	 * @param alternateServersList
	 *            comma separated list of "hostname:port" alternate pdl hubs.
	 * @throws Exception
	 */
	public EmbeddedPDLClient(final File dataDirectory, final String serverHost,
			final Integer serverPort, final String alternateServersList)
			throws Exception {
		EIDSClient client = new EIDSClient();
		client.setServerHost(serverHost);
		client.setServerPort(serverPort);
		client.setAlternateServersList(alternateServersList);
		client.setTrackingFileName(new File(dataDirectory,
				EMBEDDED_TRACKING_FILE).getCanonicalPath());

		
		eidsReceiver = new EIDSNotificationReceiver();
		eidsReceiver.setName(EMBEDDED_NAME);
		eidsReceiver.setNotificationIndex(new JDBCNotificationIndex(new File(
				dataDirectory, EMBEDDED_INDEX_FILE).getCanonicalPath()));
		eidsReceiver.setProductStorage(new FileProductStorage(new File(
				dataDirectory, EMBEDDED_STORAGE_DIRECTORY)));
		// default these to 15 minutes
		eidsReceiver.setProductStorageMaxAge(900000L);
		eidsReceiver.setReceiverCleanupInterval(900000L);
		eidsReceiver.setClient(client);
		client.addListener(eidsReceiver);
	}

	/**
	 * Get the embedded EIDSNotificationReceiver object for further
	 * configuration, adding/removing listeners, and starting/stopping
	 * distribution.
	 * 
	 * @return the embedded EIDSNotificationReceiver object.
	 */
	public EIDSNotificationReceiver getReceiver() {
		return eidsReceiver;
	}

	/**
	 * Example main method that uses the EmbeddedPDLClient.
	 * 
	 * @param args
	 *            not used.
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		// disable product tracker messages
		ProductTracker.setTrackerEnabled(false);

		// client for production hub
		File dataDirectory = new File("embeddedStorage");
		String hostname = "prod01-pdl01.cr.usgs.gov";
		Integer port = 39977;
		String alternateServers = "prod02-pdl01.wr.usgs.gov:39977";

		// create embedded client
		final EmbeddedPDLClient client = new EmbeddedPDLClient(dataDirectory,
				hostname, port, alternateServers);

		// create a listener that tries to convert messages to cube
		final DefaultNotificationListener listener = new DefaultNotificationListener() {
			// convert a product to a cube message, if possible
			private final LegacyConverter converter = LegacyConverter
					.cubeConverter();

			@Override
			public void onProduct(final Product product) {
				System.err.println("Processing product "
						+ product.getId().toString());
				try {
					byte[] cubeBytes = converter.convert(product);
					if (cubeBytes != null) {
						CubeMessage cubeMessage = CubeMessage.parse(new String(
								cubeBytes));
						// CubeMessage instanceof CubeEvent
						// or CubeMessage instanceof CubeDelete
						System.err.println(cubeMessage.toCUBE());
					}
				} catch (Exception e) {
					// ignore
				}
			}
		};
		// only listen for origin messages
		listener.getIncludeTypes().add("origin");
		// name appears in log messages
		listener.setName("embeddedListener");
		// add listener index for more reliable notification across restarts
		listener.setNotificationIndex(new JDBCNotificationIndex(new File(
				dataDirectory, "embedded_listener_index.db").getCanonicalPath()));

		// add listener to receiver
		client.getReceiver().addNotificationListener(listener);

		// start
		listener.startup();
		client.getReceiver().startup();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					client.getReceiver().shutdown();
					listener.shutdown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
