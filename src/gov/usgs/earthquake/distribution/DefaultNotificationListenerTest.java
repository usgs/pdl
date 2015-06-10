package gov.usgs.earthquake.distribution;

import java.io.File;
import java.util.Iterator;
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

public class DefaultNotificationListenerTest {

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

		DefaultNotificationListener listener = new DefaultNotificationListener() {
			@Override
			public void onNotification(final NotificationEvent event)
					throws Exception {
				super.onNotification(event);
				synchronized (syncObject) {
					syncObject.notify();
				}
			}
		};
		listener.setName("listener");
		listener.setNotificationIndex(new JDBCNotificationIndex(senderIndex
				.getCanonicalPath()));
		listener.setCleanupInterval(900000L);
		listener.startup();

		SocketProductReceiver receiver = new SocketProductReceiver();
		receiver.setName("receiver");
		receiver.setNotificationIndex(new JDBCNotificationIndex(receiverIndex
				.getCanonicalPath()));
		receiver.setProductStorage(new FileProductStorage(receiverStorage));
		receiver.setProductStorageMaxAge(900000L);
		receiver.addNotificationListener(listener);
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

		// 3) remove from listener index and resend
		Iterator<Notification> iter = listener.getNotificationIndex()
				.findNotifications(product.getId()).iterator();
		while (iter.hasNext()) {
			listener.getNotificationIndex().removeNotification(iter.next());
		}
		receiver.receiveNotification(notification);
		synchronized (syncObject) {
			syncObject.wait();
		}

		// done, now cleanup
		receiver.shutdown();
		listener.shutdown();

		// cleanup

		senderIndex.delete();
		receiverIndex.delete();
		FileUtils.deleteTree(senderStorage);
		FileUtils.deleteTree(polldir);
		FileUtils.deleteTree(receiverStorage);
	}

}
