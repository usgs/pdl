/*
 * DefaultAssociatorTest
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.ProductId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test methods for the DefaultAssociator class.
 */
public class DefaultAssociatorTest {

	/**
	 * Test that the associator builds a ProductIndexQuery that will associate
	 * based on location information around the equator. This test first builds
	 * a ProductSummary with lat, lon, and time information. Then it checks that
	 * the mins and maxes of the query are in the right place.
	 */
	@Test
	public void testNearEquator() {
		Date eventTime = new Date();
		BigDecimal eventLatitude = new BigDecimal("0.0");
		BigDecimal eventLongitude = new BigDecimal("0.0");

		ProductSummary summary = new ProductSummary();
		summary.setEventTime(eventTime);
		summary.setEventLatitude(eventLatitude);
		summary.setEventLongitude(eventLongitude);

		DefaultAssociator associator = new DefaultAssociator();
		ProductIndexQuery query = getProductIndexQuery(associator, summary, 0);

		Assert.assertEquals("query min date", new Date(eventTime.getTime()
				- DefaultAssociator.TIME_DIFF_MILLISECONDS),
				query.getMinEventTime());
		Assert.assertEquals("query max date", new Date(eventTime.getTime()
				+ DefaultAssociator.TIME_DIFF_MILLISECONDS),
				query.getMaxEventTime());

		// within one degree of latitude
		Assert.assertTrue("query max latitude",
				eventLatitude.add(new BigDecimal("0.5")).doubleValue() < query
						.getMaxEventLatitude().doubleValue());
		Assert.assertTrue(
				"query min latitude",
				eventLatitude.subtract(new BigDecimal("0.5")).doubleValue() > query
						.getMinEventLatitude().doubleValue());

		// at the equator, it's about 0.8 test at 0.5
		Assert.assertTrue("query max longitude",
				eventLongitude.add(new BigDecimal("0.5")).doubleValue() < query
						.getMaxEventLongitude().doubleValue());
		Assert.assertTrue(
				"query min longitude",
				eventLongitude.subtract(new BigDecimal("0.5")).doubleValue() > query
						.getMinEventLongitude().doubleValue());
	}

	/**
	 * Test that the associator builds a ProductIndexQuery that will associate
	 * based on location information around the north pole. This test first
	 * builds a ProductSummary with lat, lon, and time information. Then it
	 * checks that the mins and maxes of the query are in the right place.
	 */
	@Test
	public void testNorthernLatitude() {
		Date eventTime = new Date();
		BigDecimal eventLatitude = new BigDecimal("88.0");
		BigDecimal eventLongitude = new BigDecimal("0.0");

		ProductSummary summary = new ProductSummary();
		summary.setEventTime(eventTime);
		summary.setEventLatitude(eventLatitude);
		summary.setEventLongitude(eventLongitude);

		DefaultAssociator associator = new DefaultAssociator();
		// ProductIndexQuery query = associator.getProductIndexQuery(summary);
		ProductIndexQuery query = getProductIndexQuery(associator, summary, 0);

		Assert.assertEquals("query min date", new Date(eventTime.getTime()
				- DefaultAssociator.TIME_DIFF_MILLISECONDS),
				query.getMinEventTime());
		Assert.assertEquals("query max date", new Date(eventTime.getTime()
				+ DefaultAssociator.TIME_DIFF_MILLISECONDS),
				query.getMaxEventTime());

		// within one degree of latitude
		Assert.assertTrue("query max latitude",
				eventLatitude.add(new BigDecimal("0.5")).doubleValue() < query
						.getMaxEventLatitude().doubleValue());
		Assert.assertTrue(
				"query min latitude",
				eventLatitude.subtract(new BigDecimal("0.5")).doubleValue() > query
						.getMinEventLatitude().doubleValue());

		// at northern latitudes, it gets bigger
		Assert.assertTrue("query max longitude",
				eventLongitude.add(new BigDecimal("20")).doubleValue() < query
						.getMaxEventLongitude().doubleValue());
		Assert.assertTrue(
				"query min longitude",
				eventLongitude.subtract(new BigDecimal("20")).doubleValue() > query
						.getMinEventLongitude().doubleValue());
	}

