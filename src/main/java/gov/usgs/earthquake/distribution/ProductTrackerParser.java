package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;

import gov.usgs.util.SAXAdapter;
import gov.usgs.util.XmlUtils;

import java.net.InetAddress;
import java.net.URL;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Parser for ProductTracker responses.
 */
public class ProductTrackerParser extends SAXAdapter {

	/** The tracker that generated the list being parsed. */
	private URL trackerURL;

	/** A list of parsed updates. */
	private List<ProductTrackerUpdate> updates = new LinkedList<ProductTrackerUpdate>();

	/** The current update being parsed. */
	private ProductTrackerUpdate update = null;

	/** Create a new TrackerUpdateParser. */
	public ProductTrackerParser(final URL trackerURL) {
		this.trackerURL = trackerURL;
	}

	/** Get the parsed updates. */
	public List<ProductTrackerUpdate> getUpdates() {
		return updates;
	}

	/**
	 * SAXAdapter start element handler.
	 * 
	 * @param uri
	 *            element uri.
	 * @param localName
	 *            element localName.
	 * @param qName
	 *            element qName.
	 * @param attributes
	 *            element attributes.
	 * @throws SAXException
	 *             if there is an error.
	 */
	public void onStartElement(final String uri, final String localName,
			final String qName, final Attributes attributes)
			throws SAXException {
		if (localName.equals("update")) {
			try {
				Long sequenceNumber = Long.valueOf(XmlUtils.getAttribute(
						attributes, uri, "sequenceNumber"));
				Date created = XmlUtils.getDate(XmlUtils.getAttribute(
						attributes, uri, "created"));
				InetAddress host = InetAddress.getByName(XmlUtils.getAttribute(
						attributes, uri, "host"));
				ProductId id = new ProductId(XmlUtils.getAttribute(attributes,
						uri, "source"), XmlUtils.getAttribute(attributes,
								uri, "type"), XmlUtils.getAttribute(attributes, uri,
						"code"), XmlUtils.getDate(XmlUtils.getAttribute(
						attributes, uri, "updateTime")));
				String className = XmlUtils.getAttribute(attributes, uri,
						"className");
				update = new ProductTrackerUpdate(trackerURL, sequenceNumber,
						created, host, id, className, null);
			} catch (Exception e) {
				throw new SAXException(e);
			}
		}
	}

	/**
	 * SAXAdapter end element handler. Content only includes characters that
	 * were read from this element, NOT any characters from child elements.
	 * 
	 * @param uri
	 *            element uri.
	 * @param localName
	 *            element localName.
	 * @param qName
	 *            element qName.
	 * @param content
	 *            element content.
	 * @throws SAXException
	 *             if onEndElement throws a SAXException.
	 */
	public void onEndElement(final String uri, final String localName,
			final String qName, final String content) throws SAXException {
		// message is element content
		update.setMessage(content);
		// add update to list of parsed updates
		updates.add(update);
		// reset update to null
		update = null;
	}

}
