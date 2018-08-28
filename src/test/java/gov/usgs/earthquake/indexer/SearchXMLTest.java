package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.ProductTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class SearchXMLTest {

	private static SearchResponse PRODUCT_SUMMARY_SEARCH_RESPONSE = getProductSummarySearchResponse();
	private static SearchResponse PRODUCT_DETAIL_SEARCH_RESPONSE = getProductDetailSearchResponse();
	private static SearchResponse EVENT_SUMMARY_SEARCH_RESPONSE = getEventSummarySearchResponse();
	private static SearchResponse EVENT_DETAIL_SEARCH_RESPONSE = getEventDetailSearchResponse();

	/**
	 * @return A SearchRequest object for use by other tests.
	 */
	public static SearchRequest getSearchRequest(SearchMethod method) {
		SearchRequest request = new SearchRequest();

		ProductIndexQuery query = new ProductIndexQuery();
		query.setEventSource("testSource");
		query.setEventSourceCode("testEventSourceCode");
		query.setMaxEventDepth(new BigDecimal("123"));
		query.setMaxEventLatitude(new BigDecimal("23.456"));
		query.setMaxEventLongitude(new BigDecimal("123.456"));
		query.setMaxEventMagnitude(new BigDecimal("10.0"));
		query.setMaxEventTime(new Date());
		query.setMaxProductUpdateTime(new Date());
		query.setMinEventDepth(new BigDecimal("1.23"));
		query.setMinEventLatitude(new BigDecimal("-23.456"));
		query.setMinEventLongitude(new BigDecimal("-123.456"));
		query.setMinEventMagnitude(new BigDecimal("0.7"));
		query.setMinEventTime(new Date(new Date().getTime() - 100000000));
		query.setMinProductUpdateTime(new Date(new Date().getTime() - 100000000));
		query.setProductCode("testProductCode");
		query.setProductSource("testProductSource");
		query.setProductStatus("testProductStatus");
		query.setProductType("testProductType");
		query.setProductVersion("testProductVersion");
		query.getProductIds().add(new ProductTest().getProduct().getId());

		SearchQuery searchQuery = SearchQuery.getSearchQuery(method, query);
		request.addQuery(searchQuery);

		return request;
	}

	/**
	 * @return A ProductSummary SearchResponse object for use by other tests.
	 */
	public static SearchResponse getProductSummarySearchResponse() {
		SearchResponse response = new SearchResponse();
		List<SearchQuery> queries = getSearchRequest(
				SearchMethod.PRODUCTS_SUMMARY).getQueries();
		ProductIndexQuery piQuery = queries.get(0).getProductIndexQuery();
		ProductsSummaryQuery query = new ProductsSummaryQuery(piQuery);
		ProductSummary summary = new ProductSummary();
		summary.setId(new ProductId("us", "test-product", "test2", new Date()));
		summary.setEventDepth(BigDecimal.TEN);
		summary.setEventLatitude(BigDecimal.ZERO);
		summary.setEventLongitude(BigDecimal.ZERO);
		summary.setEventMagnitude(BigDecimal.ONE);
		summary.setEventSource("us");
		summary.setEventSourceCode("test1");
		summary.setEventTime(new Date());
		Map<String, List<URI>> links = new HashMap<String, List<URI>>();
		links.put("parent", new ArrayList<URI>());
		links.get("parent").add(URI.create("http://localhost/"));
		summary.setLinks(links);
		summary.setPreferredWeight(100);
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("testproperty", "testvalue");
		summary.setProperties(properties);
		summary.setStatus("test");
		try {
			summary.setTrackerURL(new URL("http://localhost/"));
		} catch (MalformedURLException e) {
			// Ignore
		}
		summary.setVersion("1");
		List<ProductSummary> resultList = new ArrayList<ProductSummary>();
		resultList.add(summary);
		query.setResult(resultList);
		response.addResult(query);
		return response;
	}

	/**
	 * @return A Product SearchResponse object for use by other tests.
	 */
	public static SearchResponse getProductDetailSearchResponse() {
		SearchResponse response = new SearchResponse();
		List<SearchQuery> queries = getSearchRequest(
				SearchMethod.PRODUCT_DETAIL).getQueries();
		ProductIndexQuery piQuery = queries.get(0).getProductIndexQuery();
		ProductDetailQuery query = new ProductDetailQuery(piQuery);
		Product product = new ProductTest().getProduct();
		List<Product> resultList = new ArrayList<Product>();
		resultList.add(product);
		query.setResult(resultList);
		response.addResult(query);
		return response;
	}

	/**
	 * @return An EventSummary SearchResponse object for use by other tests.
	 */
	public static SearchResponse getEventSummarySearchResponse() {
		SearchResponse response = new SearchResponse();
		List<SearchQuery> queries = getSearchRequest(
				SearchMethod.EVENTS_SUMMARY).getQueries();
		ProductIndexQuery piQuery = queries.get(0).getProductIndexQuery();
		EventsSummaryQuery query = new EventsSummaryQuery(piQuery);
		EventSummary summary = new EventSummary();
		summary.setDepth(new BigDecimal(100));
		summary.setLatitude(new BigDecimal(5));
		summary.setLongitude(new BigDecimal(6));
		summary.setMagnitude(new BigDecimal(9));
		summary.setSource("us");
		summary.setSourceCode("testcode");
		summary.setTime(new Date());
		summary.getProperties().put("testprop", "testval");
		summary.getEventCodes().put("us", "test2");
		if (query.getResult() == null) {
			query.setResult(new ArrayList<EventSummary>());
		}
		query.getResult().add(summary);
		response.addResult(query);
		return response;
	}

	/**
	 * @return An Event SearchResponse object for use by other tests.
	 */
	public static SearchResponse getEventDetailSearchResponse() {
		SearchResponse response = new SearchResponse();
		List<SearchQuery> queries = getSearchRequest(SearchMethod.EVENT_DETAIL)
				.getQueries();
		ProductIndexQuery piQuery = queries.get(0).getProductIndexQuery();
		EventDetailQuery query = new EventDetailQuery(piQuery);
		Event event = new Event();
		event.addProduct(new ProductSummary(new ProductTest().getProduct()));
		if (query.getResult() == null) {
			query.setResult(new ArrayList<Event>());
		}
		query.getResult().add(event);
		response.addResult(query);
		return response;
	}

	/**
	 * Convert a SearchRequest object to an from xml, verifying it is the same
	 * before and after.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSearchRequest() throws Exception {
		SearchRequest originalRequest = getSearchRequest(SearchMethod.PRODUCTS_SUMMARY);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		SearchXML.toXML(originalRequest, baos);

		SearchRequest parsedRequest = SearchXML
				.parseRequest(new ByteArrayInputStream(baos.toByteArray()));

		Assert.assertTrue("Parsed request matches original.",
				originalRequest.equals(parsedRequest));
	}

	/**
	 * Convert a ProductSummary SearchResponse object to and from xml, verifying
	 * it is the same before and after.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProductSummarySearchResponse() throws Exception {
		SearchResponse originalResponse = PRODUCT_SUMMARY_SEARCH_RESPONSE;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		SearchXML.toXML(originalResponse, baos);

		SearchResponse parsedResponse = SearchXML.parseResponse(
				new ByteArrayInputStream(baos.toByteArray()), null);

		Assert.assertTrue("Parsed response matches original.",
				originalResponse.equals(parsedResponse));
	}

	/**
	 * Convert a Product SearchResponse object to and from xml, verifying it is
	 * the same before and after.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProductDetailSearchResponse() throws Exception {
		// This feature is not yet supported.

		SearchResponse originalResponse = PRODUCT_DETAIL_SEARCH_RESPONSE;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		SearchXML.toXML(PRODUCT_DETAIL_SEARCH_RESPONSE, baos);

		// System.out.write(baos.toByteArray());

		SearchResponse parsedResponse = SearchXML.parseResponse(
				new ByteArrayInputStream(baos.toByteArray()),
				new FileProductStorage());

		Assert.assertTrue("Parsed response matches original.",
				originalResponse.equals(parsedResponse));
	}

	/**
	 * Convert an EventSummary SearchResponse object to and from xml, verifying
	 * it is the same before and after.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEventSummarySearchResponse() throws Exception {
		SearchResponse originalResponse = EVENT_SUMMARY_SEARCH_RESPONSE;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		SearchXML.toXML(EVENT_SUMMARY_SEARCH_RESPONSE, baos);

		SearchResponse parsedResponse = SearchXML.parseResponse(
				new ByteArrayInputStream(baos.toByteArray()), null);

		Assert.assertTrue(originalResponse.equals(parsedResponse));
	}

	/**
	 * Convert an EventSummary SearchResponse object to and from xml, verifying
	 * it is the same before and after.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEventDetailSearchResponse() throws Exception {
		SearchResponse originalResponse = EVENT_DETAIL_SEARCH_RESPONSE;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		SearchXML.toXML(EVENT_DETAIL_SEARCH_RESPONSE, baos);

		SearchResponse parsedResponse = SearchXML.parseResponse(
				new ByteArrayInputStream(baos.toByteArray()), null);

		Assert.assertTrue(originalResponse.equals(parsedResponse));
	}

	/**
	 * Send a SearchRequest over a socket, and parse the returned
	 * SearchResponse, verifying the objects are identical on the client and
	 * server side.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSearchSocket() throws Exception {
		SearchRequest request = getSearchRequest(SearchMethod.PRODUCTS_SUMMARY);
		// create and startup the server socket
		TestSearchServerSocket server = new TestSearchServerSocket();
		server.setPort(12345);
		server.startup();

		// create the client socket
		SearchSocket client = new SearchSocket(InetAddress.getLocalHost(),
				12345);

		// send the search request and parse the response
		SearchResponse receivedResponse = client.search(request,
				(FileProductStorage) null);

		// The server has received the request, and sent the response
		// make sure everything is consistent
		Assert.assertTrue("Received request equal",
				server.receivedRequest.equals(request));
		Assert.assertTrue("Received response equal",
				receivedResponse.equals(PRODUCT_SUMMARY_SEARCH_RESPONSE));
	}

	/**
	 * A Testing version of the SearchServerSocket.
	 * 
	 * Its search method saves the received search request, and always returns
	 * the same search response.
	 */
	private static class TestSearchServerSocket extends SearchServerSocket {
		public SearchRequest receivedRequest = null;

		public SearchResponse search(final SearchRequest request) {
			this.receivedRequest = request;
			return PRODUCT_SUMMARY_SEARCH_RESPONSE;
		}

	}

}
