/*
 * EventTest
 */
package gov.usgs.earthquake.indexer;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import org.junit.Assert;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;

import org.junit.Test;

/**
 * Test cases for the Event class.
 */
public class EventTest {

	/**
	 * Test that when two products of the same type exist, the product with a
	 * larger preferredWeight is preferred.
	 */
	@Test
	public void testPreferred() {
		Event event = new Event();

		ProductSummary morePreferred = new ProductSummary();
		morePreferred.setId(new ProductId("source", "type", "code"));
		morePreferred.setPreferredWeight(3);
		event.addProduct(morePreferred);

		ProductSummary lessPreferred = new ProductSummary();
		lessPreferred.setId(new ProductId("source2", "type", "code2"));
		lessPreferred.setPreferredWeight(2);
		event.addProduct(lessPreferred);

		ProductSummary preferred = event.getPreferredProduct("type");
		Assert.assertSame("morePreferred is preferred over lessPreferred",
				morePreferred, preferred);
	}

	/**
	 * Copy constructor is supposed to clone map of products, so changes to a
	 * copy object do not affect the original.
	 */
	@Test
	public void testCopyConstructor() {
		Event event = new Event();

		ProductSummary morePreferred = new ProductSummary();
		morePreferred.setId(new ProductId("source", "type", "code"));
		morePreferred.setPreferredWeight(3);
		event.addProduct(morePreferred);

		ProductSummary lessPreferred = new ProductSummary();
		lessPreferred.setId(new ProductId("source2", "type", "code2"));
		lessPreferred.setPreferredWeight(2);
		event.addProduct(lessPreferred);

		Event eventCopy = new Event(event);
		// change the copy
		eventCopy.getAllProducts().get("type").remove(morePreferred);

		// verify the original event is unchanged
		ProductSummary preferred = event.getPreferredProduct("type");
		Assert.assertSame("morePreferred is preferred over lessPreferred",
				morePreferred, preferred);

		// verify the copy event is changed
		preferred = eventCopy.getPreferredProduct("type");
		Assert.assertSame("Copy object only has lessPreferred product",
				lessPreferred, preferred);
	}

	/**
	 * Create a dummy event and 3 products: a magnitude product and 2 origin
	 * products. Make sure that the proper product is marked as preferred. Then
	 * create another event with 3 products, none of which is an origin product.
	 * Make sure that the proper one of these is marked as preferred.
	 */
	@Test
	public void testPreferredProductProperties() {
		double epsilon = .01;
		Event event = new Event();

		ProductSummary p1 = new ProductSummary();
		p1.setId(new ProductId("source1", "magnitude", "code"));
		p1.setPreferredWeight(3);
		p1.setEventMagnitude(new BigDecimal(4.9));
		p1.setEventSource("source1");
		p1.setEventSourceCode("source1code");
		p1.setEventLatitude(new BigDecimal(1.2));
		p1.setEventLongitude(new BigDecimal(2.3));
		p1.setEventTime(new Date());
		event.addProduct(p1);

		ProductSummary p2 = new ProductSummary();
		p2.setId(new ProductId("source2", "origin", "code2"));
		p2.setPreferredWeight(2);
		p2.setEventMagnitude(new BigDecimal(5.0));
		p2.setEventSource("source2");
		p2.setEventSourceCode("source2code");
		p2.setEventLatitude(new BigDecimal(1.2));
		p2.setEventLongitude(new BigDecimal(2.3));
		p2.setEventTime(new Date());
		event.addProduct(p2);

		ProductSummary p3 = new ProductSummary();
		p3.setId(new ProductId("source3", "origin", "code2"));
		p3.setPreferredWeight(8);
		p3.setEventMagnitude(new BigDecimal(5.1));
		p3.setEventSource("source3");
		p3.setEventSourceCode("source3code");
		p3.setEventLatitude(new BigDecimal(1.2));
		p3.setEventLongitude(new BigDecimal(2.3));
		p3.setEventTime(new Date());
		event.addProduct(p3);

		Assert.assertEquals("source3", event.getSource());
		// magnitude products were merged into origin, so this used to test for
		// 4.9, but needs to check for the most preferred origin's magnitude
		// (5.1)
		Assert.assertEquals(5.1, event.getMagnitude().doubleValue(), epsilon);

		event = new Event();
		p1 = new ProductSummary();
		p1.setId(new ProductId("source1", "test_product", "code"));
		p1.setPreferredWeight(4);
		p1.setEventMagnitude(new BigDecimal(4.9));
		p1.setEventSource("source1");
		p1.setEventDepth(new BigDecimal(10));
		p1.setEventLatitude(new BigDecimal(10));
		p1.setEventLongitude(new BigDecimal(10));
		p1.setEventSourceCode("sourcecode1");
		Date d = new Date();
		p1.setEventTime(d);
		event.addProduct(p1);

		p2 = new ProductSummary();
		p2.setId(new ProductId("source2", "test_product2", "code2"));
		p2.setPreferredWeight(2);
		p2.setEventMagnitude(new BigDecimal(5.0));
		p2.setEventSource("source2");
		p2.setEventDepth(new BigDecimal(15));
		p2.setEventLatitude(new BigDecimal(15));
		p2.setEventLongitude(new BigDecimal(15));
		p2.setEventSourceCode("sourcecode2");
		Date d2 = new Date();
		p2.setEventTime(d2);
		event.addProduct(p2);

		p3 = new ProductSummary();
		p3.setId(new ProductId("source3", "test_product3", "code3"));
		p3.setPreferredWeight(2);
		p3.setEventMagnitude(new BigDecimal(5.1));
		p3.setEventSource("source3");
		p3.setEventDepth(new BigDecimal(15));
		p3.setEventLatitude(new BigDecimal(15));
		p3.setEventLongitude(new BigDecimal(15));
		p3.setEventSourceCode("sourcecode3");
		Date d3 = new Date();
		p3.setEventTime(d3);
		event.addProduct(p3);

		Assert.assertEquals("source1", event.getSource());
		Assert.assertEquals(4.9, event.getMagnitude().doubleValue(), epsilon);
		Assert.assertEquals(10.0, event.getDepth().doubleValue(), epsilon);
		Assert.assertEquals(10.0, event.getLatitude().doubleValue(), epsilon);
		Assert.assertEquals(10.0, event.getLongitude().doubleValue(), epsilon);
		Assert.assertEquals("sourcecode1", event.getSourceCode());
		Assert.assertEquals(d, event.getTime());
	}

