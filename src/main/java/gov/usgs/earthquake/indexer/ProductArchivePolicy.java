package gov.usgs.earthquake.indexer;

import java.util.Date;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.util.Config;

/**
 * An archive policy for products, instead of events.
 * 
 * Allows removal of superseded products, preserving latest versions. Also
 * allows targeting unassociated products.
 */
public class ProductArchivePolicy extends ArchivePolicy {

	private static final Logger LOGGER = Logger
			.getLogger(ProductArchivePolicy.class.getName());

	// --------------------------------------------------------------------
	// Names of configurable parameters
	// --------------------------------------------------------------------

	public static final String ARCHIVE_MIN_PRODUCT_AGE_PROPERTY = "minProductAge";
	public static final String ARCHIVE_MAX_PRODUCT_AGE_PROPERTY = "maxProductAge";

	public static final String ARCHIVE_MIN_PRODUCT_TIME_PROPERTY = "minProductTime";
	public static final String ARCHIVE_MAX_PRODUCT_TIME_PROPERTY = "maxProductTime";

	public static final String ARCHIVE_TYPE_PROPERTY = "productType";
	public static final String ARCHIVE_SOURCE_PROPERTY = "productSource";
	public static final String ARCHIVE_SUPERSEDED_PROPERTY = "onlySuperseded";
	public static final String ARCHIVE_UNASSOCIATED_PROPERTY = "onlyUnassociated";
	public static final String ARCHIVE_STATUS_PROPERTY = "productStatus";

	public static final String DEFAULT_ARCHIVE_SUPERSEDED = "true";
	public static final String DEFAULT_ARCHIVE_UNASSOCIATED = "false";
	// --------------------------------------------------------------------
	// Configured parameters.
	// --------------------------------------------------------------------

	protected Long minProductAge = null;
	protected Long maxProductAge = null;
	protected Long minProductTime = null;
	protected Long maxProductTime = null;

	protected String productType = null;
	protected String productSource = null;
	protected boolean onlySuperseded = true;
	protected boolean onlyUnassociated = false;
	protected String productStatus = null;

	@SuppressWarnings("deprecation")
	@Override
	public void configure(Config config) throws Exception {
		super.configure(config);

		minProductAge = parseLong(config, ARCHIVE_MIN_PRODUCT_AGE_PROPERTY);
		maxProductAge = parseLong(config, ARCHIVE_MAX_PRODUCT_AGE_PROPERTY);

		minProductTime = parseDateOrLong(config, ARCHIVE_MIN_PRODUCT_TIME_PROPERTY);
		maxProductTime = parseDateOrLong(config, ARCHIVE_MAX_PRODUCT_TIME_PROPERTY);

		if (minProductAge != null && maxProductTime != null) {
			LOGGER.config("Both minProductAge and maxProductTime were specified. "
					+
					"Ignoring minProductAge. Only maxProductTime will be used.");
		}
		if (maxProductAge != null && minProductTime != null) {
			LOGGER.config("Both maxProductAge and minProductTime were specified. "
					+
					"Ignoring maxProductAge. Only minProductTime will be used.");
		}

		if ((minAge != null || maxAge != null) &&
				(minProductAge != null || maxProductAge != null ||
				 minProductTime != null || maxProductTime != null)) {
			/*
			 * Do we need to log in addition to throwing the exception?
			 * LOGGER.severe("Configuration mismatch. Can not specify both " +
			 * "minAge/maxAge (legacy) properties as well as " +
			 * "minEventAge/maxEventAge.");
			 */
			ConfigurationException ce = new ConfigurationException(
					"Configuration mismatch. Can not specify both " +
							"minAge/maxAge (legacy) properties as well as " +
							"minProductAge/maxProductAge.");
			ce.fillInStackTrace();
			throw ce;
		}

		if ((minProductAge != null && maxProductAge != null) &&
				(minProductAge  > maxProductAge)) {

			ConfigurationException ce = new ConfigurationException(
					"Configuration mismatch. minProductAge " +
							"greater than maxProductAge.");
			ce.fillInStackTrace();
			throw ce;
		}		

		if ((minProductTime != null && maxProductTime != null) &&
				(minProductTime  > maxProductTime)) {

			ConfigurationException ce = new ConfigurationException(
					"Configuration mismatch. minProductTime " +
							"greater than maxProductTime.");
			ce.fillInStackTrace();
			throw ce;
		}				
		
		productType = config.getProperty(ARCHIVE_TYPE_PROPERTY);
		productSource = config.getProperty(ARCHIVE_SOURCE_PROPERTY);
		onlySuperseded = Boolean.valueOf(config.getProperty(
				ARCHIVE_SUPERSEDED_PROPERTY, DEFAULT_ARCHIVE_SUPERSEDED));
		onlyUnassociated = Boolean.valueOf(config.getProperty(
				ARCHIVE_UNASSOCIATED_PROPERTY, DEFAULT_ARCHIVE_UNASSOCIATED));

		productStatus = config.getProperty(ARCHIVE_STATUS_PROPERTY);
	}

