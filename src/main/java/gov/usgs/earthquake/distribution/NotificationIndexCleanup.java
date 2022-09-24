package gov.usgs.earthquake.distribution;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NotificationIndexCleanup manages cleaning up expired notifications.
 *
 * Uses background thread to remove expired notifications while they exist,
 * then uses wait/notify to pause until shutdown() or wakeUp() methods are called.
 *
 * NOTE: this class does not schedule periodic cleanup, and the wakeUp() method
 * must be called periodically.
 */
public class NotificationIndexCleanup implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(NotificationIndexCleanup.class.getName());

  public final NotificationIndex index;

  // listener that can take additional actions during cleanup
  public final Listener listener;
  // object used to wake up cleanup thread
  public final Object syncObject = new Object();
  // thread where cleanup loop runs
  public Thread cleanupThread = null;

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
    LOGGER.finer("NotificationIndexCleanup starting for " + this.index.getName());
    while (true) {
      // check here in case no exception was raised
      if (Thread.interrupted()) {
        // interrupt = shutdown
        break;
      }
      int removed = 0;
      try {
        for (final Notification expired : this.index.findExpiredNotifications()) {
          try {
            if (this.listener != null) {
              this.listener.onExpiredNotification(expired);
            }
            this.index.removeNotification(expired);
            removed++;
          } catch (Exception e) {
            LOGGER.log(Level.FINE, "Exception removing expired notification from " + this.index.getName(), e);
          }
        }
        LOGGER.fine("Removed " + removed + " expired notifications from " + this.index.getName());
      } catch (InterruptedException e) {
        // interrupt = shutdown
        break;
      } catch (Exception e) {
        LOGGER.log(Level.FINE, "Exception finding expired notifications from " + this.index.getName(), e);
      }
      try {
        if (removed > 0) {
          // keep removing expired notifications, don't wait for next interval
          // but pause to let other threads access notification index
          Thread.sleep(1L);
          continue;
        }
        // wait for next interval before checking again
        synchronized (syncObject) {
          syncObject.wait();
        }
      } catch (InterruptedException e) {
        // interrupt = shutdown
        break;
      }
    }
    LOGGER.finer("NotificationIndexCleanup exiting for " + this.index.getName());
    this.cleanupThread = null;
  }

  /**
   * Start cleanup process.
   *
   * @throws Exception
   */
  public void startup() throws Exception {
    if (this.cleanupThread != null) {
      throw new IllegalStateException("Already started");
    }
    // start thread
    this.cleanupThread = new Thread(this);
    this.cleanupThread.start();
  }

  /**
   * Stop cleanup process.
   *
   * @throws Exception
   */
  public void shutdown() throws Exception {
    if (this.cleanupThread == null) {
      throw new IllegalStateException("Already stopped");
    }
    // stop thread
    this.cleanupThread.interrupt();
    this.cleanupThread.join();
  }

  /**
   * Wake up the background thread if it is waiting.
   */
  public void wakeUp() {
    synchronized(syncObject) {
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
