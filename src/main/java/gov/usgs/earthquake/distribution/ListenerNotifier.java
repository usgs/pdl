package gov.usgs.earthquake.distribution;

import gov.usgs.util.Configurable;

public interface ListenerNotifier extends Configurable {

	public void addNotificationListener(NotificationListener listener)
			throws Exception;

	public void removeNotificationListener(NotificationListener listener)
			throws Exception;

	public void notifyListeners(final NotificationEvent event) throws Exception;

}
