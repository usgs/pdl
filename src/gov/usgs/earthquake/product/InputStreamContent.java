/*
 * InputStreamContent
 */
package gov.usgs.earthquake.product;

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
	 * Free any resources associated with this content.
	 */
	public void close() {
		StreamUtils.closeStream(content);
	}
}
