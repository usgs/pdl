/*
 * EIDSListener
 */
package gov.usgs.earthquake.eidsutil;

/**
 * Receives messages from an EIDSClient.
 */
public interface EIDSListener {

	/**
	 * Receive a message from an EIDSClient.
	 * 
	 * @param event
	 *            an event object representing the message that was received.
	 */
	public void onEIDSMessage(final EIDSMessageEvent event);

}
