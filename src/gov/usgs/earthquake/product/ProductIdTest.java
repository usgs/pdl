/*
 * ProductIdTest
 */
package gov.usgs.earthquake.product;

import org.junit.Test;
import org.junit.Assert;

import java.util.Date;

/**
 * Test the ProductId classes parsing method.
 */
public class ProductIdTest {

	/** A valid product id urn for testing. */
	public static final String VALID_PRODUCT_ID = "urn:usgs-product:us:shakemap:us2009abcd:"
			+ new Date().getTime();

	/** An invalid product id urn for testing. */
	public static final String INVALID_PRODUCT_ID = "http://www.google.com";

	/** An incomplete product id urn for testing. */
	public static final String INCOMPLETE_PRODUCT_ID = "urn:usgs-product:us:shakemap:us2009abcd";

	/**
	 * Invalid Product Ids should throw exceptions when parsed.
	 */
	@Test
	public void parseInvalidIdThrowsException() {

		try {
			ProductId id = ProductId.parse(INVALID_PRODUCT_ID);
			id.getType();
		} catch (Exception e) {
			Assert.assertTrue(
					"Invalid product id throws IllegalArgumentException",
					e instanceof IllegalArgumentException);
		}

		try {
			ProductId id = ProductId.parse(INCOMPLETE_PRODUCT_ID);
			id.getCode();
		} catch (Exception e) {
			Assert.assertTrue(
					"Incomplete product id throws IllegalArgumentException",
					e instanceof IllegalArgumentException);
		}
	}

	/**
	 * Valid id should generate and parse equivalent urn.
	 */
	@Test
	public void parseValidId() {
		ProductId id = ProductId.parse(VALID_PRODUCT_ID);
		ProductId id2 = ProductId.parse(id.toString());
		id2.setUpdateTime(id.getUpdateTime());

		Assert.assertEquals("Id matches after generating and parsing", id, id2);
	}

}
