package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.eidsutil.EIDSClient;

import java.io.File;
import java.net.URL;

public class Factory {

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
