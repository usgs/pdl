package gov.usgs.earthquake.distribution;

/**
 * An exception thrown by a NotificationListener when it cannot process a
 * product.
 */
public class NotificationListenerException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * NotificationListener exception only taking a message
	 * @param message String
	 */
	public NotificationListenerException(final String message) {
		super(message);
	}

	/**
	 * NotificationListener exception taking a message and cause
	 * @param message String
	 * @param cause Exception that occured
	 */
	public NotificationListenerException(final String message,
			final Exception cause) {
		super(message, cause);
	}

		/**
	 * NotificationListener exception only taking a throwable cause
	 * @param cause Throwable
	 */
	public NotificationListenerException(final Throwable cause) {
		super(cause);
	}

}
