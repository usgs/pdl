package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.util.DefaultConfigurable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import org.junit.Test;

public class SocketProductSenderTest {

	private Object waitForProductSync = new Object();
	private Product product = null;

	@Test
	public void testSendXmlWithDeflate() throws Exception {
		testSendProduct(/* binaryFormat */false, /* enableDeflate */true);
	}

	@Test
	public void testSendBinaryWithDeflate() throws Exception {
		testSendProduct(/* binaryFormat */true, /* enableDeflate */true);
	}

	@Test
	public void testSendXmlNoDeflate() throws Exception {
		testSendProduct(/* binaryFormat */false, /* enableDeflate */false);
	}

	@Test
	public void testSendBinaryNoDeflate() throws Exception {
		testSendProduct(/* binaryFormat */true, /* enableDeflate */false);
	}

	// ------------------------------------------------------------------------
	// Helper/Utility Functions
	// ------------------------------------------------------------------------

	/**
	 * This is the testing utility method called by individual tests.
	 * 
	 * @param binaryFormat
	 *            whether to use the binary format during test.
	 * @param enableDeflate
	 *            whether to use deflate compression during test.
	 * @throws Exception
	 */
	protected void testSendProduct(final boolean binaryFormat,
			final boolean enableDeflate) throws Exception {
		TestSocketAcceptor server = new TestSocketAcceptor(this);
		server.start();

		SocketProductSender sender = new SocketProductSender();
		sender.setHost("localhost");
		sender.setPort(1984);
		sender.setBinaryFormat(binaryFormat);
		sender.setEnableDeflate(enableDeflate);
		sender.startup();

		// here is the test
		Product p = new Product(new ProductId("test", "product", "self",
				new Date()));
		sender.sendProduct(p);
		waitForProduct();
		ProductTest pt = new ProductTest();
		pt.compareProducts(p, product);

		// now shutdown
		sender.shutdown();
		server.poof();
		product = null;
	}

	protected void waitForProduct() {
		synchronized (waitForProductSync) {
			while (product == null) {
				try {
					waitForProductSync.wait();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void setProduct(Product p) {
		synchronized (waitForProductSync) {
			this.product = p;
			waitForProductSync.notify();
		}
	}

	protected Product getProduct() {
		return product;
	}

	private class TestSocketAcceptor extends Thread {
		private ServerSocket sock = null;
		private SocketProductSenderTest cb = null;

		public TestSocketAcceptor(SocketProductSenderTest cb) throws Exception {
			sock = new ServerSocket(1984);
			this.cb = cb;
		}

		public void poof() throws Exception {
			sock.close();
		}

		public void run() {
			try {
				SocketProductReceiver receiver = new SocketProductReceiver() {
					@Override
					public ProductStorage getProductStorage() {
						return new TestProductStorage();
					}

					@Override
					protected String storeAndNotify(
							final ProductSource productSource) throws Exception {
						cb.setProduct(ObjectProductHandler
								.getProduct(productSource));
						return "received product";
					}
				};

				Socket socket = sock.accept();
				receiver.onSocket(socket);

				socket.close();
				sock.close();
			} catch (IOException iox) {
				System.err.println(iox.getMessage());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * A testing product storage object, returns false for hasProduct().
	 */
	private class TestProductStorage extends DefaultConfigurable implements
			ProductStorage {

		@Override
		public boolean hasProduct(ProductId id) throws Exception {
			return false;
		}

		@Override
		public Product getProduct(ProductId id) throws Exception {
			return null;
		}

		@Override
		public ProductId storeProduct(Product product) throws Exception {
			return null;
		}

		@Override
		public ProductSource getProductSource(ProductId id) throws Exception {
			return null;
		}

		@Override
		public ProductId storeProductSource(ProductSource input)
				throws Exception {
			return null;
		}

		@Override
		public void removeProduct(ProductId id) throws Exception {
		}

		@Override
		public void notifyListeners(StorageEvent event) {
		}

		@Override
		public void addStorageListener(StorageListener listener) {
		}

		@Override
		public void removeStorageListener(StorageListener listener) {
		}
	}
}