package gov.usgs.earthquake.distribution;

import gov.usgs.util.XmlUtils;

import java.io.InputStream;

public class URLNotificationXMLConverter {

  public static final String PRODUCT_XML_NAMESPACE = "http://earthquake.usgs.gov/distribution/product";

  public static final String NOTIFICATION_ELEMENT = "notification";
  public static final String ATTRIBUTE_PRODUCT_ID = "id";
  public static final String ATTRIBUTE_PRODUCT_UPDATED = "updated";
  public static final String ATTRIBUTE_TRACKER_URL = "trackerURL";
  public static final String ATTRIBUTE_EXPIRES = "expires";
  public static final String ATTRIBUTE_URL = "url";

  /**
   * Converts a URLNotification to XML
   *
   * @param notification
   *                  The URLNotification to be converted
   * @return an XML-formatted string
   */
  public static String toXML(final URLNotification notification) {
    StringBuffer buf = new StringBuffer();

    buf.append("<?xml version=\"1.0\"?>\n");
    // start element
    buf.append("<").append(NOTIFICATION_ELEMENT);
    buf.append(" xmlns=\"").append(
            PRODUCT_XML_NAMESPACE).append("\"");

    // add attributes
    buf.append(" ").append(ATTRIBUTE_PRODUCT_ID)
            .append("=\"").append(notification.getProductId().toString()).append(
            "\"");
    buf
            .append(" ")
            .append(ATTRIBUTE_PRODUCT_UPDATED)
            .append("=\"")
            .append(
                    XmlUtils
                            .formatDate(notification.getProductId().getUpdateTime()))
            .append("\"");
    buf.append(" ").append(ATTRIBUTE_TRACKER_URL)
            .append("=\"").append(notification.getTrackerURL().toString()).append(
            "\"");
    buf.append(" ").append(ATTRIBUTE_EXPIRES).append(
            "=\"").append(XmlUtils.formatDate(notification.getExpirationDate()))
            .append("\"");
    buf.append(" ").append(ATTRIBUTE_URL).append(
            "=\"").append(notification.getProductURL().toString()).append("\"");

    // end element
    buf.append("/>");

    return buf.toString();
  }

  /**
   * Parses an XML message into a URLNotification
   *
   * @param message
   *             The convertee
   *
   * @return A parsed URL notification
   * @throws Exception If parse goes wrong
   */
  public static URLNotification parseXML(final InputStream message) throws Exception{
    URLNotificationParser parser = new URLNotificationParser();
    parser.parse(message);
    return parser.getNotification();
  }

}
