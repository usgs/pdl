/*
 * ObjectProductSource
 */
package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;

import java.net.URI;

import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.TreeSet;

/**
 * Convert a java Product object into events for a ProductHandler.
 * 
 * ObjectProductSource turns a product object into a stream of events for
 * ProductOutputs.
 * 
 * ObjectProductSources are reuseable for a given product.
 */
public class ObjectProductSource implements ProductSource {

	/** The product being sent. */
	private Product product;

	/**
	 * Construct a new ObjectProductSource.
	 * 
	 * @param product
	 *            the product used for input.
	 */
	public ObjectProductSource(final Product product) {
		this.product = product;
	}

	/**
	 * Send a product object to a ProductOutput.
	 * 
	 * Calls these methods in the following order:
	 * <ol>
	 * <li>sendBeginProduct
	 * <li>sendProperties
	 * <li>sendLinks
	 * <li>sendContents
	 * <li>sendSignature
	 * <li>sendEndProducct
	 * </ol>
	 * 
	 * @param out
	 *            the ProductOutput that will receive the Product.
	 */
	public void streamTo(final ProductHandler out) throws Exception {
		sendBeginProduct(out);
		sendProperties(out);
		sendLinks(out);
		sendContents(out);
		sendSignature(out);
		sendEndProduct(out);
	}

	/**
	 * Call out's onBeginProduct method for this product.
	 * 
	 * @param out
	 *            the receiving ProductOutput.
	 * @throws Exception
	 *             if out.onBeginProduct throws an Exception.
	 */
	public void sendBeginProduct(final ProductHandler out) throws Exception {
		out.onBeginProduct(product.getId(), product.getStatus(), product
				.getTrackerURL());
	}

	/**
	 * Call out's onProperty method for each product property. Calls in
	 * alphabetical order by property name.
	 * 
	 * @param out
	 *            the receiving ProductOutput.
	 * @throws Exception
	 *             if out.onProperty throws an Exception.
	 */
	public void sendProperties(final ProductHandler out) throws Exception {
		ProductId id = product.getId();
		Map<String, String> props = product.getProperties();
		// in alphabetical order by property name
		Iterator<String> keys = new TreeSet<String>(props.keySet()).iterator();
		while (keys.hasNext()) {
			String key = keys.next().toString();
			String value = props.get(key);
			out.onProperty(id, key, value);
		}
	}

	/**
	 * Call out's onLink method for each product link. Calls in alphabetical
	 * order by relation name, by URI.
	 * 
	 * @param out
	 * @throws Exception
	 */
	public void sendLinks(final ProductHandler out) throws Exception {
		ProductId id = product.getId();
		Map<String, List<URI>> links = product.getLinks();
		// in alphabetical order by relation name
		Iterator<String> linkRelations = new TreeSet<String>(links.keySet())
				.iterator();
		while (linkRelations.hasNext()) {
			String relation = linkRelations.next();
			// in alphabetical order by URI
			Iterator<URI> linkURIs = new TreeSet<URI>(links.get(relation))
					.iterator();
			while (linkURIs.hasNext()) {
				URI uri = linkURIs.next();
				out.onLink(id, relation, uri);
			}
		}
	}

	/**
	 * Call out's onContent method for each product content. Calls
	 * alphabetically by content path.
	 * 
	 * @param out
	 *            the receiving ProductOutput.
	 * @throws Exception
	 *             if out.onContent throws an Exception.
	 */
	public void sendContents(final ProductHandler out) throws Exception {
		ProductId id = product.getId();
		Map<String, Content> contents = product.getContents();
		// in alphabetical order by content path
		Iterator<String> paths = new TreeSet<String>(contents.keySet())
				.iterator();
		while (paths.hasNext()) {
			String path = paths.next();
			out.onContent(id, path, contents.get(path));
		}
	}

	/**
	 * Call out's onSignature method with product signature.
	 * 
	 * @param out
	 *            the receiving ProductOutput.
	 * @throws Exception
	 *             if out.onSignature throws an Exception.
	 */
	public void sendSignature(final ProductHandler out) throws Exception {
		out.onSignature(product.getId(), product.getSignature());
	}

	/**
	 * Call out's onEndProduct method for product.
	 * 
	 * @param out
	 *            the receiving ProductOutput.
	 * @throws Exception
	 *             if out.onEndProduct throws an Exception.
	 */
	public void sendEndProduct(final ProductHandler out) throws Exception {
		out.onEndProduct(product.getId());
	}


	/**
	 * Free any resources associated with this source.
	 */
	@Override
	public void close() {
		this.product = null;
	}

}
