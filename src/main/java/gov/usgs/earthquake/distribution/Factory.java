package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.eidsutil.EIDSClient;

import java.io.File;
import java.net.URL;

public class Factory {

	/**
	 * Creates EIDS Client using given params
	 * @param serverHost host
	 * @param serverPort port
	 * @param alternateServers for list of alternate servers
	 * @param trackingFileName tracking file name
	 * @return EIDSClient
	 */
	public EIDSClient createEIDSClient(final String serverHost,
			final Integer serverPort, final String alternateServers,
			final String trackingFileName) {
		EIDSClient client = new EIDSClient();

		client.setServerHost(serverHost);
		client.setServerPort(serverPort);
		client.setAlternateServersList(alternateServers);
		client.setTrackingFileName(trackingFileName);

		return client;
	}

	/**
	 * Creates EIDS Notification Receiver
	 * @param serverList serverlist
	 * @param receiverStorageDirectory file of storage directory
	 * @param receiverIndexFile file of receiver index
	 * @param client EIDSClient
	 * @return new EIDSNotificationReceiver
	 * @throws Exception if error occurs
	 */
	public EIDSNotificationReceiver createEIDSNotificationReceiver(
			final String serverList, final File receiverStorageDirectory,
			final File receiverIndexFile, final EIDSClient client)
			throws Exception {
		EIDSNotificationReceiver receiver = new EIDSNotificationReceiver();

		receiver.setName("eids");

		receiver.setProductStorage(new FileProductStorage(
				receiverStorageDirectory));
		receiver.setNotificationIndex(new JDBCNotificationIndex(
				receiverIndexFile.getCanonicalPath()));

		return receiver;
	}

	/**
	 * Create new socket product receiver
	 * @param port int of port
	 * @param numThreads int of threads
	 * @param receiverStorageDirectory file of storage directory
	 * @param receiverIndexFile file of receiver index
	 * @return new SocketProductReceiver
	 * @throws Exception if error occurs
	 */
	public SocketProductReceiver createSocketProductReceiver(final int port,
			final int numThreads, final File receiverStorageDirectory,
			final File receiverIndexFile) throws Exception {
		SocketProductReceiver receiver = new SocketProductReceiver();

		receiver.setName("socket");
		receiver.setProductStorage(new FileProductStorage(
				receiverStorageDirectory));
		receiver.setNotificationIndex(new JDBCNotificationIndex(
				receiverIndexFile.getCanonicalPath()));

		return receiver;
	}

	/**
	 * create new EIDS Notification Sender
	 * @param corbaHost String of host
	 * @param corbaPort String of port
	 * @param eidsPolldir file of eidsPoll directory
	 * @param htdocs file of htdocs
	 * @param htdocsURL URL of htdocs
	 * @return new EIDSNotificationSender
	 */
	public EIDSNotificationSender createEIDSNotificationSender(
			final String corbaHost, final String corbaPort,
			final File eidsPolldir, final File htdocs, final URL htdocsURL) {
		EIDSNotificationSender sender = new EIDSNotificationSender();

		sender.setName("eids");
		sender.setServerHost(corbaHost);
		sender.setServerPort(corbaPort);
		sender.setServerPolldir(eidsPolldir);
		sender.setProductStorage(new URLProductStorage(htdocs, htdocsURL));

		return sender;
	}

}
