package gov.usgs.earthquake.indexer;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.BinaryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Reproduce a (false) event split that led to issues on ehp4.
 * 
 * See JIRA EQH-1277
 * 
 * @author jmfee
 * 
 */
public class IndexerSplitAfterDeleteWithoutIDTest {

	private static File STORAGE_FILE = new File("_test_storage");
	private static File INDEX_FILE = new File("_test_index");

	private static File FIRST_DYFI = new File(
			"etc/test_products/usc000wc0l_at00maxjbo/us_dyfi_at00maxjbo_1348617830165.bin");
	private static File SECOND_DYFI = new File(
			"etc/test_products/usc000wc0l_at00maxjbo/us_dyfi_at00maxjbo_1348617988866.bin");
	private static File THIRD_DYFI = new File(
			"etc/test_products/usc000wc0l_at00maxjbo/us_dyfi_usc000cw0l_1348618020200.bin");

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
	public void testSplitAfterDeleteWithoutID() throws Exception {
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

		Product dyfi = null;
		// parse product
		dyfi = ObjectProductHandler.getProduct(new BinaryProductSource(StreamUtils
				.getInputStream(FIRST_DYFI)));
		// index product
		indexer.onProduct(dyfi);
		// wait for listener to be notified
		synchronized (SYNC) {
			SYNC.wait();
		}

		/**
		 * output at this point is
		 * 
		 * <pre>
		 * INFO	thread=10	[null] indexed product id=urn:usgs-product:us:dyfi:at00maxjbo:1348617830165
		 * EVENT_ADDED null => at00maxjbo
		 * </pre>
		 */
		// parse product
		dyfi = ObjectProductHandler.getProduct(new BinaryProductSource(StreamUtils
				.getInputStream(SECOND_DYFI)));
		// index product
		indexer.onProduct(dyfi);
		// wait for listener to be notified
		synchronized (SYNC) {
			SYNC.wait();
		}

		/**
		 * output at this point is
		 * 
		 * <pre>
		 * WARNING	thread=10	[null] eventid (null) no longer matches original (at00maxjbo) after split.
		 * INFO	thread=10	[null] indexed product id=urn:usgs-product:us:dyfi:at00maxjbo:1348617988866
		 * EVENT_DELETED at00maxjbo => null
		 * EVENT_SPLIT null => null
		 * </pre>
		 */

		// test to make sure we aren't in this error state
		IndexerEvent lastEvent = listener.getLastEvent();
		Assert.assertEquals("event deleted (not split)", 1, lastEvent
				.getIndexerChanges().size());

		/**
		 * the subsequent product generates a NullPointerException.
		 */
		// parse product
		dyfi = ObjectProductHandler.getProduct(new BinaryProductSource(StreamUtils
				.getInputStream(THIRD_DYFI)));
		// index product
		indexer.onProduct(dyfi);
		// wait for listener to be notified
		synchronized (SYNC) {
			SYNC.wait();
		}

		indexer.shutdown();
	}

	public class TestIndexerListener extends DefaultIndexerListener {
		private IndexerEvent lastIndexerEvent = null;

		public TestIndexerListener() {
		}

		@Override
		public void onIndexerEvent(final IndexerEvent event) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
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
