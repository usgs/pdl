/*
 * URLNotification
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import java.net.URL;
import java.util.Date;

/**
 * A URLNotification represents a product that is available via a URL.
 */
public class URLNotification extends DefaultNotification {

	/** Location where product is available. */
	private final URL productURL;

	/**
	 * Construct a URLNotification.
	 * 
	 * @param id
	 *            which product is available.
	 * @param expirationDate
	 *            how long the product is available.
	 * @param trackerURL
	 *            where to send status updates.
	 * @param productURL
	 *            where product is available.
	 */
	public URLNotification(ProductId id, Date expirationDate, URL trackerURL,
			URL productURL) {
		super(id, expirationDate, trackerURL);
		this.productURL = productURL;
	}

	/**
	 * @return Location where this product can be downloaded.
	 */
	public URL getProductURL() {
		return productURL;
	}

	/** A comparison method to see if two notifications are equal. */
	public boolean equals(Notification that) {
		return (that instanceof URLNotification
				&& getExpirationDate().equals(that.getExpirationDate())
				&& getProductId().equals(that.getProductId())
				&& getTrackerURL().equals(that.getTrackerURL()) && getProductURL()
				.equals(((URLNotification) that).getProductURL()));
	}

}
