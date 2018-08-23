/*
 * ProductSource
 */
package gov.usgs.earthquake.product.io;

/**
 * A Source of Product events.
 * 
 * ProductSources are used to read Products from other formats like XML.
 * ProductSources send a stream of events to ProductOutputs and provide stream
 * like processing for Products.
 * 
 * ProductSources should strive to call ProductOutput methods in the following
 * order:
 * <ol>
 * <li>onBeginProduct()
 * <li>onProperty(), by property name alphabetically
 * <li>onLink(), by relation alphabetically, by URI alphabetically
 * <li>onContent(), by content path alphabetically.
 * <li>onSignature()
 * <li>onEndProduct()
 * </ol>
 */
public interface ProductSource {

	/**
	 * Send a product to the ProductOutput.
	 * 
	 * @param out
	 *            the output that will receive the product.
	 */
	public void streamTo(final ProductHandler out) throws Exception;

	/**
	 * Free any resources associated with this source.
	 */
	public void close();

}
