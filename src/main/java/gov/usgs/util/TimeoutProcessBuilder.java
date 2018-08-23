/*
 * TimeoutProcessBuilder
 * 
 * $Id$
 * $URL$
 */
package gov.usgs.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The TimeoutProcessBuilder wraps a ProcessBuilder, adding support for a
 * command time out.
 * 
 * This class does not support a full command String complete with arguments.
 * You can use the StringUtils.split method to get around this.
 * 
 * @see java.lang.ProcessBuilder
 * @see TimeoutProcess
 */
public class TimeoutProcessBuilder {

	/** The wrapped process builder. */
	private ProcessBuilder builder = null;
	/** The timeout for this process. */
	private long timeout = -1;

	/**
	 * Create a new TimeoutProcessBuilder with a timeout and an array of
	 * strings.
	 * 
	 * @param timeout
	 *            timeout in milliseconds for process, or &lt;= 0 for no timeout.
	 * @param command
	 *            array of strings that represent command. The first element
	 *            must be the full path to the executable, without arguments.
	 */
	public TimeoutProcessBuilder(long timeout, String... command) {
		builder = new ProcessBuilder(command);
		this.timeout = timeout;
	}

	/**
	 * Create a new TimeoutProcessBuilder with a timeout and an array of
	 * strings.
	 * 
	 * @param timeout
	 *            timeout in milliseconds for process, or &lt;= 0 for no timeout.
	 * @param command
	 *            list of strings that represent command.
	 */
	public TimeoutProcessBuilder(long timeout, List<String> command) {
		builder = new ProcessBuilder(command);
		this.timeout = timeout;

	}

	/**
	 * This signature is preserved, but calls the alternate constructor with
	 * argument order swapped.
	 */
	@Deprecated
	public TimeoutProcessBuilder(List<String> command, long timeout) {
		this(timeout, command);
	}

	public List<String> command() {
		return builder.command();
	}

	public TimeoutProcessBuilder command(List<String> command) {
		builder.command(command);
		return this;
	}

	public TimeoutProcessBuilder command(String command) {
		builder.command(command);
		return this;
	}

	public File directory() {
		return builder.directory();
	}

	public TimeoutProcessBuilder directory(File directory) {
		builder.directory(directory);
		return this;
	}

	public Map<String, String> environment() {
		return builder.environment();
	}

	public boolean redirectErrorStream() {
		return builder.redirectErrorStream();
	}

	public TimeoutProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
		builder.redirectErrorStream(redirectErrorStream);
		return this;
	}

	public TimeoutProcess start() throws IOException {
		final TimeoutProcess process = new TimeoutProcess(builder.start());

		if (timeout > 0) {
			// set up the timeout for this process
			final Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					process.setTimeoutElapsed(true);
					process.destroy();
				}
			}, timeout);
			process.setTimer(timer);
		}

		return process;
	}

	public long getTimeout() {
		return this.timeout;
	}

	public void setTimeout(final long timeout) {
		this.timeout = timeout;
	}

}
