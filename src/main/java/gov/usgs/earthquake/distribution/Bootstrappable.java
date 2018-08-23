package gov.usgs.earthquake.distribution;

/**
 * An interface used by the Bootstrap class to invoke the main class.
 */
public interface Bootstrappable {

	/** Exit code used when run method throws exception. */
	public static final int RUN_EXCEPTION_EXIT_CODE = 111;

	/**
	 * Called by Bootstrap after processing the Configurable interface.
	 * 
	 * @param args
	 *            array of command line arguments.
	 */
	public void run(final String[] args) throws Exception;

}
