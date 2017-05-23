/*
 * ProductStorage
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.util.Configurable;

/**
 * Stores and retrieves Products.
 * 
 * This is typically used by a NotificationReceiver to store downloaded
 * products.
 */
public interface ProductStorage extends Configurable {

	/**
	 * A method to check whether a product is already in storage.
	 * 
	 * Implementers should define this method as more than
	 * "getProduct(id) != null" when it is significantly less expensive to check
	 * whether a product exists, compared to loading a product from storage.
	 * 
	 * @param id
	 *            the product to check.
	 * @return true if the product is in this storage, false otherwise.
	 * @throws Exception
	 *             if an error occurs while checking.
	 */
	public boolean hasProduct(final ProductId id) throws Exception;

	/**
	 * Retrieve a stored product.
	 * 
	 * May be implemented as
	 * 
	 * <pre>
	 * return ObjectProductHandler.getProduct(getProductInput(id));
	 * </pre>
	 * 
	 * @param id
	 *            which product to retrieve.
	 * @return the retrieved product, or null if the product isn't in storage.
	 * @throws Exception
	 *             if errors occur while retrieving product.
	 */
	public Product getProduct(final ProductId id) throws Exception;

	/**
	 * Store a product.
	 * 
	 * May be implemented as
	 * 
	 * <pre>
	 * return storeProductSource(new ObjectProductInput(product));
	 * </pre>
	 * 
	 * @param product
	 *            the product to store.
	 * @return the stored product's id.
	 * @throws Exception
	 *             if errors occur while storing product.
	 */
	public ProductId storeProduct(final Product product) throws Exception;

	/**
	 * Retrieve a ProductSource for a stored product.
	 * 
	 * @param id
	 *            which product to retrieve.
	 * @return a ProductInput for the stored product, or null if not in storage.
	 * @throws Exception
	 *             if any errors occur while getting the ProductInput.
	 */
	public ProductSource getProductSource(final ProductId id) throws Exception;

	/**
	 * Store a ProductSource.
	 * 
	 * @param input
	 *            the product to store.
	 * @return the stored product's id.
	 * @throws Exception
	 *             if errors occur while storing product.
	 */
	public ProductId storeProductSource(final ProductSource input)
			throws Exception;

	/**
	 * Remove a Product from storage, if it exists.
	 * 
	 * @param id
	 *            which product to remove.
	 * @throws Exception
	 *             if errors occur while removing product.
	 */
	public void removeProduct(final ProductId id) throws Exception;

	/**
	 * Notifies </code>StorageListener</code>s of the change to the
	 * <code>ProductStorage</code>.
	 * 
	 * @param event
	 */
	public void notifyListeners(final StorageEvent event);

	/**
	 * Adds a <code>StorageListener</code> to be notified when a change occurs
	 * in this <code>ProductStorage</code>.
	 * 
	 * @param listener
	 *            The listener to notify of changes.
	 */
	public void addStorageListener(final StorageListener listener);

	/**
	 * Removes a <code>StorageListener</code> from being notified when a change
	 * occurs in this <code>ProductStorage</code>.
	 * 
	 * @param listener
	 *            The listener to remove
	 */
	public void removeStorageListener(final StorageListener listener);
}
