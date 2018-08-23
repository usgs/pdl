/*
 * ExecutorTask
 * 
 * $Id$
 * $URL$
 */
package gov.usgs.util;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper for Runnable or Callable objects for use with an ExecutorService.
 * 
 * Can be used to schedule interrupt based timeouts, multiple attempts, and
 * Future style exception tracking for Runnable or Callable objects.
 * 
 * @param <T> return type for callable.
 */
public final class ExecutorTask<T> implements Future<T>, Runnable {

	/** Logging object. */
	private static final Logger LOGGER = Logger.getLogger(ExecutorTask.class
			.getName());

	/** Default number of milliseconds to wait before a retry. */
	public static final long DEFAULT_RETRY_DELAY = 0L;

	/** Default number of tries to run this task. */
	public static final int DEFAULT_NUM_TRIES = 1;

	/** Default timeout for this task. */
	public static final long DEFAULT_TIMEOUT = 0L;

	/** ExecutorService used to execute this task. */
	private ExecutorService service;

	/** The callable to be called. */
	private Callable<T> callable;

	/** Timeout for task. */
	private long timeout = DEFAULT_TIMEOUT;

	/** Number of tries to execute this task. */
	private int maxTries = DEFAULT_NUM_TRIES;

	/** Number of milliseconds to wait before trying again. */
	private long retryDelay = DEFAULT_RETRY_DELAY;

	/** Timer used to schedule retries, when they have a non-zero delay. */
	private Timer retryTimer;

	/** The future from the executor service. */
	private T result;

	/** List of exceptions thrown, up to maxTries in length. */
	ArrayList<Exception> exceptions;

	/** Whether this task is complete. */
	private Boolean done = false;

	/** Whether this task has been canceled. */
	private Boolean cancelled = false;

	/** Number of tries used. */
	private int numTries = 0;

	/** The thread where this is running, used to interrupt. */
	private Thread runThread = null;

	/** Name for this task. */
	private String name = null;

	private final Object syncObject = new Object();

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
	public ExecutorTask(ExecutorService service, int maxTries, long timeout,
			Callable<T> callable) {
		this(service, maxTries, timeout, callable, null, DEFAULT_RETRY_DELAY);
	}

	/**
	 * Wraps a runnable and result using the CallableRunnable class.
	 * 
	 * @see java.util.concurrent.Executors#callable(Runnable, Object)
	 */
	public ExecutorTask(ExecutorService service, int maxTries, long timeout,
			Runnable runnable, T result) {
		this(service, maxTries, timeout, Executors.callable(runnable, result));
	}

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
	 * @param retryTimer
	 *            a timer used to schedule retries when retryDelay is non-zero.
	 * @param retryDelay
	 *            the number of milliseconds to wait before retrying after an
	 *            exception.
	 * @see InterruptedException
	 */
	public ExecutorTask(ExecutorService service, int maxTries, long timeout,
			Callable<T> callable, Timer retryTimer, long retryDelay) {
		this.service = service;
		this.maxTries = maxTries;
		this.timeout = timeout;
		this.callable = callable;
		this.exceptions = new ArrayList<Exception>(maxTries);
		this.retryTimer = retryTimer;
		this.retryDelay = retryDelay;
	}

