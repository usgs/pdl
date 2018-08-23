package gov.usgs.earthquake.dyfi;

import gov.usgs.util.XmlUtils;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class EventDataXMLHandler extends DefaultHandler {

	// XML Element Names
	public static final String DYFI_EVENTDATA_ELEMENT = "event_data";
	public static final String DYFI_EVENT_ELEMENT = "event";
	public static final String DYFI_CDI_SUMMARY_ELEMENT = "cdi_summary";
	public static final String DYFI_PRODUCTS_ELEMENT = "products";

	public static final String DYFI_STOP_PARSING_BEFORE_PRODUCTS = "Stop parsing before list of product files.";

	// XML Attributes
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

	public EventDataXMLHandler(final DYFIProduct dyfi) {
		this.dyfi = dyfi;
	}

	public DYFIProduct getDYFI() {
		return this.dyfi;
	}

	public void setDYFI(final DYFIProduct dyfi) {
		this.dyfi = dyfi;
	}

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
