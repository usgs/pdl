package gov.usgs.earthquake.distribution;

/**
 * A configuration exception, thrown while loading the config file if there are
 * problems.
 */
public class ConfigurationException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Construct a configuration exception with only a string describing the
	 * error.
	 * 
	 * @param message
	 *            description of the error (and possibly, solution).
	 */
	public ConfigurationException(final String message) {
		super(message);
	}

	/**
	 * Construct a configuration exception with a string describing the error,
	 * and an exception that was caught that led to the problem.
	 * 
	 * @param message
	 * @param cause
	 */
	public ConfigurationException(final String message, final Exception cause) {
		super(message, cause);
	}

}
