/*
 * ProductHandler
 */
package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.Content;

import java.net.URI;
import java.net.URL;

/**
 * A Handler for Product events.
 * 
 * Outputs handle a stream of product events so products can be processed using
 * streams. They usually receive product events via a ProductInput.
 * ProductInputs should attempt to be ordered for uniform processing:
 * <ol>
 * <li>onBeginProduct()
 * <li>onProperty(), by property name alphabetically
 * <li>onLink(), by relation alphabetically, by URI alphabetically
 * <li>onContent(), by content path alphabetically.
 * <li>onSignature()
 * <li>onEndProduct()
 * </ol>
 * 
 * Typically a ProductHandler is used to output a single product, although there
 * is no explicit requirement preventing reuse.
 */
public interface ProductHandler {

	/**
	 * A new product is being output. The ProductHandler should expect calls to
	 * other on-Methods until the onEndProduct method is called. No calls to
	 * other on-Methods will occur before onBeginProduct.
	 * 
	 * @param id
	 *            which product.
	 * @param status
	 *            the product's status.
	 * @param trackerURL
	 *            a location to send status updates.
	 */
	public void onBeginProduct(final ProductId id, final String status,
			final URL trackerURL) throws Exception;

	/**
	 * A product property value. Products have zero or more properties.
	 * 
	 * @param id
	 *            which product.
	 * @param name
	 *            the property name.
	 * @param value
	 *            the property value.
	 */
	public void onProperty(final ProductId id, final String name,
			final String value) throws Exception;

	/**
	 * A product link. Products have zero or more links.
	 * 
	 * @param id
	 *            which product.
	 * @param relation
	 *            how the URI is related to this product.
	 * @param href
	 *            the URI that is related to this product.
	 */
	public void onLink(final ProductId id, final String relation, final URI href)
			throws Exception;

	/**
	 * Product content. Products have one or more Contents.
	 * 
	 * @param id
	 *            which product.
	 * @param path
	 *            path to content within product.
	 * @param content
	 *            the product content.
	 */
	public void onContent(final ProductId id, final String path,
			final Content content) throws Exception;

	/**
	 * Product signature. Producers may optionally sign products to confirm they
	 * were the producer.
	 * 
	 * @param id
	 *            which product.
	 * @param signature
	 *            the product signature, which can be verified using the
	 *            ProductSigner class.
	 */
	public void onSignature(final ProductId id, final String signature)
			throws Exception;

	/**
	 * A product is finished being output. The ProductHandler should expect no
	 * more calls to other on-Methods, except perhaps onBeginProduct again,
	 * after the onEndProduct method is called.
	 * 
	 * @param id
	 *            which product.
	 */
	public void onEndProduct(final ProductId id) throws Exception;

	/**
	 * Free any resources associated with this handler.
	 */
	public void close();

}
