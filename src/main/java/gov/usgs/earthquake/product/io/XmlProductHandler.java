/*
 * XmlProductHandler
 */
package gov.usgs.earthquake.product.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Base64;

import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.URLContent;
import gov.usgs.earthquake.product.ProductId;


/**
 * Store a product to an OutputStream using XML.
 */
public class XmlProductHandler implements ProductHandler {

	public static final String XML_DECLARATION = "<?xml version=\"1.0\"?>\n";
	public static final String PRODUCT_XML_NAMESPACE = "http://earthquake.usgs.gov/distribution/product";

	public static final String PRODUCT_ELEMENT = "product";
	public static final String PRODUCT_ATTRIBUTE_ID = "id";
	public static final String PRODUCT_ATTRIBUTE_UPDATED = "updateTime";
	public static final String PRODUCT_ATTRIBUTE_STATUS = "status";
	public static final String PRODUCT_ATTRIBUTE_TRACKER_URL = "trackerURL";

	public static final String PROPERTY_ELEMENT = "property";
	public static final String PROPERTY_ATTRIBUTE_NAME = "name";
	public static final String PROPERTY_ATTRIBUTE_VALUE = "value";

	public static final String LINK_ELEMENT = "link";
	public static final String LINK_ATTRIBUTE_RELATION = "rel";
	public static final String LINK_ATTRIBUTE_HREF = "href";

	public static final String CONTENT_ELEMENT = "content";
	public static final String CONTENT_ATTRIBUTE_PATH = "path";
	public static final String CONTENT_ATTRIBUTE_TYPE = "type";
	public static final String CONTENT_ATTRIBUTE_LENGTH = "length";
	public static final String CONTENT_ATTRIBUTE_MODIFIED = "modified";
	/** Used with URLContent. */
	public static final String CONTENT_ATTRIBUTE_HREF = "href";
	public static final String CONTENT_ATTRIBUTE_ENCODED = "encoded";

	public static final String SIGNATURE_ELEMENT = "signature";

	/** The OutputStream where xml is written. */
	private OutputStream out;
	
	/** Controls whether the XML Declaration is output with the XML. */
	private boolean includeDeclaration = true;

	/**
	 * Create a new XmlProductHandler object.
	 * 
	 * @param out
	 *            the OutputStream where xml will be written.
	 */
	public XmlProductHandler(final OutputStream out) {
		this.out = out;
	}
	
	/**
	 * Create a new XmlProductHandler object.
	 * 
	 * @param out
	 *            the OutputStream where xml will be written.
	 * @param includeDeclaration
	 * 			  whether to include the XML declaration with output
	 */
	public XmlProductHandler(final OutputStream out, boolean includeDeclaration) {
		this.out = out;
		this.includeDeclaration = includeDeclaration;
	}

	/**
	 * Output the product root element.
	 */
	public void onBeginProduct(ProductId id, String status, URL trackerURL)
			throws Exception {
		StringBuffer buf = new StringBuffer();
		if (includeDeclaration) {
			buf.append(XML_DECLARATION);
		}
		buf.append("<").append(PRODUCT_ELEMENT);
		buf.append(" xmlns=\"").append(PRODUCT_XML_NAMESPACE).append("\"");
		buf.append(" ").append(PRODUCT_ATTRIBUTE_ID).append("=\"")
				.append(XmlUtils.escape(id.toString())).append("\"");
		buf.append(" ").append(PRODUCT_ATTRIBUTE_UPDATED).append("=\"")
				.append(XmlUtils.formatDate(id.getUpdateTime())).append("\"");
		buf.append(" ").append(PRODUCT_ATTRIBUTE_STATUS).append("=\"")
				.append(XmlUtils.escape(status)).append("\"");
		if (trackerURL != null) {
			buf.append(" ").append(PRODUCT_ATTRIBUTE_TRACKER_URL).append("=\"")
					.append(trackerURL.toExternalForm()).append("\"");
		}
		buf.append(">\n");

		out.write(buf.toString().getBytes());
	}

