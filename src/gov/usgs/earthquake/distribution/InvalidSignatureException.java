/*
 * InvalidSignatureException
 * 
 * $Id: InvalidSignatureException.java 10673 2011-06-30 23:48:47Z jmfee $
 * $URL: https://ghttrac.cr.usgs.gov/websvn/ProductDistribution/trunk/src/gov/usgs/earthquake/distribution/InvalidSignatureException.java $
 */
package gov.usgs.earthquake.distribution;

/**
 * An exception thrown when storing a product already in storage.
 */
public class InvalidSignatureException extends Exception {

	/** Serial version UID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Construct a new StorageException object.
	 * 
	 * @param message
	 *            description of exception.
	 */
	public InvalidSignatureException(String message) {
		super(message);
	}

	/**
	 * Construct a new StorageException object.
	 * 
	 * @param message
	 *            description of exception
	 * @param cause
	 *            the exception that caused this exception.
	 */
	public InvalidSignatureException(String message, Throwable cause) {
		super(message, cause);
	}

}