	/**
	 * Test that the associator builds a ProductIndexQuery that will associate
	 * based on location information around the south pole. This test first
	 * builds a ProductSummary with lat, lon, and time information. Then it
	 * checks that the mins and maxes of the query are in the right place.
	 */
	@Test
	public void testAtSouthPole() {
		Date eventTime = new Date();
		BigDecimal eventLatitude = new BigDecimal("89.01");
		BigDecimal eventLongitude = new BigDecimal("0.0");

		ProductSummary summary = new ProductSummary();
		summary.setEventTime(eventTime);
		summary.setEventLatitude(eventLatitude);
		summary.setEventLongitude(eventLongitude);

		DefaultAssociator associator = new DefaultAssociator();
		// ProductIndexQuery query = associator.getProductIndexQuery(summary);
		ProductIndexQuery query = getProductIndexQuery(associator, summary, 0);

		Assert.assertEquals("query min date", new Date(eventTime.getTime()
				- DefaultAssociator.TIME_DIFF_MILLISECONDS),
				query.getMinEventTime());
		Assert.assertEquals("query max date", new Date(eventTime.getTime()
				+ DefaultAssociator.TIME_DIFF_MILLISECONDS),
				query.getMaxEventTime());

		// within one degree of latitude
		Assert.assertTrue("query max latitude",
				eventLatitude.add(new BigDecimal("0.5")).doubleValue() < query
						.getMaxEventLatitude().doubleValue());
		Assert.assertTrue(
				"query min latitude",
				eventLatitude.subtract(new BigDecimal("0.5")).doubleValue() > query
						.getMinEventLatitude().doubleValue());

		// at poles, doesn't check longitude
		Assert.assertNull("query max longitude", query.getMaxEventLongitude());
		Assert.assertNull("query min longitude", query.getMinEventLongitude());
	}

	/**
	 * When an event source submits two different codes, they refer to different
	 * events.
	 * 
	 * The associator should never choose an event from the same source that has
	 * a different code.
	 */
	@Test
	public void testDifferentEventCodesFromSameSource() {
		ProductSummary summary = new ProductSummary();
		summary.setEventSource("source");
		summary.setEventSourceCode("code");

		// build event from same source, with different code
		ProductSummary differentSummary = new ProductSummary();
		differentSummary.setId(new ProductId("productsource", "producttype",
				"productcode"));
		differentSummary.setEventSource("source");
		differentSummary.setEventSourceCode("othercode");

		Event differentEvent = new Event();
		differentEvent.addProduct(differentSummary);

		List<Event> events = new LinkedList<Event>();
		events.add(differentEvent);

		// test to verify event is not chosen
		DefaultAssociator associator = new DefaultAssociator();
		Event event = associator.chooseEvent(events, summary);

		Assert.assertNull(
				"Event from same source with different code not chosen", event);
	}

	/**
	 * When an event source submits data using the same code, they refer to the
	 * same event.
	 * 
	 * The associator shouldn't filter an event when it uses the same source and
	 * code (although it won't necessarily choose that event because it has the
	 * same source and code).
	 */
	@Test
	public void testSameEventCodeFromSameSource() {
		ProductSummary summary = new ProductSummary();
		summary.setEventSource("source");
		summary.setEventSourceCode("code");

		// build event from same source, with same code
		ProductSummary differentSummary = new ProductSummary();
		differentSummary.setId(new ProductId("productsource", "producttype",
				"productcode"));
		differentSummary.setEventSource("source");
		differentSummary.setEventSourceCode("code");

		Event sameEvent = new Event();
		sameEvent.addProduct(differentSummary);

		List<Event> events = new LinkedList<Event>();
		events.add(sameEvent);

		// test to verify event is chosen
		DefaultAssociator associator = new DefaultAssociator();
		Event event = associator.chooseEvent(events, summary);

		Assert.assertEquals("Event from same source with same code chosen",
				sameEvent, event);
	}

