package gov.usgs.earthquake.distribution;

/**
 * An exception thrown by a NotificationListener when it cannot process a
 * product.
 */
public class NotificationListenerException extends Exception {

	private static final long serialVersionUID = 1L;

	public NotificationListenerException(final String message) {
		super(message);
	}

	public NotificationListenerException(final String message,
			final Exception cause) {
		super(message, cause);
	}

	public NotificationListenerException(final Throwable cause) {
		super(cause);
	}

}
