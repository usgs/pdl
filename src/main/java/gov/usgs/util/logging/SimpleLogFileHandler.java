package gov.usgs.util.logging;

import gov.usgs.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A java.util.logging style Handler that does daily log rotation by default.
 * 
 */
public class SimpleLogFileHandler extends Handler {

	/** Default format used. */
	public static final String DEFAULT_FILENAME_FORMAT = "'log_'yyyyMMdd'.log'";

	/** The directory where log files are written. */
	private File logDirectory;

	/** Used to generate filename for current log message. */
	private SimpleDateFormat filenameFormat;

	/** The last filename used when logging. */
	private String currentFilename;

	/** Handle to the current log file. */
	private OutputStream currentStream;

	/**
	 * Create a default SimpleLogHandler.
	 * 
	 * Uses the system locale to roll log files once a day, and default filename
	 * format "log_YYYYMMDD.log".
	 * 
	 * @param logDirectory
	 *            the directory to write log files.
	 */
	public SimpleLogFileHandler(final File logDirectory) {
		this(logDirectory, new SimpleDateFormat(DEFAULT_FILENAME_FORMAT));
	}

	/**
	 * Create a SimpleLogHandler with a custom filename format.
	 * 
	 * @param logDirectory
	 *            the directory to write log files.
	 * @param filenameFormat
	 *            the format for log files. Files are opened as soon as the
	 *            format output changes for a given log's message.
	 */
	public SimpleLogFileHandler(final File logDirectory,
			final SimpleDateFormat filenameFormat) {
		this.logDirectory = logDirectory;
		this.filenameFormat = filenameFormat;
		this.currentFilename = null;
		this.currentStream = null;
	}

	/**
	 * Closes the current log file.
	 */
	public void close() throws SecurityException {
		if (currentStream != null) {
			try {
				// log when the file was closed, if possible
				currentStream.write(("\nClosing log file at "
						+ new Date().toString() + "\n\n").getBytes());
			} catch (IOException e) {
				// ignore
			} finally {
				StreamUtils.closeStream(currentStream);
				currentStream = null;
			}
		}
	}

	/**
	 * Attempts to flush any buffered content. If exceptions occur, the stream
	 * is closed.
	 */
	public void flush() {
		if (currentStream != null) {
			try {
				currentStream.flush();
			} catch (IOException e) {
				close();
				currentStream = null;
			}
		}
	}

	/**
	 * Retrieve the outputstream for the current log file.
	 * 
	 * @param date
	 *            the date of the message about to be logged.
	 * @return and OutputStream where the log message may be written.
	 * @throws IOException
	 *             if errors occur.
	 */
	protected OutputStream getOutputStream(final Date date) throws IOException {
		String filename = filenameFormat.format(date);
		if (currentStream == null || currentFilename == null
				|| !filename.equals(currentFilename)) {
			// close any existing stream
			close();

			// filename is what is being opened
			currentFilename = filename;
			currentStream = StreamUtils.getOutputStream(new File(logDirectory,
					filename), true);

			// log when the file was opened
			currentStream
					.write(("Opened log file at " + new Date().toString() + "\n\n")
							.getBytes());
			currentStream.flush();
		}
		return currentStream;
	}

	/**
	 * Add a LogRecord to the log file.
	 */
	public void publish(LogRecord record) {
		if (record == null) {
			return;
		}

		String message = getFormatter().format(record);
		try {
			OutputStream stream = getOutputStream(new Date(record.getMillis()));
			stream.write(message.getBytes());
			flush();
		} catch (Exception e) {
			// close if any exceptions occur
			close();
		}
	}

	private static final Logger LOGGER = Logger
			.getLogger(SimpleLogFileHandler.class.getName());

	public static void main(final String[] args) throws Exception {
		SimpleDateFormat ridiculouslyShortLogs = new SimpleDateFormat(
				"'log_'yyyyMMddHHmmss'.log'");
		File logDirectory = new File("log");

		SimpleLogFileHandler handler = new SimpleLogFileHandler(logDirectory,
				ridiculouslyShortLogs);
		handler.setFormatter(new SimpleLogFormatter());
		LOGGER.addHandler(handler);

		// there should be at least 2 log files created, and more likely 3
		LOGGER.info("message to log");
		handler.close();
		LOGGER.info("message to log");
		Thread.sleep(1000);
		LOGGER.info("another message to log");
		Thread.sleep(250);
		LOGGER.info("another message to log");
		Thread.sleep(1000);
		LOGGER.info("yet another message to log");
		Thread.sleep(250);
		LOGGER.info("yet another message to log");
	}

}
