package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.util.ISO8601;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;

import java.math.BigDecimal;
import java.util.Date;
import java.util.logging.Logger;

/**
 * A policy for the Indexer to clean up Events in its ProductIndex.
 *
 * The policy is created by configuration parameters and generates
 * a ProductIndexQuery. Any product/event matching the product index query is
 * archived. Generally, archiving means the data for that product/event is
 * removed from the index as well as the storage. Upon archiving, an
 * EVENT_ARCHIVED type of IndexerEvent is sent to interested listeners.
 *
 * All archive policies run in their own thread (one thread separate from
 * indexing for all archive policies, not one each) and execute at configured
 * intervals.
 *
 * @author emartinez
 *
 */
public class ArchivePolicy extends DefaultConfigurable {

	private static final Logger LOGGER = Logger.getLogger(ArchivePolicy.class
			.getName());

	// --------------------------------------------------------------------
	// Names of configurable parameters
	// --------------------------------------------------------------------

	/** @deprecated */
	public static final String ARCHIVE_MIN_AGE_PROPERTY = "minAge";
	/** @deprecated */
	public static final String ARCHIVE_MAX_AGE_PROPERTY = "maxAge";

	/** Property for archive minimum event age */
	public static final String ARCHIVE_MIN_EVENT_AGE_PROPERTY = "minEventAge";
	/** Property for archive maximum event age */
	public static final String ARCHIVE_MAX_EVENT_AGE_PROPERTY = "maxEventAge";

	/** Property for archive minimum event time */
	public static final String ARCHIVE_MIN_EVENT_TIME_PROPERTY = "minEventTime";
	/** Property for archive maximum event time */
	public static final String ARCHIVE_MAX_EVENT_TIME_PROPERTY = "maxEventTime";

	/** Property for archive minimum mag */
	public static final String ARCHIVE_MIN_MAG_PROPERTY = "minMag";
	/** Property for archive maximum mag */
	public static final String ARCHIVE_MAX_MAG_PROPERTY = "maxMag";

	/** Property for archive minimum latitude */
	public static final String ARCHIVE_MIN_LAT_PROPERTY = "minLat";
	/** Property for archive maximum latitude */
	public static final String ARCHIVE_MAX_LAT_PROPERTY = "maxLat";

	/** Property for archive minimum longitude */
	public static final String ARCHIVE_MIN_LNG_PROPERTY = "minLng";
	/** Property for archive maximum longitude */
	public static final String ARCHIVE_MAX_LNG_PROPERTY = "maxLng";

	/** Property for archive minimum depth */
	public static final String ARCHIVE_MIN_DEPTH_PROPERTY = "minDepth";
	/** Property for archive maximum depth */
	public static final String ARCHIVE_MAX_DEPTH_PROPERTY = "maxDepth";

	/** Property for archive event source */
	public static final String ARCHIVE_EVENT_SOURCE_PROPERTY = "eventSource";

	// --------------------------------------------------------------------
	// Configured parameters.
	// --------------------------------------------------------------------

	/** @deprecated */
	protected Long minAge = null;
	/** @deprecated */
	protected Long maxAge = null;

	/** Configured parameter var for minEventAge */
	protected Long minEventAge = null;
	/** Configured parameter var for maxEventAge */
	protected Long maxEventAge = null;

	/** Configured parameter var for minEventTime */
	protected Long minEventTime = null;
	/** Configured parameter var for maxEventTime */
	protected Long maxEventTime = null;

	/** Configured parameter var for minMag */
	protected BigDecimal minMag = null;
	/** Configured parameter var for maxMag */
	protected BigDecimal maxMag = null;

	/** Configured parameter var for minLat */
	protected BigDecimal minLat = null;
	/** Configured parameter var for maxLat */
	protected BigDecimal maxLat = null;

	/** Configured parameter var for minLng */
	protected BigDecimal minLng = null;
	/** Configured parameter var for maxLng */
	protected BigDecimal maxLng = null;

	/** Configured parameter var for minDepth */
	protected BigDecimal minDepth = null;
	/** Configured parameter var for maxDepth */
	protected BigDecimal maxDepth = null;

