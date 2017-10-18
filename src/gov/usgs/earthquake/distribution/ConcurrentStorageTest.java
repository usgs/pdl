package gov.usgs.earthquake.distribution;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.earthquake.product.io.BinaryProductHandler;
import gov.usgs.earthquake.product.io.BinaryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This test is designed to recreate the conditions on a hub when a product is
 * being received over a socket, and notification at the same time.
 */
public class ConcurrentStorageTest {

	public static final String SHAKEMAP_PRODUCT_PATH = "etc/test_products/usa00040xz/us_shakemap_usa00040xz_1287260900624.bin";
	private Product SHAKEMAP;

	/** EIDS receiver to receive URL notification and attempt download. */
	private DefaultNotificationReceiver eidsReceiver;
	/** Socket receiver to receive product over socket. */
	private SocketProductReceiver socketReceiver;

	/** Shared receiver storage. */
	private FileProductStorage receiverStorage;
	/** Shared receiver index. */
	private JDBCNotificationIndex receiverIndex;

	/** Listener to request product from receivers. */
	private TestingNotificationListener listener;

	private static final Object listenerSync = new Object();

	@Before
	public void setup() throws Exception {
		// turn off tracking during test
		ProductTracker.setTrackerEnabled(false);

		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		// handler.setLevel(Level.FINEST);
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINEST);

		SHAKEMAP = ObjectProductHandler.getProduct(new BinaryProductSource(
				StreamUtils.getInputStream(new File(SHAKEMAP_PRODUCT_PATH))));
		// resign using a key we can verify against...
		SHAKEMAP.sign(ProductTest.SIGNATURE_KEY_PAIR.getPrivate());

		receiverStorage = new FileProductStorage();
		// configure receiver to verify signatures
		receiverStorage.setRejectInvalidSignatures(true);
		receiverStorage.setTestSignatures(true);
		// add signature key to keychain so it can successfully verify

		ProductKey signatureKey = new ProductKey();
		signatureKey.setKey(ProductTest.SIGNATURE_KEY_PAIR.getPublic());
		signatureKey.getSources().add(SHAKEMAP.getId().getSource());
		signatureKey.getTypes().add(SHAKEMAP.getId().getType());
		ProductKeyChain keychain = new ProductKeyChain();
		keychain.getKeychain().add(signatureKey);
		receiverStorage.setKeychain(keychain);

		receiverIndex = new JDBCNotificationIndex();

		listener = new TestingNotificationListener();

		eidsReceiver = new DefaultNotificationReceiver();
		eidsReceiver.setNotificationIndex(receiverIndex);
		eidsReceiver.setProductStorage(receiverStorage);
		eidsReceiver.addNotificationListener(listener);
		eidsReceiver.startup();

