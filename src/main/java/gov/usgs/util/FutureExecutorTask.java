package gov.usgs.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FutureExecutorTask overrides how timeouts are handled to use a
 * separate executor service with Futures.
 */
public class FutureExecutorTask<T> extends ExecutorTask<T> {

  /** Logging object. */
  private static final Logger LOGGER = Logger.getLogger(FutureExecutorTask.class
      .getName());

  /** Default number of milliseconds to wait before a retry. */
  public static final long DEFAULT_RETRY_DELAY = 0L;

  /** Default number of tries to run this task. */
  public static final int DEFAULT_NUM_TRIES = 1;

  /** Default timeout for this task. */
  public static final long DEFAULT_TIMEOUT = 0L;

  /** ExecutorService used to execute callable. */
  protected ExecutorService backgroundService;

  /**
   * Construct a new ExecutorTask
   *
   * @param service
   *            ExecutorService that this task will be submitted to.
   * @param maxTries
   *            maximum number of tries callable can throw an exception or
   *            timeout before giving up. &lt; 1 means never run.
   * @param timeout
   *            number of milliseconds to allow callable to run before it is
   *            interrupted. &lt;= 0 means never timeout.
   * @param callable
   *            the callable to call. To work well, the callable should handle
   *            interrupts gracefully.
   * @see InterruptedException
   */
  public FutureExecutorTask(ExecutorService backgroundService, ExecutorService service,
      int maxTries, long timeout, Callable<T> callable) {
    super(service, maxTries, timeout, callable, null, DEFAULT_RETRY_DELAY);
    this.backgroundService = backgroundService;
  }

  /**
   * Wraps a runnable and result using the CallableRunnable class.
   *
   * @see java.util.concurrent.Executors#callable(Runnable, Object)
   */
  public FutureExecutorTask(ExecutorService backgroundService, ExecutorService service,
      int maxTries, long timeout, Runnable runnable, T result) {
    super(service, maxTries, timeout, Executors.callable(runnable, result));
    this.backgroundService = backgroundService;
  }

  /**
   * Construct a new FutureExecutorTask
   *
   * @param service
   *            ExecutorService that this task will be submitted to.
   * @param maxTries
   *            maximum number of tries callable can throw an exception or
   *            timeout before giving up. &lt; 1 means never run.
   * @param timeout
   *            number of milliseconds to allow callable to run before it is
   *            interrupted. &lt;= 0 means never timeout.
   * @param callable
   *            the callable to call. To work well, the callable should handle
   *            interrupts gracefully.
   * @param retryTimer
   *            a timer used to schedule retries when retryDelay is non-zero.
   * @param retryDelay
   *            the number of milliseconds to wait before retrying after an
   *            exception.
   * @see InterruptedException
   */
  public FutureExecutorTask(ExecutorService backgroundService, ExecutorService service,
      int maxTries, long timeout, Callable<T> callable, Timer retryTimer,
      long retryDelay) {
    super(service, maxTries, timeout, callable, retryTimer, retryDelay);
    this.backgroundService = backgroundService;
  }

  /**
   * Run calls the callable, scheduling timeout interruption, catching
   * exceptions, and potentially resubmitting to the executor service.
   */
  @Override
  public void run() {
    Future<T> future = null;
    try {
      if (done || cancelled || numTries >= maxTries) {
        // already done, cancelled, or out of attempts
        return;
      }

      // otherwise,
      ++numTries;

      // signal that we are running
      runThread = Thread.currentThread();

      // use future to manage timeout
      future = backgroundService.submit(this.callable);
      try {
        if (timeout > 0) {
          result = future.get(timeout, TimeUnit.MILLISECONDS);
        } else {
          result = future.get();
        }
      } finally {
        // cancel whether successful (noop) or exception (interrupt callable)
        future.cancel(true);
      }

      // signal that we are done running
      runThread = null;

      // computed without exceptions, done
      setDone();
    } catch (Exception e) {
      if (e instanceof ExecutionException) {
        // unpack cause
        Throwable cause = e.getCause();
        if (cause != null && cause instanceof Exception) {
          e = (Exception) cause;
        }
      }
      LOGGER.log(Level.INFO, "Exception executing task", e);
      // signal that we are not running
      runThread = null;

      // track this exception
      exceptions.add(e);

      // try to resubmit
      if (!cancelled && numTries < maxTries) {
        LOGGER.info("Resubmitting task to executor " + numTries + "/"
            + maxTries + " attempts");
        SubmitTaskToExecutor retryTask = new SubmitTaskToExecutor(this);
        if (retryDelay <= 0L || retryTimer == null) {
          retryTask.run();
        } else {
          retryTimer.schedule(retryTask, retryDelay);
        }
      } else {
        // cancelled or out of tries, done
        setDone();
      }
    }
  }

  /**
   * Submit a FutureExecutorTask to an ExecutorService.
   *
   * Used to defer resubmission of a task after it fails, but scheduling its
   * resubmission using a timer.
   */
  private class SubmitTaskToExecutor extends TimerTask {

    /** The task to resubmit. */
    private FutureExecutorTask<T> task;

    /**
     * Construct a new SubmitTaskToExecutor instance.
     *
     * @param task
     *            the task to resubmit.
     */
    public SubmitTaskToExecutor(final FutureExecutorTask<T> task) {
      this.task = task;
    }

    /**
     * Submits the task to the executor.
     */
    public void run() {
      service.submit(task);
    }

  }

}
