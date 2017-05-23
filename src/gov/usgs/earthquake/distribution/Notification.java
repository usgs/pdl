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

	/** The product that is available. */
	public ProductId getProductId();

	/** How long the product is available. */
	public Date getExpirationDate();

	/** A tracking url where status updates can be sent. */
	public URL getTrackerURL();
	
	/** A comparison method to see if two notifications are equal. */
	public boolean equals(Notification that);

}
