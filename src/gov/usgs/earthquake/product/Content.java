/*
 * Content
 */
package gov.usgs.earthquake.product;

import java.io.InputStream;
import java.io.IOException;

import java.util.Date;

/**
 * Content describes a group of bytes with associated mime type information. *
 */
public interface Content {

	/**
	 * The type of content.
	 * 
	 * @return the mime content type for this content.
	 */
	public String getContentType();

	/**
	 * The content bytes as a stream.
	 * 
	 * @return an InputStream from which content can be read.
	 * @throws IOException
	 *             if an error occurs while creating the stream.
	 */
	public InputStream getInputStream() throws IOException;

	/**
	 * When the content was modified.
	 * 
	 * @return Date when the content was modified.
	 */
	public Date getLastModified();

	/**
	 * How much content there is.
	 * 
	 * @return the actual content length, or -1 if unknown.
	 */
	public Long getLength();

	/**
	 * Free any resources associated with this content.
	 */
	public void close();
}