	/** Configured parameter var for eventSource */
	protected String eventSource = null;

	/** Default Constructor */
	public ArchivePolicy() {
		// Default constructor
	}

	@Override
	public void configure(Config config) throws Exception {
		minEventAge = parseLong(config, ARCHIVE_MIN_EVENT_AGE_PROPERTY);
		maxEventAge = parseLong(config, ARCHIVE_MAX_EVENT_AGE_PROPERTY);

		minEventTime = parseDateOrLong(config, ARCHIVE_MIN_EVENT_TIME_PROPERTY);
		maxEventTime = parseDateOrLong(config, ARCHIVE_MAX_EVENT_TIME_PROPERTY);

		minAge = parseLong(config, ARCHIVE_MIN_AGE_PROPERTY);
		if (minAge != null) {
			LOGGER.config("Use of minAge property is deprecated.");
		}
		maxAge = parseLong(config, ARCHIVE_MAX_AGE_PROPERTY);
		if (maxAge != null) {
			LOGGER.config("Use of maxAge property is deprecated.");
		}

		if (minEventAge != null && maxEventTime != null) {
			LOGGER.config("Both minEventAge and maxEventTime were specified. "
					+ "Ignoring minEventAge. Only maxEventTime will be used.");
		}
		if (maxEventAge != null && minEventTime != null) {
			LOGGER.config("Both maxEventAge and minEventTime were specified. "
					+ "Ignoring maxEventAge. Only minEventTime will be used.");
		}

		if ((minAge != null || maxAge != null)
				&& (minEventAge != null || maxEventAge != null
						|| minEventTime != null || maxEventTime != null)) {

			ConfigurationException ce = new ConfigurationException(
					"Configuration mismatch. Can not specify both "
							+ "minAge/maxAge (legacy) properties as well as "
							+ "minEventAge/maxEventAge or minEventTime/maxEventTime.");

			ce.fillInStackTrace();
			throw ce;
		}

		if ((minEventAge != null && maxEventAge != null)
				&& (minEventAge > maxEventAge)) {

			ConfigurationException ce = new ConfigurationException(
					"Configuration mismatch. minEventAge "
							+ "greater than maxEventAge.");
			ce.fillInStackTrace();
			throw ce;
		}

		if ((minEventTime != null && maxEventTime != null)
				&& (minEventTime > maxEventTime)) {

			ConfigurationException ce = new ConfigurationException(
					"Configuration mismatch. minEventTime "
							+ "greater than maxEventTime.");
			ce.fillInStackTrace();
			throw ce;
		}

		minMag = parseBigDecimal(config, ARCHIVE_MIN_MAG_PROPERTY);
		maxMag = parseBigDecimal(config, ARCHIVE_MAX_MAG_PROPERTY);
		minLat = parseBigDecimal(config, ARCHIVE_MIN_LAT_PROPERTY);
		maxLat = parseBigDecimal(config, ARCHIVE_MAX_LAT_PROPERTY);
		minLng = parseBigDecimal(config, ARCHIVE_MIN_LNG_PROPERTY);
		maxLng = parseBigDecimal(config, ARCHIVE_MAX_LNG_PROPERTY);
		minDepth = parseBigDecimal(config, ARCHIVE_MIN_DEPTH_PROPERTY);
		maxDepth = parseBigDecimal(config, ARCHIVE_MAX_DEPTH_PROPERTY);
		eventSource = config.getProperty(ARCHIVE_EVENT_SOURCE_PROPERTY);
	}

	@Override
	public void shutdown() throws Exception {
		// Nothing to do
	}

	@Override
	public void startup() throws Exception {
		// Nothing to do
	}

