/*
 * ByteContent
 */
package gov.usgs.earthquake.product;

import gov.usgs.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * Content stored in a byte array.
 */
public class ByteContent extends AbstractContent {

	/** The actual content. */
	private final byte[] content;

	/**
	 * Default constructor accepts a byte array.
	 * 
	 * @param content
	 *            the actual content. This should not be null.
	 */
	public ByteContent(final byte[] content) {
		this.content = content;
		setLength(new Long(content.length));
	}

	/**
	 * Convert any Content into ByteContent.
	 * 
	 * Any content is read from the InputStream into a new underlying byte
	 * array.
	 * 
	 * @param content
	 *            existing content to read.
	 * @throws IOException
	 *             when errors occur reading content.
	 */
	public ByteContent(final Content content) throws IOException {
		super(content);
		this.content = StreamUtils.readStream(content.getInputStream());
		setLength(new Long(this.content.length));
	}

	/**
	 * @return a ByteArrayInputStream for the wrapped content.
	 */
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(content);
	}

	/**
	 * @return the wrapped byte array.
	 */
	public byte[] getByteArray() {
		return content;
	}

	/**
	 * Free any resources associated with this content.
	 */
	public void close() {
		// nothing to free
	}
}
