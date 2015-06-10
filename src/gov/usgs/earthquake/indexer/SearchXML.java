package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.util.XmlUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * XML (de)serialization for SearchRequest and SearchResponse.
 * 
 */
public class SearchXML {

	public static final String INDEXER_XMLNS = "http://earthquake.usgs.gov/distribution/indexer";

	public static final String REQUEST_ELEMENT = "searchrequest";
	public static final String RESPONSE_ELEMENT = "searchresponse";

	public static final String RESULT_ELEMENT = "result";
	public static final String QUERY_ELEMENT = "query";
	public static final String EVENT_ELEMENT = "event";
	public static final String ERROR_ELEMENT = "error";

	public static final String METHOD_ATTRIBUTE = "method";

	public static final String EVENT_SOURCE_ATTRIBUTE = "eventSource";
	public static final String EVENT_SOURCE_CODE_ATTRIBUTE = "eventSourceCode";
	public static final String MIN_EVENT_TIME_ATTRIBUTE = "minEventTime";
	public static final String MAX_EVENT_TIME_ATTRIBUTE = "maxEventTime";
	public static final String MIN_EVENT_LATITUDE_ATTRIBUTE = "minEventLatitude";
	public static final String MAX_EVENT_LATITUDE_ATTRIBUTE = "maxEventLatitude";
	public static final String MIN_EVENT_LONGITUDE_ATTRIBUTE = "minEventLongitude";
	public static final String MAX_EVENT_LONGITUDE_ATTRIBUTE = "maxEventLongitude";
	public static final String MIN_EVENT_DEPTH_ATTRIBUTE = "minEventDepth";
	public static final String MAX_EVENT_DEPTH_ATTRIBUTE = "maxEventDepth";
	public static final String MIN_EVENT_MAGNITUDE_ATTRIBUTE = "minEventMagnitude";
	public static final String MAX_EVENT_MAGNITUDE_ATTRIBUTE = "maxEventMagnitude";
	public static final String MIN_PRODUCT_UPDATE_TIME_ATTRIBUTE = "minProductUpdateTime";
	public static final String MAX_PRODUCT_UPDATE_TIME_ATTRIBUTE = "maxProductUpdateTime";
	public static final String PRODUCT_SOURCE_ATTRIBUTE = "productSource";
	public static final String PRODUCT_TYPE_ATTRIBUTE = "productType";
	public static final String PRODUCT_CODE_ATTRIBUTE = "productCode";
	public static final String PRODUCT_VERSION_ATTRIBUTE = "productVersion";
	public static final String PRODUCT_STATUS_ATTRIBUTE = "productStatus";

	public static final String EVENT_SUMMARY_ELEMENT = "eventSummary";
	public static final String PRODUCT_SUMMARY_ELEMENT = "productSummary";

	public static final String ID_ATTRIBUTE = "id";
	public static final String UPDATE_TIME_ATTRIBUTE = "updateTime";
	public static final String STATUS_ATTRIBUTE = "status";
	public static final String SOURCE_ATTRIBUTE = "source";
	public static final String SOURCE_CODE_ATTRIBUTE = "sourceCode";
	public static final String TIME_ATTRIBUTE = "time";
	public static final String LATITUDE_ATTRIBUTE = "latitude";
	public static final String LONGITUDE_ATTRIBUTE = "longitude";
	public static final String DEPTH_ATTRIBUTE = "depth";
	public static final String MAGNITUDE_ATTRIBUTE = "magnitude";
	public static final String VERSION_ATTRIBUTE = "version";
	public static final String PREFERRED_WEIGHT_ATTRIBUTE = "preferredWeight";

	/**
	 * Parse an input stream with xml to a SearchRequest object.
	 * 
	 * @param in
	 *            the input stream containing xml.
	 * @return the parsed SearchRequest object.
	 */
	public static SearchRequest parseRequest(final InputStream in)
			throws Exception {
		SearchRequestParser parser = new SearchRequestParser();
		XmlUtils.parse(in, parser);
		return parser.getSearchRequest();
	}

	/**
	 * Parse an input stream with xml to a SearchResponse object.
	 * 
	 * @param in
	 *            the input stream containing xml.
	 * @param storage
	 *            the storage where received products are stored.
	 * @return the parsed SearchResponse object.
	 * @throws Exception
	 */
	public static SearchResponse parseResponse(final InputStream in,
			final FileProductStorage storage) throws Exception {
		SearchResponseParser parser = new SearchResponseParser(storage);
		XmlUtils.parse(in, parser);
		return parser.getSearchResponse();
	}

