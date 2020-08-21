package gov.usgs.earthquake.eids;

import gov.usgs.earthquake.distribution.ProductSender;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StreamUtils;

/**
 * Send products by printing them in xml format on the command line.
 */
public class DebugProductSender extends DefaultConfigurable implements ProductSender {

	public DebugProductSender() {
		setName("debug_sender");
	}

	@Override
	public void sendProduct(Product product) throws Exception {
		try (
			final ObjectProductSource source = new ObjectProductSource(product);
			final XmlProductHandler handler = new XmlProductHandler(
					new StreamUtils.UnclosableOutputStream(System.err));
		) {
			source.streamTo(handler);
		}
	}

}
