/*
 * AbstractContent
 */
package gov.usgs.earthquake.product;

import java.util.Date;

/**
 * AbstractContent is a base class for other content classes and implements
 * common functionality.
 */
public abstract class AbstractContent implements Content {

	/** The content mime type. */
	private String contentType;

	/** When this content was created. */
	private Date lastModified;

	/** How much content there is. */
	private Long length;

	/**
	 * Null values are replaced with defaults.
	 * 
	 * @param contentType
	 *            defaults to "text/plain".
	 * @param lastModified
	 *            defaults to new Date().
	 * @param length
	 *            defaults to -1L.
	 */
	public AbstractContent(final String contentType, final Date lastModified,
			final Long length) {
		setContentType(contentType);
		setLastModified(lastModified);
		setLength(length);
	}

	/**
	 * Copy constructor from another content.
	 * 
	 * @param content
	 *            the content to copy.
	 */
	public AbstractContent(final Content content) {
		this(content.getContentType(), content.getLastModified(), content
				.getLength());
	}

	/**
	 * Default Constructor which requires no arguments. Default values are used
	 * for all fields.
	 */
	public AbstractContent() {
		this(null, null, null);
	}

	/**
	 * @return the content mime type.
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Set the content mime type.
	 * 
	 * @param contentType
	 *            the content mime type.
	 */
	public void setContentType(final String contentType) {
		if (contentType == null) {
			this.contentType = "text/plain";
		} else {
			this.contentType = contentType;
		}
	}

	/**
	 * @return the content creation date.
	 */
	public Date getLastModified() {
		return lastModified;
	}

	/**
	 * Set when this content was created.
	 * 
	 * @param lastModified
	 *            when this content was created.
	 */
	public void setLastModified(final Date lastModified) {
		if (lastModified == null) {
			this.lastModified = new Date();
		} else {
			this.lastModified = new Date(lastModified.getTime());
		}

		// Round to nearest second. Some file systems do not preserve
		// milliseconds.
		this.lastModified
				.setTime((this.lastModified.getTime() / 1000L) * 1000L);
	}

	/**
	 * @return the content length, or -1 if unknown.
	 */
	public Long getLength() {
		return length;
	}

	/**
	 * Set the content length.
	 * 
	 * @param length
	 */
	public void setLength(final Long length) {
		if (length == null) {
			this.length = -1L;
		} else {
			this.length = length;
		}
	}

}
