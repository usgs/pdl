/*
 * URLContent
 */
package gov.usgs.earthquake.product;

import gov.usgs.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.activation.MimetypesFileTypeMap;

/**
 * Content stored at a URL.
 */
public class URLContent extends AbstractContent {

	/** Used to look up file types. */
	private static MimetypesFileTypeMap MIME_TYPES = new MimetypesFileTypeMap();

	/** The actual content. */
	private URL content;

	/**
	 * Create a new URLContent object.
	 * 
	 * @param content
	 *            the content available at a URL.
	 * @throws URISyntaxException
	 */
	public URLContent(final URL content) throws URISyntaxException {
		this.setContentType(MIME_TYPES.getContentType(content.toURI()
				.toString()));
		this.content = content;
	}

	/**
	 * Create a new URLContent object from a FileContent.
	 * 
	 * @param fc
	 *            the file content.
	 */
	public URLContent(final FileContent fc) throws MalformedURLException {
		super(fc);
		this.content = fc.getFile().toURI().toURL();
	}

	/**
	 * @return an InputStream for the wrapped content.
	 */
	public InputStream getInputStream() throws IOException {
		return StreamUtils.getURLInputStream(content);
	}

	/**
	 * @return the wrapped url.
	 */
	public URL getURL() {
		return content;
	}

	/**
	 * Free any resources associated with this content.
	 */
	public void close() {
		// nothing to free
	}

}