	/**
	 * Test if the associator selects using the most important attribute first.
	 * That is, use eventId before location.
	 * 
	 * This test creates 2 events, A and B, and a summary X. A and B both have
	 * eventId and location information. B's location is closest to X's
	 * location, but A's eventId matches X's. The associator should choose A.
	 * 
	 */
	@Test
	public void testAssociationOrder() {
		Date eventTime = new Date();

		// Create the first event
		ProductSummary eventSummaryA = new ProductSummary();
		eventSummaryA.setId(new ProductId("productsource", "producttype",
				"productcode"));
		eventSummaryA.setEventSource("source");
		eventSummaryA.setEventSourceCode("code");
		eventSummaryA.setEventLatitude(new BigDecimal("10.0"));
		eventSummaryA.setEventLongitude(new BigDecimal("10.0"));
		eventSummaryA.setEventTime(eventTime);
		Event eventA = new Event();
		eventA.addProduct(eventSummaryA);

		// Create the second event
		ProductSummary eventSummaryB = new ProductSummary();
		eventSummaryB.setId(new ProductId("productsource", "producttype",
				"productcode"));
		eventSummaryB.setEventSource("source2");
		eventSummaryB.setEventSourceCode("code2");
		eventSummaryB.setEventLatitude(new BigDecimal("0.0"));
		eventSummaryB.setEventLongitude(new BigDecimal("0.0"));
		eventSummaryB.setEventTime(eventTime);
		Event eventB = new Event();
		eventB.addProduct(eventSummaryB);

		// Create the test summary
		ProductSummary summaryX = new ProductSummary();
		summaryX.setId(new ProductId("productsource", "producttype",
				"productcode"));
		summaryX.setEventSource("source");
		summaryX.setEventSourceCode("code");
		summaryX.setEventLatitude(new BigDecimal("0.0"));
		summaryX.setEventLongitude(new BigDecimal("0.0"));
		summaryX.setEventTime(eventTime);

		// Add the events to a list
		List<Event> events = new LinkedList<Event>();
		events.add(eventA);
		events.add(eventB);

		// Test to verify event A is chosen.
		DefaultAssociator associator = new DefaultAssociator();
		Event event = associator.chooseEvent(events, summaryX);
		Assert.assertEquals("Event from same source with same code chosen",
				eventA, event);
	}

	/**
	 * The DefaultAssociator will filter events that are from the same source as
	 * a summary if they have a different code.
	 * 
	 * This test exists to prevent a nullpointerexception regression.
	 */
	@Test
	public void testChooseEventWithoutSummarySourceCode() {
		ProductSummary summary = new ProductSummary();
		List<Event> events = new LinkedList<Event>();
		DefaultAssociator associator = new DefaultAssociator();

		try {
			Event event = associator.chooseEvent(events, summary);
			Assert.assertNull("Returned event null", event);
		} catch (NullPointerException npe) {
			Assert.fail("NullPointerException thrown");
		}
	}

