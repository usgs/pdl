package gov.usgs.earthquake.product;

public class InvalidProductException extends Exception {

	private static final long serialVersionUID = 0x2943A1B7;

	public InvalidProductException() {
		super();
	}
	
	public InvalidProductException(String message) {
		super(message);
	}
	
	public InvalidProductException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public InvalidProductException(Throwable cause) {
	}
}
