/*
 * ExternalNotificationListenerTest
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductTest;

import org.junit.Test;
import org.junit.Assert;

public class ExternalNotificationListenerTest {

	private ProductTest productTest = new ProductTest();
	private Product product = productTest.getProduct();
	private Product otherProduct = productTest.getOtherProduct();

	@Test
	public void testExcludeFilter() {
		ExternalNotificationListener listener = new ExternalNotificationListener();
		listener.getExcludeTypes().add(product.getId().getType());
		Assert.assertFalse("Reject excluded type",
				listener.accept(product.getId()));
		Assert.assertTrue("Accept unexcluded type",
				listener.accept(otherProduct.getId()));
	}

	@Test
	public void testIncludeFilter() {
		ExternalNotificationListener listener = new ExternalNotificationListener();
		listener.getIncludeTypes().add(product.getId().getType());
		Assert.assertTrue("Accept included type",
				listener.accept(product.getId()));
		Assert.assertFalse("Reject un-included type",
				listener.accept(otherProduct.getId()));
	}

	@Test
	public void testSplitCommand() {
		String command1 = "test \"quoted argument with spaces\" regular arguments";
		String[] args = ExternalNotificationListener.splitCommand(command1);
		Assert.assertArrayEquals("arguments match expected",
				new String[] { "test", "quoted argument with spaces",
						"regular", "arguments" }, args);

		String command2 = "test \"quoted_argument_without_spaces\" regular arguments";
		String[] args2 = ExternalNotificationListener.splitCommand(command2);
		Assert.assertArrayEquals("arguments match expected", new String[] {
				"test", "quoted_argument_without_spaces", "regular",
				"arguments" }, args2);

		String command3 = "test \"'double quoted argument'\" regular arguments";
		String[] args3 = ExternalNotificationListener.splitCommand(command3);
		Assert.assertArrayEquals("arguments match expected", new String[] {
				"test", "'double quoted argument'", "regular", "arguments" },
				args3);

	}

}
