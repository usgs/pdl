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
	public URLContent(final URL url) throws URISyntaxException {
		this.setContentType(MIME_TYPES.getContentType(url.toURI().toString()));
		this.content = url;
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

	/** Create a new URLContent object using existing content metadata. */
	public URLContent(final Content content, final URL url) throws URISyntaxException {
		this.content = url;
		this.setContentType(content.getContentType());
		this.setLastModified(content.getLastModified());
		this.setLength(content.getLength());
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

	public boolean isFileURL() {
		return getURL().toString().startsWith("file:");
	}

	/**
	 * Free any resources associated with this content.
	 */
	public void close() {
		// nothing to free
	}

}