		socketReceiver = new SocketProductReceiver();
		socketReceiver.setNotificationIndex(receiverIndex);
		socketReceiver.setProductStorage(receiverStorage);
		socketReceiver.addNotificationListener(listener);
		socketReceiver.startup();
	}

	@After
	public void teardown() throws Exception {
		eidsReceiver.shutdown();
		socketReceiver.shutdown();
	}

	@Test
	public void shakemapSocketFirst() throws Exception {
		deliverProductSocketFirst(SHAKEMAP);
	}

	@Test
	public void shakemapNotificationFirst() throws Exception {
		deliverNotificationFirst(SHAKEMAP);
	}

	public void deliverProductSocketFirst(final Product product)
			throws Exception {
		final ProductId id = product.getId();

		// guarantee test pre-conditions
		listener.onProduct(null);
		receiverStorage.removeProduct(id);
		Iterator<Notification> existingNotifications = receiverIndex
				.findNotifications(id).iterator();
		while (existingNotifications.hasNext()) {
			receiverIndex.removeNotification(existingNotifications.next());
		}

		final ProductWebServer webServer = new ProductWebServer(product, 9999);
		final URL webURL = new URL(
				"http://localhost:9999/somepath/doesnt/matter");

		final SendViaNotification viaNotification = new SendViaNotification(
				eidsReceiver, product, webURL, 100);
		final SendViaSocket viaSocket = new SendViaSocket(product, 50);

		webServer.start();
		viaSocket.start();
		viaNotification.start();

		viaSocket.join();
		viaNotification.join();
		webServer.getServerSocket().close();
		webServer.join();

		Assert.assertNotNull("Listener received product",
				listener.getLastProduct());
		Assert.assertTrue("Storage has product", receiverStorage.hasProduct(id));
		Assert.assertTrue("Listener received correct product", listener
				.getLastProduct().getId().equals(id));

		Assert.assertNull("No notification exceptions",
				viaNotification.getException());
		Assert.assertNull("No socket exceptions", viaSocket.getException());
	}

	public void deliverNotificationFirst(final Product product)
			throws Exception {
		final ProductId id = product.getId();

		// guarantee test pre-conditions
		listener.onProduct(null);
		receiverStorage.removeProduct(id);
		Iterator<Notification> existingNotifications = receiverIndex
				.findNotifications(id).iterator();
		while (existingNotifications.hasNext()) {
			receiverIndex.removeNotification(existingNotifications.next());
		}

		final ProductWebServer webServer = new ProductWebServer(product, 9999);
		final URL webURL = new URL(
				"http://localhost:9999/somepath/doesnt/matter");

		final SendViaNotification viaNotification = new SendViaNotification(
				eidsReceiver, product, webURL, 50);
		final SendViaSocket viaSocket = new SendViaSocket(product, 100);

		webServer.start();
		viaNotification.start();
		viaSocket.start();

		viaNotification.join();
		viaSocket.join();
		webServer.getServerSocket().close();
		webServer.join();

		Assert.assertNotNull("Listener received product",
				listener.getLastProduct());
		Assert.assertTrue("Storage has product", receiverStorage.hasProduct(id));
		Assert.assertTrue("Listener received correct product", listener
				.getLastProduct().getId().equals(id));

		Assert.assertNull("No notification exceptions",
				viaNotification.getException());
		Assert.assertNull("No socket exceptions", viaSocket.getException());
	}

	private static class SendViaNotification extends Thread {
		private NotificationReceiver receiver;
		private URLNotification notification;
		private long sleep;
		private Exception e;

		public SendViaNotification(final NotificationReceiver receiver,
				final Product product, final URL productURL, final long sleep) {
			this.receiver = receiver;
			this.notification = new URLNotification(product.getId(), new Date(
					new Date().getTime() + 600000), product.getTrackerURL(),
					productURL);
			this.sleep = sleep;
		}

		public void run() {
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException ignore) {
			}

			try {
				receiver.receiveNotification(notification);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Assert.fail("Exception receiving notification");
			}
		}

		public Exception getException() {
			return this.e;
		}
	}

	private static class SendViaSocket extends Thread {
		private Product product;
		private long sleep;
		private Exception e;

		public SendViaSocket(final Product product, final long sleep) {
			this.product = product;
			this.sleep = sleep;
		}

		public void run() {
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException ignore) {
			}

			SocketProductSender sender = new SocketProductSender("localhost",
					Integer.parseInt(SocketProductSender.DEFAULT_SENDER_PORT));
			sender.setBinaryFormat(true);
			sender.setEnableDeflate(false);
			try {
				sender.sendProduct(product);
			} catch (Exception e) {
				e.printStackTrace();
				// TODO Auto-generated catch block
				this.e = e;
			}
		}

		public Exception getException() {
			return this.e;
		}
	}

	/**
	 * NotificationListener used for testing.
	 *
	 * Saves most recently received product.
	 */
	private static class TestingNotificationListener extends
			DefaultNotificationListener {
		private Product lastProduct = null;

		public Product getLastProduct() throws InterruptedException {
			synchronized (listenerSync) {
				if (this.lastProduct == null) {
					listenerSync.wait();
				}
			}
			return this.lastProduct;
		}

		@Override
		public void onProduct(final Product product) throws Exception {
			synchronized (listenerSync) {
				lastProduct = product;
				listenerSync.notify();
			}
		}
	}

	/**
	 * Fake web server used to simulate downloading product from web server.
	 */
	private static class ProductWebServer extends Thread {
		private final ServerSocket serverSocket;
		private byte[] data;

		ProductWebServer(final Product product, final int port)
				throws Exception {
			serverSocket = new ServerSocket(port);

			// generate xml in advance
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			new ObjectProductSource(product).streamTo(new BinaryProductHandler(
					baos));
			data = baos.toByteArray();
		}

		public ServerSocket getServerSocket() {
			return serverSocket;
		}

		public void run() {
			try {
				while (true) {
					Socket socket = serverSocket.accept();
					if (socket != null) {
						OutputStream out = socket.getOutputStream();
						out.write("HTTP/1.0 200 OK\r\n".getBytes());
						out.write(("Content-length: " + data.length + "\r\n")
								.getBytes());
						out.write("\r\n".getBytes());

						Thread.sleep(100);

						out.write(data);
						socket.shutdownOutput();
						StreamUtils.closeStream(out);
						socket.close();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
