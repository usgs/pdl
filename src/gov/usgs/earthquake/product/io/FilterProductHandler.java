/*
 * FilterProductHandler
 */
package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.ProductId;

import java.net.URI;
import java.net.URL;

/**
 * Filter calls to another ProductHandler.
 * 
 * By default, calls are passed directly on to the wrapped ProductOutput.
 */
public class FilterProductHandler implements ProductHandler {

	/** The product output being filtered. */
	private ProductHandler output;

	/**
	 * Create a new FilterProductHandler.
	 * 
	 * A ProductOutput should be set using setProductOutput before any calls to
	 * other methods.
	 */
	public FilterProductHandler() {
	}

	/**
	 * Create a new FilterProductHandler using and existing ProductOutput.
	 * 
	 * @param output
	 *            the ProductOutput to wrap.
	 */
	public FilterProductHandler(final ProductHandler output) {
		setProductOutput(output);
	}

	/**
	 * Set the wrapped ProductOutput.
	 * 
	 * @param output
	 *            the ProductOutput being wrapped.
	 */
	public void setProductOutput(final ProductHandler output) {
		this.output = output;
	}

	/**
	 * Calls the wrapped ProductOutput onBeginProduct method.
	 */
	public void onBeginProduct(ProductId id, String status, URL trackerURL)
			throws Exception {
		output.onBeginProduct(id, status, trackerURL);
	}

	/**
	 * Calls the wrapped ProductOutput onContent method.
	 */
	public void onContent(ProductId id, String path, Content content)
			throws Exception {
		output.onContent(id, path, content);
	}

	/**
	 * Calls the wrapped ProductOutput onEndProduct method.
	 */
	public void onEndProduct(ProductId id) throws Exception {
		output.onEndProduct(id);
	}

	/**
	 * Calls the wrapped ProductOutput onLink method.
	 */
	public void onLink(ProductId id, String relation, URI href)
			throws Exception {
		output.onLink(id, relation, href);
	}

	/**
	 * Calls the wrapped ProductOutput onProperty method.
	 */
	public void onProperty(ProductId id, String name, String value)
			throws Exception {
		output.onProperty(id, name, value);
	}

	/**
	 * Calls the wrapped ProductOutput onSignature method.
	 */
	public void onSignature(ProductId id, String signature) throws Exception {
		output.onSignature(id, signature);
	}


	/**
	 * Free any resources associated with this source.
	 */
	@Override
	public void close() {
		output.close();
	}

}