	/** @return a ProductIndexQuery */
	public ProductIndexQuery getIndexQuery() {
		ProductIndexQuery productIndexQuery = new ProductIndexQuery();
		Date now = new Date();

		if (minAge != null) {
			// min age corresponds to minimum event time
			productIndexQuery.setMinEventTime(new Date(now.getTime()
					- minAge.longValue()));
		}
		if (maxAge != null) {
			// max age corresponds to maximum event time
			productIndexQuery.setMaxEventTime(new Date(now.getTime()
					- maxAge.longValue()));
		}

		/*-
		 Epoch                                                            Now
		   |------------------- maxAge <---------------------------------- |
		   |                       |                                       |
		   |                       |                minAge <---------------|
		   |                       |                   |                   |
		   |-----------------------|-------------------|-------------------|
		   |                       |                   |
		   |------------------> minTime                |
		   |                                           |
		   |--------------------------------------> maxTime

		 Simple Example (not to scale)

		   0 (Epoch)                                              (Now) 100,000
		   |------------------- maxAge <--- (10,000) --------------------- |
		   |                       |                                       |
		   |                       |                minAge <--- (1,000) ---|
		   |                       |                   |                   |
		   |-------------------- 90,000 ************ 99,000 ---------------|
		   |                       |                   |
		   |----- (90,000) ---> minTime                |
		   |                                           |
		   |------------------------- (99,000) ---> maxTime

		   Events occurring in the *** time span will match the query and be
		   archived.
		 */

		if (maxEventAge != null) {
			productIndexQuery.setMinEventTime(new Date(now.getTime()
					- maxEventAge.longValue()));
		}
		if (minEventAge != null) {
			productIndexQuery.setMaxEventTime(new Date(now.getTime()
					- minEventAge.longValue()));
		}

		if (minEventTime != null) {
			productIndexQuery
					.setMinEventTime(new Date(minEventTime.longValue()));
		}
		if (maxEventTime != null) {
			productIndexQuery
					.setMaxEventTime(new Date(maxEventTime.longValue()));
		}

		productIndexQuery.setMinEventMagnitude(minMag);
		productIndexQuery.setMaxEventMagnitude(maxMag);
		productIndexQuery.setMinEventLatitude(minLat);
		productIndexQuery.setMaxEventLatitude(maxLat);
		productIndexQuery.setMinEventLongitude(minLng);
		productIndexQuery.setMaxEventLongitude(maxLng);
		productIndexQuery.setMinEventDepth(minDepth);
		productIndexQuery.setMaxEventDepth(maxDepth);
		productIndexQuery.setEventSource(eventSource);

		// this archive policy is only for events, only remove events based on
		// their preferred properties
		productIndexQuery
				.setEventSearchType(ProductIndexQuery.SEARCH_EVENT_PREFERRED);

		productIndexQuery.setResultType(ProductIndexQuery.RESULT_TYPE_ALL);

		return productIndexQuery;
	}

	/** @return boolean if the policy is valid */
	public boolean isValidPolicy() {
		// Not valid if using both old and new configuration methods
		boolean valid = !((minAge != null || maxAge != null) && (minEventAge != null || maxEventAge != null));

		return valid
				&& (minAge != null || maxAge != null || minEventAge != null
						|| maxEventAge != null || minEventTime != null
						|| maxEventTime != null || minMag != null
						|| maxMag != null || minLat != null || maxLat != null
						|| minLng != null || maxLng != null || minDepth != null
						|| maxDepth != null || eventSource != null);
	}

	/**
	 * Gets the property 'name' from config and returns a BigDecimal of it
	 * @param config Config file
	 * @param name name of property from config
	 * @return BigDecimal of property
	 */
	protected BigDecimal parseBigDecimal(Config config, String name) {
		BigDecimal property = null;
		try {
			String buffer = config.getProperty(name);
			if (buffer != null) {
				property = new BigDecimal(buffer);
			}
		} catch (NumberFormatException npx) {
			property = null;
		}
		return property;
	}

	/**
	 * Gets the property 'name' from config and returns a Date/Long of it
	 * @param config Config file
	 * @param name name of property from config
	 * @return Date/Long of property
	 */
	protected Long parseDateOrLong(Config config, String name) {
		Long property = null;
		try {
			String buffer = config.getProperty(name);
			if (buffer != null) {
				if (buffer.indexOf("T") != -1) {
					// try parsing as date
					Date date = ISO8601.parse(buffer);
					if (date != null) {
						property = date.getTime();
					}
				} else {
					property = Long.valueOf(buffer);
				}
			}
		} catch (NumberFormatException npx) {
			property = null;
		}
		return property;
	}

