package gov.usgs.earthquake.distribution;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NotificationIndexCleanup manages cleaning up expired notifications.
 *
 * Uses background thread to remove expired notifications while they exist,
 * then uses wait/notify to pause until shutdown() or wakeUp() methods are
 * called.
 *
 * NOTE: this class does not schedule periodic cleanup, and the wakeUp() method
 * must be called periodically.
 */
public class NotificationIndexCleanup implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(NotificationIndexCleanup.class.getName());

  public final NotificationIndex index;

  // listener that can take additional actions during cleanup
  public final Listener listener;

  // object used to synchronize state access between threads
  public final Object syncObject = new Object();
  // thread where cleanup loop runs
  public Thread cleanupThread = null;
  // whether thread should stop running
  private boolean stopThread = false;

  public NotificationIndexCleanup(final NotificationIndex index, final Listener listener) {
    this.index = index;
    this.listener = listener;
  }

  /**
   * Notification cleanup thread loop.
   *
   * This method blocks and should probably not be called by you.
   */
  public void run() {
    final String indexName = this.index.getName();

    LOGGER.finer(() -> "[" + indexName + "] NotificationIndexCleanup starting");
    // run until thread stopped
    while (!stopThread) {
      List<Notification> expiredNotifications = null;
      synchronized (syncObject) {
        try {
          expiredNotifications = this.index.findExpiredNotifications();
        } catch (Exception e) {
          LOGGER.log(Level.INFO, e, () -> "[" + indexName + "] exception finding expired notifications");
        }
        if (expiredNotifications == null || expiredNotifications.size() == 0) {
          // Wait for expired notifications to process
          try {
            syncObject.wait();
          } catch (InterruptedException ignore) {
            // signal from another thread (stopThread checked above)
            continue;
          }
        }
      }

      // remove batch of expired notifications
      int removed = 0;
      for (final Notification expired : expiredNotifications) {
        synchronized (syncObject) {
          if (stopThread) {
            break;
          }
        }
        try {
          if (this.listener != null) {
            this.listener.onExpiredNotification(expired);
          }
          this.index.removeNotification(expired);
          removed++;
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, e, () -> "[" + indexName + "] Exception removing expired notification");
        }
      }
      final int total = removed;
      LOGGER.fine(() -> "[" + indexName + "] Removed " + total + " expired notifications");
    }
    LOGGER.finer(() -> "[" + indexName + "] NotificationIndexCleanup exiting");
    this.cleanupThread = null;
  }

  /**
   * Start cleanup process.
   *
   * @throws Exception
   */
  public void startup() throws Exception {
    synchronized (syncObject) {
      if (this.cleanupThread != null) {
        throw new IllegalStateException("Already started");
      }
      // start thread
      stopThread = false;
      this.cleanupThread = new Thread(this);
    }
    this.cleanupThread.start();
  }

  /**
   * Stop cleanup process.
   *
   * @throws Exception
   */
  public void shutdown() throws Exception {
    synchronized (syncObject) {
      if (this.cleanupThread == null) {
        throw new IllegalStateException("Already stopped");
      }
      // stop thread
      stopThread = true;
      this.cleanupThread.interrupt();
    }
    this.cleanupThread.join();
  }

  /**
   * Wake up the background thread if it is waiting.
   */
  public void wakeUp() {
    synchronized (syncObject) {
      syncObject.notify();
    }
  }

  /**
   * Interface for cleanup listeners to take additional steps before a
   * notification is removed.
   */
  public static interface Listener {
    public void onExpiredNotification(final Notification expired) throws Exception;
  }
}
