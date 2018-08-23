package gov.usgs.earthquake.dyfi;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.distribution.ExternalNotificationListener;
import gov.usgs.earthquake.product.Product;
import gov.usgs.util.Config;

import java.util.logging.Logger;

public class DYFIIndexerWedge extends ExternalNotificationListener {

	private static final Logger LOGGER = Logger
			.getLogger("gov.usgs.earthquake.dyfi.DYFIIndexerWedge");

	public static final String BASE_DIRECTORY_PROPERTY = "baseDirectory";
	private String baseDirectory = null;

	public DYFIIndexerWedge() {
		getIncludeTypes().add("dyfi");
	}

	/**
	 * Builds the command to index the product. Just appends the relative
	 * product directory (from the DYFILegacyStorage) to the configured index
	 * command.
	 * 
	 * @param product
	 *            the Product used to build the indexing command.
	 * @throws Exception
	 */
	@Override
	public String getProductCommand(final Product product) throws Exception {
		StringBuffer pc = new StringBuffer(getCommand());

		pc.append(" ").append("--directory=").append(baseDirectory)
				.append(getStorage().getProductPath(product.getId()));

		return pc.toString();
	}

	@Override
	public void configure(Config config) throws Exception {
		super.configure(config);

		// Base directory
		baseDirectory = config.getProperty(BASE_DIRECTORY_PROPERTY);
		if (baseDirectory == null) {
			throw new ConfigurationException("[" + getName()
					+ "] 'baseDirectory' is a required configuration property");
		}
		LOGGER.config("[" + getName() + "] baseDirectory is '" + baseDirectory
				+ "'");
	}
}
