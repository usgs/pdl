package gov.usgs.earthquake.distribution;

/**
 * Wrapper exception class. This is thrown by NotificationListeners from the
 * <code>onNotification</code> method. This exception exists only so we can
 * identify the type of exception and attempt to resubmit a failed notification
 * for indexing.
 *
 * @author emartinez
 *
 */
public class ContinuableListenerException extends Exception {

	private static final long serialVersionUID = 0x256D2BEL;

	/**
	 * Wrapper for exception. Takes message
	 * @param message string
	 */
	public ContinuableListenerException(String message) {
		super(message);
	}

	/**
	 * Wrapper for exception. Takes cause
	 * @param cause throwable
	 */
	public ContinuableListenerException(Throwable cause) {
		super(cause);
	}

	/**
	 * Wrapper for exception. Takes message and cause
	 * @param message string
	 * @param cause throwable
	 */
	public ContinuableListenerException(String message, Throwable cause) {
		super(message, cause);
	}

}
