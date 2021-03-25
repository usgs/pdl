/*
 * TimeoutProcess
 *
 * $Id$
 * $URL$
 */
package gov.usgs.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;

/**
 * TimeoutProcess wraps a Process object.
 *
 * It is most commonly used with TimeoutProcessBuilder, which configures the
 * process timeout (and sets the timed out state once the timeout is reached).
 *
 * @see java.lang.Process
 * @see TimeoutProcessBuilder
 * @see ProcessTimeoutException
 */
public class TimeoutProcess {

	/** The wrapped process */
	private Process process;

	/** Whether this process timed out. */
	private boolean timeoutElapsed = false;

	/** Timer object that will destroy this process. */
	private Timer timer = null;

	/** Standard error output. */
	private byte[] errorOutput;

	/**
	 * Construct a new TimeoutProcess.
	 *
	 * @param process
	 *            the wrapped process.
	 */
	protected TimeoutProcess(Process process) {
		this.process = process;
	}

	/** Destroys a process */
	public void destroy() {
		process.destroy();
	}

	/** @return errorOutput byte array */
	public byte[] errorOutput() {
		return errorOutput;
	}

	/** @return exit value */
	public int exitValue() {
		return process.exitValue();
	}

	/** @return InputStream of error stream */
	public InputStream getErrorStream() {
		return process.getErrorStream();
	}

	/** @return InputStream */
	public InputStream getInputStream() {
		return process.getInputStream();
	}

	/** @return OutputStream */
	public OutputStream getOutputStream() {
		return process.getOutputStream();
	}

	/**
	 * Wait for the process to complete, either normally or because its timeout
	 * was reached.
	 *
	 * @return exitStatus.
	 * @throws InterruptedException
	 *             if thread interruption occurs
	 * @throws IOException
	 *             if IO error occurs
	 * @throws ProcessTimeoutException
	 *             if the process timed out before exiting.
	 */
	public int waitFor() throws InterruptedException, IOException, ProcessTimeoutException {
		int status = -1;
		try {
			status = process.waitFor();

			if (timeoutElapsed()) {
				throw new ProcessTimeoutException("The process has timed out.");
			}
		} finally {
			if (timer != null) {
				// the timer hasn't destroyed this process already, cancel it.
				timer.cancel();
			}
		}

		try {
			errorOutput = StreamUtils.readStream(getErrorStream());
		} finally {
			// close streams
			StreamUtils.closeStream(getErrorStream());
			StreamUtils.closeStream(getInputStream());
			StreamUtils.closeStream(getOutputStream());
		}

		return status;
	}

	/** @param timeoutElapsed to set */
	protected void setTimeoutElapsed(boolean timeoutElapsed) {
		this.timeoutElapsed = timeoutElapsed;
	}

	/** @return timeoutElapsed boolean */
	protected boolean timeoutElapsed() {
		return timeoutElapsed;
	}

	/** @param timer to set */
	protected void setTimer(final Timer timer) {
		this.timer = timer;
	}

}
