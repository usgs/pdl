package gov.usgs.earthquake.indexer;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.util.XmlUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SearchResponseParser extends DefaultHandler {

	private SearchResponse response = null;

	private SearchQuery query = null;

	private ProductSummary pSummary = null;
	private EventSummary eSummary = null;

	private Event event = null;

	private boolean inQueryElement = false;
	private boolean inErrorElement = false;

	private FileProductStorage storage;
	private SearchResponseXmlProductSource productHandler = null;

	public SearchResponseParser(final FileProductStorage storage) {
		this.storage = storage;
	}

	public SearchResponse getSearchResponse() {
		return response;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (productHandler != null) {
			productHandler.startElement(uri, localName, qName, attributes);
		} else if (SearchXML.INDEXER_XMLNS.equals(uri)) {
			if (SearchXML.RESPONSE_ELEMENT.equals(localName)) {
				response = new SearchResponse();
			} else if (SearchXML.RESULT_ELEMENT.equals(localName)) {
				if (response == null)
					throw new SAXException(
							"Unexpected result element without response element parent.");
				SearchMethod method = SearchMethod.fromXmlMethodName(XmlUtils
						.getAttribute(attributes, uri,
								SearchXML.METHOD_ATTRIBUTE));
				query = SearchQuery.getSearchQuery(method,
						new ProductIndexQuery());
				// create results container now
				if (query instanceof EventDetailQuery) {
					((EventDetailQuery) query)
							.setResult(new ArrayList<Event>());
				} else if (query instanceof EventsSummaryQuery) {
					((EventsSummaryQuery) query)
							.setResult(new ArrayList<EventSummary>());
				} else if (query instanceof ProductDetailQuery) {
					((ProductDetailQuery) query)
							.setResult(new ArrayList<Product>());
				} else if (query instanceof ProductsSummaryQuery) {
					((ProductsSummaryQuery) query)
							.setResult(new ArrayList<ProductSummary>());
				}
			} else if (SearchXML.QUERY_ELEMENT.equals(localName)) {
				if (query == null)
					throw new SAXException(
							"Unexpected query element without result element parent.");
				inQueryElement = true;
				ProductIndexQuery piQuery = query.getProductIndexQuery();

				// Update the ProductIndexQuery with each given attribute
				// Event Source Attribute
				String eventSource = XmlUtils.getAttribute(attributes, uri,
						SearchXML.EVENT_SOURCE_ATTRIBUTE);
				if (eventSource != null) {
					piQuery.setEventSource(eventSource);
				}
				// Event Source Code Attribute
				String eventSourceCode = XmlUtils.getAttribute(attributes, uri,
						SearchXML.EVENT_SOURCE_CODE_ATTRIBUTE);
				if (eventSourceCode != null) {
					piQuery.setEventSourceCode(eventSourceCode);
				}
				// Max Event Depth Attribute
				String maxEventDepth = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MAX_EVENT_DEPTH_ATTRIBUTE);
				if (maxEventDepth != null) {
					piQuery.setMaxEventDepth(new BigDecimal(maxEventDepth));
				}
				// Max Event Latitude Attribute
				String maxEventLatitude = XmlUtils.getAttribute(attributes,
						uri, SearchXML.MAX_EVENT_LATITUDE_ATTRIBUTE);
				if (maxEventLatitude != null) {
					piQuery.setMaxEventLatitude(new BigDecimal(maxEventLatitude));
				}
				// Max Event Longitude Attribute
				String maxEventLongitude = XmlUtils.getAttribute(attributes,
						uri, SearchXML.MAX_EVENT_LONGITUDE_ATTRIBUTE);
				if (maxEventLongitude != null) {
					piQuery.setMaxEventLongitude(new BigDecimal(
							maxEventLongitude));
				}
				// Max Event Magnitude Attribute
				String maxEventMagnitude = XmlUtils.getAttribute(attributes,
						uri, SearchXML.MAX_EVENT_MAGNITUDE_ATTRIBUTE);
				if (maxEventMagnitude != null) {
					piQuery.setMaxEventMagnitude(new BigDecimal(
							maxEventMagnitude));
				}
				// Max Event Time Attribute
				String maxEventTime = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MAX_EVENT_TIME_ATTRIBUTE);
				if (maxEventTime != null) {
					piQuery.setMaxEventTime(XmlUtils.getDate(maxEventTime));
				}
				// Max Product Update Time Attribute
				String maxProductUpdateTime = XmlUtils.getAttribute(attributes,
						uri, SearchXML.MAX_PRODUCT_UPDATE_TIME_ATTRIBUTE);
				if (maxProductUpdateTime != null) {
					piQuery.setMaxProductUpdateTime(XmlUtils
							.getDate(maxProductUpdateTime));
				}
				// Min Event Depth Attribute
				String minEventDepth = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MIN_EVENT_DEPTH_ATTRIBUTE);
				if (minEventDepth != null) {
					piQuery.setMinEventDepth(new BigDecimal(minEventDepth));
				}
				// Min Event Latitude Attribute
				String minEventLatitude = XmlUtils.getAttribute(attributes,
						uri, SearchXML.MIN_EVENT_LATITUDE_ATTRIBUTE);
				if (minEventLatitude != null) {
					piQuery.setMinEventLatitude(new BigDecimal(minEventLatitude));
				}
				// Min Event Longitude Attribute
				String minEventLongitude = XmlUtils.getAttribute(attributes,
						uri, SearchXML.MIN_EVENT_LONGITUDE_ATTRIBUTE);
				if (minEventLongitude != null) {
					piQuery.setMinEventLongitude(new BigDecimal(
							minEventLongitude));
				}
				// Min Event Magnitude Attribute
				String minEventMagnitude = XmlUtils.getAttribute(attributes,
						uri, SearchXML.MIN_EVENT_MAGNITUDE_ATTRIBUTE);
				if (minEventMagnitude != null) {
					piQuery.setMinEventMagnitude(new BigDecimal(
							minEventMagnitude));
				}
				// Min Event Time Attribute
				String minEventTime = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MIN_EVENT_TIME_ATTRIBUTE);
				if (minEventTime != null) {
					piQuery.setMinEventTime(XmlUtils.getDate(minEventTime));
				}
				// Min Product Update Time Attribute
				String minProductUpdateTime = XmlUtils.getAttribute(attributes,
						uri, SearchXML.MIN_PRODUCT_UPDATE_TIME_ATTRIBUTE);
				if (minProductUpdateTime != null) {
					piQuery.setMinProductUpdateTime(XmlUtils
							.getDate(minProductUpdateTime));
				}
				// Product Code Attribute
				String productCode = XmlUtils.getAttribute(attributes, uri,
						SearchXML.PRODUCT_CODE_ATTRIBUTE);
				if (productCode != null) {
					piQuery.setProductCode(productCode);
				}
				// Product Source Attribute
				String productSource = XmlUtils.getAttribute(attributes, uri,
						SearchXML.PRODUCT_SOURCE_ATTRIBUTE);
				if (productSource != null) {
					piQuery.setProductSource(productSource);
				}
				// Product Status Attribute
				String productStatus = XmlUtils.getAttribute(attributes, uri,
						SearchXML.PRODUCT_STATUS_ATTRIBUTE);
				if (productStatus != null) {
					piQuery.setProductStatus(productStatus);
				}
				// Product Type Attribute
				String productType = XmlUtils.getAttribute(attributes, uri,
						SearchXML.PRODUCT_TYPE_ATTRIBUTE);
				if (productType != null) {
					piQuery.setProductType(productType);
				}
				// Product Version Attribute
				String productVersion = XmlUtils.getAttribute(attributes, uri,
						SearchXML.PRODUCT_VERSION_ATTRIBUTE);
				if (productVersion != null) {
					piQuery.setProductVersion(productVersion);
				}

				// Set result type. At the moment we ony support the "current"
				// type.
				piQuery.setResultType(ProductIndexQuery.RESULT_TYPE_CURRENT);
			} else if (SearchXML.PRODUCT_SUMMARY_ELEMENT.equals(localName)) {
				if (inQueryElement) {
					// This product summary is being used to pass ID information
					// for the query
					ProductIndexQuery piQuery = query.getProductIndexQuery();
					piQuery.getProductIds().add(
							ProductId.parse(XmlUtils.getAttribute(attributes,
									uri, SearchXML.ID_ATTRIBUTE)));
				} else {
					// This is a more complete returned product summary
					pSummary = new ProductSummary();

					// Set the attributes of the ProductSummary
					// Depth attribute
					String depth = XmlUtils.getAttribute(attributes, uri,
							SearchXML.DEPTH_ATTRIBUTE);
					if (depth != null) {
						pSummary.setEventDepth(new BigDecimal(depth));
					}
					// Latitude attribute
					String latitude = XmlUtils.getAttribute(attributes, uri,
							SearchXML.LATITUDE_ATTRIBUTE);
					if (latitude != null) {
						pSummary.setEventLatitude(new BigDecimal(latitude));
					}
					// Longitude attribute
					String longitude = XmlUtils.getAttribute(attributes, uri,
							SearchXML.LONGITUDE_ATTRIBUTE);
					if (longitude != null) {
						pSummary.setEventLongitude(new BigDecimal(longitude));
					}
					// Magnitude attribute
					String magnitude = XmlUtils.getAttribute(attributes, uri,
							SearchXML.MAGNITUDE_ATTRIBUTE);
					if (magnitude != null) {
						pSummary.setEventMagnitude(new BigDecimal(magnitude));
					}
					// Event Source attribute
					String eventSource = XmlUtils.getAttribute(attributes, uri,
							SearchXML.EVENT_SOURCE_ATTRIBUTE);
					if (eventSource != null) {
						pSummary.setEventSource(eventSource);
					}
					// Event Source Code attribute
					String eventSourceCode = XmlUtils.getAttribute(attributes,
							uri, SearchXML.EVENT_SOURCE_CODE_ATTRIBUTE);
					if (eventSourceCode != null) {
						pSummary.setEventSourceCode(eventSourceCode);
					}
					// Time attribute
					String time = XmlUtils.getAttribute(attributes, uri,
							SearchXML.TIME_ATTRIBUTE);
					if (time != null) {
						pSummary.setEventTime(XmlUtils.getDate(time));
					}
					// ID attribute
					String id = XmlUtils.getAttribute(attributes, uri,
							SearchXML.ID_ATTRIBUTE);
					if (id != null) {
						pSummary.setId(ProductId.parse(id));
					}
					// Preferred Weight attribute
					String preferredWeight = XmlUtils.getAttribute(attributes,
							uri, SearchXML.PREFERRED_WEIGHT_ATTRIBUTE);
					if (preferredWeight != null) {
						pSummary.setPreferredWeight(Integer
								.parseInt(preferredWeight));
					}
					// Status attribute
					String status = XmlUtils.getAttribute(attributes, uri,
							SearchXML.STATUS_ATTRIBUTE);
					if (status != null) {
						pSummary.setStatus(status);
					}
					// Version attribute
					String version = XmlUtils.getAttribute(attributes, uri,
							SearchXML.VERSION_ATTRIBUTE);
					if (version != null) {
						pSummary.setVersion(version);
					}
				}
			} else if (SearchXML.EVENT_ELEMENT.equals(localName)) {
				event = new Event();
				// Nothing further needs to be done here as properties are set
				// by ProductSummaries for the event
			} else if (SearchXML.EVENT_SUMMARY_ELEMENT.equals(localName)) {
				eSummary = new EventSummary();

				// Configure EventSummary attributes
				// Depth attribute
				String depth = XmlUtils.getAttribute(attributes, uri,
						SearchXML.DEPTH_ATTRIBUTE);
				if (depth != null) {
					eSummary.setDepth(new BigDecimal(depth));
				}
				// Latitude attribute
				String latitude = XmlUtils.getAttribute(attributes, uri,
						SearchXML.LATITUDE_ATTRIBUTE);
				if (latitude != null) {
					eSummary.setLatitude(new BigDecimal(latitude));
				}
				// Longitude attribute
				String longitude = XmlUtils.getAttribute(attributes, uri,
						SearchXML.LONGITUDE_ATTRIBUTE);
				if (longitude != null) {
					eSummary.setLongitude(new BigDecimal(longitude));
				}
				// Magnitude attribute
				String magnitude = XmlUtils.getAttribute(attributes, uri,
						SearchXML.MAGNITUDE_ATTRIBUTE);
				if (magnitude != null) {
					eSummary.setMagnitude(new BigDecimal(magnitude));
				}
				// Source attribute
				String source = XmlUtils.getAttribute(attributes, uri,
						SearchXML.SOURCE_ATTRIBUTE);
				if (source != null) {
					eSummary.setSource(source);
				}
				// Source code attribute
				String sourceCode = XmlUtils.getAttribute(attributes, uri,
						SearchXML.SOURCE_CODE_ATTRIBUTE);
				if (sourceCode != null) {
					eSummary.setSourceCode(sourceCode);
				}
				// Time attribute
				String time = XmlUtils.getAttribute(attributes, uri,
						SearchXML.TIME_ATTRIBUTE);
				if (time != null) {
					eSummary.setTime(XmlUtils.getDate(time));
				}
			} else if (SearchXML.ERROR_ELEMENT.equals(localName)) {
				inErrorElement = true;
			}
		} else if (XmlProductHandler.PRODUCT_XML_NAMESPACE.equals(uri)) {
			if (XmlProductHandler.PROPERTY_ELEMENT.equals(localName)) {
				if (pSummary != null) {
					// Currently working on a Product Summary
					pSummary.getProperties()
							.put(XmlUtils.getAttribute(attributes, uri,
									XmlProductHandler.PROPERTY_ATTRIBUTE_NAME),
									XmlUtils.getAttribute(
											attributes,
											uri,
											XmlProductHandler.PROPERTY_ATTRIBUTE_VALUE));
				} else if (eSummary != null) {
					// Currently working on an Event Summary
					eSummary.getProperties()
							.put(XmlUtils.getAttribute(attributes, uri,
									XmlProductHandler.PROPERTY_ATTRIBUTE_NAME),
									XmlUtils.getAttribute(
											attributes,
											uri,
											XmlProductHandler.PROPERTY_ATTRIBUTE_VALUE));
				} else if (inQueryElement) {
					// Currently working on a query
					// TODO This is defined in the schema, but not in the class
					// for either query
				} else {
					throw new SAXException(
							"Property element without appropriate parent encountered.");
				}
			} else if (XmlProductHandler.LINK_ELEMENT.equals(localName)) {
				if (pSummary != null) {
					// This link is occurring as part of a product summary
					String relation = XmlUtils.getAttribute(attributes, uri,
							XmlProductHandler.LINK_ATTRIBUTE_RELATION);
					Map<String, List<URI>> links = pSummary.getLinks();
					if (links.containsKey(relation))
						links.get(relation)
								.add(URI.create(XmlUtils.getAttribute(
										attributes, uri,
										XmlProductHandler.LINK_ATTRIBUTE_HREF)));
					else {
						List<URI> newList = new ArrayList<URI>();
						newList.add(URI.create(XmlUtils.getAttribute(
								attributes, uri,
								XmlProductHandler.LINK_ATTRIBUTE_HREF)));
						links.put(relation, newList);
					}
				} else {
					throw new SAXException(
							"Link element without appropriate parent encountered.");
				}
			} else if (XmlProductHandler.PRODUCT_ELEMENT.equals(localName)) {
				// We are starting to process a product and need to pass data
				// through
				// until that is complete.
				productHandler = new SearchResponseXmlProductSource(storage);
				productHandler.startElement(uri, localName, qName, attributes);
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (productHandler != null) {
			productHandler.endElement(uri, localName, qName);
			if (XmlProductHandler.PRODUCT_ELEMENT.equals(localName)) {
				ProductDetailQuery pdQuery = (ProductDetailQuery) query;
				pdQuery.getResult().add(productHandler.getProduct());
				productHandler = null;
			}
		} else if (SearchXML.INDEXER_XMLNS.equals(uri)) {
			if (SearchXML.RESPONSE_ELEMENT.equals(localName)) {
				// Do nothing, this is the end element of the document.
			} else if (SearchXML.RESULT_ELEMENT.equals(localName)) {
				// One of the results has completed
				if (response == null)
					throw new SAXException(
							"result element found without response parent");
				else {
					response.addResult(query);
					query = null;
				}
			} else if (SearchXML.QUERY_ELEMENT.equals(localName)) {
				// The query element of a result has completed
				// Because the ProductIndexQuery is part of the
				// query object controlled by the result element
				// nothing particular needs to be done other than
				// set the flag for being in the query element to false
				inQueryElement = false;
			} else if (SearchXML.PRODUCT_SUMMARY_ELEMENT.equals(localName)) {
				if (inQueryElement) {
					// Nothing needs to be done because this was just used
					// to get the product ID into the list of IDs for the
					// query.
				} else {
					if (event != null) {
						// We're adding product summaries to events.
						Map<String, List<ProductSummary>> eventProducts = event
								.getAllProducts();
						String productType = pSummary.getType();
						if (eventProducts.containsKey(productType)) {
							// Key exists, so just add the product summary to it
							eventProducts.get(productType).add(pSummary);
						} else {
							List<ProductSummary> newList = new ArrayList<ProductSummary>();
							newList.add(pSummary);
							eventProducts.put(productType, newList);
						}
					} else if (query != null
							&& query.getType() == SearchMethod.PRODUCTS_SUMMARY) {
						// This was a product summary query and these are its
						// results
						ProductsSummaryQuery psQuery = (ProductsSummaryQuery) query;
						psQuery.getResult().add(pSummary);
					} else {
						throw new SAXException(
								"productSummary element encountered without recognized parent");
					}
					pSummary = null;
				}
			} else if (SearchXML.EVENT_ELEMENT.equals(localName)) {
				if (query != null
						&& query.getType() == SearchMethod.EVENT_DETAIL) {
					// This was an event detail query and has opened properly
					EventDetailQuery edQuery = (EventDetailQuery) query;
					edQuery.getResult().add(event);
					event = null;
				} else {
					throw new SAXException(
							"event element encountered without recognized parent");
				}
			} else if (SearchXML.EVENT_SUMMARY_ELEMENT.equals(localName)) {
				if (query != null
						&& query.getType() == SearchMethod.EVENTS_SUMMARY) {
					// This was an event summary query and has opened properly
					EventsSummaryQuery esQuery = (EventsSummaryQuery) query;
					esQuery.getResult().add(eSummary);
					esQuery = null;
				} else {
					throw new SAXException(
							"eventSummary element encountered without recognized parent");
				}
			} else if (SearchXML.ERROR_ELEMENT.equals(localName)) {
				inErrorElement = false;
			}
		} else if (XmlProductHandler.PRODUCT_XML_NAMESPACE.equals(uri)) {
			if (XmlProductHandler.PROPERTY_ELEMENT.equals(localName)) {
				// Simple tag is handled when the start element is encountered
			} else if (XmlProductHandler.LINK_ELEMENT.equals(localName)) {
				// Simple tag is handled when the start element is encountered
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (productHandler != null) {
			// Pass through if product is being generated
			productHandler.characters(ch, start, length);
		} else if (inErrorElement) {
			query.setError(new String(ch));
		}
	}

}
