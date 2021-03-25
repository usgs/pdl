package gov.usgs.earthquake.dyfi;

import gov.usgs.util.XmlUtils;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for DYFI "eventdata.xml" metadata.
 */
public class EventDataXMLHandler extends DefaultHandler {

	/** XML Element Name for event_data */
	public static final String DYFI_EVENTDATA_ELEMENT = "event_data";
	/** XML Element Name  for event */
	public static final String DYFI_EVENT_ELEMENT = "event";
	/** XML Element Name for cdi_summary */
	public static final String DYFI_CDI_SUMMARY_ELEMENT = "cdi_summary";
	/** XML Element Name for products */
	public static final String DYFI_PRODUCTS_ELEMENT = "products";

	/** Static string to stop parsing before list of products */
	public static final String DYFI_STOP_PARSING_BEFORE_PRODUCTS = "Stop parsing before list of product files.";

	/** Map of XML attributes */
	public static final Map<String, String[]> DYFI_ELEMENT_ATTRIBUTES = new HashMap<String, String[]>();
	static {
		// Statically add all these attributes and associate them to their
		// corresponding elements

		// Currently we only care about the max MMI and number of responses.
		DYFI_ELEMENT_ATTRIBUTES.put(DYFI_CDI_SUMMARY_ELEMENT, new String[] {
				DYFIProduct.DYFI_NUM_RESP_PROPERTY,
				DYFIProduct.DYFI_MAX_MMI_PROPERTY });
	}

	private DYFIProduct dyfi = null;

	/**
	 * Constructor
	 * @param dyfi takes in DYFIProduct
	 */
	public EventDataXMLHandler(final DYFIProduct dyfi) {
		this.dyfi = dyfi;
	}

	/** @return DYFIProduct */
	public DYFIProduct getDYFI() {
		return this.dyfi;
	}

	/** @param dyfi Product to set */
	public void setDYFI(final DYFIProduct dyfi) {
		this.dyfi = dyfi;
	}

	/**
	 *
	 * @param in XML object to parse
	 * @return DYFIProduct
	 * @throws Exception if exception message equals stop_parsing string
	 */
	public DYFIProduct parse(final Object in) throws Exception {
		try {
			XmlUtils.parse(in, this);
		} catch (Exception e) {
			if (!DYFI_STOP_PARSING_BEFORE_PRODUCTS.equals(e.getMessage())) {
				throw e;
			}
		}
		return getDYFI();
	}

	public final void startElement(final String uri, final String localName,
			final String qName, final Attributes attributes)
			throws SAXException {

		if (localName != null && DYFI_PRODUCTS_ELEMENT.equals(localName)) {
			// We don't need the list of product files at this time.
			throw new SAXException(DYFI_STOP_PARSING_BEFORE_PRODUCTS);
		}

		if (DYFI_CDI_SUMMARY_ELEMENT.equals(localName)) {
			dyfi.setNumResponses(attributes
					.getValue(DYFIProduct.DYFI_NUM_RESP_ATTRIBUTE));
			dyfi.setMaxMMI(attributes
					.getValue(DYFIProduct.DYFI_MAX_MMI_ATTRIBUTE));
		}
	}
}
