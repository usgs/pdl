package gov.usgs.earthquake.shakemap;

import gov.usgs.util.XmlUtils;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class InfoXMLHandler extends DefaultHandler {

	private static final String XML_TAG = "tag";
	private static final String XML_NAME = "name";
	private static final String XML_VALUE = "value";
	private HashMap<String, String> info = new HashMap<String, String>();

	/**
	 * Construct a new SAX Handler for an info.xml document.
	 */
	public InfoXMLHandler() {
	}

	/**
	 * @param in
	 *            - the file or stream to parse
	 * @return the ShakeMap associated with this XML handler
	 * @throws Exception
	 */
	public HashMap<String, String> parse(final Object in) throws Exception {
		XmlUtils.parse(in, this);
		return this.info;
	}

	/**
	 * @return the parsed info.
	 */
	public HashMap<String, String> getInfo() {
		return this.info;
	}

	/**
	 * @param uri
	 *            - the uri for this element
	 * @param localName
	 *            - the local name for this element
	 * @param qName
	 *            - the fully-qualified name for this element
	 * @param attributes
	 *            - the attributes of the element
	 */
	public final void startElement(final String uri, final String localName,
			final String qName, final Attributes attributes) {
		if (localName.equals(XML_TAG)) {
			info.put(attributes.getValue(XML_NAME),
					attributes.getValue(XML_VALUE));
		}
	}

}