	/**
	 * Test to make sure getProducts does not return deleted products.
	 */
	@Test
	public void testGetProducts() {
		Event event = new Event();

		// add a product that isn't deleted
		ProductSummary summary = new ProductSummary();
		summary.setId(new ProductId("source", "type", "code"));
		summary.setStatus(Product.STATUS_UPDATE);
		event.addProduct(summary);

		// add one that is
		ProductSummary deleted = new ProductSummary();
		deleted.setId(new ProductId("source2", "type", "code"));
		deleted.setStatus(Product.STATUS_DELETE);
		event.addProduct(deleted);

		Assert.assertTrue("getAllProducts includes deleted products", event
				.getAllProducts().get("type").contains(deleted));
		Assert.assertFalse("getProducts does not include deleted products",
				event.getProducts().get("type").contains(deleted));
	}

	/**
	 * This test creates an event with 2 products. One of the products has
	 * multiple versions, with a different event id on the middle version. The
	 * event should only consider current product versions when splitting into
	 * sub events, because later versions of products supersede earlier versions
	 * of products.
	 */
	@Test
	public void testSubEvents() {
		Event event = new Event();
		Date now = new Date();

		ProductSummary summary1 = new ProductSummary();
		summary1.setId(new ProductId("source1", "type", "code"));
		summary1.setEventSource("source1");
		summary1.setEventSourceCode("code1");

		ProductSummary summary2v1 = new ProductSummary();
		summary2v1.setId(new ProductId("source2", "type", "code", now));
		summary2v1.setEventSource("source2");
		summary2v1.setEventSourceCode("code2");

		ProductSummary summary2v2 = new ProductSummary();
		summary2v2.setId(new ProductId("source2", "type", "code", new Date(now
				.getTime() + 1L)));
		summary2v2.setEventSource("source2");
		summary2v2.setEventSourceCode("code1");

		ProductSummary summary2v3 = new ProductSummary();
		summary2v3.setId(new ProductId("source2", "type", "code", new Date(now
				.getTime() + 2L)));
		summary2v3.setEventSource("source2");
		summary2v3.setEventSourceCode("code2");

		event.addProduct(summary1);
		event.addProduct(summary2v1);
		event.addProduct(summary2v2);
		event.addProduct(summary2v3);

		Map<String, Event> subEvents = event.getSubEvents();
		Assert.assertEquals("two sub events", 2, subEvents.size());
		Assert.assertEquals("event1 has 1 product", 1, subEvents.get(
				"source1code1").getAllProductList().size());
		Assert.assertEquals("event2 has 3 products", 3, subEvents.get(
				"source2code2").getAllProductList().size());
	}
}
