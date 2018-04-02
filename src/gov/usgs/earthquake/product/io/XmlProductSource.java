/*
 * XmlProductSource
 */
package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.InputStreamContent;
import gov.usgs.earthquake.product.URLContent;
import gov.usgs.earthquake.product.ProductId;

import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;
import gov.usgs.util.Base64;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.net.URI;
import java.net.URL;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;

/**
 * Load a product from an InputStream containing XML.
 */
public class XmlProductSource extends DefaultHandler implements ProductSource {

	/** The input stream where xml is read. */
	private InputStream in;

	/** The ProductOutput where events are sent. */
	private ProductHandler out;

	/** The Product being parsed. */
	private ProductId id;

	/** Used to read content in ProductOutput while parsing continues. */
	private ContentOutputThread contentOutputThread;

	/** Used to send content to ProductOutput as it is read. */
	private PipedOutputStream contentOutputStream;

	/** Used for signature ProductOutput. */
	private StringBuffer signatureBuffer;

	/**
	 * Create a new XmlProductSource.
	 * 
	 * @param in
	 *            the input stream where xml is read.
	 */
	public XmlProductSource(final InputStream in) {
		this.in = in;
	}

	/**
	 * Create a new XmlProductSource for embedding in another default handler.
	 * 
	 * @param out
	 *            the ProductHandler to receive product events.
	 */
	public XmlProductSource(final ProductHandler out) {
		this.out = out;
	}

