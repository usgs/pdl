/*
 * InputStreamContent
 */
package gov.usgs.earthquake.product;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import gov.usgs.util.StreamUtils;

/**
 * Content within an InputStream.
 */
public class InputStreamContent extends AbstractContent {

	/** The actual content. */
	private InputStream content;

	/**
	 * Create a new InputStream content.
	 *
	 * @param content
	 *            the content.
	 */
	public InputStreamContent(final InputStream content) {
		this.content = content;
	}

	/**
	 * Create an InputStreamContent from another Content.
	 *
	 * @param content
	 *            the content to duplicate.
	 */
	public InputStreamContent(final Content content) throws IOException {
		super(content);
		this.content = content.getInputStream();
	}

	/**
	 * @return InputStream to content.
	 */
	public InputStream getInputStream() throws IOException {
		return content;
	}

	/**
	 * InputStream can only be read once.
	 *
	 * <p>If sha256 is null, read and convert to in memory stream.
	 */
	public String getSha256() throws Exception {
		String sha256 = super.getSha256(false);
		if (sha256 == null) {
			// convert stream into byte array to read multiple times
			final byte[] contentBytes;
			try (final InputStream in = getInputStream()) {
				contentBytes = StreamUtils.readStream(in);
			}
			// generate sha256 from byte stream
			this.content = new ByteArrayInputStream(contentBytes);
			sha256 = super.getSha256();
			// set byte stream for next reader
			this.content = new ByteArrayInputStream(contentBytes);
		}
		return sha256;
	}

	/**
	 * Free any resources associated with this content.
	 */
	public void close() {
		StreamUtils.closeStream(content);
		content = null;
	}
}
