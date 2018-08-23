package gov.usgs.util;

/**
 * Default implementation of all methods on the Configurable interface.
 *
 * Classes may override individual methods as needed.
 */
public class DefaultConfigurable implements Configurable {

	/** Name of this configurable object. */
	private String name;

	/**
	 * Process configuration settings.
	 *
	 * Called before startup().
	 *
	 * @param config
	 *            the Config object with settings.
	 */
	@Override
	public void configure(Config config) throws Exception {
	}

	/**
	 * Start any processing/background threads.
	 */
	@Override
	public void startup() throws Exception {
	}

	/**
	 * Stop any processing/background threads.
	 */
	@Override
	public void shutdown() throws Exception {
	}

	/**
	 * @return the name.
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name.
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

}
