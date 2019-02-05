/*
 * DefaultNotification
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;

import java.net.URL;
import java.util.Date;

/**
 * A DefaultNotification is a implementation of the Notification interface. No
 * location information is stored about products, and must be tracked
 * separately.
 */
public class DefaultNotification implements Notification {

	/** The product that is available. */
	private final ProductId id;

	/** How long the product is available. */
	private final Date expirationDate;

	/** Where to send tracking updates. */
	private final URL trackerURL;

	/**
	 * Construct a DefaultNotification.
	 * 
	 * @param id
	 *            the product that is available.
	 * @param expirationDate
	 *            how long the product is available.
	 * @param trackerURL
	 *            where to send tracking updates.
	 */
	public DefaultNotification(final ProductId id, final Date expirationDate,
			final URL trackerURL) {
		this.id = id;
		this.expirationDate = expirationDate;
		this.trackerURL = trackerURL;
	}

	/**
	 * @return how long the product is available.
	 */
	public Date getExpirationDate() {
		return expirationDate;
	}

	/**
	 * @return which product is available.
	 */
	public ProductId getProductId() {
		return id;
	}

	/**
	 * @return location to send tracking updates.
	 */
	public URL getTrackerURL() {
		return trackerURL;
	}
	
	/** A comparison method to see if two notifications are equal. */
	public boolean equals(Notification that) {
		return
		(
			that instanceof DefaultNotification &&
			getExpirationDate().equals(that.getExpirationDate()) &&
			getProductId().equals(that.getProductId()) &&
			getTrackerURL().equals(that.getTrackerURL())
		);
	}

}