	/**
	 * Convert a SearchRequest object to xml.
	 * 
	 * @param request
	 *            the search request object to convert.
	 * @param out
	 *            the output stream where xml is written.
	 * @throws Exception
	 */
	public static void toXML(final SearchRequest request, final OutputStream out)
			throws Exception {
		OutputStreamWriter outStream = new OutputStreamWriter(out);
		outStream.write("<?xml version=\"1.0\"?>");
		outStream.write("<" + REQUEST_ELEMENT);
		outStream.write(" xmlns=\"" + INDEXER_XMLNS + "\"");
		outStream.write(">");
		List<SearchQuery> queries = request.getQueries();
		for (Iterator<SearchQuery> queryIterator = queries.iterator(); queryIterator
				.hasNext();) {
			SearchQuery query = queryIterator.next();
			outStream.write(getQueryXMLString(query));
		}
		outStream.write("</" + REQUEST_ELEMENT + ">");
		outStream.flush();
	}

	/**
	 * Convert a SearchResponse object to xml.
	 * 
	 * @param response
	 *            the search response object to convert.
	 * @param out
	 *            the output stream where xml is written.
	 * @throws Exception
	 */
	public static void toXML(final SearchResponse response,
			final OutputStream out) throws Exception {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		writer.write("<?xml version=\"1.0\"?>");
		writer.write("<" + RESPONSE_ELEMENT);
		writer.write(" xmlns=\"" + INDEXER_XMLNS + "\"");
		writer.write(" xmlns:product=\""
				+ XmlProductHandler.PRODUCT_XML_NAMESPACE + "\"");
		writer.write(">");
		List<SearchQuery> results = response.getResults();
		for (Iterator<SearchQuery> resultsIterator = results.iterator(); resultsIterator
				.hasNext();) {
			SearchQuery result = resultsIterator.next();
			writer.write("<" + RESULT_ELEMENT);
			writer.write(" " + METHOD_ATTRIBUTE + "=\""
					+ result.getType().getXmlMethodName() + "\"");
			writer.write(">");
			writer.write(getQueryXMLString(result));

			if (result.getType() == SearchMethod.EVENT_DETAIL) {
				EventDetailQuery edResult = (EventDetailQuery) result;
				List<Event> events = edResult.getResult();
				for (Iterator<Event> eventIter = events.iterator(); eventIter
						.hasNext();) {
					Event event = eventIter.next();
					writer.write("<" + EVENT_ELEMENT);
					if (event.getSource() != null)
						writer.write(" " + SOURCE_ATTRIBUTE + "=\""
								+ event.getSource() + "\"");
					if (event.getSourceCode() != null)
						writer.write(" " + SOURCE_CODE_ATTRIBUTE + "=\""
								+ event.getSourceCode() + "\"");
					if (event.getTime() != null)
						writer.write(" " + TIME_ATTRIBUTE + "=\""
								+ XmlUtils.formatDate(event.getTime()) + "\"");
					if (event.getLatitude() != null)
						writer.write(" " + LATITUDE_ATTRIBUTE + "=\""
								+ event.getLatitude().toString() + "\"");
					if (event.getLongitude() != null)
						writer.write(" " + LONGITUDE_ATTRIBUTE + "=\""
								+ event.getLongitude().toString() + "\"");
					if (event.getDepth() != null)
						writer.write(" " + DEPTH_ATTRIBUTE + "=\""
								+ event.getDepth().toString() + "\"");
					if (event.getMagnitude() != null)
						writer.write(" " + MAGNITUDE_ATTRIBUTE + "=\""
								+ event.getMagnitude().toString() + "\"");
					writer.write(">");
					if (!event.getProducts().isEmpty()) {
						Map<String, List<ProductSummary>> products = event
								.getProducts();
						for (Iterator<String> prodIter = products.keySet()
								.iterator(); prodIter.hasNext();) {
							String prodType = prodIter.next();
							for (Iterator<ProductSummary> summaryIter = products
									.get(prodType).iterator(); summaryIter
									.hasNext();) {
								writer.write(getProductSummaryXmlString(summaryIter
										.next()));
							}
						}
					}
					writer.write("</" + EVENT_ELEMENT + ">");
				}
			} else if (result.getType() == SearchMethod.EVENTS_SUMMARY) {
				EventsSummaryQuery esResult = (EventsSummaryQuery) result;
				List<EventSummary> summaries = esResult.getResult();
				for (Iterator<EventSummary> summaryIter = summaries.iterator(); summaryIter
						.hasNext();) {
					writer.write("<" + EVENT_SUMMARY_ELEMENT);
					EventSummary summary = summaryIter.next();
					if (summary.getSource() != null)
						writer.write(" " + SOURCE_ATTRIBUTE + "=\""
								+ summary.getSource() + "\"");
					if (summary.getSourceCode() != null)
						writer.write(" " + SOURCE_CODE_ATTRIBUTE + "=\""
								+ summary.getSourceCode() + "\"");
					if (summary.getTime() != null)
						writer.write(" " + TIME_ATTRIBUTE + "=\""
								+ XmlUtils.formatDate(summary.getTime()) + "\"");
					if (summary.getLatitude() != null)
						writer.write(" " + LATITUDE_ATTRIBUTE + "=\""
								+ summary.getLatitude().toString() + "\"");
					if (summary.getLongitude() != null)
						writer.write(" " + LONGITUDE_ATTRIBUTE + "=\""
								+ summary.getLongitude().toString() + "\"");
					if (summary.getDepth() != null)
						writer.write(" " + DEPTH_ATTRIBUTE + "=\""
								+ summary.getDepth().toString() + "\"");
					if (summary.getMagnitude() != null)
						writer.write(" " + MAGNITUDE_ATTRIBUTE + "=\""
								+ summary.getMagnitude().toString() + "\"");
					writer.write(">");
					if (!summary.getProperties().isEmpty()) {
						Map<String, String> properties = summary
								.getProperties();
						for (Iterator<String> propIter = properties.keySet()
								.iterator(); propIter.hasNext();) {
							String property = propIter.next();
							String value = properties.get(property);
							writer.write("<product:"
									+ XmlProductHandler.PROPERTY_ELEMENT);
							writer.write(" "
									+ XmlProductHandler.PROPERTY_ATTRIBUTE_NAME
									+ "=\"" + property + "\"");
							writer.write(" "
									+ XmlProductHandler.PROPERTY_ATTRIBUTE_VALUE
									+ "=\"" + value + "\"");
							writer.write(" />");
						}
					}
					writer.write("</" + EVENT_SUMMARY_ELEMENT + ">");
				}
			} else if (result.getType() == SearchMethod.PRODUCT_DETAIL) {
				ProductDetailQuery pdResult = (ProductDetailQuery) result;
				List<Product> products = pdResult.getResult();
				writer.flush();
				for (Iterator<Product> prodIter = products.iterator(); prodIter
						.hasNext();) {
					Product product = prodIter.next();
					XmlProductHandler handler = new XmlProductHandler(out, false);
					handler.onBeginProduct(product.getId(),
							product.getStatus(), product.getTrackerURL());
					Map<String, String> properties = product.getProperties();
					for (Iterator<String> propIter = properties.keySet()
							.iterator(); propIter.hasNext();) {
						String name = propIter.next();
						handler.onProperty(product.getId(), name,
								properties.get(name));
					}
					Map<String, List<URI>> links = product.getLinks();
					for (Iterator<String> relIter = links.keySet().iterator(); relIter
							.hasNext();) {
						String relation = relIter.next();
						for (Iterator<URI> uriIter = links.get(relation)
								.iterator(); uriIter.hasNext();) {
							URI href = uriIter.next();
							handler.onLink(product.getId(), relation, href);
						}
					}
					Map<String, Content> contents = product.getContents();
					for (Iterator<String> pathIter = contents.keySet()
							.iterator(); pathIter.hasNext();) {
						String path = pathIter.next();
						handler.onContent(product.getId(), path,
								contents.get(path));
					}
					if (product.getSignature() != null)
						handler.onSignature(product.getId(),
								product.getSignature());
					handler.onEndProduct(product.getId());
				}
			} else if (result.getType() == SearchMethod.PRODUCTS_SUMMARY) {
				ProductsSummaryQuery psQuery = (ProductsSummaryQuery) result;
				List<ProductSummary> summaries = psQuery.getResult();
				for (Iterator<ProductSummary> summaryIter = summaries
						.iterator(); summaryIter.hasNext();) {
					writer.write(getProductSummaryXmlString(summaryIter.next()));
				}
			}

			writer.write("</" + RESULT_ELEMENT + ">");
		}
		writer.write("</" + RESPONSE_ELEMENT + ">");
		writer.flush();
	}

