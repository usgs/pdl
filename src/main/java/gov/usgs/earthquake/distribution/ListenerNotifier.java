package gov.usgs.earthquake.distribution;

import gov.usgs.util.Configurable;

public interface ListenerNotifier extends Configurable {

	/**
	 * Interface method to add NotificationListener
	 * @param listener NotificationListener
	 * @throws Exception if error occurs
	 */
	public void addNotificationListener(NotificationListener listener)
			throws Exception;

	/**
	 * Interface method to remove NotificationListener
	 * @param listener NotificationListener
	 * @throws Exception if error occurs
	 */
	public void removeNotificationListener(NotificationListener listener)
			throws Exception;

	/**
	 * Interface method to notify Listener
	 * @param event NotificationEvent
	 * @throws Exception if error occurs
	 */
	public void notifyListeners(final NotificationEvent event) throws Exception;

}
