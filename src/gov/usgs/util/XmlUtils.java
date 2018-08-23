/*
 * XmlUtils
 *
 * $Id$
 * $HeadURL$
 */
package gov.usgs.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Xml parsing utility functions.
 *
 * @author jmfee
 *
 */
public class XmlUtils {

	public static final Map<String, String> ESCAPES = new HashMap<String, String>();
	static {
		// xml
		ESCAPES.put("&", "&amp;");
		ESCAPES.put("<", "&lt;");
		ESCAPES.put(">", "&gt;");
		ESCAPES.put("\"", "&quot;");
		ESCAPES.put("'", "&apos;");
		// whitespace characters
		ESCAPES.put("\t", "&#x9;"); // tab
		ESCAPES.put("\n", "&#xA;"); // newline
		ESCAPES.put("\r", "&#xD;"); // carriage return
	}

	/**
	 * Convenience method to format a Date as an XML DateTime String.
	 *
	 * @param date
	 *            the date to format.
	 * @return the XML representation as a string.
	 */
	public static String formatDate(final Date date) {
		if (date == null) {
			return null;
		}
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(date.getTime());
		return formatGregorianCalendar(calendar);
	}

	/**
	 * Format a Gregorian Calendar as an XML DateTime String.
	 *
	 * @param calendar
	 *            the calendar to format.
	 * @return the XML representation as a string.
	 */
	public static String formatGregorianCalendar(
			final GregorianCalendar calendar) {
		try {
			return DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(calendar).normalize()
					.toXMLFormat();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Convenience method to parse an XML Date Time into a Date. Only useful
	 * when the XML Date Time is within the Date object time range.
	 *
	 * @param toParse
	 *            the xml date time string to parse.
	 * @return the parsed Date object.
	 */
	public static Date getDate(final String toParse) {
		XMLGregorianCalendar calendar = getXMLGregorianCalendar(toParse);
		if (calendar != null) {
			return new Date(calendar.toGregorianCalendar().getTimeInMillis());
		} else {
			return null;
		}
	}

	/**
	 * Parse an XML Date Time into an XMLGregorianCalendar.
	 *
	 * @param toParse
	 *            the xml date time string to parse.
	 * @return the parsed XMLGregorianCalendar object.
	 */
	public static XMLGregorianCalendar getXMLGregorianCalendar(
			final String toParse) {
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(
					toParse);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Converts an XMLGregorianCalendar to a Date.
	 *
	 * @param xmlDate
	 *            XMLGregorianCalendar to convert.
	 * @return corresponding date object.
	 */
	public static Date getDate(final XMLGregorianCalendar xmlDate) {
		// TODO: is this equivalent to getDate(String) processing above??

		// start with UTC, i.e. no daylight savings time.
		TimeZone timezone = TimeZone.getTimeZone("GMT");

		// adjust timezone to match xmldate
		int offsetMinutes = xmlDate.getTimezone();
		if (offsetMinutes != DatatypeConstants.FIELD_UNDEFINED) {
			timezone.setRawOffset(
			// convert minutes to milliseconds
			offsetMinutes * 60 // seconds per minute
			* 1000 // milliseconds per second
			);
		}

		// use calendar so parsed date will be UTC
		Calendar calendar = Calendar.getInstance(timezone);
		calendar.clear();
		calendar.set(xmlDate.getYear(),
				// xmlcalendar is 1 based, calender is 0 based
				xmlDate.getMonth() - 1, xmlDate.getDay(), xmlDate.getHour(),
				xmlDate.getMinute(), xmlDate.getSecond());
		Date date = calendar.getTime();
		int millis = xmlDate.getMillisecond();
		if (millis != DatatypeConstants.FIELD_UNDEFINED) {
			calendar.setTimeInMillis(calendar.getTimeInMillis() + millis);
		}

		return date;
	}

	/**
	 * Creates an XMLReader and uses handler as a content and error handler.
	 *
	 * @param xml
	 *            source of xml.
	 * @param handler
	 *            SAX handler for xml.
	 * @throws Exception
	 *             if any exceptions occur during parsing.
	 */
	public static void parse(final Object xml, final DefaultHandler handler)
			throws SAXException, IOException, ParserConfigurationException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		xr.setContentHandler(handler);
		xr.setErrorHandler(handler);
		InputStream in = StreamUtils.getInputStream(xml);
		try {
			xr.parse(new InputSource(in));
		} finally {
			StreamUtils.closeStream(in);
		}
	}

	/**
	 * Sometimes parsers do not preserve the namespace for attributes. This
	 * attempts to use the namespace and localname, and, if not available using
	 * the namespace, checks for an attribute using only localname.
	 *
	 * @param attributes
	 *            Attributes object to search.
	 * @param uri
	 *            namespace of attribute.
	 * @param localName
	 *            local name of attribute.
	 * @return value of attribute.
	 */
	public static String getAttribute(final Attributes attributes,
			final String uri, final String localName) {
		String value = attributes.getValue(uri, localName);
		if (value == null) {
			value = attributes.getValue(localName);
		}
		return value;
	}

	/**
	 * Escape a value when writing XML.
	 *
	 * Replaces each character in the ESCAPES map with its escaped value.
	 *
	 * This method should only be used when generating xml manually, since most
	 * xml writers escape automatically.
	 *
	 * @param value
	 *            the value to escape
	 * @return the escaped value.
	 */
	public static String escape(final String value) {
		String escapedValue = value;

		// replace each escapeable character
		Iterator<String> iter = ESCAPES.keySet().iterator();
		while (iter.hasNext()) {
			String raw = iter.next();
			String escaped = ESCAPES.get(raw);
			escapedValue = escapedValue.replace(raw, escaped);
		}

		return escapedValue;
	}

	/**
	 * Unescape a value when reading XML.
	 *
	 * Replaces each escaped character in the ESCAPES map with its unescaped
	 * value.
	 *
	 * This method should only be used when parsing xml manually, since most xml
	 * parsers unescape automatically.
	 *
	 * @param value
	 *            the value to unescape
	 * @return the unescaped value.
	 */
	public static String unescape(final String value) {
		String unescapedValue = value;

		// replace each escapeable character
		Iterator<String> iter = ESCAPES.keySet().iterator();
		while (iter.hasNext()) {
			String raw = iter.next();
			String escaped = ESCAPES.get(raw);
			unescapedValue = unescapedValue.replace(escaped, raw);
		}

		return unescapedValue;
	}
}