	/**
	 * Output a content object as xml.
	 */
	public void onContent(ProductId id, String path, Content content)
			throws Exception {
		// open element
		StringBuffer buf = new StringBuffer();
		buf.append("\t<").append(CONTENT_ELEMENT);
		buf.append(" ").append(CONTENT_ATTRIBUTE_PATH).append("=\"")
				.append(XmlUtils.escape(path)).append("\"");
		buf.append(" ").append(CONTENT_ATTRIBUTE_TYPE).append("=\"")
				.append(XmlUtils.escape(content.getContentType())).append("\"");
		buf.append(" ").append(CONTENT_ATTRIBUTE_LENGTH).append("=\"")
				.append(content.getLength()).append("\"");
		buf.append(" ").append(CONTENT_ATTRIBUTE_MODIFIED).append("=\"")
				.append(XmlUtils.formatDate(content.getLastModified()))
				.append("\"");

		if (content instanceof URLContent) {
			// URL CONTENT
			buf.append(" ").append(CONTENT_ATTRIBUTE_HREF).append("=\"")
					.append(((URLContent) content).getURL().toString())
					.append("\"");

			// close element early, url is alternative to embedded content
			buf.append("/>");
			out.write(buf.toString().getBytes());
			return;
		}

		else {
			// EMBEDDED CONTENT
			buf.append(" ").append(CONTENT_ATTRIBUTE_ENCODED)
					.append("=\"true\"");

			buf.append(">");
			out.write(buf.toString().getBytes());

			InputStream in = null;
			OutputStream base64out = null;
			try {
				in = content.getInputStream();
				base64out = Base64.getEncoder().wrap(
						new StreamUtils.UnclosableOutputStream(out));
				// write element content
				StreamUtils.transferStream(in, base64out);
			} finally {
				StreamUtils.closeStream(in);
				StreamUtils.closeStream(base64out);
			}

			// close element
			buf = new StringBuffer();
			buf.append("</").append(CONTENT_ELEMENT).append(">\n");
			out.write(buf.toString().getBytes());
		}
	}

	/**
	 * Output the closing product element.
	 */
	public void onEndProduct(ProductId id) throws Exception {
		StringBuffer buf = new StringBuffer();
		buf.append("</").append(PRODUCT_ELEMENT).append(">\n");

		out.write(buf.toString().getBytes());
		out.flush();
		out.close();
	}

	/**
	 * Output a link element as xml.
	 */
	public void onLink(ProductId id, String relation, URI href)
			throws Exception {
		StringBuffer buf = new StringBuffer();
		buf.append("\t<").append(LINK_ELEMENT);
		buf.append(" ").append(LINK_ATTRIBUTE_RELATION).append("=\"")
				.append(XmlUtils.escape(relation)).append("\"");
		buf.append(" ").append(LINK_ATTRIBUTE_HREF).append("=\"")
				.append(XmlUtils.escape(href.toString())).append("\"");
		buf.append("/>\n");

		out.write(buf.toString().getBytes());
	}

	/**
	 * Output the property element as xml.
	 */
	public void onProperty(ProductId id, String name, String value)
			throws Exception {
		StringBuffer buf = new StringBuffer();
		buf.append("\t<").append(PROPERTY_ELEMENT);
		buf.append(" ").append(PROPERTY_ATTRIBUTE_NAME).append("=\"")
				.append(XmlUtils.escape(name)).append("\"");
		buf.append(" ").append(PROPERTY_ATTRIBUTE_VALUE).append("=\"")
				.append(XmlUtils.escape(value)).append("\"");
		buf.append("/>\n");

		out.write(buf.toString().getBytes());
	}

	/**
	 * Output the signature element as xml.
	 */
	public void onSignature(ProductId id, String signature) throws Exception {
		if (signature == null) {
			return;
		}

		StringBuffer buf = new StringBuffer();
		buf.append("\t<").append(SIGNATURE_ELEMENT).append(">");
		buf.append(signature);
		buf.append("</").append(SIGNATURE_ELEMENT).append(">\n");

		out.write(buf.toString().getBytes());
	}

	/**
	 * Free any resources associated with this handler.
	 */
	@Override
	public void close() {
		StreamUtils.closeStream(out);
	}


}
