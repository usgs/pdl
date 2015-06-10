/*
 * ProductSender
 * 
 * $Id: ProductSender.java 10673 2011-06-30 23:48:47Z jmfee $
 * $URL: https://ghttrac.cr.usgs.gov/websvn/ProductDistribution/trunk/src/gov/usgs/earthquake/distribution/ProductSender.java $
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
