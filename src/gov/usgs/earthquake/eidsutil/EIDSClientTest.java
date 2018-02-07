package gov.usgs.earthquake.eidsutil;

import org.junit.Test;

public class EIDSClientTest {

	private Object syncObject = new Object();

	@Test
	public void testConnection() throws InterruptedException {
		final EIDSClient client = new EIDSClient("prod01-pdl01.cr.usgs.gov", 39977);
		client.addListener(new EIDSListener() {
			@Override
			public void onEIDSMessage(final EIDSMessageEvent event) {
				// a message was received
				System.err.println("Received message " + event.getServerHost()
						+ ":" + event.getServerSequence() + ": "
						+ event.getMessage());
				client.shutdown();
				synchronized (syncObject) {
					syncObject.notify();
				}
			}
		});
		client.startup();

		synchronized (syncObject) {
			// wait for a message to be received
			syncObject.wait();
		}
	}

}
