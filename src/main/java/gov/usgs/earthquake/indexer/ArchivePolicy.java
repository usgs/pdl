package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.util.ISO8601;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;

import java.math.BigDecimal;
import java.util.Date;
import java.util.logging.Logger;

/**
 * An ArchivePolicy sets a policy for the indexer to to clean up its
 * ProductIndex. The policy is created by configuration parameters and generates
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

	public static final String ARCHIVE_MIN_EVENT_AGE_PROPERTY = "minEventAge";
	public static final String ARCHIVE_MAX_EVENT_AGE_PROPERTY = "maxEventAge";

	public static final String ARCHIVE_MIN_EVENT_TIME_PROPERTY = "minEventTime";
	public static final String ARCHIVE_MAX_EVENT_TIME_PROPERTY = "maxEventTime";

	public static final String ARCHIVE_MIN_MAG_PROPERTY = "minMag";
	public static final String ARCHIVE_MAX_MAG_PROPERTY = "maxMag";

	public static final String ARCHIVE_MIN_LAT_PROPERTY = "minLat";
	public static final String ARCHIVE_MAX_LAT_PROPERTY = "maxLat";

	public static final String ARCHIVE_MIN_LNG_PROPERTY = "minLng";
	public static final String ARCHIVE_MAX_LNG_PROPERTY = "maxLng";

	public static final String ARCHIVE_MIN_DEPTH_PROPERTY = "minDepth";
	public static final String ARCHIVE_MAX_DEPTH_PROPERTY = "maxDepth";

	public static final String ARCHIVE_EVENT_SOURCE_PROPERTY = "eventSource";

	// --------------------------------------------------------------------
	// Configured parameters.
	// --------------------------------------------------------------------

	/** @deprecated */
	protected Long minAge = null;
	/** @deprecated */
	protected Long maxAge = null;

	protected Long minEventAge = null;
	protected Long maxEventAge = null;

	protected Long minEventTime = null;
	protected Long maxEventTime = null;

	protected BigDecimal minMag = null;
	protected BigDecimal maxMag = null;

	protected BigDecimal minLat = null;
	protected BigDecimal maxLat = null;

	protected BigDecimal minLng = null;
	protected BigDecimal maxLng = null;

	protected BigDecimal minDepth = null;
	protected BigDecimal maxDepth = null;

	protected String eventSource = null;

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

	/** @deprecated */
	public Long getMinAge() {
		return minAge;
	}

	/** @deprecated */
	public void setMinAge(Long minAge) {
		this.minAge = minAge;
	}

	/** @deprecated */
	public Long getMaxAge() {
		return maxAge;
	}

	/** @deprecated */
	public void setMaxAge(Long maxAge) {
		this.maxAge = maxAge;
	}

	public Long getMinEventAge() {
		return minEventAge;
	}

	public void setMinEventAge(Long minEventAge) {
		this.minEventAge = minEventAge;
	}

	public Long getMaxEventAge() {
		return maxEventAge;
	}

	public void setMaxEventAge(Long maxEventAge) {
		this.maxEventAge = maxEventAge;
	}

	public Long getMinEventTime() {
		return minEventTime;
	}

	public void setMinEventTime(Long minEventTime) {
		this.minEventTime = minEventTime;
	}

	public Long getMaxEventTime() {
		return maxEventTime;
	}

	public void setMaxEventTime(Long maxEventTime) {
		this.maxEventTime = maxEventTime;
	}

	public BigDecimal getMinMag() {
		return minMag;
	}

	public void setMinMag(BigDecimal minMag) {
		this.minMag = minMag;
	}

	public BigDecimal getMaxMag() {
		return maxMag;
	}

	public void setMaxMag(BigDecimal maxMag) {
		this.maxMag = maxMag;
	}

	public BigDecimal getMinLat() {
		return minLat;
	}

	public void setMinLat(BigDecimal minLat) {
		this.minLat = minLat;
	}

	public BigDecimal getMaxLat() {
		return maxLat;
	}

	public void setMaxLat(BigDecimal maxLat) {
		this.maxLat = maxLat;
	}

	public BigDecimal getMinLng() {
		return minLng;
	}

	public void setMinLng(BigDecimal minLng) {
		this.minLng = minLng;
	}

	public BigDecimal getMaxLng() {
		return maxLng;
	}

	public void setMaxLng(BigDecimal maxLng) {
		this.maxLng = maxLng;
	}

	public BigDecimal getMinDepth() {
		return minDepth;
	}

	public void setMinDepth(BigDecimal minDepth) {
		this.minDepth = minDepth;
	}

	public BigDecimal getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(BigDecimal maxDepth) {
		this.maxDepth = maxDepth;
	}

	public String getEventSource() {
		return eventSource;
	}

	public void setEventSource(String eventSource) {
		this.eventSource = eventSource;
	}
}
