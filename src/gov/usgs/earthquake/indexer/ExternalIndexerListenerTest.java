package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.distribution.ProductTracker;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.earthquake.product.io.BinaryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.shakemap.ShakeMap;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ExternalIndexerListenerTest {

	@Test
	public void testAccept() throws Exception {
		Product product = ObjectProductHandler.getProduct(new BinaryProductSource(
				StreamUtils.getInputStream(new File(
						"etc/test_products/usa00040xz/us_shakemap_usa00040xz_1287260900624.bin"))));

		ShakeMap shakemap = new ShakeMap(product);

		IndexerEvent change = new IndexerEvent(new Indexer());
		change.setSummary(new ProductSummary(shakemap));

		ExternalIndexerListener listener = new ExternalIndexerListener();
		listener.getIncludeTypes().add(change.getSummary().getType());
		Assert.assertTrue("listener accepts included type",
				listener.accept(change));

		listener = new ExternalIndexerListener();
		listener.getExcludeTypes().add(change.getSummary().getType());
		Assert.assertFalse("listener rejects excluded type",
				listener.accept(change));

		Event event = new Event();
		ProductSummary morePreferred = new ProductSummary();
		morePreferred.setId(new ProductId("source", "type", "code"));
		morePreferred.setPreferredWeight(3);
		event.addProduct(morePreferred);
		ProductSummary lessPreferred = new ProductSummary();
		lessPreferred.setId(new ProductId("source2", "type", "code2"));
		lessPreferred.setPreferredWeight(2);
		event.addProduct(lessPreferred);
		change.addIndexerChange(new IndexerChange(IndexerChange.EVENT_UPDATED,
				null, event));

		listener = new ExternalIndexerListener();
		listener.setProcessOnlyPreferredProducts(true);
		change.setSummary(lessPreferred);
		Assert.assertFalse("listener rejects non preferred product",
				listener.accept(change));

		change.setSummary(morePreferred);
		Assert.assertTrue("listener accepts preferred product",
				listener.accept(change));
	}

	@Before
	public void setup() throws Exception {
		// turn off tracking during test
		ProductTracker.setTrackerEnabled(false);

		// turn up logging during test
		LogManager.getLogManager().reset();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		handler.setFormatter(new SimpleLogFormatter());
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(handler);
		rootLogger.setLevel(Level.FINEST);
	}

	/**
	 * Test to reproduce ticket # 1556
	 * 
	 * @throws Exception
	 */
	@Test
	public void testArchivingProduct() throws Exception {
		Indexer indexer = new Indexer();
		ExternalIndexerListener ex = new ExternalIndexerListener();
		IndexerEvent event = new IndexerEvent(indexer);
		event.addIndexerChange(new IndexerChange(
				IndexerChange.PRODUCT_ARCHIVED, null, null));
		event.setSummary(new ProductSummary(new ProductTest().getProduct()));
		indexer.startup();

		ex.setStorage(new FileProductStorage());
		ex.setCommand("echo");
		try {
			ex.onIndexerEvent(event);
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			Assert.fail("On indexer event threw null pointer exception");
		}
	}
}
