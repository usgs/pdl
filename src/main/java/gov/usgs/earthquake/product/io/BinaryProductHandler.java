package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.StreamUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

/**
 * Generator of binary format for product data.
 * 
 * Binary representation of data types:
 * <dl>
 * <dt>Integer</dt>
 * <dd>4-bytes
 * <dd>
 * <dt>Long</dt>
 * <dd>8-bytes</dd>
 * <dt>Date</dt>
 * <dd>Long (Date.getTime())</dd>
 * <dt>byte[]</dt>
 * <dd>Integer length, raw bytes</dt>
 * <dt>String</dt>
 * <dd>byte[] (String.getBytes("UTF8"))</dd>
 * <dt>URL/URI</dt>
 * <dd>String (URL.toString())</dd>
 * </dl>
 * 
 * 
 * Product is stored in this order:
 * 
 * <ol>
 * 
 * <li>Header, exactly 1
 * <ol>
 * <li>"BEGINPRODUCT" (string)</li>
 * <li>ProductId (String)</li>
 * <li>Status (String)</li>
 * <li>TrackerURL (URL)</li>
 * </ol>
 * </li>
 * 
 * <li>Properties, 0 to many:
 * <ol>
 * <li>"PROPERTY" (String)</li>
 * <li>name (String)</li>
 * <li>value (String)</li>
 * </ol>
 * </li>
 * 
 * <li>Links, 0 to many:
 * <ol>
 * <li>"LINK" (String)</li>
 * <li>relation (String)</li>
 * <li>href (URI)</li>
 * </ol>
 * </li>
 * 
 * <li>Contents, 0 to many:
 * <ol>
 * <li>"CONTENT" (String)</li>
 * <li>path (String)</li>
 * <li>contentType (String)</li>
 * <li>lastModified (Date)</li>
 * <li>length (Long)</li>
 * <li>raw bytes</li>
 * </ol>
 * </li>
 * 
 * <li>Signature, 0 or 1:
 * <ol>
 * <li>"SIGNATURE" (String)</li>
 * <li>signature (String)</li>
 * </ol>
 * </li>
 * 
 * <li>Footer, exactly 1:
 * <ol>
 * <li>"ENDPRODUCT" (String)</li>
 * </ol>
 * </li>
 * 
 * </ol>
 */
public class BinaryProductHandler implements ProductHandler {

	public static final String HEADER = "BEGINPRODUCT";
	public static final String PROPERTY = "PROPERTY";
	public static final String LINK = "LINK";
	public static final String CONTENT = "CONTENT";
	public static final String SIGNATURE = "SIGNATURE";
	public static final String FOOTER = "ENDPRODUCT";

	private OutputStream out;
	private BinaryIO io;

	public BinaryProductHandler(final OutputStream out) {
		this.out = out;
		this.io = new BinaryIO();
	}

	@Override
	public void onBeginProduct(ProductId id, String status, URL trackerURL)
			throws Exception {
		io.writeString(HEADER, out);
		io.writeString(id.toString(), out);
		io.writeString(status, out);
		// allow trackerURL to be null
		if (trackerURL == null) {
			io.writeString("null", out);
		} else {
			io.writeString(trackerURL.toString(), out);
		}
	}

	@Override
	public void onProperty(ProductId id, String name, String value)
			throws Exception {
		io.writeString(PROPERTY, out);
		io.writeString(name, out);
		io.writeString(value, out);
	}

	@Override
	public void onLink(ProductId id, String relation, URI href)
			throws Exception {
		io.writeString(LINK, out);
		io.writeString(relation, out);
		io.writeString(href.toString(), out);
	}

	@Override
	public void onContent(ProductId id, String path, Content content)
			throws Exception {
		if (content.getLength() == null || content.getLength() < 0) {
			// binary io only handles streams with length, convert to content
			// with length
			content = new ByteContent(content);
		}

		io.writeString(CONTENT, out);
		io.writeString(path, out);

		io.writeString(content.getContentType(), out);
		io.writeDate(content.getLastModified(), out);
		InputStream contentInputStream = content.getInputStream();
		try {
			io.writeStream(content.getLength().longValue(), contentInputStream, out);
		} finally {
			StreamUtils.closeStream(contentInputStream);
		}
	}

	@Override
	public void onSignature(ProductId id, String signature) throws Exception {
		// allow signature to be null
		if (signature == null) {
			return;
		}

		io.writeString(SIGNATURE, out);
		io.writeString(signature, out);
	}

	@Override
	public void onEndProduct(ProductId id) throws Exception {
		io.writeString(FOOTER, out);

		out.flush();
		out.close();
	}

	/**
	 * Free any resources associated with this source.
	 */
	@Override
	public void close() {
		StreamUtils.closeStream(out);
		out = null;
	}

}
