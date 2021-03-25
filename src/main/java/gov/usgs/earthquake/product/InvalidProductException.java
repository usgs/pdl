package gov.usgs.earthquake.product;

public class InvalidProductException extends Exception {

	private static final long serialVersionUID = 0x2943A1B7;

	/** Generic Invalid Product exception constructor */
	public InvalidProductException() {
		super();
	}

	/**
	 * Exception taking in a message
	 * @param message Message relating to exception
	 */
	public InvalidProductException(String message) {
		super(message);
	}

	/**
	 * Exception taking in a message, cause
	 * @param message Message relating to exception
	 * @param cause throwable relating to exception
	 */
	public InvalidProductException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Exception taking in a cause
	 * @param cause throwable relating to exception
	 */
	public InvalidProductException(Throwable cause) {
	}
}
