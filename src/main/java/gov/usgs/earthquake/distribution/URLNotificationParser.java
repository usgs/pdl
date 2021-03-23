/*
 * URLNotificationParser
 */
package gov.usgs.earthquake.distribution;

import java.net.URL;
import java.util.Date;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.SAXAdapter;
import gov.usgs.util.XmlUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class URLNotificationParser extends SAXAdapter {

	/** Namespace for product XML */
	public static final String PRODUCT_XML_NAMESPACE = "http://earthquake.usgs.gov/distribution/product";

	/** static var for notification element */
	public static final String NOTIFICATION_ELEMENT = "notification";
	/** attribute for product id */
	public static final String ATTRIBUTE_PRODUCT_ID = "id";
	/** attribute for updated */
	public static final String ATTRIBUTE_PRODUCT_UPDATED = "updated";
	/** attribute for trackerURL */
	public static final String ATTRIBUTE_TRACKER_URL = "trackerURL";
	/** attribute for expires */
	public static final String ATTRIBUTE_EXPIRES = "expires";
	/** attribute for url */
	public static final String ATTRIBUTE_URL = "url";

	/** The parsed notification. */
	private URLNotification notification;

	/**
	 * Construct a URLNotificationParser. This class is not intended to be
	 * instantiated directly. Instead, use the static URLNotification.parse
	 * method.
	 */
	protected URLNotificationParser() {
	}

	/**
	 * @return the parsed notification
	 */
	public URLNotification getNotification() {
		return notification;
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

		if (!uri.equals(PRODUCT_XML_NAMESPACE)) {
			return;
		}

		if (localName.equals(NOTIFICATION_ELEMENT)) {
			ProductId id = ProductId.parse(XmlUtils.getAttribute(
					attributes, uri, ATTRIBUTE_PRODUCT_ID));
			id.setUpdateTime(XmlUtils.getDate(XmlUtils.getAttribute(
					attributes, uri, ATTRIBUTE_PRODUCT_UPDATED)));

			URL trackerURL = null;
			try {
				trackerURL = new URL(XmlUtils.getAttribute(attributes, uri,
						ATTRIBUTE_TRACKER_URL));
			} catch (Exception e) {
				throw new SAXException("Unable to parse tracker url", e);
			}

			Date expirationDate = XmlUtils.getDate(XmlUtils.getAttribute(
					attributes, uri, ATTRIBUTE_EXPIRES));

			URL productURL = null;
			try {
				productURL = new URL(XmlUtils.getAttribute(attributes, uri,
						ATTRIBUTE_URL));
			} catch (Exception e) {
				throw new SAXException("Unable to parse product url", e);
			}

			notification = new URLNotification(id, expirationDate,
					trackerURL, productURL);
		}
	}

}