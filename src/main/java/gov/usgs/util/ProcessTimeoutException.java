/*
 * ProcessTimeoutException
 *
 * $Id$
 * $URL$
 */
package gov.usgs.util;

/**
 * Exception thrown when TimeoutProcess times out while it is being waited for.
 */
public class ProcessTimeoutException extends Exception {
	private static final long serialVersionUID = 0x52AF13AL;

	/**
	 * @param message relating to exception
	 */
	public ProcessTimeoutException(String message) {
		super(message);
	}
}
