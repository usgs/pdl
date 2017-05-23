/*
 * NotificationIndex
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Configurable;

import java.util.List;

/**
 * Stores and retrieves Notifications.
 * 
 * This is typically used by a NotificationReceiver to track its Notifications,
 * but may also be used by NotificationListeners. Each object should maintain a
 * separate NotificationIndex.
 */
public interface NotificationIndex extends Configurable {

	/**
	 * Add a notification to the index.
	 * 
	 * If an identical notification is already in the index, the implementation
	 * may choose whether or not to store the duplicate information.
	 * 
	 * @param notification
	 *            the notification to add.
	 * @throws Exception
	 *             if an error occurs while storing the notification.
	 */
	public void addNotification(final Notification notification)
			throws Exception;

	/**
	 * Remove a notification from the index.
	 * 
	 * All matching notifications should be removed from the index.
	 * 
	 * @param notification
	 *            the notification to remove.
	 * @throws Exception
	 *             if an error occurs while removing the notification.
	 */
	public void removeNotification(final Notification notification)
			throws Exception;

	/**
	 * Search the index for notifications matching id.
	 * 
	 * If more than one notification matches, all should be returned.
	 * 
	 * @param id
	 *            the ProductId to find.
	 * @return a list of matching notifications.
	 * @throws Exception
	 *             if an error occurs while searching the index.
	 */
	public List<Notification> findNotifications(final ProductId id)
			throws Exception;

	/**
	 * Search the index for notifications matching the sources, types, and
	 * codes.
	 * 
	 * Only one notification for each unique ProductId
	 * (source+type+code+updateTime) should be returned. If sources, types,
	 * and/or codes are null, that parameter should be considered a wildcard. If
	 * sources, types, and codes are all null, a notification for each unique
	 * ProductId in the index should be returned.
	 * 
	 * @param source
	 *            sources to include, or all if null.
	 * @param type
	 *            types to include, or all if null.
	 * @param code
	 *            codes to include, or all if null.
	 * @return a list of matching notifications.
	 * @throws Exception
	 *             if an error occurs while searching the index.
	 */
	public List<Notification> findNotifications(final String source,
			final String type, final String code) throws Exception;

	/**
	 * Search the index for notifications matching the sources, types, and
	 * codes.
	 * 
	 * Only one notification for each unique ProductId
	 * (source+type+code+updateTime) should be returned. If sources, types,
	 * and/or codes are null, that parameter should be considered a wildcard. If
	 * sources, types, and codes are all null, a notification for each unique
	 * ProductId in the index should be returned.
	 * 
	 * @param sources
	 *            sources to include, or all if null.
	 * @param types
	 *            types to include, or all if null.
	 * @param codes
	 *            codes to include, or all if null.
	 * @return a list of matching notifications.
	 * @throws Exception
	 *             if an error occurs while searching the index.
	 */
	public List<Notification> findNotifications(List<String> sources,
			List<String> types, List<String> codes) throws Exception;
	
	/**
	 * Search the index for expired notifications.
	 * 
	 * All expired notifications, even if duplicate, should be returned.
	 * 
	 * @return a list of expired notifications.
	 * @throws Exception
	 *             if an error occurs while searching the index.
	 */
	public List<Notification> findExpiredNotifications() throws Exception;

}
