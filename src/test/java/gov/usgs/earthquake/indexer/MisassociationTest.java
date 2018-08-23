package gov.usgs.earthquake.indexer;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.distribution.ProductTracker;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.XmlProductSource;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.logging.SimpleLogFormatter;

public class MisassociationTest {

	public static final String[] TEST_PRODUCTS = {
			"etc/test_products/20110725_usc00053hg_nc71606670/us_origin_usc00053hg_1311534397000.xml",
			"etc/test_products/20110725_usc00053hg_nc71606670/us_focal-mechanism_usc00053hg-neic-mwc_1311537586000.xml",
			"etc/test_products/20110725_usc00053hg_nc71606670/nc_origin_nc71606670_1311633433000.xml" };

	private Indexer indexer;

	public List<Product> getProducts() throws Exception {
		List<Product> products = new LinkedList<Product>();
		for (String path : TEST_PRODUCTS) {
			products.add(ObjectProductHandler.getProduct(new XmlProductSource(
					StreamUtils.getInputStream(new File(path)))));
		}
		return products;
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

		indexer = new Indexer();

		// empty storage
		FileUtils.deleteTree(((FileProductStorage) indexer.getProductStorage())
				.getBaseDirectory());
		// empty notification index
		new File(JDBCProductIndex.JDBC_DEFAULT_FILE).delete();

		indexer.startup();
	}

	@After
	public void teardown() throws Exception {
		indexer.shutdown();
	}

	@Test
	public void associate() throws Exception {
		Iterator<Product> iter = getProducts().iterator();
		while (iter.hasNext()) {
			indexer.onProduct(iter.next());
		}

		SearchRequest request = new SearchRequest();
		request.addQuery(new EventsSummaryQuery(new ProductIndexQuery()));
		SearchResponse response = indexer.search(request);
		List<EventSummary> events = ((EventsSummaryQuery) response.getResults()
				.get(0)).getResult();

		// problem was they previously merged
		// make sure there are two distinct events
		Assert.assertTrue(events.size() == 2);
	}

}
