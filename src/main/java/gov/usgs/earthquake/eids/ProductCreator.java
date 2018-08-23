package gov.usgs.earthquake.eids;

import gov.usgs.earthquake.product.Product;

import java.io.File;
import java.util.List;

/**
 * Interface used by the EIDSInputWedge.
 * 
 * Parse a file (or directory) into a list of products.
 */
public interface ProductCreator {

	/**
	 * Parse product(s) from a file or directory.
	 * 
	 * @param file
	 *            file or directory.
	 * @return list of parsed products.
	 */
	List<Product> getProducts(final File file) throws Exception;

	/**
	 * @return whether product creator is currently validating.
	 */
	boolean isValidate();

	/**
	 * Enable validation during getProducts method.
	 * 
	 * @param validate
	 * @throws IllegalArgumentException
	 *             if creator doesn't support validation.
	 */
	void setValidate(boolean validate);

}
