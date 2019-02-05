/*
 * SAXAdapter
 * 
 * $Id$
 * $HeadURL$
 */
package gov.usgs.util;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;


/**
 * SAXAdapter is a sax handler that accumulates element content, which is a
 * common sax handler task.
 * 
 * Users should be cautious because this in some ways removes efficiency gained
 * by handling streaming events, because element content is being buffered. One
 * buffer for each element nesting is maintained, so this works best for shallow
 * documents, whose elements contain little content.
 */
public class SAXAdapter extends DefaultHandler {

    /** Buffers for element content, since it may be delivered in pieces. */
    private LinkedList<StringBuffer> buffers = new LinkedList<StringBuffer>();

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
    }

    /**
     * Override DefaultHandler startElement. Adds a new element content buffer
     * and calls onStartElement.
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
     *             if onStartElement throws a SAXException.
     */
    public final void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
            throws SAXException {
        buffers.add(new StringBuffer());
        onStartElement(uri, localName, qName, attributes);
    }

    /**
     * Override DefaultHandler endElement. Retrieves element content buffer and
     * passes it to onEndElement.
     * 
     * @param uri
     *            element uri.
     * @param localName
     *            element localName.
     * @param qName
     *            element qName.
     * @throws SAXException
     *             if onEndElement throws a SAXException.
     */
    public final void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
        String elementContent = buffers.removeLast().toString();
        onEndElement(uri, localName, qName, elementContent);
    }

    /**
     * Override DefaultHandler characters. Appends content to current element
     * buffer, or skips if before first element.
     * 
     * @param ch
     *            content.
     * @param start
     *            position in content to read.
     * @param length
     *            lenth of content to read.
     * @throws SAXException
     *             never.
     */
    public final void characters(final char[] ch, final int start,
            final int length) throws SAXException {
        if (buffers.size() > 0) {
            buffers.getLast().append(ch, start, length);
        }
    }

    /**
     * Use this handler to parse a string. Wraps string bytes in a
     * ByteArrayInputStream.
     * 
     * @param xml
     *            string containing xml to parse.
     * @return any exception that occurs while parsing, or null if no exceptions
     *         occur.
     */
    public final Exception parse(final String xml) {
        return parse(new ByteArrayInputStream(xml.getBytes()));
    }

    /**
     * Use this handler to parse an input stream. Uses self as a content and
     * error handler in an XMLReader. If an error occurs, use getException to
     * retrieve the error.
     * 
     * @param xml
     *            input stream of xml to parse.
     * @return any exception that occurs while parsing, or null if no exceptions
     *         occur.
     */
    public final Exception parse(InputStream xml) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(this);
            xr.setErrorHandler(this);
            xr.parse(new InputSource(xml));
            return null;
        } catch (Exception e) {
            return e;
        } finally {
            StreamUtils.closeStream(xml);
        }
    }

}
