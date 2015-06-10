package gov.usgs.earthquake.dyfi;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.product.ProductId;

import java.io.File;

public class DYFILegacyStorage extends FileProductStorage {

	@Override
	public String getProductPath(ProductId id) {
		StringBuffer path = new StringBuffer();
		String fs = System.getProperty("file.separator");

		path.append(id.getCode().substring(0, 2)).append(fs); // Legacy network
		path.append(id.getCode().substring(2)).append(fs);    // Legacy code
		path.append(id.getSource());
		
		return path.toString();
	}
	
	@Override
	public boolean hasProduct(ProductId id) throws Exception {
		
		File productDir = new File(getBaseDirectory(), getProductPath(id));
		boolean hasProduct = false;
		
		if (productDir.exists()) {
		
			// Legacy storage only keeps most recent product. So can't just check
			// for directory existence since this may be from an earlier version.
			// Check version to make sure incoming product is newer than current.
			ProductId storedProduct = getProduct(id).getId();
			
			int status = id.compareTo(storedProduct);
			
			if (status == 0) {
				// Equal product id we already have product
				hasProduct = true;
			} else if (status > 0) {
				// This product is newer than stored product. We don't have this yet
				hasProduct = false;
			} else if (status < 0) {
				// This product is older than stored product. Skip it.
				hasProduct = true;
			}
		}
		
		return hasProduct;
	}
}
