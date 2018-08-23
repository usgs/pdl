/*
 * ProductSender
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.util.Configurable;

/**
 * Send a product to a recipient.
 */
public interface ProductSender extends Configurable {

	/**
	 * Send a product to a recipient.
	 * 
	 * @param product
	 *            the product to send.
	 * @throws Exception
	 *             if any errors occur while sending.
	 */
	public void sendProduct(final Product product) throws Exception;

}