	/**
	 * Test choosing the "closest" event to associate a product to based on the
	 * lat, lon, and eventTime values. This method creates a product with some
	 * lat, lon and time values then it creates 3 events whose parameters only
	 * differ slightly from the product. It then chooses the event whose
	 * Euclidean distance is lowest.
	 * 
	 * @see #createEvent(BigDecimal, BigDecimal, Date)
	 * @see gov.usgs.earthquake.indexer.DefaultAssociator#chooseMostSimilar(ProductSummary,
	 *      List)
	 */
	@Test
	public void testChooseMostSimilar() {
		Date d = new Date();
		double lat = 10.0;
		double lon = 10.0;

		// Make a product with location and time
		ProductSummary product = new ProductSummary();
		product.setEventLatitude(BigDecimal.valueOf(lat));
		product.setEventLongitude(BigDecimal.valueOf(lon));
		product.setEventTime(d);

		// Make 3 events with similar locations and times
		Event e1 = createEvent("test", "1", BigDecimal.valueOf(lat + 0.1),
				BigDecimal.valueOf(lon + 0.1), new Date(d.getTime() + 10));

		Event e2 = createEvent("test", "2", BigDecimal.valueOf(lat - 0.5),
				BigDecimal.valueOf(lon), new Date(d.getTime() + 50));

		Event e3 = createEvent("test", "3", BigDecimal.valueOf(lat + 1),
				BigDecimal.valueOf(lon + 1), d);

		// Now add the events to a list, and find the most similar
		List<Event> events = new ArrayList<Event>();
		events.add(e1);
		events.add(e2);
		events.add(e3);

		// Calculate which event is most similar to the product
		DefaultAssociator associator = new DefaultAssociator();
		Event mostSimilar = associator.chooseMostSimilar(product, events);
		Assert.assertEquals(e1, mostSimilar);
	}

	
	@Test
	public void testAlertMultipleAssociations() {
		Date d = new Date();
		double lat = 10.0;
		double lon = 10.0;

		// Make a product with location and time
		ProductSummary product = new ProductSummary();
		product.setEventLatitude(BigDecimal.valueOf(lat));
		product.setEventLongitude(BigDecimal.valueOf(lon));
		product.setEventTime(d);
		product.setId(new ProductId("us", "origin", "c0001234"));
		product.setEventSource("us");
		product.setEventSourceCode("c0001234");

		// Make 3 events with similar locations and times
		Event e1 = createDifferentEvent(BigDecimal.valueOf(lat + 0.1),
				BigDecimal.valueOf(lon + 0.1), new Date(d.getTime() + 10),
				"us", "origin", "c0002222");

		Event e2 = createDifferentEvent(BigDecimal.valueOf(lat - 0.5),
				BigDecimal.valueOf(lon), new Date(d.getTime() + 50),
				"nc", "origin", "11112222");

		Event e3 = createDifferentEvent(BigDecimal.valueOf(lat + 1),
				BigDecimal.valueOf(lon + 1), d, "ci", "origin", "aaaa2222");

		Event e4 = createDifferentEvent(BigDecimal.valueOf(lat),
				BigDecimal.valueOf(lon), d, "uw", "origin", "00005555");
		
		Event e5 = createDifferentEvent(BigDecimal.valueOf(lat),
				BigDecimal.valueOf(lon), d, "us", "origin", "00005555");

		// Now add the events to a list, and find the most similar
		List<Event> events = new ArrayList<Event>();
		events.add(e1);
		events.add(e2);
		events.add(e3);
		events.add(e4);
		events.add(e5);

		// Calculate which event is most similar to the product
		DefaultAssociator associator = new DefaultAssociator();
		Event mostSimilar = associator.chooseEvent(events, product);
		Assert.assertEquals(e4, mostSimilar);
	}

	
	@Test
	public void testDateTimeLine() {

		Date eventTime = new Date();
		DefaultAssociator associator = new DefaultAssociator();

		// Create the first event
		ProductSummary eventSummaryA = new ProductSummary();
		eventSummaryA.setId(new ProductId("us", "origin", "1234abcd"));
		eventSummaryA.setEventSource("us");
		eventSummaryA.setEventSourceCode("1234abcd");
		eventSummaryA.setEventLatitude(new BigDecimal("0.0"));
		eventSummaryA.setEventLongitude(new BigDecimal("179.80"));
		eventSummaryA.setEventTime(eventTime);
		Event eventA = new Event();
		eventA.addProduct(eventSummaryA);

		// Create the second event
		ProductSummary eventSummaryB = new ProductSummary();
		eventSummaryB.setId(new ProductId("ci", "origin", "5678efgh"));
		eventSummaryB.setEventSource("ci");
		eventSummaryB.setEventSourceCode("5678efgh");
		eventSummaryB.setEventLatitude(new BigDecimal("0.0"));
		eventSummaryB.setEventLongitude(new BigDecimal("179.50"));
		eventSummaryB.setEventTime(eventTime);
		Event eventB = new Event();
		eventB.addProduct(eventSummaryB);

		/*
		 * Verify that events associate (0.3 degrees apart) when the points are
		 * not crossing date line
		 */

		Assert.assertTrue(associator.eventsAssociated(eventA, eventB));

		// Create the third event
		ProductSummary eventSummaryC = new ProductSummary();
		eventSummaryC.setId(new ProductId("us", "origin", "1234abcd"));
		eventSummaryC.setEventSource("us");
		eventSummaryC.setEventSourceCode("1234abcd");
		eventSummaryC.setEventLatitude(new BigDecimal("0.0"));
		eventSummaryC.setEventLongitude(new BigDecimal("179.85"));
		eventSummaryC.setEventTime(eventTime);
		Event eventC = new Event();
		eventC.addProduct(eventSummaryC);

		// Create the fourth event
		ProductSummary eventSummaryD = new ProductSummary();
		eventSummaryD.setId(new ProductId("ci", "origin", "5678efgh"));
		eventSummaryD.setEventSource("ci");
		eventSummaryD.setEventSourceCode("5678efgh");
		eventSummaryD.setEventLatitude(new BigDecimal("0.0"));
		eventSummaryD.setEventLongitude(new BigDecimal("-179.85"));
		eventSummaryD.setEventTime(eventTime);
		Event eventD = new Event();
		eventD.addProduct(eventSummaryD);

		/*
		 * Verify that events associate (0.3 degrees apart) when the points do
		 * cross the date line
		 */

		Assert.assertTrue(associator.eventsAssociated(eventC, eventD));

		// Create the fifth event
		ProductSummary eventSummaryE = new ProductSummary();
		eventSummaryE.setId(new ProductId("us", "origin", "1234abcd"));
		eventSummaryE.setEventSource("us");
		eventSummaryE.setEventSourceCode("1234abcd");
		eventSummaryE.setEventLatitude(new BigDecimal("0.0"));
		eventSummaryE.setEventLongitude(new BigDecimal("-179.85"));
		eventSummaryE.setEventTime(eventTime);
		Event eventE = new Event();
		eventE.addProduct(eventSummaryE);

		// Create the sixth event
		ProductSummary eventSummaryF = new ProductSummary();
		eventSummaryF.setId(new ProductId("ci", "origin", "5678efgh"));
		eventSummaryF.setEventSource("ci");
		eventSummaryF.setEventSourceCode("5678efgh");
		eventSummaryF.setEventLatitude(new BigDecimal("0.0"));
		eventSummaryF.setEventLongitude(new BigDecimal("179.85"));
		eventSummaryF.setEventTime(eventTime);
		Event eventF = new Event();
		eventF.addProduct(eventSummaryF);

		/*
		 * Verify that events associate (0.3 degrees apart) when the points do
		 * cross the date line, reversing the order. This checks that
		 * jdbcProductIndex.normalizeLongitude is working in both directions
		 * across the dateline.
		 */

		Assert.assertTrue(associator.eventsAssociated(eventE, eventF));

		// Create the seventh event
		ProductSummary eventSummaryG = new ProductSummary();
		eventSummaryG.setId(new ProductId("us", "origin", "1234abcd"));
		eventSummaryG.setEventSource("us");
		eventSummaryG.setEventSourceCode("1234abcd");
		eventSummaryG.setEventLatitude(new BigDecimal("0.0"));
		eventSummaryG.setEventLongitude(new BigDecimal("50"));
		eventSummaryG.setEventTime(eventTime);
		Event eventG = new Event();
		eventG.addProduct(eventSummaryG);

		// Create the eighth event
		ProductSummary eventSummaryH = new ProductSummary();
		eventSummaryH.setId(new ProductId("ci", "origin", "5678efgh"));
		eventSummaryH.setEventSource("ci");
		eventSummaryH.setEventSourceCode("5678efgh");
		eventSummaryH.setEventLatitude(new BigDecimal("0.0"));
		eventSummaryH.setEventLongitude(new BigDecimal("100"));
		eventSummaryH.setEventTime(eventTime);
		Event eventH = new Event();
		eventH.addProduct(eventSummaryH);

		/*
		 * Verify that events do NOT associate, when they are more than 100km
		 * apart
		 */

		Assert.assertFalse(associator.eventsAssociated(eventG, eventH));

	}

