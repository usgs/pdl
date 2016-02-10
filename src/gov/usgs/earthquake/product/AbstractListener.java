package gov.usgs.earthquake.product;

import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A base listener implementation for both NotificationListener and
 * IndexerListener implementations.
 *
 * The AbstractListener implements the Configurable interface and defines the
 * following configuration parameters:
 *
 * <dl>
 * <dt>includeTypes</dt>
 * <dd>(Optional) A comma separated list of product types to include. If this
 * list is defined it constitutes an "allow" list, and only types in this list
 * are allowed.</dd>
 *
 * <dt>excludeTypes</dt>
 * <dd>(Optional) A comma separated list of product types to exclude. If this
 * list is defined it constitutes a "block" list, and types in this list are not
 * allowed.</dd>
 *
 * <dt>includeSources</dt>
 * <dd>(Optional) A comma separated list of product types to include. If this
 * list is defined it constitutes an "allow" list, and only types in this list
 * are allowed.</dd>
 *
 * <dt>excludeSources</dt>
 * <dd>(Optional) A comma separated list of product types to exclude. If this
 * list is defined it constitutes a "block" list, and types in this list are not
 * allowed.</dd>
 *
 * <dt>includeTests</dt>
 * <dd>(Optional, Default false) Flag to indicate test products should be
 * accepted. Set to "true" or "yes" to enable.</dd>
 *
 * <dt>includeScenarios</dt>
 * <dd>(Optional, Default false) Flag to indicate scenario products should be
 * accepted. Set to "true" or "yes" to enable.</dd>
 *
 * <dt>includeActuals</dt>
 * <dd>(Optional, Default true) Flag to indicate Actual products should be
 * accepted. Set to "true" or "yes" to enable.</dd>
 *
 * <dt>includeInternals</dt>
 * <dd>(Optional, Default false) Flag to indicate internal products should be
 * accepted. Set to "true" or "yes" to enable.</dd>
 *
 * <dt>includeDevelopments</dt>
 * <dd>(Optional, Default false) Flag to indicate development products should be
 * accepted. Set to "true" or "yes" to enable.</dd>
 *
 * <dt>maxTries</dt>
 * <dd>(Optional, Default 1) Number of times to attempt delivery of each
 * notification, if the listener throws a ContinuableListenerException during
 * onNotification(). A value <= 1 means do not re-attempt.</dd>
 *
 * <dt>timeout</dt>
 * <dd>(Optional, Default 0) Number of milliseconds before thread running
 * onNotification() is interrupted. A value <= 0 means never interrupt.</dd>
 *
 * <dt>retryDelay</dt>
 * <dd>(Optional, Default 0) Number of milliseconds to wait before re-attempting
 * processing of a notification, if the listener throws a
 * ContinuableListenerException during onNotification().
 * A value &lt;= 0 means no delay.</dd>
 * </dl>
 *
 * <p>
 * When excludeTypes (or sources) and includeTypes (or sources) are defined,
 * excludes are processed first. This is usually not recommended.
 * </p>
 */
public class AbstractListener extends DefaultConfigurable {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(AbstractListener.class.getName());

	// --------------------------------------------------
	// Configurable property names
	// --------------------------------------------------

	/** Configuration parameter for include types list. */
	public static final String INCLUDE_TYPES_PROPERTY = "includeTypes";

	/** Configuration parameter for exclude types list. */
	public static final String EXCLUDE_TYPES_PROPERTY = "excludeTypes";

	/** Configuration parameter for include sources list. */
	public static final String INCLUDE_SOURCES_PROPERTY = "includeSources";

	/** Configuration parameter for exclude sources list. */
	public static final String EXCLUDE_SOURCES_PROPERTY = "excludeSources";

	/** Flag to indicate if scenario type products should be included **/
	public static final String INCLUDE_TESTS_PROPERTY = "includeTests";

	/** Flag to indicate if scenario type products should be included **/
	public static final String INCLUDE_SCENARIOS_PROPERTY = "includeScenarios";
	
	/** Flag to indicate if actual type products should be included **/
	public static final String INCLUDE_ACTUALS_PROPERTY = "includeActuals";

	/** Flag to indicate if scenario type products should be included **/
	public static final String INCLUDE_INTERNALS_PROPERTY = "includeInternals";

	/** Flag to indicate if scenario type products should be included **/
	public static final String INCLUDE_DEVELOPMENTS_PROPERTY = "includeDevelopments";

	/**
	 * Configuration parameters for attemptCount.
	 *
	 * @deprecated Use MAX_TRIES_PROPERTY instead.
	 */
	public static final String ATTEMPT_COUNT_PROPERTY = "attemptCount";