	/**
	 * Run calls the callable, scheduling timeout interruption, catching
	 * exceptions, and potentially resubmitting to the executor service.
	 */
	@Override
	public void run() {
		// used to schedule timeout
		Timer timeoutTimer = new Timer();

		try {
			// synchronized (this) {
			if (done || cancelled || numTries >= maxTries) {
				// already done, cancelled, or out of attempts
				return;
			}

			// otherwise,
			++numTries;
			// signal that we are running
			runThread = Thread.currentThread();
			if (timeout > 0) {
				// schedule interrupt
				final Thread currentThread = runThread;
				timeoutTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						LOGGER.fine("Interrupting executor thread");
						currentThread.interrupt();
					}
				}, timeout);
			}
			// }

			// compute result, outside synchronized
			result = callable.call();

			// synchronized (this) {
			// signal that we are done running
			runThread = null;

			// computed without exceptions, done
			done();
			// }
		} catch (Exception e) {
			LOGGER.log(Level.INFO, "Exception executing task", e);
			// synchronized (this) {
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
				done();
			}
			// }
		} finally {
			// cancel timeout based interrupt
			timeoutTimer.cancel();
		}
	}

	/**
	 * Called when task is completed, either successfully, or unsuccessfully and
	 * has no more tries
	 */
	private void done() {
		// done running, either successfully or because out of tries
		done = true;
		// notify anyone waiting for task to complete
		synchronized (syncObject) {
			syncObject.notifyAll();
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (cancelled || done) {
			// already canceled or complete
			return cancelled;
		}

		cancelled = true;
		if (runThread != null && mayInterruptIfRunning) {
			// running, try to interrupt
			runThread.interrupt();
		}

		// thread may still be running, if it doesn't handle interrupts well,
		// but Future interface says we are done
		done();

		return cancelled;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	/**
	 * Get the result returned by the callable.
	 */
	@Override
	public T get() throws InterruptedException, ExecutionException {
		while (!cancelled && !done && numTries < maxTries) {
			synchronized (syncObject) {
				syncObject.wait();
			}
		}

		if (numTries == maxTries && exceptions.size() == maxTries) {
			// can't execute any more, signal using most recent exception
			throw new ExecutionException(exceptions.get(maxTries - 1));
		}

		return result;
	}

	/**
	 * Get the result returned by the callable.
	 */
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		if (!cancelled && !done && numTries < maxTries) {
			synchronized (syncObject) {
				unit.timedWait(syncObject, timeout);
			}
		}

		if (!cancelled && !done) {
			// must have timed out
			throw new TimeoutException();
		}

		if (numTries == maxTries && exceptions.size() == maxTries) {
			// can't execute any more, signal using most recent exception
			throw new ExecutionException(exceptions.get(maxTries - 1));
		}

		return result;
	}

	/**
	 * Number of tries used.
	 * 
	 * @return actual number of attempts.
	 */
	public int getNumTries() {
		return numTries;
	}

	/**
	 * Maximum number of tries before giving up.
	 * 
	 * @return maximum number of attempts.
	 */
	public int getMaxTries() {
		return maxTries;
	}

	/**
	 * Any exceptions thrown, during any execution attempt.
	 * 
	 * @return array of thrown exceptions. should contain no more than numTries
	 *         exceptions.
	 */
	public ArrayList<Exception> getExceptions() {
		return exceptions;
	}

	/**
	 * The callable object that is/was called.
	 * 
	 * @return The callable object for this task. If this task was created using
	 *         a runnable, this was created using Executors.callable(Runnable).
	 */
	public Callable<T> getCallable() {
		return callable;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @return the retryDelay
	 */
	public long getRetryDelay() {
		return retryDelay;
	}

	/**
	 * @param retryDelay
	 *            the retryDelay to set
	 */
	public void setRetryDelay(long retryDelay) {
		this.retryDelay = retryDelay;
	}

	/**
	 * @return the retryTimer
	 */
	public Timer getRetryTimer() {
		return retryTimer;
	}

	/**
	 * @param retryTimer
	 *            the retryTimer to set
	 */
	public void setRetryTimer(Timer retryTimer) {
		this.retryTimer = retryTimer;
	}

	/**
	 * Submit an ExecutorTask to an ExecutorService.
	 * 
	 * Used to defer resubmission of a task after it fails, but scheduling its
	 * resubmission using a timer.
	 */
	private class SubmitTaskToExecutor extends TimerTask {

		/** The task to resubmit. */
		private ExecutorTask<T> task;

		/**
		 * Construct a new SubmitTaskToExecutor instance.
		 * 
		 * @param task
		 *            the task to resubmit.
		 */
		public SubmitTaskToExecutor(final ExecutorTask<T> task) {
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
