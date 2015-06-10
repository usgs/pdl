package gov.usgs.earthquake.distribution;

import java.io.File;
import java.net.URL;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Test;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.util.FileUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

public class EIDSNotificationSenderTest {

	@Test
	public void testDuplicateHandling() throws Exception {
		LogManager.getLogManager().reset();
		Logger rootLogger = Logger.getLogger("");
		rootLogger.setLevel(Level.FINER);
		ConsoleHandler console = new ConsoleHandler();
		console.setFormatter(new SimpleLogFormatter());
		console.setLevel(Level.FINER);
		rootLogger.addHandler(console);
		ProductTracker.setTrackerEnabled(false);

		// setup
		File senderIndex = new File("testSenderIndex.db");
		File senderStorage = new File("testSenderStorage");
		File polldir = new File("testSenderPolldir");
		File receiverStorage = new File("testReceiverStorage");
		File receiverIndex = new File("testReceiverIndex.db");

		senderIndex.delete();
		receiverIndex.delete();
		FileUtils.deleteTree(senderStorage);
		FileUtils.deleteTree(polldir);
		FileUtils.deleteTree(receiverStorage);

		final Object syncObject = new Object();

		EIDSNotificationSender sender = new EIDSNotificationSender() {
			@Override
			public void onNotification(final NotificationEvent event)
					throws Exception {
				super.onNotification(event);
				synchronized (syncObject) {
					syncObject.notify();
				}
			}
		};
		sender.setName("sender");
		sender.setNotificationIndex(new JDBCNotificationIndex(senderIndex
				.getCanonicalPath()));
		sender.setProductStorage(new URLProductStorage(senderStorage, new URL(
				"http://localhost/")));
		sender.setCleanupInterval(900000L);
		sender.setProductStorageMaxAge(1800000L);
		sender.setServerPolldir(polldir);
		sender.startup();

		SocketProductReceiver receiver = new SocketProductReceiver();
		receiver.setName("receiver");
		receiver.setNotificationIndex(new JDBCNotificationIndex(receiverIndex
				.getCanonicalPath()));
		receiver.setProductStorage(new FileProductStorage(receiverStorage));
		receiver.setProductStorageMaxAge(900000L);
		receiver.addNotificationListener(sender);
		receiver.startup();

		// test
		Product product = new ProductTest().getProduct();

		// 1) send product
		Notification notification = receiver
				.storeProductSource(new ObjectProductSource(product));
		receiver.receiveNotification(notification);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// 2) resend product
		receiver.receiveNotification(notification);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// 3) remove from sender storage and resend
		sender.getProductStorage().removeProduct(product.getId());
		receiver.receiveNotification(notification);
		synchronized (syncObject) {
			syncObject.wait();
		}

		receiver.shutdown();
		sender.shutdown();

		// cleanup
		senderIndex.delete();
		receiverIndex.delete();
		FileUtils.deleteTree(senderStorage);
		FileUtils.deleteTree(polldir);
		FileUtils.deleteTree(receiverStorage);
	}

}
