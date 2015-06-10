/*
 * EIDSListener
 * 
 * $Id: EIDSListener.java 17874 2012-12-12 19:24:42Z jmfee $
 * $URL: https://ghttrac.cr.usgs.gov/websvn/ProductDistribution/trunk/src/gov/usgs/earthquake/eidsutil/EIDSListener.java $
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
