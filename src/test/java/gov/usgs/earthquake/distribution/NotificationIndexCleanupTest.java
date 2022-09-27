package gov.usgs.earthquake.distribution;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import gov.usgs.earthquake.product.ProductId;

public class NotificationIndexCleanupTest {

  /**
   * Test thread continues removing expired notifications before waiting.
   *
   * @throws Exception
   */
  @Test
  public void testNotificationCleanup() throws Exception{
    final MockNotificationIndex index = new MockNotificationIndex();
    final NotificationIndexCleanup cleanup = new NotificationIndexCleanup(index, null);
    // thread should start and wait
    cleanup.startup();
    Assert.assertEquals(index.removedNotifications.size(), 0);

    // use this notification multiple times for testing
    final Notification notification = new DefaultNotification(
      new ProductId("source", "type", "code"),
      new Date(),
      null
    );
    final List<Notification> notifications = new LinkedList<>();
    notifications.add(notification);
    notifications.add(notification);

    // run cleanup thread, should remove 2 notifications
    index.expiredNotificationsReturns.add(notifications);
    cleanup.wakeUp();
    Thread.sleep(1L);
    Assert.assertEquals(index.removedNotifications.size(), 2);

    // run with multiple findExpiredNotifications returns, should remove all
    index.removedNotifications.clear();
    index.expiredNotificationsReturns.add(notifications);
    index.expiredNotificationsReturns.add(notifications);
    cleanup.wakeUp();
    Thread.sleep(2L);
    Assert.assertEquals(index.removedNotifications.size(), 4);
  }

  /**
   * Test listener is notified before notification is removed.
   *
   * @throws Exception
   */
  @Test
  public void testNotificationCleanupListener() throws Exception{
    final MockNotificationIndex index = new MockNotificationIndex();
    // capture notifications listener receives
    final LinkedList<Notification> listenerNotifications = new LinkedList<>();
    final NotificationIndexCleanup cleanup = new NotificationIndexCleanup(index, (Notification expired) -> {
      listenerNotifications.add(expired);
      // check size before return
      Assert.assertTrue(listenerNotifications.size() > index.removedNotifications.size());
    });
    // thread should start and wait
    cleanup.startup();
    Assert.assertEquals(0, index.removedNotifications.size());
    Assert.assertEquals(0, listenerNotifications.size());
    // use this notification multiple times for testing
    final Notification notification = new DefaultNotification(
      new ProductId("source", "type", "code"),
      new Date(),
      null
    );
    final List<Notification> notifications = new LinkedList<>();
    notifications.add(notification);
    notifications.add(notification);

    // run cleanup thread, should remove 2 notifications
    index.expiredNotificationsReturns.add(notifications);
    cleanup.wakeUp();
    Thread.sleep(5L);
    Assert.assertEquals(2, index.removedNotifications.size());
    Assert.assertEquals(2, listenerNotifications.size());
    // run with multiple findExpiredNotifications returns, should remove all
    index.removedNotifications.clear();
    listenerNotifications.clear();
    index.expiredNotificationsReturns.add(notifications);
    index.expiredNotificationsReturns.add(notifications);
    cleanup.wakeUp();
    Thread.sleep(5L);
    Assert.assertEquals(4, index.removedNotifications.size());
    Assert.assertEquals(4, listenerNotifications.size());
  }

  /**
   * Test notification is not removed when listener throws exception.
   *
   * @throws Exception
   */
  @Test
  public void testNotificationCleanupListenerException() throws Exception{
    final MockNotificationIndex index = new MockNotificationIndex();
    // capture notifications listener receives
    final LinkedList<Notification> listenerNotifications = new LinkedList<>();
    final LinkedList<Boolean> listenerThrow = new LinkedList<>();
    final NotificationIndexCleanup cleanup = new NotificationIndexCleanup(index, (Notification expired) -> {
      if (listenerThrow.size() > 0) {
        throw new Exception("listener error");
      }
      listenerNotifications.add(expired);
    });
    // thread should start and wait
    cleanup.startup();
    Assert.assertEquals(0, index.removedNotifications.size());
    Assert.assertEquals(0, listenerNotifications.size());
    // use this notification multiple times for testing
    final Notification notification = new DefaultNotification(
      new ProductId("source", "type", "code"),
      new Date(),
      null
    );
    final List<Notification> notifications = new LinkedList<>();
    notifications.add(notification);
    notifications.add(notification);

    // run with listener throwing exceptions
    index.expiredNotificationsReturns.add(notifications);
    listenerThrow.add(true);
    cleanup.wakeUp();
    Thread.sleep(5L);
    Assert.assertEquals(0, index.removedNotifications.size());
    Assert.assertEquals(0, listenerNotifications.size());
    // run with multiple findExpiredNotifications returns, should remove all
    index.expiredNotificationsReturns.add(notifications);
    listenerThrow.clear();
    cleanup.wakeUp();
    Thread.sleep(5L);
    Assert.assertEquals(2, index.removedNotifications.size());
    Assert.assertEquals(2, listenerNotifications.size());
  }

  /** Class for testing NotificationIndexCleanup interaction with a NotificationIndex */
  public static class MockNotificationIndex extends JDBCNotificationIndex {
    public List<List<Notification>> expiredNotificationsReturns = new LinkedList<>();
    public List<Notification> removedNotifications = new LinkedList<>();

    public MockNotificationIndex() throws Exception {
      super();
    }

    // not using actual database connection for this test
    public void startup() throws Exception {}
    public void shutdown() throws Exception {}

    /**
     * Mock the find expired notification method to return lists from expiredNotificationsReturns.
     */
    public List<Notification> findExpiredNotifications() throws Exception {
      if (expiredNotificationsReturns == null || expiredNotificationsReturns.size() == 0) {
        return new ArrayList<Notification>();
      }
      return expiredNotificationsReturns.remove(0);
    }

    @Override
    public void removeNotification(final Notification notification) throws Exception {
      removedNotifications.add(notification);
    }

    @Override
    public void removeNotifications(final List<Notification> notifications) throws Exception {
      removedNotifications.addAll(notifications);
    }

  }

}
