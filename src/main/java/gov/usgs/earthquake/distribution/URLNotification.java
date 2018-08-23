/*
 * URLNotification
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * A URLNotification represents a product that is available via a URL.
 */
public class URLNotification extends DefaultNotification {

	/** Location where product is available. */
	private final URL productURL;

	/**
	 * Construct a URLNotification.
	 * 
	 * @param id
	 *            which product is available.
	 * @param expirationDate
	 *            how long the product is available.
	 * @param trackerURL
	 *            where to send status updates.
	 * @param productURL
	 *            where product is available.
	 */
	public URLNotification(ProductId id, Date expirationDate, URL trackerURL,
			URL productURL) {
		super(id, expirationDate, trackerURL);
		this.productURL = productURL;
	}

	/**
	 * @return Location where this product can be downloaded.
	 */
	public URL getProductURL() {
		return productURL;
	}

	/** A comparison method to see if two notifications are equal. */
	public boolean equals(Notification that) {
		return (that instanceof URLNotification
				&& getExpirationDate().equals(that.getExpirationDate())
				&& getProductId().equals(that.getProductId())
				&& getTrackerURL().equals(that.getTrackerURL()) && getProductURL()
				.equals(((URLNotification) that).getProductURL()));
	}

	/**
	 * Parse an XML URL Notification from an input stream.
	 * 
	 * @param in
	 *            the input stream containing a URLNotification.
	 * @return the parsed URLNotification
	 * @throws Exception
	 */
	public static URLNotification parse(final InputStream in) throws Exception {
		try {
			URLNotificationParser parser = new URLNotificationParser();
			// do actual parsing
			parser.parse(in);
			// return parsed notification
			return parser.getNotification();
		} finally {
			StreamUtils.closeStream(in);
		}
	}

	/**
	 * Generate an XML URL Notification
	 * 
	 * @return the URL notification as an XML string.
	 */
	public String toXML() {
		StringBuffer buf = new StringBuffer();

		buf.append("<?xml version=\"1.0\"?>\n");
		// start element
		buf.append("<").append(URLNotificationParser.NOTIFICATION_ELEMENT);
		buf.append(" xmlns=\"").append(
				URLNotificationParser.PRODUCT_XML_NAMESPACE).append("\"");

		// add attributes
		buf.append(" ").append(URLNotificationParser.ATTRIBUTE_PRODUCT_ID)
				.append("=\"").append(this.getProductId().toString()).append(
						"\"");
		buf
				.append(" ")
				.append(URLNotificationParser.ATTRIBUTE_PRODUCT_UPDATED)
				.append("=\"")
				.append(
						XmlUtils
								.formatDate(this.getProductId().getUpdateTime()))
				.append("\"");
		buf.append(" ").append(URLNotificationParser.ATTRIBUTE_TRACKER_URL)
				.append("=\"").append(this.getTrackerURL().toString()).append(
						"\"");
		buf.append(" ").append(URLNotificationParser.ATTRIBUTE_EXPIRES).append(
				"=\"").append(XmlUtils.formatDate(this.getExpirationDate()))
				.append("\"");
		buf.append(" ").append(URLNotificationParser.ATTRIBUTE_URL).append(
				"=\"").append(this.getProductURL().toString()).append("\"");

		// end element
		buf.append("/>");

		return buf.toString();
	}

}
