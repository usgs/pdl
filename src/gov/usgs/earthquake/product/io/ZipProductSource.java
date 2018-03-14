package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.InputStreamContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;

import java.io.File;
import java.util.Date;
import java.util.Enumeration;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

/**
 * Load a product from an InputStream containing ZIP.
 * 
 * ZipProductSource reads an input stream containing a product zip file.
 * 
 * This zip file's first entry must be a product xml file containing all product
 * metadata and inline content.
 */
public class ZipProductSource implements ProductSource {

	/** The input stream where zip content is read. */
	private File zip;

	/**
	 * Construct a new ZipProductSource.
	 * 
	 * @param zip
	 *            the input stream where zip content is read.
	 */
	public ZipProductSource(final File zip) {
		this.zip = zip;
	}

	/**
	 * Parse the zip stream and send product to product output.
	 * 
	 * @param out
	 *            ProductOutput that will receive the product.
	 */
	public void streamTo(ProductHandler out) throws Exception {
		ZipFile zis = null;

		try {
			zis = new ZipFile(this.zip);
	
			Enumeration<? extends ZipEntry> entries = zis.entries();
			ZipEntry entry = entries.nextElement();
	
			if (!entry.getName()
					.equals(ZipProductHandler.PRODUCT_XML_ZIP_ENTRYNAME)) {
				throw new Exception("Unexpected first entry " + entry.getName()
						+ ", expected "
						+ ZipProductHandler.PRODUCT_XML_ZIP_ENTRYNAME);
			}
	
			Product product = ObjectProductHandler.getProduct(new XmlProductSource(
					zis.getInputStream(entry)));
			ProductId id = product.getId();
	
			// send all except signature and end product, until after all
			// contents
			new ObjectProductSource(product) {
				public void sendSignature(final ProductHandler out)
						throws Exception {
					// do nothing
				}
	
				public void sendEndProduct(final ProductHandler out)
						throws Exception {
					// do nothing
				}
			}.streamTo(out);
	
			// send other contents
			while (entries.hasMoreElements()) {
				entry = entries.nextElement();
				InputStreamContent content = new InputStreamContent(
						zis.getInputStream(entry));
				content.setLength(entry.getSize());
				content.setLastModified(new Date(entry.getTime()));
				content.setContentType(entry.getComment());
				out.onContent(id, entry.getName(), content);
			}
	
			// finish sending product
			out.onSignature(id, product.getSignature());
			out.onEndProduct(id);
		} finally {
			try {
				zis.close();
			} catch (Exception ignore) {
			}
		}
	}

	/**
	 * Free any resources associated with this handler.
	 */
	@Override
	public void close() {
	}

}