	@SuppressWarnings("deprecation")
	@Override
	public ProductIndexQuery getIndexQuery() {
		ProductIndexQuery productIndexQuery = super.getIndexQuery();
		Date now = new Date();

		// Order of minAge, minProductAge, minProductTime is important here.
		// Preference order is minProductTime > minProductAge > minAge
		// Similar for max* properties.

		if (minAge != null) {
			// min age corresponds to minimum product created time
			productIndexQuery.setMinProductUpdateTime(new Date(now.getTime()
					- minAge.longValue()));
			productIndexQuery.setMinEventTime(null);
		}
		if (maxAge != null) {
			// max age corresponds to maximum product created time
			productIndexQuery.setMaxProductUpdateTime(new Date(now.getTime()
					- maxAge.longValue()));
			productIndexQuery.setMaxEventTime(null);
		}

		// See ASCII art in ArchivePolicy.getIndexQuery if you are confused by
		// maxAge --> minTime differences.

		if (maxProductAge != null) {
			productIndexQuery.setMinProductUpdateTime(new Date(now.getTime()
					- maxProductAge.longValue()));
		}
		if (minProductAge != null) {
			productIndexQuery.setMaxProductUpdateTime(new Date(now.getTime()
					- minProductAge.longValue()));
		}

		if (minProductTime != null) {
			productIndexQuery.setMinProductUpdateTime(new Date(minProductTime
					.longValue()));
		}
		if (maxProductTime != null) {
			productIndexQuery.setMaxProductUpdateTime(new Date(maxProductTime
					.longValue()));
		}

		// search for products of a specific type
		productIndexQuery.setProductType(productType);
		// search for products from a specific source
		productIndexQuery.setProductSource(productSource);

		if (onlySuperseded) {
			// remove only old versions of products (keep the latest)
			productIndexQuery
					.setResultType(ProductIndexQuery.RESULT_TYPE_SUPERSEDED);
		} else {
			// otherwise include all products
			productIndexQuery.setResultType(ProductIndexQuery.RESULT_TYPE_ALL);
		}

		productIndexQuery.setProductStatus(productStatus);

		// this archive policy searches products, so this shouldn't matter, but
		// just to be safe in case the default changes
		productIndexQuery
				.setEventSearchType(ProductIndexQuery.SEARCH_EVENT_PRODUCTS);

		return productIndexQuery;
	}

	@Override
	public boolean isValidPolicy() {
		boolean valid = super.isValidPolicy();
		return valid || (minProductAge != null || maxProductAge != null
				|| minProductTime != null || maxProductTime != null
				|| productType != null || productSource != null
				|| productStatus != null);
	}

	public Long getMinProductAge() {
		return minProductAge;
	}

	public void setMinProductAge(Long minProductAge) {
		this.minProductAge = minProductAge;
	}

	public Long getMaxProductAge() {
		return maxProductAge;
	}

	public void setMaxProductAge(Long maxProductAge) {
		this.maxProductAge = maxProductAge;
	}

	public Long getMinProductTime() {
		return minProductTime;
	}

	public void setMinProductTime(Long minProductTime) {
		this.minProductTime = minProductTime;
	}

	public Long getMaxProductTime() {
		return maxProductTime;
	}

	public void setMaxProductTime(Long maxProductTime) {
		this.maxProductTime = maxProductTime;
	}

	public String getProductType() {
		return productType;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}

	public String getProductSource() {
		return productSource;
	}

	public void setProductSource(String productSource) {
		this.productSource = productSource;
	}

	public boolean isOnlySuperseded() {
		return onlySuperseded;
	}

	public void setOnlySuperseded(boolean onlySuperseded) {
		this.onlySuperseded = onlySuperseded;
	}

	public boolean isOnlyUnassociated() {
		return onlyUnassociated;
	}

	public void setOnlyUnassociated(boolean onlyUnassociated) {
		this.onlyUnassociated = onlyUnassociated;
	}

	public String getProductStatus() {
		return productStatus;
	}

	public void setProductStatus(String productStatus) {
		this.productStatus = productStatus;
	}
}
