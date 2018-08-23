/*
 * IOTest
 */
package gov.usgs.earthquake.product.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductTest;

import org.junit.Test;

/**
 * Contains unit tests for testing io package classes.
 */
public class IOTest {

	public static File TESTING_DIRECTORY = new File(
			System.getProperty("user.home"), "Desktop/productio");

	private ProductTest productTest = new ProductTest();
	private Product product = productTest.getProduct();

	/**
	 * Build a product for use during testing.
	 * 
	 * @return product for use during testing.
	 */
	public Product getProduct() {
		return product;
	}

	/**
	 * Write and then read a product xml file.
	 * 
	 * @param product
	 *            the product to write.
	 * @return the product that was read.
	 * @throws Exception
	 */
	@Test
	public void binaryWriteThenRead() throws Exception {
		Product product = getProduct();
		// store the written xml in a byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// write to xml
		BinaryProductHandler binaryOutput = new BinaryProductHandler(baos);
		new ObjectProductSource(product).streamTo(binaryOutput);

		// read from xml
		BinaryProductSource binaryInput = new BinaryProductSource(
				new ByteArrayInputStream(baos.toByteArray()));
		Product loadedProduct = ObjectProductHandler.getProduct(binaryInput);
		productTest.compareProducts(product, loadedProduct);
	}

	/**
	 * Write and then read a product xml file.
	 * 
	 * @param product
	 *            the product to write.
	 * @return the product that was read.
	 * @throws Exception
	 */
	@Test
	public void xmlWriteThenRead() throws Exception {
		Product product = getProduct();
		// store the written xml in a byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// write to xml
		XmlProductHandler xmlOutput = new XmlProductHandler(baos);
		new ObjectProductSource(product).streamTo(xmlOutput);

		// read from xml
		XmlProductSource xmlInput = new XmlProductSource(
				new ByteArrayInputStream(baos.toByteArray()));
		Product loadedProduct = ObjectProductHandler.getProduct(xmlInput);
		productTest.compareProducts(product, loadedProduct);
	}

	/**
	 * Write and then read a product directory.
	 * 
	 * @param product
	 *            the product to write.
	 * @return the product that was read.
	 * @throws Exception
	 */
	@Test
	public void directoryWriteThenRead() throws Exception {
		Product product = getProduct();
		File directory = new File(TESTING_DIRECTORY, "product");

		// write to directory
		DirectoryProductHandler dirOutput = new DirectoryProductHandler(
				directory);
		new ObjectProductSource(product).streamTo(dirOutput);

		// read from directory
		DirectoryProductSource dirInput = new DirectoryProductSource(directory);
		Product loadedProduct = ObjectProductHandler.getProduct(dirInput);
		productTest.compareProducts(product, loadedProduct);

		// clean up
		FileUtils.deleteTree(directory);
	}

	/**
	 * Write and then read a product zip file.
	 * 
	 * @param product
	 *            the product to write.
	 * @return the product that was read.
	 * @throws Exception
	 */
	@Test
	public void zipWriteThenRead() throws Exception {
		Product product = getProduct();
		File zipFile = new File("product.zip");
		// store the written zip in a byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// write to zip
		ZipProductHandler zipOutput = new ZipProductHandler(baos);
		new ObjectProductSource(product).streamTo(zipOutput);

		StreamUtils.transferStream(
				new ByteArrayInputStream(baos.toByteArray()),
				StreamUtils.getOutputStream(zipFile));

		// read from zip
		ZipProductSource zipInput = new ZipProductSource(zipFile);
		Product loadedProduct = ObjectProductHandler.getProduct(zipInput);
		productTest.compareProducts(product, loadedProduct);
		
		// clean up
		FileUtils.deleteTree(zipFile);
	}

}
