/*
 * Configurable
 *
 * $Id: Configurable.java 7868 2010-10-21 23:05:34Z jmfee $
 * $URL: https://ehptools.cr.usgs.gov/svn/ProductDistribution/trunk/src/gov/usgs/earthquake/distribution/Configurable.java $
 */
package gov.usgs.util;

/**
 * An interface for objects that require configuration.
 *
 * Implementers of this interface should include an empty constructor.
 */
public interface Configurable {

	/**
	 * This method is called after an object is instantiated, but before any
	 * other methods are called.
	 *
	 * @param config
	 *            the Config to load.
	 * @throws Exception
	 *            if configuration exceptions occur.
	 */
	public void configure(final Config config) throws Exception;

	/**
	 * This method is called after all objects are configured, and processing
	 * should begin.
	 *
	 * @throws Exception
	 *            if exceptions occur while starting.
	 */
	public void startup() throws Exception;

	/**
	 * This method is called when the client is shutting down.
	 *
	 * @throws Exception
	 *            if exceptions occur while starting.
	 */
	public void shutdown() throws Exception;

	/**
	 * Get this object name.
	 *
	 * @return the name.
	 */
	public String getName();

	/**
	 * Set this object name.
	 *
	 * This method is typically called by a Config object when the configurable
	 * object is loaded out of a config file. name will be set to the config
	 * section.
	 *
	 * @param string
	 *            the name.
	 */
	public void setName(final String string);

}