	/**
	 * Private  helper method to convert query elements into XML
	 * @param query
	 * 		the query element to convert to xml
	 * @return an xml string representing the query object
	 */
	private static String getQueryXMLString(SearchQuery query) {
		StringBuffer queryXmlString = new StringBuffer();
		queryXmlString.append("<" + QUERY_ELEMENT);
		queryXmlString.append(" " + METHOD_ATTRIBUTE + "=\"");
		queryXmlString.append(query.getType().getXmlMethodName());
		queryXmlString.append("\"");
		ProductIndexQuery prodIndexQuery = query.getProductIndexQuery();
		if (prodIndexQuery.getEventSource() != null)
			queryXmlString.append(" " + EVENT_SOURCE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getEventSource() + "\"");
		if (prodIndexQuery.getEventSourceCode() != null)
			queryXmlString.append(" " + EVENT_SOURCE_CODE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getEventSourceCode() + "\"");
		if (prodIndexQuery.getMinEventTime() != null)
			queryXmlString.append(" " + MIN_EVENT_TIME_ATTRIBUTE + "=\""
					+ XmlUtils.formatDate(prodIndexQuery.getMinEventTime())
					+ "\"");
		if (prodIndexQuery.getMaxEventTime() != null)
			queryXmlString.append(" " + MAX_EVENT_TIME_ATTRIBUTE + "=\""
					+ XmlUtils.formatDate(prodIndexQuery.getMaxEventTime())
					+ "\"");
		if (prodIndexQuery.getMinEventLatitude() != null)
			queryXmlString.append(" " + MIN_EVENT_LATITUDE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getMinEventLatitude() + "\"");
		if (prodIndexQuery.getMaxEventLatitude() != null)
			queryXmlString.append(" " + MAX_EVENT_LATITUDE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getMaxEventLatitude() + "\"");
		if (prodIndexQuery.getMinEventLongitude() != null)
			queryXmlString.append(" " + MIN_EVENT_LONGITUDE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getMinEventLongitude() + "\"");
		if (prodIndexQuery.getMaxEventLongitude() != null)
			queryXmlString.append(" " + MAX_EVENT_LONGITUDE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getMaxEventLongitude() + "\"");
		if (prodIndexQuery.getMinEventDepth() != null)
			queryXmlString.append(" " + MIN_EVENT_DEPTH_ATTRIBUTE + "=\""
					+ prodIndexQuery.getMinEventDepth() + "\"");
		if (prodIndexQuery.getMaxEventDepth() != null)
			queryXmlString.append(" " + MAX_EVENT_DEPTH_ATTRIBUTE + "=\""
					+ prodIndexQuery.getMaxEventDepth() + "\"");
		if (prodIndexQuery.getMinEventMagnitude() != null)
			queryXmlString.append(" " + MIN_EVENT_MAGNITUDE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getMinEventMagnitude() + "\"");
		if (prodIndexQuery.getMaxEventMagnitude() != null)
			queryXmlString.append(" " + MAX_EVENT_MAGNITUDE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getMaxEventMagnitude() + "\"");
		if (prodIndexQuery.getMinProductUpdateTime() != null)
			queryXmlString.append(" "
					+ MIN_PRODUCT_UPDATE_TIME_ATTRIBUTE
					+ "=\""
					+ XmlUtils.formatDate(prodIndexQuery
							.getMinProductUpdateTime()) + "\"");
		if (prodIndexQuery.getMaxProductUpdateTime() != null)
			queryXmlString.append(" "
					+ MAX_PRODUCT_UPDATE_TIME_ATTRIBUTE
					+ "=\""
					+ XmlUtils.formatDate(prodIndexQuery
							.getMaxProductUpdateTime()) + "\"");
		if (prodIndexQuery.getProductSource() != null)
			queryXmlString.append(" " + PRODUCT_SOURCE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getProductSource() + "\"");
		if (prodIndexQuery.getProductType() != null)
			queryXmlString.append(" " + PRODUCT_TYPE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getProductType() + "\"");
		if (prodIndexQuery.getProductCode() != null)
			queryXmlString.append(" " + PRODUCT_CODE_ATTRIBUTE + "=\""
					+ prodIndexQuery.getProductCode() + "\"");
		if (prodIndexQuery.getProductVersion() != null)
			queryXmlString.append(" " + PRODUCT_VERSION_ATTRIBUTE + "=\""
					+ prodIndexQuery.getProductVersion() + "\"");
		if (prodIndexQuery.getProductStatus() != null)
			queryXmlString.append(" " + PRODUCT_STATUS_ATTRIBUTE + "=\""
					+ prodIndexQuery.getProductStatus() + "\"");

		queryXmlString.append(">");

		Iterator<ProductId> ids = prodIndexQuery.getProductIds().iterator();
		while (ids.hasNext()) {
			queryXmlString.append("<" + PRODUCT_SUMMARY_ELEMENT);
			queryXmlString.append(" " + ID_ATTRIBUTE + "=\""
					+ ids.next().toString() + "\"");
			queryXmlString.append(" />");
		}

		queryXmlString.append("</" + QUERY_ELEMENT + ">");

		return queryXmlString.toString();
	}

