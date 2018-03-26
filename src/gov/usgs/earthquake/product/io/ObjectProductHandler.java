/*
 * ObjectProductHandler
 */
package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.URLContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;

import java.net.URI;
import java.net.URL;

import java.util.List;
import java.util.LinkedList;

/**
 * Convert ProductSource events into a java Product object.
 * 
 * ObjectProductHandlers are not designed to handle multiple products
 * simultaneously and separate objects must be created for each unique product
 * id.
 * 
 * The static method ObjectProductHandler.getProduct(ProductInput) should
 * usually be used instead of constructing objects manually.
 */
public class ObjectProductHandler implements ProductHandler {

	/** The Product being created. */
	private Product product = null;

	/** Whether onEndProduct has been called yet. */
	private boolean complete = false;

	public ObjectProductHandler() {
	}

	/**
	 * @return the product object that was created.
	 */
	public Product getProduct() throws Exception {
		if (product == null) {
			throw new IllegalArgumentException(
					"Called getProduct before onBeginProduct");
		} else if (!complete) {
			throw new IllegalArgumentException(
					"Called getProduct before onEndProduct");
		}

		return product;
	}

	public void onBeginProduct(final ProductId id, final String status,
			final URL trackerURL) throws Exception {
		if (product != null) {
			throw new IllegalArgumentException(
					"Called onBeginProduct after onBeginProduct");
		} else if (complete) {
			throw new IllegalArgumentException(
					"Called onBeginProduct after onEndProduct");
		}

		// System.err.println("onBeginProduct(" + id.toString() + ", " + status
		// + ", " + trackerURL + ")");

		product = new Product(id, status);
		product.setTrackerURL(trackerURL);
	}

	public void onContent(final ProductId id, final String path,
			final Content content) throws Exception {
		if (product == null) {
			throw new IllegalArgumentException(
					"Called onContent before onBeginProduct");
		} else if (complete) {
			throw new IllegalArgumentException(
					"Called onContent after onEndProduct");
		} else if (!product.getId().equals(id)) {
			throw new IllegalArgumentException("ProductIds do not match");
		}

		// System.err.println("onContent(" + id.toString() + ", " + path + ", "
		// + content.toString() + ")");

		if (content instanceof FileContent || content instanceof ByteContent
				|| content instanceof URLContent) {
			// these types of content do not need to be read immediately
			product.getContents().put(path, content);
		} else {
			// new ByteContent reads stream into byte array
			product.getContents().put(path, new ByteContent(content));
		}

	}

	public void onEndProduct(final ProductId id) throws Exception {
		// System.err.println("onEndProduct(" + id.toString() + ")");

		complete = true;
	}

	public void onLink(final ProductId id, final String relation, final URI href)
			throws Exception {
		if (product == null) {
			throw new IllegalArgumentException(
					"Called onLink before onBeginProduct");
		} else if (complete) {
			throw new IllegalArgumentException(
					"Called onLink after onEndProduct");
		} else if (!product.getId().equals(id)) {
			throw new IllegalArgumentException("ProductIds do not match");
		}

		// get list of links for relation
		List<URI> links = product.getLinks().get(relation);
		if (links == null) {
			// create if doesn't already exist
			links = new LinkedList<URI>();
			product.getLinks().put(relation, links);
		}

		// add if doesn't already contain
		if (!links.contains(href)) {
			links.add(href);
		}
	}

	public void onProperty(final ProductId id, final String name,
			final String value) throws Exception {
		if (product == null) {
			throw new IllegalArgumentException(
					"Called onProperty before onBeginProduct");
		} else if (complete) {
			throw new IllegalArgumentException(
					"Called onProperty after onEndProduct");
		} else if (!product.getId().equals(id)) {
			throw new IllegalArgumentException("ProductIds do not match");
		}

		// System.err.println("onProperty(" + id.toString() + ", " + name + ", "
		// + value + ")");
		product.getProperties().put(name, value);
	}

	public void onSignature(final ProductId id, final String signature)
			throws Exception {
		if (product == null) {
			throw new IllegalArgumentException(
					"Called onSignature before onBeginProduct");
		} else if (complete) {
			throw new IllegalArgumentException(
					"Called onSignature after onEndProduct");
		} else if (!product.getId().equals(id)) {
			throw new IllegalArgumentException("ProductIds do not match");
		}

		product.setSignature(signature);
	}

	/**
	 * Convenience method to get a Product object from a ProductInput.
	 * 
	 * @param in
	 *            the ProductInput to read.
	 * @return the Product read, or null if errors occur.
	 */
	public static Product getProduct(final ProductSource in) throws Exception {
		ObjectProductHandler out = new ObjectProductHandler();
		try {
			in.streamTo(out);
		} finally {
			in.close();
		}
		return out.getProduct();
	}


	/**
	 * Free any resources associated with this handler.
	 */
	@Override
	public void close() {
		this.product = null;
	}

}
