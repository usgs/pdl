package gov.usgs.earthquake.indexer;

import java.math.BigDecimal;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.util.XmlUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SearchRequestParser extends DefaultHandler {

	/** The request being parsed. */
	private SearchRequest searchRequest;

	/** The query being parsed. */
	private SearchQuery searchQuery;

	public SearchRequestParser() {
	}

	public SearchRequest getSearchRequest() {
		return searchRequest;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (SearchXML.INDEXER_XMLNS.equals(uri)) {
			if (SearchXML.REQUEST_ELEMENT.equals(localName)) {
				searchRequest = new SearchRequest();
			} else if (SearchXML.PRODUCT_SUMMARY_ELEMENT.equals(localName)) {
				if (searchQuery == null) {
					throw new SAXException(
							"Expected searchQuery element around productSummary element");
				} else {
					ProductId id = ProductId.parse(XmlUtils.getAttribute(
							attributes, uri, SearchXML.ID_ATTRIBUTE));
					if (id != null) {
						searchQuery.getProductIndexQuery().getProductIds()
								.add(id);
					} else {
						throw new SAXException(
								"Expected id attribute on productSummary element");
					}
				}
			} else if (SearchXML.QUERY_ELEMENT.equals(localName)) {
				String method = XmlUtils.getAttribute(attributes,
						SearchXML.INDEXER_XMLNS, SearchXML.METHOD_ATTRIBUTE);
				String value;

				ProductIndexQuery query = new ProductIndexQuery();
				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.EVENT_SOURCE_ATTRIBUTE);
				query.setEventSource(value);

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.EVENT_SOURCE_CODE_ATTRIBUTE);
				query.setEventSourceCode(value);

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MIN_EVENT_TIME_ATTRIBUTE);
				if (value != null) {
					query.setMinEventTime(XmlUtils.getDate(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MAX_EVENT_TIME_ATTRIBUTE);
				if (value != null) {
					query.setMaxEventTime(XmlUtils.getDate(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MIN_EVENT_LATITUDE_ATTRIBUTE);
				if (value != null) {
					query.setMinEventLatitude(new BigDecimal(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MAX_EVENT_LATITUDE_ATTRIBUTE);
				if (value != null) {
					query.setMaxEventLatitude(new BigDecimal(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MIN_EVENT_LONGITUDE_ATTRIBUTE);
				if (value != null) {
					query.setMinEventLongitude(new BigDecimal(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MAX_EVENT_LONGITUDE_ATTRIBUTE);
				if (value != null) {
					query.setMaxEventLongitude(new BigDecimal(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MIN_EVENT_DEPTH_ATTRIBUTE);
				if (value != null) {
					query.setMinEventDepth(new BigDecimal(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MAX_EVENT_DEPTH_ATTRIBUTE);
				if (value != null) {
					query.setMaxEventDepth(new BigDecimal(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MIN_EVENT_MAGNITUDE_ATTRIBUTE);
				if (value != null) {
					query.setMinEventMagnitude(new BigDecimal(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MAX_EVENT_MAGNITUDE_ATTRIBUTE);
				if (value != null) {
					query.setMaxEventMagnitude(new BigDecimal(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MIN_PRODUCT_UPDATE_TIME_ATTRIBUTE);
				if (value != null) {
					query.setMinProductUpdateTime(XmlUtils.getDate(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MAX_PRODUCT_UPDATE_TIME_ATTRIBUTE);
				if (value != null) {
					query.setMaxProductUpdateTime(XmlUtils.getDate(value));
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.PRODUCT_SOURCE_ATTRIBUTE);
				if (value != null) {
					query.setProductSource(value);
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.PRODUCT_TYPE_ATTRIBUTE);
				if (value != null) {
					query.setProductType(value);
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.PRODUCT_CODE_ATTRIBUTE);
				if (value != null) {
					query.setProductCode(value);
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.PRODUCT_VERSION_ATTRIBUTE);
				if (value != null) {
					query.setProductVersion(value);
				}

				value = XmlUtils.getAttribute(attributes, uri,
						SearchXML.PRODUCT_STATUS_ATTRIBUTE);
				if (value != null) {
					query.setProductStatus(value);
				}

				searchQuery = SearchQuery.getSearchQuery(
						SearchMethod.fromXmlMethodName(method), query);
			}
		} else if (XmlProductHandler.PRODUCT_XML_NAMESPACE.equals(uri)) {
			// Possible inclusion of properties in the future
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (SearchXML.INDEXER_XMLNS.equals(uri)) {
			if (SearchXML.REQUEST_ELEMENT.equals(localName)) {
				// search request, done
			} else if (SearchXML.QUERY_ELEMENT.equals(localName)) {
				searchRequest.addQuery(searchQuery);
				searchQuery = null;
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
	}

}