	/**
	 * Create an event with an origin product that has the given parameters
	 * 
	 * @param lat
	 * @param lon
	 * @param d
	 * @return Generated Event
	 */
	private Event createEvent(String source, String code, BigDecimal lat, BigDecimal lon, Date d) {
		ProductSummary p = new ProductSummary();
		ProductId id = new ProductId("testSource", "origin", "abc123");
		p.setId(id);
		p.setEventSource(source);
		p.setEventSourceCode(code);
		p.setEventLatitude(lat);
		p.setEventLongitude(lon);
		p.setEventTime(d);
		Event event = new Event();
		event.addProduct(p);
		return event;
	}
	
	private Event createDifferentEvent(BigDecimal lat, BigDecimal lon, Date d, String source, String type, String code) {
		ProductSummary p = new ProductSummary();
		ProductId id = new ProductId(source, type, code);
		p.setId(id);
		p.setEventLatitude(lat);
		p.setEventLongitude(lon);
		p.setEventTime(d);
		p.setEventSource(source);
		p.setEventSourceCode(code);
		Event event = new Event();
		event.addProduct(p);
		return event;
	}
	
	/**
	 * The associator only returns a SearchRequest, so this is a wrapper
	 * function to return the specified query from the SearchRequest object.
	 * 
	 * @param associator
	 * @param summary
	 * @param index
	 *            Index of the query in the SearchRequest object
	 * @return
	 */
	private ProductIndexQuery getProductIndexQuery(Associator associator,
			ProductSummary summary, int index) {
		SearchRequest request = associator.getSearchRequest(summary);
		List<SearchQuery> queries = request.getQueries();
		return queries.get(index).getProductIndexQuery();
	}
}
