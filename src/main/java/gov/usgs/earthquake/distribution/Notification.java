/*
 * Notification
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;

import java.net.URL;

import java.util.Date;

/**
 * A Notification describes an updated product that is available.
 */
public interface Notification {

	/** The product that is available.
	 * @return ProductId
	 */
	public ProductId getProductId();

	/** How long the product is available.
	 * @return Date
	 */
	public Date getExpirationDate();

	/** A tracking url where status updates can be sent.
	 * @return Tracker URL
	 */
	public URL getTrackerURL();

	/** A comparison method to see if two notifications are equal.
	 * @param that Notification
	 * @return boolean
	 */
	public boolean equals(Notification that);

}
