package gov.usgs.earthquake.indexer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

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

public class MultipleSameSourceTest {

	public static final String[] TEST_PRODUCTS = {
		// update nc72418635
		"etc/test_products/multiple_samesource/origin_nc72418635_nc_1427489045050.xml",
		"etc/test_products/multiple_samesource/phase-data_nc72418635_nc_1427489045050.xml",
		// update nn00488363, associates to nc72418635
		"etc/test_products/multiple_samesource/origin_nn00488363_nn_1427492032529.xml",
		"etc/test_products/multiple_samesource/phase-data_nn00488363_nn_1427492032529.xml",
		// delete nn00488363, remains associated
		"etc/test_products/multiple_samesource/origin_nn00488363_nn_1427492772035.xml",
		// update nn00488366; associates but shouldn't (nn00488363 already associated)
		"etc/test_products/multiple_samesource/origin_nn00488366_nn_1427492221309.xml",
		"etc/test_products/multiple_samesource/phase-data_nn00488366_nn_1427492221309.xml",
		// delete nn00488366
		"etc/test_products/multiple_samesource/origin_nn00488366_nn_1427492772081.xml"
	};

	private Indexer indexer;

	public List<Product> getProducts() throws Exception {
		List<Product> products = new ArrayList<Product>();
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

		// TODO: check that US event never merges with NC
		SearchRequest request = new SearchRequest();
		request.addQuery(new EventDetailQuery(new ProductIndexQuery()));
		SearchResponse response = indexer.search(request);
		List<Event> events = ((EventDetailQuery) response.getResults()
				.get(0)).getResult();

		// problem was they previously merged
		// make sure there are four distinct events
		Assert.assertTrue(events.size() == 2);
		for (Event event : events) {
			event.log(Logger.getLogger("test"));
		}
	}

}
