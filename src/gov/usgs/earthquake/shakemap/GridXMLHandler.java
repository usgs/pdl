package gov.usgs.earthquake.shakemap;

import gov.usgs.util.XmlUtils;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * An XML Handler to load necessary product information from grid.xml
 * 
 * Accepts a ShakeMap object and updates the properties of that product based on
 * the product's grid.xml file.
 */
public class GridXMLHandler extends DefaultHandler {

	// ShakeMap griddata parameters
	public static final String GRIDDATA_ELEMENT = "grid_data";
	public static final String STOP_PARSING_BEFORE_GRIDDATA =
			"Stop parsing before grid data.";

	private static final String XML_NAME = "name";
	private static final String XML_VALUE = "value";
	private HashMap<String, String> grid = new HashMap<String, String>();

	/**
	 * Construct a new SAX Handler for an grid.xml document.
	 */
	public GridXMLHandler() {}

	/**
	 * @param in
	 *            - the file or stream to parse
	 * @return the ShakeMap associated with this XML handler
	 * @throws Exception
	 */
	public HashMap<String, String> parse(final Object in) throws Exception {
		try {
			XmlUtils.parse(in, this);
		} catch (Exception se) {
			if (!se.getMessage().equals(STOP_PARSING_BEFORE_GRIDDATA)) {
				throw se;
			}
		}
		return this.grid;
	}

	/**
	 * @return the parsed info.
	 */
	public HashMap<String, String> getInfo() {
		return this.grid;
	}


	/**
	 * @param uri
	 *            the uri for this element
	 * @param localName
	 *            the local name for this element
	 * @param qName
	 *            the fully-qualified name for this element
	 * @param attributes
	 *            the attributes of the element
	 */
	public final void startElement(final String uri, final String localName,
			final String qName, final Attributes attributes) throws SAXException {

		if (localName != null && GRIDDATA_ELEMENT.equals(localName)) {
			// don't read the whole grid
			throw new SAXException(STOP_PARSING_BEFORE_GRIDDATA);
		}

		for (int i = 0, len = attributes.getLength(); i < len; i++) {
			//String name = localName + "[" + attributes.getLocalName(i) + "]";
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			this.grid.put(name, value);
		}
	}
}
