package gov.usgs.earthquake.distribution;

import java.io.File;
import java.io.OutputStream;
import java.net.Socket;
import java.util.zip.DeflaterOutputStream;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.earthquake.product.io.BinaryProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StreamUtils.UnclosableOutputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SocketProductReceiverTest {

	private SocketProductReceiver receiver = null;
	final ProductTest productTest = new ProductTest();
	final Product product = productTest.getProduct();

	@Before
	public void startupReceiver() throws Exception {
		ProductTracker.setTrackerEnabled(false);

		// remove existing test database
		try {
			new File("/tmp/socketproductreceivertestdb").delete();
		} catch (Exception e) {
			// ignore
		}

		Config config = new Config();
		config.setProperty("port", "1984");
		config.setProperty("storageage", "50000");

		config.setProperty("index", "myindex");
		config.setSectionProperty("myindex",
				JDBCNotificationIndex.JDBC_FILE_PROPERTY,
				"/tmp/socketproductreceivertestdb");
		config.setSectionProperty("myindex", "type",
				"gov.usgs.earthquake.distribution.JDBCNotificationIndex");

		config.setProperty("storage", "mystorage");
		config.setSectionProperty("mystorage", "type",
				"gov.usgs.earthquake.distribution.FileProductStorage");

		Config.setConfig(config);

		receiver = new SocketProductReceiver();
		receiver.configure(config);
		receiver.startup();
	}

	@After
	public void shutdownReceiver() throws Exception {
		receiver.shutdown();
	}

	@Test
	public void testReceiveXmlWithDeflate() throws Exception {
		// make sure the product isn't already in storage
		ProductStorage ps = receiver.getProductStorage();
		ps.removeProduct(product.getId());

		TestNotificationListener listener = new TestNotificationListener();
		receiver.addNotificationListener(listener);

		// send product over socket
		ObjectProductSource source = new ObjectProductSource(this.product);
		Socket socket = new Socket("localhost", 1984);
		DeflaterOutputStream out = new DeflaterOutputStream(
				socket.getOutputStream());
		XmlProductHandler handler = new XmlProductHandler(
				new UnclosableOutputStream(out));
		source.streamTo(handler);
		out.finish();
		socket.close();

		// listener.getProduct() blocks until the product has been retrieved
		Product p2 = listener.getProduct();
		productTest.compareProducts(this.product, p2);
	}

	@Test
	public void testReceiveXmlNoDeflate() throws Exception {
		// make sure the product isn't already in storage
		ProductStorage ps = receiver.getProductStorage();
		ps.removeProduct(product.getId());

		TestNotificationListener listener = new TestNotificationListener();
		receiver.addNotificationListener(listener);

		// send product over socket
		ObjectProductSource source = new ObjectProductSource(this.product);
		Socket socket = new Socket("localhost", 1984);
		OutputStream out = socket.getOutputStream();
		XmlProductHandler handler = new XmlProductHandler(
				new UnclosableOutputStream(out));
		source.streamTo(handler);
		socket.close();

		// listener.getProduct() blocks until the product has been retrieved
		Product p2 = listener.getProduct();
		productTest.compareProducts(this.product, p2);
	}

	@Test
	public void testReceiveBinaryWithDeflate() throws Exception {
		// make sure the product isn't already in storage
		ProductStorage ps = receiver.getProductStorage();
		ps.removeProduct(product.getId());

		TestNotificationListener listener = new TestNotificationListener();
		receiver.addNotificationListener(listener);

		// send product over socket
		ObjectProductSource source = new ObjectProductSource(this.product);
		Socket socket = new Socket("localhost", 1984);
		DeflaterOutputStream out = new DeflaterOutputStream(
				socket.getOutputStream());
		BinaryProductHandler handler = new BinaryProductHandler(
				new UnclosableOutputStream(out));
		source.streamTo(handler);
		out.finish();
		socket.close();

		// listener.getProduct() blocks until the product has been retrieved
		Product p2 = listener.getProduct();
		productTest.compareProducts(this.product, p2);
	}

	@Test
	public void testReceiveBinaryNoDeflate() throws Exception {
		// make sure the product isn't already in storage
		ProductStorage ps = receiver.getProductStorage();
		ps.removeProduct(product.getId());

		TestNotificationListener listener = new TestNotificationListener();
		receiver.addNotificationListener(listener);

		// send product over socket
		ObjectProductSource source = new ObjectProductSource(this.product);
		Socket socket = new Socket("localhost", 1984);
		OutputStream out = socket.getOutputStream();
		BinaryProductHandler handler = new BinaryProductHandler(
				new UnclosableOutputStream(out));
		source.streamTo(handler);
		socket.close();

		// listener.getProduct() blocks until the product has been retrieved
		Product p2 = listener.getProduct();
		productTest.compareProducts(this.product, p2);
	}

	@Test
	public void testReceiveExistingProduct() throws Exception {
		ProductStorage ps = receiver.getProductStorage();
		try {
			// make sure storage has product
			ps.storeProduct(product);
		} catch (Exception ignore) {}

		SocketProductSender sps = new SocketProductSender("localhost", 1984);
		try {
			sps.sendProduct(product);
		} catch (Exception e) {
			// should "succeed" even if not sent, because product already exists.
			e.printStackTrace();
			Assert.fail();
		}
	}

	private class TestNotificationListener extends DefaultConfigurable
			implements NotificationListener {

		private Product product = null;

		public synchronized void onNotification(NotificationEvent event)
				throws Exception {
			this.product = event.getProduct();
			this.notifyAll();
		}

		public synchronized Product getProduct() {
			if (this.product == null) {
				try {
					this.wait();
				} catch (Exception e) {
					// ignore
				}
			}
			return this.product;
		}

		public void configure(Config config) throws Exception {
			// ignore
		}

		public void shutdown() throws Exception {
			// ignore
		}

		public void startup() throws Exception {
			// ignore
		}

		@Override
		public int getMaxTries() {
			return 1;
		}

		@Override
		public long getTimeout() {
			return 0;
		}

	}
}