	/**
	 * Private  helper method to convert product summary elements into XML
	 * @param summary
	 * 		the product summary element to convert to xml
	 * @return an xml string representing the product summary object
	 */
	private static String getProductSummaryXmlString(ProductSummary summary) {
		String summaryXmlString = "";
		summaryXmlString += "<" + PRODUCT_SUMMARY_ELEMENT;
		if (summary.getId() != null)
			summaryXmlString += " " + ID_ATTRIBUTE + "=\""
					+ summary.getId().toString() + "\"";
		if (summary.getId().getUpdateTime() != null)
			summaryXmlString += " " + UPDATE_TIME_ATTRIBUTE + "=\""
					+ XmlUtils.formatDate(summary.getId().getUpdateTime())
					+ "\"";
		if (summary.getStatus() != null)
			summaryXmlString += " " + STATUS_ATTRIBUTE + "=\""
					+ summary.getStatus() + "\"";
		if (summary.getSource() != null)
			summaryXmlString += " " + SOURCE_ATTRIBUTE + "=\""
					+ summary.getSource() + "\"";
		if (summary.getCode() != null)
			summaryXmlString += " " + SOURCE_CODE_ATTRIBUTE + "=\"" +
					summary.getCode() + "\"";
		if (summary.getEventTime() != null)
			summaryXmlString += " " + TIME_ATTRIBUTE + "=\""
					+ XmlUtils.formatDate(summary.getEventTime()) + "\"";
		if (summary.getEventLatitude() != null)
			summaryXmlString += " " + LATITUDE_ATTRIBUTE + "=\""
					+ summary.getEventLatitude().toString() + "\"";
		if (summary.getEventLongitude() != null)
			summaryXmlString += " " + LONGITUDE_ATTRIBUTE + "=\""
					+ summary.getEventLongitude().toString() + "\"";
		if (summary.getEventDepth() != null)
			summaryXmlString += " " + DEPTH_ATTRIBUTE + "=\""
					+ summary.getEventDepth().toString() + "\"";
		if (summary.getEventMagnitude() != null)
			summaryXmlString += " " + MAGNITUDE_ATTRIBUTE + "=\""
					+ summary.getEventMagnitude().toString() + "\"";
		if (summary.getVersion() != null)
			summaryXmlString += " " + VERSION_ATTRIBUTE + "=\""
					+ summary.getVersion() + "\"";
		summaryXmlString += " " + PREFERRED_WEIGHT_ATTRIBUTE + "=\""
				+ summary.getPreferredWeight() + "\"";
		summaryXmlString += ">";

		Map<String, String> properties = summary.getProperties();
		for (Iterator<String> propIter = properties.keySet().iterator(); propIter
				.hasNext();) {
			String name = propIter.next();
			String value = properties.get(name);
			summaryXmlString += "<product:"
					+ XmlProductHandler.PROPERTY_ELEMENT;
			summaryXmlString += " " + XmlProductHandler.PROPERTY_ATTRIBUTE_NAME
					+ "=\"" + name + "\"";
			summaryXmlString += " "
					+ XmlProductHandler.PROPERTY_ATTRIBUTE_VALUE + "=\""
					+ value + "\"";
			summaryXmlString += " />";
		}

		Map<String, List<URI>> links = summary.getLinks();
		for (Iterator<String> linkIter = links.keySet().iterator(); linkIter
				.hasNext();) {
			String relation = linkIter.next();
			for (Iterator<URI> uriIter = links.get(relation).iterator(); uriIter
					.hasNext();) {
				URI href = uriIter.next();
				summaryXmlString += "<product:"
						+ XmlProductHandler.LINK_ELEMENT;
				summaryXmlString += " "
						+ XmlProductHandler.LINK_ATTRIBUTE_RELATION + "=\""
						+ relation + "\"";
				summaryXmlString += " " + XmlProductHandler.LINK_ATTRIBUTE_HREF
						+ "=\"" + href + "\"";
				summaryXmlString += " />";
			}
		}

		summaryXmlString += "</" + PRODUCT_SUMMARY_ELEMENT + ">";
		return summaryXmlString;
	}

}
