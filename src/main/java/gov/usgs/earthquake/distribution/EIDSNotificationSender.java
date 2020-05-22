/*
 *
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.eidsutil.CorbaSender;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;

import java.io.File;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EIDSNotificationSender extends DefaultNotificationSender {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(EIDSNotificationSender.class.getName());

	/** Property referencing directory where notifications are "sent". */
	public static final String EIDS_POLLDIR_PROPERTY = "serverPolldir";

	/** Default directory where notifications are sent. */
	public static final String EIDS_DEFAULT_POLLDIR = "polldir";

	/** Directory for Polldir send, in case CORBA send fails. */
	private File serverPolldir = null;
	/** CORBA sending object. */
	private CorbaSender corbaSender = null;

	@Override
	protected void sendNotification(final Notification notification) throws Exception {
		boolean sent = false;
		String message = URLNotificationXMLConverter.toXML((URLNotification) notification);

		if (serverHost != null && serverPort != null) {
			try {
				if (corbaSender == null) {
					// try to establish a connection
					corbaSender = new CorbaSender(serverHost, serverPort);
				}
				sent = corbaSender.sendMessage(message);
				if (sent) {
					LOGGER.fine("[" + getName()
							+ "] sent notification to EIDS via CORBA");
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] unable to send notification using CORBA", e);
			}
		}

		if (!sent) {
			// when unable to send directly to corba service
			// send notification via EIDS polldir

			// create a uniqueish filename
			String filename = "out_" + new Date().getTime();
			File outFile = new File(serverPolldir, filename + ".xml");
			while (outFile.exists()) {
				filename = filename += "_1";
				outFile = new File(serverPolldir, filename + ".xml");
			}

			// is this atomic enough?, write then move may be better
			FileUtils.writeFile(outFile, message.getBytes());
			LOGGER.log(Level.INFO, "[" + getName()
					+ "] sent notification to EIDS via " + outFile.getPath());
		}
	}

	public void configure(Config config) throws Exception {
		// let default notification sender configure itself
		super.configure(config);

		serverPolldir = new File(config.getProperty(EIDS_POLLDIR_PROPERTY,
				EIDS_DEFAULT_POLLDIR));
		LOGGER.config("[" + getName() + "] EIDS server polldir '"
				+ serverPolldir + "'");

	}

	@Override
	public void shutdown() throws Exception {
		super.shutdown();

		if (corbaSender != null) {
			try {
				corbaSender.destroy();
			} catch (Exception e) {
				// ignore
			}
			corbaSender = null;
		}
	}

	@Override
	public void startup() throws Exception {
		if (serverHost != null && serverPort != null) {
			try {
				corbaSender = new CorbaSender(serverHost, serverPort);
			} catch (org.omg.CORBA.COMM_FAILURE e) {
				LOGGER.warning("[" + getName()
						+ "] unable to connect to EIDS using CORBA");
				corbaSender = null;
			}
		}

		super.startup();
	}

	public File getServerPolldir() {
		return serverPolldir;
	}

	public void setServerPolldir(File serverPolldir) {
		this.serverPolldir = serverPolldir;
	}

}
