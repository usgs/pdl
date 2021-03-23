/*
 * ProductTrackerUpdate
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;

import java.net.URL;
import java.net.InetAddress;

import java.util.Date;

/**
 * Represents a single update sent to a product tracker.
 */
public class ProductTrackerUpdate {

	/** String for products created */
	public static final String PRODUCT_CREATED = "Product Created";
	/** String for products received */
	public static final String PRODUCT_RECEIVED = "Product Received";
	/** String for product exception*/
	public static final String PRODUCT_EXCEPTION = "Exception";
	/** String for notification sent*/
	public static final String NOTIFICATION_SENT = "Notification Sent";
	/** String for notification received*/
	public static final String NOTIFICATION_RECEIVED = "Notification Received";
	/** String for product downloaded*/
	public static final String PRODUCT_DOWNLOADED = "Product Downloaded";
	/** String for product indexed*/
	public static final String PRODUCT_INDEXED = "Product Indexed";

	/** Which ProductTracker stored this update. */
	private URL trackerURL;

	// Assigned by the ProductTracker
	/** A sequence number assigned by the ProductTracker. */
	private Long sequenceNumber;

	/** When the update was received by the tracker. */
	private Date created;

	/** The host that sent the update to the tracker. */
	private InetAddress host;

	// Assigned by the component
	/** Product to which this update refers. */
	private ProductId id;

	/** The software component that is sending the update. */
	private String className;

	/** The update being sent. */
	private String message;

	/**
	 * Create a tracker update for submission. Calls other constructor with null
	 * arguments for those that are not included.
	 *
	 * @param trackerURL url of ProductTracker
	 * @param id the id of the product being updated
	 * @param className which component is sending the update
	 * @param message the update being send
	 */
	public ProductTrackerUpdate(final URL trackerURL, final ProductId id,
			final String className, final String message) {
		this(trackerURL, null, null, null, id, className, message);
	}

	/**
	 * Create a new ProductTrackerUpdate object.
	 *
	 * @param trackerURL url of ProductTracker
	 * @param sequenceNumber assigned by ProductTracker
	 * @param created When the update was created
	 * @param host Host that sent the update
	 * @param id the id of the product being updated
	 * @param className which component is sending the update
	 * @param message the update being send
	 */
	public ProductTrackerUpdate(final URL trackerURL,
			final Long sequenceNumber, final Date created,
			final InetAddress host, final ProductId id, final String className,
			final String message) {
		this.trackerURL = trackerURL;
		this.sequenceNumber = sequenceNumber;
		this.created = created;
		this.host = host;
		this.id = id;
		this.className = className;
		this.message = message;
	}

	/**
	 * @return the trackerURL
	 */
	public URL getTrackerURL() {
		return trackerURL;
	}

	/**
	 * @param trackerURL
	 *            the trackerURL to set
	 */
	public void setTrackerURL(URL trackerURL) {
		this.trackerURL = trackerURL;
	}

	/**
	 * @return the sequenceNumber
	 */
	public Long getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * @param sequenceNumber
	 *            the sequenceNumber to set
	 */
	public void setSequenceNumber(Long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	/**
	 * @return the created
	 */
	public Date getCreated() {
		return created;
	}

	/**
	 * @param created
	 *            the created to set
	 */
	public void setCreated(Date created) {
		this.created = created;
	}

	/**
	 * @return the host
	 */
	public InetAddress getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(InetAddress host) {
		this.host = host;
	}

	/**
	 * @return the id
	 */
	public ProductId getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(ProductId id) {
		this.id = id;
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @param className
	 *            the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message
	 *            the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

}