	/**
	 * Begin reading the input stream, sending events to out.
	 * 
	 * @param out
	 *            the receiving ProductOutput.
	 */
	public synchronized void streamTo(ProductHandler out) throws Exception {
		try {
			this.out = out;
			XmlUtils.parse(in, this);
		} finally {
			StreamUtils.closeStream(in);
		}
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
	public synchronized void startElement(final String uri,
			final String localName, final String qName,
			final Attributes attributes) throws SAXException {

		if (XmlProductHandler.PRODUCT_XML_NAMESPACE.equals(uri)) {
			// PRODUCT
			if (XmlProductHandler.PRODUCT_ELEMENT.equals(localName)) {
				id = ProductId.parse(XmlUtils.getAttribute(attributes, uri,
						XmlProductHandler.PRODUCT_ATTRIBUTE_ID));
				id.setUpdateTime(XmlUtils.getDate(XmlUtils.getAttribute(
						attributes, uri,
						XmlProductHandler.PRODUCT_ATTRIBUTE_UPDATED)));

				String status = XmlUtils.getAttribute(attributes, uri,
						XmlProductHandler.PRODUCT_ATTRIBUTE_STATUS);

				URL trackerURL = null;
				try {
					trackerURL = new URL(XmlUtils.getAttribute(attributes, uri,
							XmlProductHandler.PRODUCT_ATTRIBUTE_TRACKER_URL));
				} catch (Exception e) {
					// ignore
				}

				try {
					out.onBeginProduct(id, status, trackerURL);
				} catch (Exception e) {
					throw new SAXException(e);
				}
			}
			// PROPERTY
			else if (XmlProductHandler.PROPERTY_ELEMENT.equals(localName)) {
				String name = XmlUtils.getAttribute(attributes, uri,
						XmlProductHandler.PROPERTY_ATTRIBUTE_NAME);
				String value = XmlUtils.getAttribute(attributes, uri,
						XmlProductHandler.PROPERTY_ATTRIBUTE_VALUE);

				try {
					out.onProperty(id, name, value);
				} catch (Exception e) {
					throw new SAXException(e);
				}
			}
			// LINK
			else if (XmlProductHandler.LINK_ELEMENT.equals(localName)) {
				String relation = XmlUtils.getAttribute(attributes, uri,
						XmlProductHandler.LINK_ATTRIBUTE_RELATION);
				URI href = null;
				try {
					href = new URI(XmlUtils.getAttribute(attributes, uri,
							XmlProductHandler.LINK_ATTRIBUTE_HREF));
				} catch (Exception e) {
					return;
				}

				try {
					out.onLink(id, relation, href);
				} catch (Exception e) {
					throw new SAXException(e);
				}
			}
			// CONTENT
			else if (XmlProductHandler.CONTENT_ELEMENT.equals(localName)) {
				try {
					String type = XmlUtils.getAttribute(attributes, uri,
							XmlProductHandler.CONTENT_ATTRIBUTE_TYPE);
					Long length = Long.valueOf(XmlUtils.getAttribute(
							attributes, uri,
							XmlProductHandler.CONTENT_ATTRIBUTE_LENGTH));
					Date modified = XmlUtils.getDate(XmlUtils.getAttribute(
							attributes, uri,
							XmlProductHandler.CONTENT_ATTRIBUTE_MODIFIED));
					String path = XmlUtils.getAttribute(attributes, uri,
							XmlProductHandler.CONTENT_ATTRIBUTE_PATH);
					String encoded = XmlUtils.getAttribute(attributes, uri,
							XmlProductHandler.CONTENT_ATTRIBUTE_ENCODED);
					String href = XmlUtils.getAttribute(attributes, uri,
							XmlProductHandler.CONTENT_ATTRIBUTE_HREF);

					if (href != null) {
						// URL CONTENT
						URL url = null;
						try {
							url = new URL(href);
						} catch (Exception e) {
							throw new SAXException(e);
						}
						URLContent content = new URLContent(url);
						content.setContentType(type);
						content.setLength(length);
						content.setLastModified(modified);

						out.onContent(id, path, content);
						return;
					}

					else {
						// EMBEDDED CONTENT
						contentOutputStream = new PipedOutputStream();
						InputStream contentInputStream = new PipedInputStream(
								contentOutputStream);

						// check if content is base 64 encoded, by default it
						// isn't
						if (encoded != null && encoded.equals("true")) {
							contentInputStream = new Base64.InputStream(
									contentInputStream, Base64.DONT_GUNZIP);
						}

						InputStreamContent content = new InputStreamContent(
								contentInputStream);
						content.setContentType(type);
						content.setLength(length);
						content.setLastModified(modified);

						// call onContent in separate thread so parsing thread
						// can continue. Element content is fed during the
						// characters method.
						contentOutputThread = new ContentOutputThread(out, id, path, content);
						contentOutputThread.start();
					}

				} catch (Exception e) {
					throw new SAXException(e);
				}
			}
			// SIGNATURE
			else if (XmlProductHandler.SIGNATURE_ELEMENT.equals(localName)) {
				signatureBuffer = new StringBuffer();
			}
		}
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
	public synchronized void endElement(final String uri,
			final String localName, final String qName) throws SAXException {
		if (XmlProductHandler.PRODUCT_XML_NAMESPACE.equals(uri)) {
			try {
				if (XmlProductHandler.CONTENT_ELEMENT.equals(localName)) {
					// done reading content content, close piped stream to
					// signal
					// EOF.
					StreamUtils.closeStream(contentOutputStream);
					contentOutputStream = null;
					try {
						// wait for background thread to complete
						contentOutputThread.join();
					} catch (Exception e) {
						// ignore
					}
					contentOutputThread = null;
				} else if (XmlProductHandler.SIGNATURE_ELEMENT
						.equals(localName)) {
					String signature = signatureBuffer.toString();
					signatureBuffer = null;
					out.onSignature(id, signature);
				} else if (XmlProductHandler.PRODUCT_ELEMENT.equals(localName)) {
					out.onEndProduct(id);
				}
			} catch (Exception e) {
				throw new SAXException(e);
			}
		}
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
	public synchronized void characters(final char[] ch, final int start,
			final int length) throws SAXException {
		String chars = new String(ch, start, length);
		if (contentOutputStream != null) {
			try {
				contentOutputStream.write(chars.getBytes());
			} catch (Exception e) {
				// close the piped stream if there was an exception
				StreamUtils.closeStream(contentOutputStream);
				contentOutputThread = null;
				throw new SAXException(e);
			}
		} else if (signatureBuffer != null) {
			signatureBuffer.append(chars);
		} else {
			// ignore, only interested in content or signature
		}
	}

	protected synchronized ProductHandler getHandler() {
		return out;
	}

	protected synchronized void setHandler(ProductHandler out) {
		this.out = out;
	}


	/**
	 * Free any resources associated with this handler.
	 */
	@Override
	public void close() {
		StreamUtils.closeStream(in);
		if (out != null) {
			out.close();
		}
	}


}
