package gov.usgs.earthquake.distribution;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.BinaryProductHandler;
import gov.usgs.earthquake.product.io.IOUtil;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.util.FileUtils;
import gov.usgs.util.SocketAcceptor;
import gov.usgs.util.SocketListenerInterface;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * A test to reproduce the listener backup issue.
 */
public class ListenerBackupTest {

	public static final String SHAKEMAP_PRODUCT_PATH = "etc/test_products/usa00040xz/us_shakemap_usa00040xz_1287260900624.bin";
	public static final String LOSSPAGER_PRODUCT_PATH = "etc/test_products/usa00040xz/us_losspager_usa00040xz_1287260989064.bin";

	public static final File[] TEST_FILES = new File[] {
		new File(SHAKEMAP_PRODUCT_PATH),
		new File(LOSSPAGER_PRODUCT_PATH)
	};

	public static final int PRODUCT_SERVER_PORT = 1234;

	private ProductServer server;
	private SocketAcceptor acceptor;
	private DefaultNotificationReceiver receiver;
	private TestNotificationListener listener1;
	private TestNotificationListener listener2;

	private File receiverStorage = new File("receiverStorage");
	private File listener1Storage = new File("listener1Storage");
	private File listener2Storage = new File("listener2Storage");

	@Before
	public void setup() throws Exception {
		ProductTracker.setTrackerEnabled(false);

		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINEST);

		// set up receiver
		receiver = new DefaultNotificationReceiver();
		receiver.setNotificationIndex(new JDBCNotificationIndex());
		receiver.setProductStorage(new FileProductStorage(receiverStorage));
		listener1 = new TestNotificationListener(listener1Storage);
		receiver.addNotificationListener(listener1);
		listener2 = new TestNotificationListener(listener2Storage);
		receiver.addNotificationListener(listener2);
		receiver.startup();

		server = new ProductServer(null);
		ServerSocket serverSocket = new ServerSocket(1234);
		acceptor = new SocketAcceptor(serverSocket, server);
		acceptor.start();
	}

	@After
	public void teardown() throws Exception {
		receiver.shutdown();
		acceptor.stop();

		// remove receiver index and storage
		new File(JDBCNotificationIndex.JDBC_FILE_PROPERTY).delete();
		FileUtils.deleteTree(receiverStorage);
		FileUtils.deleteTree(listener1Storage);
		FileUtils.deleteTree(listener2Storage);
	}

	@Test
	public void testListenerBackup() throws Exception {
		for (File f : TEST_FILES) {
			sendProduct(readProduct(f));
		}
	}

	public void sendProduct(final Product product) throws Exception {
		long timeStart = new Date().getTime();

		// prepare server to serve product
		server.setProduct(product);

		// load a url notification for a product
		URL url = new URL("http://localhost:1234/doesnt/matter");
		URLNotification notification = new URLNotification(product.getId(),
				new Date(new Date().getTime() + 24 * 60 * 60 * 1000), url, url);

		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch finish = new CountDownLatch(2);

		listener1.setCountDownLatches(start, finish);
		listener2.setCountDownLatches(start, finish);

		// this queues the notifications for processing
		receiver.receiveNotification(notification);

		// the queues block until here
		start.countDown();
		// this blocks until both have completed.
		finish.await();

		Assert.assertNotNull("listener1 product is not null",
				listener1.getProduct());
		Assert.assertNotNull("listener2 product is not null",
				listener2.getProduct());

		long timeEnd = new Date().getTime();
		System.err.println((timeEnd - timeStart) + "ms sending " + product.getId());
	}

	public Product readProduct(final File file) throws Exception {
		long start = new Date().getTime();
		try (
			final InputStream in = StreamUtils.getInputStream(file);
		) {
			return ObjectProductHandler.getProduct(IOUtil.autoDetectProductSource(in));
		} finally {
			long end = new Date().getTime();
			System.err.println((end - start) + " ms reading " + file.getName());
		}
	}

	private class ProductServer implements SocketListenerInterface {

		private Product product;

		public ProductServer(Product product) {
			setProduct(product);
		}

		public void setProduct(Product product) {
			this.product = product;
		}

		@Override
		public void onSocket(final Socket socket) {
			long start = new Date().getTime();
			try (
				final BufferedReader in = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				final OutputStream out = socket.getOutputStream();
			) {
				// read request from socket
				String line;
				while ((line = in.readLine()) != null) {
					if ("".equals(line)) {
						break;
					}
				}

				// convert product to bytes first
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final ObjectProductSource source = new ObjectProductSource(this.product);
				source.streamTo(new BinaryProductHandler(baos));
				final byte[] productBytes = baos.toByteArray();
				long encoded = new Date().getTime();
				System.err.println((encoded - start) + "ms encoding product "
						+ this.product.getId().toString());

				// this is an http response
				out.write(
					String.join("\r\n",
						"HTTP/1.0 200 OK",
						"Connection: close",
						"Content-Length: " + productBytes.length,
						"Content-Type: application/octet-stream",
						"", "").getBytes());

				// then transfer
				out.write(productBytes);
				out.flush();

				// close socket before try w/resources ends
				try {
					socket.shutdownOutput();
				} catch (Exception e) { e.printStackTrace(); }
				try {
					socket.close();
				} catch (Exception e) { e.printStackTrace(); }
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				long end = new Date().getTime();
				System.err.println((end - start) + "ms serving product "
						+ this.product.getId().toString());
			}
		}
	}

	/**
	 * Class that retrieves a product from a notification receiver in a
	 * background thread.
	 */
	private class TestNotificationListener extends DefaultNotificationListener {

		/** the retrieved product. */
		private Product product;

		private CountDownLatch start;
		private CountDownLatch finish;

		private FileProductStorage storage;

		public TestNotificationListener(File storage) {
			this.storage = new FileProductStorage(storage);
		}

		public void setCountDownLatches(CountDownLatch start,
				CountDownLatch finish) {
			this.start = start;
			this.finish = finish;
			// this.setTimeout(500);
			this.setMaxTries(2);
			this.setRetryDelay(0);
		}

		public Product getProduct() {
			return this.product;
		}

		/**
		 * Modified so onNotification sleeps longer than the timeout.
		 *
		 * This led to a synchronization issue with ExecutorTask, and this test
		 * hangs when ExecutorTask remains synchronized... Without
		 * synchronzation on ExecutorTask, this works as expected.
		 */
		@Override
		public void onNotification(final NotificationEvent notification)
				throws Exception {
			start.await();
			System.err.println("retrieving product " + Thread.currentThread().getName());
			// just retrieve the product
			retrieveProduct(notification.getNotification().getProductId());
			System.err.println("retrieved product " + Thread.currentThread().getName());
			finish.countDown();
		}

		/**
		 * Retrieve the product, this is called by the Thread run method.
		 */
		public void retrieveProduct(ProductId id) {
			try {
				this.product = receiver.retrieveProduct(id);
				try {
					this.storage.storeProduct(this.product);
				} catch (ProductAlreadyInStorageException paise) {
					// ignore
				}
			} catch (Exception e) {
				e.printStackTrace();
				this.product = null;
			}
		}

	}

}