	/** Configuration parameters for maxTries. */
	public static final String MAX_TRIES_PROPERTY = "maxTries";

	/** Configuration parameter for timeout. */
	public static final String TIMEOUT_PROPERTY = "timeout";

	/** Configuration parameter for retryDelay. */
	public static final String RETRY_DELAY_PROPERTY = "retryDelay";

	// --------------------------------------------------
	// Default values for configurable settings
	// --------------------------------------------------

	// Default setting for include/exclude types/sources variables are to
	// remain empty lists. Default interpretation of this setting is defined in
	// the "accept" method and will accept all types/sources. This can be
	// overridden in a subclass.

	/** Default, do not include tests */
	private static final boolean DEFAULT_INCLUDE_TESTS = false;

	/** Default, do not include scenarios */
	private static final boolean DEFAULT_INCLUDE_SCENARIOS = false;
	
	/** Default, do include actuals */
	private static final boolean DEFAULT_INCLUDE_ACTUALS = true;

	/** Default, do not include internals */
	private static final boolean DEFAULT_INCLUDE_INTERNALS = false;

	/** Default, do not include developments */
	private static final boolean DEFAULT_INCLUDE_DEVELOPMENTS = false;

	/** Default attempt count value is 1 = don't retry. */
	public static final int DEFAULT_ATTEMPT_COUNT = 1;

	/** Default timeout is 0 = infinity. */
	public static final long DEFAULT_TIMEOUT = 0L;

	/** Default delay is 300000ms = 5 minute delay. */
	public static final long DEFAULT_RETRY_DELAY = 300000L;

	// --------------------------------------------------
	// Member variables
	// --------------------------------------------------

	/** Types of products to allow. */
	private final ArrayList<String> includeTypes = new ArrayList<String>();

	/** Types of products to block. */
	private final ArrayList<String> excludeTypes = new ArrayList<String>();

	/** Sources of products to allow. */
	private final ArrayList<String> includeSources = new ArrayList<String>();

	/** Sources of products to block. */
	private final ArrayList<String> excludeSources = new ArrayList<String>();

	/** Whether or not to include test type products */
	private boolean includeTests = DEFAULT_INCLUDE_TESTS;

	/** Whether or not to include scenario type products */
	private boolean includeScenarios = DEFAULT_INCLUDE_SCENARIOS;
	
	/** Whether or not to include actual type products */
	private boolean includeActuals = DEFAULT_INCLUDE_ACTUALS;

	/** Whether or not to include internal type products */
	private boolean includeInternals = DEFAULT_INCLUDE_INTERNALS;

	/** Whether or not to include development type products */
	private boolean includeDevelopments = DEFAULT_INCLUDE_DEVELOPMENTS;

	/** Number of retries when a continuable listener exception is thrown. */
	private int maxTries = DEFAULT_ATTEMPT_COUNT;

	/** Number of milliseconds before the onNotification thread is interrupted. */
	private long timeout = DEFAULT_TIMEOUT;

	/** Number of milliseconds before retries, after the first attempt fails. */
	private long retryDelay = DEFAULT_RETRY_DELAY;

	/**
	 * Determines if a listener accepts a message based on the incoming product
	 * id.
	 *
	 * @param id
	 *            The incoming id of the product that triggered the message.
	 *
	 * @return True if the message should be processed, false otherwise.
	 */
	public boolean accept(ProductId id) {
		String type = id.getType();
		String source = id.getSource();

		// excluded type
		if (excludeTypes.size() > 0 && excludeTypes.contains(type)) {
			LOGGER.finer("[" + getName() + "] product type '" + type
					+ "' excluded");
			return false;
		}

		// not included type
		if (includeTypes.size() > 0 && !includeTypes.contains(type)) {
			LOGGER.finer("[" + getName() + "] product type '" + type
					+ "' not included");
			return false;
		}

		// excluded source
		if (excludeSources.size() > 0 && excludeSources.contains(source)) {
			LOGGER.finer("[" + getName() + "] product source '" + source
					+ "' excluded");
			return false;
		}

		// not included source
		if (includeSources.size() > 0 && !includeSources.contains(source)) {
			LOGGER.finer("[" + getName() + "] product source '" + source
					+ "' not included");
			return false;
		}

		if (type.endsWith("-test") && !includeTests) {
			LOGGER.finer("[" + getName()
					+ "] product type was test. Not included.");
			return false;
		}

		if (type.endsWith("-scenario") && !includeScenarios) {
			LOGGER.finer("[" + getName()
					+ "] product type was scenario. Not included.");
			return false;
		}
		
		if (!includeActuals &&
			    !type.endsWith("-scenario") &&
			    !type.startsWith("internal-") &&
			    !type.endsWith("-devel")
			) {
			LOGGER.finer("[" + getName()
					+ "] product type was actual. Not included.");
			return false;
		}

		if (type.startsWith("internal-") && !includeInternals) {
			LOGGER.finer("[" + getName()
					+ "] product type was internal. Not included.");
			return false;
		}

		if (type.endsWith("-devel") && !includeDevelopments) {
			LOGGER.finer("[" + getName()
					+ "] product type was development version. Not included.");
			return false;
		}

		// otherwise accept
		return true;
	}

