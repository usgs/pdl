/*
 * ZipProductHandler
 */
package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;

import gov.usgs.util.StreamUtils;

import java.io.OutputStream;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

/**
 * Store a product to an OutputStream using ZIP.
 * 
 * Accumulates entire product the same as ObjectProductOutput, then onEndProduct
 * outputs a zip file containing "product.xml" as the first entry and all
 * product content as other entries.
 * 
 * Because the zip file is not written until after all content has been
 * "received", all product content may result in in-memory buffering. This is
 * not the case when dealing with File-backed products. If this is a concern,
 * users are encouraged to use the Xml Input and Output classes, which always
 * use streams and never buffer content.
 */
public class ZipProductHandler extends ObjectProductHandler {

	/** The entry filename used for product metadata. */
	public static final String PRODUCT_XML_ZIP_ENTRYNAME = "product.xml";

	/** The output stream where zip content is written. */
	private OutputStream out;

	/**
	 * Construct a new ZipProductHandler object.
	 * 
	 * @param out
	 *            the output stream where zip content is written.
	 */
	public ZipProductHandler(final OutputStream out) {
		this.out = out;
	}

	/**
	 * Creates and outputs the zip stream.
	 */
	public void onEndProduct(ProductId id) throws Exception {
		super.onEndProduct(id);
		Product product = getProduct();

		// separate all contents that will be zip entries from product
		Map<String, Content> contents = new HashMap<String, Content>(
				product.getContents());
		product.getContents().clear();

		// check for inline content that won't have a zip entry
		Content inlineContent = contents.get("");
		if (inlineContent != null) {
			product.getContents().put("", inlineContent);
		}

		// write zip stream
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(out);

			// product xml entry
			ZipEntry entry = new ZipEntry(PRODUCT_XML_ZIP_ENTRYNAME);
			entry.setTime(product.getId().getUpdateTime().getTime());
			zos.putNextEntry(entry);
			new ObjectProductSource(product).streamTo(new XmlProductHandler(
					new StreamUtils.UnclosableOutputStream(zos)));
			// zos.closeEntry();

			// all other content entries
			Iterator<String> paths = contents.keySet().iterator();
			while (paths.hasNext()) {
				String path = paths.next();
				if ("".equals(path)) {
					// inline content doesn't get separate entry
					continue;
				}
				Content content = contents.get(path);
				entry = new ZipEntry(path);
				entry.setTime(content.getLastModified().getTime());
				entry.setComment(content.getContentType());
				Long length = content.getLength();
				if (length != null && length > 0) {
					entry.setSize(content.getLength());
				}
				zos.putNextEntry(entry);
				StreamUtils.transferStream(content.getInputStream(),
						new StreamUtils.UnclosableOutputStream(zos));
				// zos.closeEntry();
			}
		} finally {
			// done
			zos.finish();
			zos.flush();
			StreamUtils.closeStream(zos);
		}
	}


	/**
	 * Free any resources associated with this handler.
	 */
	@Override
	public void close() {
		StreamUtils.closeStream(out);
	}

}
