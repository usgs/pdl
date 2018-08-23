/*
 * IndexerModule
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.Product;

/**
 * An IndexerModule provides product specific functionality for the Indexer.
 */
public interface IndexerModule {

	/** A constant to indicate a module does not support a product. */
	public static final int LEVEL_UNSUPPORTED = 0;

	/** A constant used by the default module for every type of product. */
	public static final int LEVEL_DEFAULT = 1;

	/** A constant to indicate better than default support for a product. */
	public static final int LEVEL_SUPPORTED = 2;

	/**
	 * Determine the support level for a given product.
	 * 
	 * The Indexer uses this method to determine which module will be used to
	 * summarize a product as it is being processed. Usually, returning one of
	 * the LEVEL_ constants will be sufficient.
	 * 
	 * @return the support level. Should be greater than 0 if a product is
	 *         supported, larger values indicate better support.
	 */
	public int getSupportLevel(final Product product);

	/**
	 * Summarize a product.
	 * 
	 * @param product
	 *            the product to summarize
	 * @return the ProductSummary
	 * @throws Exception
	 */
	public ProductSummary getProductSummary(final Product product)
			throws Exception;

}