	/**
	 * Read the include and exclude types from config.
	 */
	public void configure(final Config config) throws Exception {
		String includeTypeNames = config.getProperty(INCLUDE_TYPES_PROPERTY);
		if (includeTypeNames != null) {
			includeTypes.addAll(StringUtils.split(includeTypeNames, ","));
			LOGGER.config("[" + getName() + "] includeTypes = "
					+ includeTypeNames);
		}
		String excludeTypeNames = config.getProperty(EXCLUDE_TYPES_PROPERTY);
		if (excludeTypeNames != null) {
			excludeTypes.addAll(StringUtils.split(excludeTypeNames, ","));
			LOGGER.config("[" + getName() + "] excludeTypes = "
					+ excludeTypeNames);
		}

		String includeSourceNames = config
				.getProperty(INCLUDE_SOURCES_PROPERTY);
		if (includeSourceNames != null) {
			includeSources.addAll(StringUtils.split(includeSourceNames, ","));
			LOGGER.config("[" + getName() + "] includeSources = "
					+ includeSourceNames);
		}
		String excludeSourceNames = config
				.getProperty(EXCLUDE_SOURCES_PROPERTY);
		if (excludeSourceNames != null) {
			excludeSources.addAll(StringUtils.split(excludeSourceNames, ","));
			LOGGER.config("[" + getName() + "] excludeSources = "
					+ excludeSourceNames);
		}

		String includeFlag = config.getProperty(INCLUDE_TESTS_PROPERTY);
		if (includeFlag != null) {
			includeTests = (includeFlag.equalsIgnoreCase("yes") ||
					includeFlag.equalsIgnoreCase("true"));
			LOGGER.config("[" + getName() + "] includeTests = "
					+ includeTests);
		} else {
			includeDevelopments = DEFAULT_INCLUDE_TESTS;
		}

		includeFlag = config.getProperty(INCLUDE_SCENARIOS_PROPERTY);
		if (includeFlag != null) {
			includeScenarios = (includeFlag.equalsIgnoreCase("yes") ||
					includeFlag.equalsIgnoreCase("true"));
			LOGGER.config("[" + getName() + "] includeScenarios = "
					+ includeScenarios);
		} else {
			includeDevelopments = DEFAULT_INCLUDE_SCENARIOS;
		}
		
		includeFlag = config.getProperty(INCLUDE_ACTUALS_PROPERTY);
		if (includeFlag != null) {
			includeActuals = !(includeFlag.equalsIgnoreCase("no") ||
					includeFlag.equalsIgnoreCase("false"));
			LOGGER.config("[" + getName() + "] includeActuals = "
					+ includeActuals);
		}

		includeFlag = config.getProperty(INCLUDE_INTERNALS_PROPERTY);
		if (includeFlag != null) {
			includeInternals = (includeFlag.equalsIgnoreCase("yes") ||
					includeFlag.equalsIgnoreCase("true"));
			LOGGER.config("[" + getName() + "] includeInternals = "
					+ includeInternals);
		} else {
			includeDevelopments = DEFAULT_INCLUDE_INTERNALS;
		}

		includeFlag = config.getProperty(INCLUDE_DEVELOPMENTS_PROPERTY);
		if (includeFlag != null) {
			includeDevelopments = (includeFlag.equalsIgnoreCase("yes") ||
					includeFlag.equalsIgnoreCase("true"));
			LOGGER.config("[" + getName() + "] includeDevelopments = "
					+ includeDevelopments);
		} else {
			includeDevelopments = DEFAULT_INCLUDE_DEVELOPMENTS;
		}

		String maxTriesStr = config.getProperty(ATTEMPT_COUNT_PROPERTY);
		if (maxTriesStr != null) {
			maxTries = Integer.parseInt(maxTriesStr);
			LOGGER.config("[" + getName() + "] Configured using deprecated '" +
					ATTEMPT_COUNT_PROPERTY +
					"' property. Prefer to use the new '" + MAX_TRIES_PROPERTY +
					"' instead. Support for the deprecated property may be " +
					"removed in a subsequent release.");
		}
		LOGGER.config("[" + getName() + "] maxTries = " + maxTries);

		// By checking for this parameter after the ATTEMPT_COUNT_PROPERTY, the
		// value of the MAX_TRIES_PROPERTY (if specified) will take precedence.
		maxTriesStr = config.getProperty(MAX_TRIES_PROPERTY);
		if (maxTriesStr != null) {
			maxTries = Integer.parseInt(maxTriesStr);
		}
		LOGGER.config("[" + getName() + "] maxTries = " + maxTries);

		String timeoutStr = config.getProperty(TIMEOUT_PROPERTY);
		if (timeoutStr != null) {
			timeout = Long.parseLong(timeoutStr);
		}
		LOGGER.config("[" + getName() + "] timeout = " + timeout + "ms");

		String retryDelayStr = config.getProperty(RETRY_DELAY_PROPERTY);
		if (retryDelayStr != null) {
			retryDelay = Long.parseLong(retryDelayStr);
		}
		LOGGER.config("[" + getName() + "] retry delay = " + retryDelay + "ms");
	}

