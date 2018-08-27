package gov.usgs.util.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Console redirection utility.
 * 
 * Replaces system.out and system.err with
 * printstreams that log all writes when flushed.
 */
public class RedirectConsole {

	/** Logger to handle writes to system.err. */
	private static final Logger SYSTEM_ERR_LOGGER = Logger
			.getLogger("system.err");

	/** Logger to handle writes to system.out. */
	private static final Logger SYSTEM_OUT_LOGGER = Logger
			.getLogger("system.out");

	/** The previous system.err stream. */
	private static PrintStream PREVIOUS_SYSTEM_ERR;

	/** The previous system.out.stream. */
	private static PrintStream PREVIOUS_SYSTEM_OUT;

	/**
	 * Redirect System.out and System.err to java.util.logging.Logger objects.
	 * 
	 * System.out is redirected to logger "system.out". System.err is redirected
	 * to logger "system.err".
	 * 
	 * Use the cancel method to undo this redirection.
	 */
	public static void redirect() {
		if (PREVIOUS_SYSTEM_ERR != null || PREVIOUS_SYSTEM_OUT != null) {
			cancel();
		}

		PREVIOUS_SYSTEM_OUT = System.out;
		System.setOut(new LoggerPrintStream("STDOUT", SYSTEM_OUT_LOGGER));

		PREVIOUS_SYSTEM_ERR = System.err;
		System.setErr(new LoggerPrintStream("STDERR", SYSTEM_ERR_LOGGER));
	}

	/**
	 * Undo a redirection previously setup using redirect().
	 * 
	 * Restores System.out and System.err to their state before redirect was
	 * called.
	 */
	public static void cancel() {
		if (PREVIOUS_SYSTEM_OUT != null) {
			// flush any pending output
			System.out.flush();
			// restore previous output stream
			System.setOut(PREVIOUS_SYSTEM_OUT);
			PREVIOUS_SYSTEM_OUT = null;
		}
		if (PREVIOUS_SYSTEM_ERR != null) {
			// flush any pending output
			System.err.flush();
			// restore previous error stream
			System.setErr(PREVIOUS_SYSTEM_ERR);
			PREVIOUS_SYSTEM_ERR = null;
		}
	}

	/**
	 * A PrintStream that writes messages to a Logger object.
	 */
	private static class LoggerPrintStream extends PrintStream {

		/** Name for output, prepended to LogRecord. */
		private String name;

		/** Logger used when flush is called. */
		private Logger logger;

		public LoggerPrintStream(final String name, final Logger logger) {
			// true is for autoflush
			super(new ByteArrayOutputStream(), true);
			this.name = name;
			this.logger = logger;
		}

		/**
		 * Override to force synchronization.
		 */
		@Override
		public synchronized void write(final byte[] b) throws IOException {
			super.write(b);
		}

		/**
		 * Override to force synchronization.
		 */
		@Override
		public synchronized void write(final int b) {
			super.write(b);
		}

		/**
		 * Override to force synchronization.
		 */
		@Override
		public synchronized void write(final byte[] buf, final int off,
				final int len) {
			super.write(buf, off, len);
		}

		/**
		 * Flush forces message to be written to log file.
		 */
		@Override
		public synchronized void flush() {
			ByteArrayOutputStream baos = (ByteArrayOutputStream) out;
			try {
				logger.log(new LogRecord(Level.INFO, this.name + " "
						+ baos.toString().trim()));
			} finally {
				// tried to write at least, clear buffer...
				out = new ByteArrayOutputStream();
			}
		}

	}

}
