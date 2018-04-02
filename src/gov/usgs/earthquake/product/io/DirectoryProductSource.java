/*
 * DirectoryProductSource
 */
package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.URLContent;

import gov.usgs.util.StreamUtils;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load a product from a Directory.
 * 
 * Usually a directory is created using DirectoryProductOutput. It should
 * contain a product xml file named "product.xml". All other files are treated
 * as attachments.
 */
public class DirectoryProductSource implements ProductSource {

	/** The directory this product input references. */
	private File directory;

	private static final Logger LOGGER = Logger
			.getLogger(DirectoryProductSource.class.getName());

	/**
	 * Construct a new DirectoryProductSource object.
	 * 
	 * @param directory
	 *            the directory containing a product.
	 */
	public DirectoryProductSource(final File directory) {
		this.directory = directory;
	}

	/**
	 * Load Product from a directory, then send product events to the
	 * ProductOutput.
	 * 
	 * @param out
	 *            the ProductOutput that will receive the product.
	 */
	public void streamTo(ProductHandler out) throws Exception {
		InputStream in = null;

		try {
			in = StreamUtils.getInputStream(new File(directory,
					DirectoryProductHandler.PRODUCT_XML_FILENAME));

			// load product from xml
			Product product = ObjectProductHandler
					.getProduct(new XmlProductSource(in));

			// Convert URLContent to FileContent
			Map<String, Content> contents = product.getContents();
			Content urlContent;
			boolean foundURLContent = false;
			for (String key : contents.keySet()) {
				urlContent = contents.get(key);
				if (urlContent instanceof URLContent) {
					File filePath = new File(directory, key);
					if (filePath.exists()) {
						FileContent fileContent = new FileContent(filePath);
						fileContent.setContentType(urlContent.getContentType());
						fileContent.setLastModified(urlContent.getLastModified());
						fileContent.setLength(urlContent.getLength());
						// go direct to file based on key
						contents.put(key, fileContent);
					} else {
						// old way
						contents.put(key, new FileContent((URLContent) urlContent));
					}
					foundURLContent = true;
				}
			}

			if (!foundURLContent) {
				LOGGER.log(Level.FINER,
						"[DirectoryProductSource] Product does not have any "
								+ " URLContent. Scraping directory for files.");

				// load contents from directory
				contents.putAll(FileContent.getDirectoryContents(directory));

				// except for product.xml which is the product, not its content
				contents.remove(DirectoryProductHandler.PRODUCT_XML_FILENAME);
			}

			// now use ObjectProductInput to send loaded product
			new ObjectProductSource(product).streamTo(out);
		} finally {
			StreamUtils.closeStream(in);
		}
	}


	/**
	 * Free any resources associated with this source.
	 */
	@Override
	public void close() {
		directory = null;
	}

}
