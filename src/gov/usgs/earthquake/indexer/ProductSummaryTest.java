/*
 * ProductSummary
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductTest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This test class examines the functionality of the ProductSummary class.
 * Specifically we test the non-trivial constructors and the addLink methods. We
 * do not explicitly test the simple getter/setter methods for the reasons
 * outlined in the jUnit test documentation pages.
 * 
 */
public class ProductSummaryTest {

	private Product product, otherProduct;
	private ProductSummary summary, otherSummary;
	private ProductTest productFactory = new ProductTest();

	@Before
	public void setUpTestEnvironment() {
		product = productFactory.getProduct();
		otherProduct = productFactory.getOtherProduct();

		summary = new ProductSummary(product);
		otherSummary = new ProductSummary(otherProduct);
	}

	@After
	public void tearDownTestEnvironment() {

	}

	// --------------------------------------------------------------------
	// -- Helper Methods
	// --------------------------------------------------------------------

	/**
	 * Helper method. Checks if the given summary contains the the given
	 * relation, and further, that said relation also contains the given uri.
	 * 
	 * @return True of the above is met, false otherwise.
	 */
	protected boolean containsLinkRelation(ProductSummary summary,
			String relation, URI uri) {
		Map<String, List<URI>> linkRelations = summary.getLinks();
		if (linkRelations.containsKey(relation)) {
			List<URI> links = linkRelations.get(relation);
			return links.contains(uri);
		}
		return false;
	}

	// --------------------------------------------------------------------
	// -- Test Methods
	// --------------------------------------------------------------------

	/**
	 * Tests the copy constructor for the ProductSummary class. This creates a
	 * single product summary and then uses the copy constructor to create a
	 * second product summary. Finally it asserts all the members of a the
	 * product summaries are equal.
	 * 
	 * Any member differing is enough to fail the test.
	 * 
	 * @see gov.usgs.earthquake.indexer.ProductSummary#ProductSummary(ProductSummary)
	 */
	@Test
	public void testCopyConstructor() {
		ProductSummary testSummary = new ProductSummary(summary);

		// Assert each of the scalar members are equal
		Assert.assertEquals(summary.getIndexId(), testSummary.getIndexId());
		Assert.assertEquals(summary.getId(), testSummary.getId());
		Assert.assertEquals(summary.getStatus(), testSummary.getStatus());
		Assert.assertEquals(summary.getTrackerURL(),
				testSummary.getTrackerURL());
		Assert.assertEquals(summary.getPreferredWeight(), testSummary.getPreferredWeight());
		Assert.assertEquals(summary.getEventId(), testSummary.getEventId());
		Assert.assertEquals(summary.getEventSource(),
				testSummary.getEventSource());
		Assert.assertEquals(summary.getEventTime(), testSummary.getEventTime());
		Assert.assertEquals(summary.getEventLatitude(),
				testSummary.getEventLatitude());
		Assert.assertEquals(summary.getEventLongitude(),
				testSummary.getEventLongitude());
		Assert.assertEquals(summary.getEventDepth(),
				testSummary.getEventDepth());
		Assert.assertEquals(summary.getEventMagnitude(),
				testSummary.getEventMagnitude());
		Assert.assertEquals(summary.getVersion(), testSummary.getVersion());

		// These are "Map" properties. We use the default "equals" method to
		// ensure these are in fact equal. Maybe we should implement our own
		// "equals" method if we don't care to be as strict?
		Assert.assertEquals(summary.getLinks(), testSummary.getLinks());
		Assert.assertEquals(summary.getProperties(),
				testSummary.getProperties());

	}

	/**
	 * Tests the ProductSummary constructor that creates a summary from a
	 * Product object. This method first creates a product then uses that
	 * product as a basis for a summary. Finally it compares all the members of
	 * the product to the members of the summary and asserts they are each
	 * equal.
	 * 
	 * Any member differing is enough to fail the test.
	 * 
	 * @see gov.usgs.earthquake.indexer.ProductSummary#ProductSummary(gov.usgs.earthquake.product.Product)
	 */
	@Test
	public void testProductConstructor() {

		// Assert each of the scalar members are equal
		Assert.assertNull(summary.getIndexId());
		Assert.assertEquals(product.getId(), summary.getId());
		Assert.assertEquals(product.getStatus(), summary.getStatus());
		Assert.assertEquals(product.getTrackerURL(), summary.getTrackerURL());
		Assert.assertEquals(1L, summary.getPreferredWeight());
		Assert.assertEquals(product.getEventId(), summary.getEventId());
		Assert.assertEquals(product.getEventSource(), summary.getEventSource());
		Assert.assertEquals(product.getEventTime(), summary.getEventTime());
		Assert.assertEquals(product.getLatitude(), summary.getEventLatitude());
		Assert.assertEquals(product.getLongitude(), summary.getEventLongitude());
		Assert.assertEquals(product.getDepth(), summary.getEventDepth());
		Assert.assertEquals(product.getMagnitude(), summary.getEventMagnitude());
		Assert.assertEquals(product.getVersion(), summary.getVersion());

		// These are "Map" properties. We use the default "equals" method to
		// ensure these are in fact equal. Maybe we should implement our own
		// "equals" method if we don't care to be as strict?
		Assert.assertEquals(product.getLinks(), summary.getLinks());
		Assert.assertEquals(product.getProperties(), summary.getProperties());
	}

	/**
	 * This method creates a ProductSummary object and then adds a link to the
	 * it. After adding the link, it is asserted that the link was successfully
	 * added.
	 * 
	 * @see gov.usgs.earthquake.indexer.ProductSummary#addLink(String,
	 *      java.net.URI)
	 */
	@Test
	public void testAddLink() {

		try {
			// Add a single link and check
			URI uri = new URI("http://test.relation/");
			String relation = "TestRelation";

			otherSummary.addLink(relation, uri);
			Assert.assertTrue(containsLinkRelation(otherSummary, relation, uri));

		} catch (URISyntaxException uix) {
			Assert.fail(uix.getMessage());
		}
	}
}