	/**
	 * Gets the property 'name' from config and returns a Long of it
	 * @param config Config file
	 * @param name name of property from config
	 * @return Long of property
	 */
	protected Long parseLong(Config config, String name) {
		Long property = null;
		try {
			String buffer = config.getProperty(name);
			if (buffer != null) {
				property = Long.valueOf(buffer);
			}
		} catch (NumberFormatException npx) {
			property = null;
		}
		return property;
	}

	/** @deprecated
	 * @return minAge
	 */
	public Long getMinAge() {
		return minAge;
	}

	/** @deprecated
	 * @param minAge to set
	 */
	public void setMinAge(Long minAge) {
		this.minAge = minAge;
	}

	/** @deprecated
	 * @return maxAge
	 */
	public Long getMaxAge() {
		return maxAge;
	}

	/** @deprecated
	 * @param maxAge to set
	 */
	public void setMaxAge(Long maxAge) {
		this.maxAge = maxAge;
	}

	/** @return minEventAge */
	public Long getMinEventAge() {
		return minEventAge;
	}

	/** @param minEventAge to set */
	public void setMinEventAge(Long minEventAge) {
		this.minEventAge = minEventAge;
	}

	/** @return maxEventAge */
	public Long getMaxEventAge() {
		return maxEventAge;
	}

	/** @param maxEventAge to set */
	public void setMaxEventAge(Long maxEventAge) {
		this.maxEventAge = maxEventAge;
	}

	/** @return minEventTime */
	public Long getMinEventTime() {
		return minEventTime;
	}

	/** @param minEventTime to set */
	public void setMinEventTime(Long minEventTime) {
		this.minEventTime = minEventTime;
	}

	/** @return maxEventTime */
	public Long getMaxEventTime() {
		return maxEventTime;
	}

	/** @param maxEventTime to set  */
	public void setMaxEventTime(Long maxEventTime) {
		this.maxEventTime = maxEventTime;
	}

	/** @return minMag */
	public BigDecimal getMinMag() {
		return minMag;
	}

	/** @param minMag to set  */
	public void setMinMag(BigDecimal minMag) {
		this.minMag = minMag;
	}

	/** @return maxMag */
	public BigDecimal getMaxMag() {
		return maxMag;
	}

	/** @param maxMag to set */
	public void setMaxMag(BigDecimal maxMag) {
		this.maxMag = maxMag;
	}

	/** @return minLat */
	public BigDecimal getMinLat() {
		return minLat;
	}

	/** @param minLat to set */
	public void setMinLat(BigDecimal minLat) {
		this.minLat = minLat;
	}

	/** @return maxLat */
	public BigDecimal getMaxLat() {
		return maxLat;
	}

	/** @param maxLat to set */
	public void setMaxLat(BigDecimal maxLat) {
		this.maxLat = maxLat;
	}

	/** @return minLng */
	public BigDecimal getMinLng() {
		return minLng;
	}

	/** @param minLng to set */
	public void setMinLng(BigDecimal minLng) {
		this.minLng = minLng;
	}

	/** @return maxLng */
	public BigDecimal getMaxLng() {
		return maxLng;
	}

	/** @param maxLng to set */
	public void setMaxLng(BigDecimal maxLng) {
		this.maxLng = maxLng;
	}

	/** @return minDepth */
	public BigDecimal getMinDepth() {
		return minDepth;
	}

	/** @param minDepth to set */
	public void setMinDepth(BigDecimal minDepth) {
		this.minDepth = minDepth;
	}

	/** @return maxDepth */
	public BigDecimal getMaxDepth() {
		return maxDepth;
	}

	/** @param maxDepth to set */
	public void setMaxDepth(BigDecimal maxDepth) {
		this.maxDepth = maxDepth;
	}

	/** @return eventSource */
	public String getEventSource() {
		return eventSource;
	}

	/** @param eventSource to set */
	public void setEventSource(String eventSource) {
		this.eventSource = eventSource;
	}
}
