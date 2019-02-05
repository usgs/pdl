package gov.usgs.earthquake.indexer;

/**
 * Different types of searches that are supported.
 */
public enum SearchMethod {
	/** Summary for multiple events. */
	EVENTS_SUMMARY("getEventSummary"),

	/** Detail for one event. */
	EVENT_DETAIL("getEvent"),

	/** Summary for multiple products. */
	PRODUCTS_SUMMARY("getProductSummary"),

	/** Detail for one product. */
	PRODUCT_DETAIL("getProduct");

	private String xmlMethodName;

	private SearchMethod(final String xmlMethodName) {
		this.xmlMethodName = xmlMethodName;
	}

	/**
	 * @return The XML string used to represent this response type.
	 */
	public String getXmlMethodName() {
		return xmlMethodName;
	}

	/**
	 * Get the enumerated value for the given xml string.
	 * 
	 * @param xmlMethodName
	 *            the xml name.
	 * @return null if xmlMethodName is unknown.
	 */
	public static SearchMethod fromXmlMethodName(
			final String xmlMethodName) {
		if (xmlMethodName == null) {
			return null;
		}

		if (EVENTS_SUMMARY.getXmlMethodName().equals(xmlMethodName)) {
			return EVENTS_SUMMARY;
		} else if (EVENT_DETAIL.getXmlMethodName().equals(xmlMethodName)) {
			return EVENT_DETAIL;
		} else if (PRODUCTS_SUMMARY.getXmlMethodName().equals(xmlMethodName)) {
			return PRODUCTS_SUMMARY;
		} else if (PRODUCT_DETAIL.getXmlMethodName().equals(xmlMethodName)) {
			return PRODUCT_DETAIL;
		}

		return null;
	}
}
