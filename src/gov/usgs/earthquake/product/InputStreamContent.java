/*
 * InputStreamContent
 * 
 * $Id: InputStreamContent.java 10673 2011-06-30 23:48:47Z jmfee $
 * $URL: https://ghttrac.cr.usgs.gov/websvn/ProductDistribution/trunk/src/gov/usgs/earthquake/product/InputStreamContent.java $
 */
package gov.usgs.earthquake.product;

import java.io.IOException;
import java.io.InputStream;

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

}
