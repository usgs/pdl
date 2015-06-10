/*
 * DirectoryProductHandler
 * 
 * $Id: DirectoryProductHandler.java 11059 2011-08-02 16:32:44Z bkfindley $
 * $URL: https://ghttrac.cr.usgs.gov/websvn/ProductDistribution/trunk/src/gov/usgs/earthquake/product/io/DirectoryProductHandler.java $
 */
package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.URLContent;
import gov.usgs.util.StreamUtils;

import java.io.File;
import java.io.OutputStream;

/**
 * Store a product to a Directory.
 * 
 * Product attributes are stored to a file named "product.xml". All
 * ProductOutput methods are passed to an ObjectProductOutput object, except
 * files with non-empty paths. Files are stored in the directory, and all other
 * product attributes are stored using the product xml format to a file name
 * "product.xml".
 */
public class DirectoryProductHandler extends ObjectProductHandler {

	/** The file where product attributes are stored. */
	public static final String PRODUCT_XML_FILENAME = "product.xml";

	/** Directory where product contents are stored. */
	private File directory;

	/**
	 * Construct a new DirectoryProductHandler object.
	 * 
	 * @param directory
	 *            where product contents will be stored.
	 */
	public DirectoryProductHandler(final File directory) {
		this.directory = directory;
	}

	/**
	 * Extract content when path isn't empty.
	 */
	public void onContent(ProductId id, String path, Content content)
			throws Exception {
		if ("".equals(path)) {
			super.onContent(id, path, content);
		} else {
			// FileContent copy constructor extracts content
			FileContent fc = new FileContent(content, new File(directory, path));
			super.onContent(id, path, new URLContent(fc));
		}
	}

	/**
	 * Store all except product contents to product.xml.
	 */
	public void onEndProduct(ProductId id) throws Exception {
		super.onEndProduct(id);

		// save reference to stream, so it can be forced close.
		OutputStream out = null;
		try {
			out = StreamUtils.getOutputStream(new File(directory,
					PRODUCT_XML_FILENAME));

			// save product attributes as xml
			new ObjectProductSource(getProduct())
					.streamTo(new XmlProductHandler(out));
		} finally {
			// close stream
			StreamUtils.closeStream(out);
		}
	}

}
