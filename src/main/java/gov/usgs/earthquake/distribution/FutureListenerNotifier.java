package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.AbstractListener;
import gov.usgs.util.FutureExecutorTask;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

/**
 * FutureListenerNotifier is similar to ExecutorListenerNotifier, but uses
 * Futures with an ExecutorService to implement timeouts instead of Timers.
 *
 * backgroundService is an unbounded executor, but will execute only as many
 * threads are allowed by listener executors since listener executors submit
 * tasks to the backgroundService and wait on the future.
 *
 * This ends up being more efficient because the threads where jobs execute are
 * cached, instead of a new Timer thread created for each task.
 */
public class FutureListenerNotifier extends ExecutorListenerNotifier {

  private static final Logger LOGGER = Logger
      .getLogger(FutureListenerNotifier.class.getName());

  /** Service where tasks execute using futures for timeouts. */
  private ExecutorService backgroundService;

  public FutureListenerNotifier(final DefaultNotificationReceiver receiver) {
    super(receiver);
  }

  @Override
  protected void queueNotification(final NotificationListener listener,
      final NotificationEvent event) {
    if (acceptBeforeQueuing
        && listener instanceof DefaultNotificationListener) {
      DefaultNotificationListener defaultListener = (DefaultNotificationListener) listener;
      if (!defaultListener.accept(event.getNotification().getProductId())) {
        return;
      }
    }

    // determine retry delay
    long retryDelay = 0L;
    if (listener instanceof AbstractListener) {
      retryDelay = ((AbstractListener) listener).getRetryDelay();
    }

    ExecutorService listenerExecutor = notificationListeners.get(listener);
    FutureExecutorTask<Void> listenerTask = new FutureExecutorTask<Void>(
        backgroundService, listenerExecutor, listener.getMaxTries(),
        listener.getTimeout(), new NotificationListenerCallable(
            listener, event), retryTimer, retryDelay);
    listenerExecutor.submit(listenerTask);

    // log how many notifications are pending
    if (listenerExecutor instanceof ThreadPoolExecutor) {
      BlockingQueue<Runnable> pending = ((ThreadPoolExecutor) listenerExecutor)
          .getQueue();
      LOGGER.fine("[" + event.getNotificationReceiver().getName()
          + "] listener (" + listener.getName() + ") has "
          + pending.size() + " queued notifications");
    }
  }

  @Override
  public void shutdown() throws Exception {
    super.shutdown();
    backgroundService.shutdown();
    backgroundService = null;
  }

  @Override
  public void startup() throws Exception {
    backgroundService = Executors.newCachedThreadPool();
    super.startup();
  }

}