	/**
	 * @return the includeTypes
	 */
	public List<String> getIncludeTypes() {
		return includeTypes;
	}

	/**
	 * @return the excludeTypes
	 */
	public List<String> getExcludeTypes() {
		return excludeTypes;
	}

	/**
	 * @return the includeSources
	 */
	public List<String> getIncludeSources() {
		return includeSources;
	}

	/**
	 * @return the excludeSources
	 */
	public List<String> getExcludeSources() {
		return excludeSources;
	}

	/**
	 * Number of tries to deliver notification, when an Exception is thrown
	 * during onNotification().
	 *
	 * @return the attemptCount. A value < 1 means never try to deliver.
	 */
	public int getMaxTries() {
		return maxTries;
	}

	/**
	 * Set the maxTries.
	 *
	 * @param maxTries
	 *            the maxTries. A value < 1 means never try to deliver.
	 */
	public void setMaxTries(final int maxTries) {
		this.maxTries = maxTries;
	}

	/**
	 * Number of milliseconds onNotification is allowed to run before being
	 * interrupted.
	 *
	 * @return the timeout in milliseconds. A value <= 0 means never timeout.
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * Set the timeout.
	 *
	 * @param timeout
	 *            the timeout in milliseconds. A value <= 0 means never timeout.
	 */
	public void setTimeout(final long timeout) {
		this.timeout = timeout;
	}

	/**
	 * @return the includeTests
	 */
	public boolean isIncludeTests() {
		return includeTests;
	}

	/**
	 * @param includeTests the includeTests to set
	 */
	public void setIncludeTests(boolean includeTests) {
		this.includeTests = includeTests;
	}

	/**
	 * @return the includeScenarios
	 */
	public boolean isIncludeScenarios() {
		return includeScenarios;
	}

	/**
	 * @param includeScenarios the includeScenarios to set
	 */
	public void setIncludeScenarios(boolean includeScenarios) {
		this.includeScenarios = includeScenarios;
	}
	
	/**
	 * @return the includeActuals
	 */
	public boolean isIncludeActuals() {
		return includeActuals;
	}

	/**
	 * @param includeActuals the includeActuals to set
	 */
	public void setIncludeActuals(boolean includeActuals) {
		this.includeActuals = includeActuals;
	}

	/**
	 * @return the includeInternals
	 */
	public boolean isIncludeInternals() {
		return includeInternals;
	}

	/**
	 * @param includeInternals the includeInternals to set
	 */
	public void setIncludeInternals(boolean includeInternals) {
		this.includeInternals = includeInternals;
	}

	/**
	 * @return the includeDevelopments
	 */
	public boolean isIncludeDevelopments() {
		return includeDevelopments;
	}

	/**
	 * @param includeDevelopments the includeDevelopments to set
	 */
	public void setIncludeDevelopments(boolean includeDevelopments) {
		this.includeDevelopments = includeDevelopments;
	}

	/**
	 * @return the retryDelay
	 */
	public long getRetryDelay() {
		return retryDelay;
	}

	/**
	 * @param retryDelay the retryDelay to set
	 */
	public void setRetryDelay(long retryDelay) {
		this.retryDelay = retryDelay;
	}

}
