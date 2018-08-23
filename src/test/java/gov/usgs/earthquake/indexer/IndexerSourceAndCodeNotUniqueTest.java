package gov.usgs.earthquake.indexer;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.XmlProductSource;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

public class IndexerSourceAndCodeNotUniqueTest {

	
	private static File STORAGE_FILE = new File("_test_storage");
	private static File INDEX_FILE = new File("_test_index");

	private static File[] PRODUCTS = {
		new File("etc/test_products/ci37645983/ci-origin-ci37645983-1494867948050.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645983-1494868047790.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645991-1494868066040.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645991-1494874160300.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645991-1494874160350.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645991-1494874160800.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645991-1494874160830.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645983-1494874177010.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645983-1494874177050.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645983-1494874177480.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645983-1494874177530.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645983-1494874177890.xml"),
		// this is the product that causes the exception
		new File("etc/test_products/ci37645983/ci-origin-ci37645983-1494892985010.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645991-1494893051170.xml"),
		new File("etc/test_products/ci37645983/ci-origin-ci37645991-1494893051860.xml")
	};

	private static final Object SYNC = new Object();

	@Before
	public void before() {
		FileUtils.deleteTree(STORAGE_FILE);
		FileUtils.deleteTree(INDEX_FILE);
	}

	@After
	public void after() {
		FileUtils.deleteTree(STORAGE_FILE);
		FileUtils.deleteTree(INDEX_FILE);
	}
	

	@Test
	public void sourceAndCodeNotUniqueTestci37645983() throws Exception {
		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		// handler.setLevel(Level.FINEST);
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINEST);

		Indexer indexer = new Indexer();
		indexer.setProductIndex(new JDBCProductIndex(INDEX_FILE.getName()));
		indexer.setProductStorage(new FileProductStorage(STORAGE_FILE));

		TestIndexerListener listener = new TestIndexerListener();
		indexer.addListener(listener);

		indexer.startup();

		try {
			Product product = null;
			for (File file : PRODUCTS) {
				product = ObjectProductHandler.getProduct(new XmlProductSource(StreamUtils
						.getInputStream(file)));
				indexer.onProduct(product);

				synchronized (SYNC) {
					SYNC.wait();
				}
				IndexerEvent lastEvent = listener.getLastEvent();

				for (IndexerChange ev : lastEvent.getIndexerChanges()) {
					System.err.println(ev.getType());
				}
			}
		} catch (Exception e) {
			Assert.fail("Indexer threw exception while processing products");
		} finally {
			indexer.shutdown();
		}
	}

	public class TestIndexerListener extends DefaultIndexerListener {
		private IndexerEvent lastIndexerEvent = null;

		@Override
		public void onIndexerEvent(final IndexerEvent event) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			lastIndexerEvent = event;
			synchronized (SYNC) {
				SYNC.notify();
			}
		}

		public IndexerEvent getLastEvent() throws InterruptedException {
			return lastIndexerEvent;
		}

		public void clear() {
			lastIndexerEvent = null;
		}

	}

}
