/*
 * StreamUtils
 *
 * $Id$
 * $HeadURL$
 */
package gov.usgs.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/**
 * Stream input, output, and transfer utilities.
 */
public class StreamUtils {

	/** Default buffer size used for stream reads and writes. */
	public static final int DEFAULT_BUFFER_SIZE = 4096;

	/** Default connect timeout for url connections. */
	public static final int DEFAULT_URL_CONNECT_TIMEOUT = 15000;

	/** Default read timeout for url connections. */
	public static final int DEFAULT_URL_READ_TIMEOUT = 15000;

	/**
	 * Get an input stream for an Object if possible.
	 *
	 * @param obj
	 *            an InputStream, File, byte[], or String.
	 * @return an InputStream or null. If obj is a File, the stream is Buffered.
	 * @throws IOException
	 *             if an error occurs.
	 * @throws IllegalArgumentException
	 *             if obj is not an InputStream, URL, File, byte[], or String.
	 */
	public static InputStream getInputStream(final Object obj)
			throws IOException, IllegalArgumentException {
		InputStream stream = null;
		byte[] bytes = null;

		if (obj instanceof InputStream) {
			stream = (InputStream) obj;
		} else if (obj instanceof URL) {
			stream = getURLInputStream((URL) obj);
		} else if (obj instanceof File) {
			stream = new BufferedInputStream(new FileInputStream((File) obj));
		} else if (obj instanceof byte[]) {
			bytes = (byte[]) obj;
		} else if (obj instanceof String) {
			bytes = ((String) obj).getBytes();
		} else {
			throw new IllegalArgumentException(
					"Expected an InputStream, URL, File, byte[], or String");
		}

		if (bytes != null) {
			stream = new ByteArrayInputStream(bytes);
		}

		return stream;
	}

	/**
	 * Get an InputStream from a URL. If URL is a HTTP url, attempts gzip
	 * compression.
	 *
	 * @param url
	 *            the url being accessed.
	 * @return an InputStream to content at URL.
	 * @throws IOException
	 *             if an error occurs.
	 */
	public static InputStream getURLInputStream(final URL url)
			throws IOException {
		return getURLInputStream(url, DEFAULT_URL_CONNECT_TIMEOUT,
				DEFAULT_URL_READ_TIMEOUT);
	}

	/**
	 * Get an InputStream from a URL. If URL is a HTTP url, attempts gzip
	 * compression.
	 *
	 * @param url
	 *            the url being accessed.
	 * @param connectTimeout allowed time in milliseconds before connection.
	 * @param readTimeout allowed time in milliseconds before read.
	 * @return an InputStream to content at URL.
	 * @throws IOException
	 *             if an error occurs.
	 */
	public static InputStream getURLInputStream(final URL url,
			final int connectTimeout, final int readTimeout)
			throws IOException {
		InputStream in = null;

		// initialize connection
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("Accept-Encoding", "gzip");
		conn.setConnectTimeout(connectTimeout);
		conn.setReadTimeout(readTimeout);

		// connect
		conn.connect();

		// get response
		in = conn.getInputStream();
		String contentEncoding = conn.getContentEncoding();
		if (contentEncoding != null && contentEncoding.equals("gzip")) {
			in = new GZIPInputStream(in);
		}

		return in;
	}

	/**
	 * Turn an object into an OutputStream if possible.
	 *
	 * @param obj
	 *            an OutputStream or File.
	 * @param append
	 *            if obj is a file and this parameter is true, the output stream
	 *            will be opened in append mode.
	 * @return an OutputStream. If obj is a File, the stream is Buffered.
	 * @throws IOException
	 *             if an error occurs
	 */
	public static OutputStream getOutputStream(final Object obj,
			final boolean append) throws IOException {
		OutputStream stream = null;

		if (obj instanceof OutputStream) {
			stream = (OutputStream) obj;
		} else if (obj instanceof File) {
			File file = (File) obj;
			// create parent directory first, if needed
			File parent = file.getAbsoluteFile().getParentFile();
			if (!parent.exists() && !parent.mkdirs()) {
				throw new IOException("Unable to create directory "
						+ parent.getAbsolutePath());
			}
			if (!parent.canWrite()) {
				throw new IOException(
						"Do not have write permission for directory "
								+ parent.getAbsolutePath());
			}

			stream = new BufferedOutputStream(
					new FileOutputStream(file, append));
		} else {
			throw new IllegalArgumentException(
					"Expected an OutputStream or File");
		}

		return stream;
	}

	/**
	 * Same as calling getOutputStream(obj, false). If obj is a file, it will
	 * open a new output stream and will not append.
	 *
	 * @param obj
	 *            an OutputStream or File.
	 * @return an OutputStream. If obj is a File, the stream is Buffered.
	 * @throws IOException
	 *             if an error occurs.
	 */
	public static OutputStream getOutputStream(final Object obj)
			throws IOException {
		return getOutputStream(obj, false);
	}

	/**
	 * Read stream contents into a byte array.
	 *
	 * @param from
	 *            stream to read.
	 * @return byte array of file content.
	 * @throws IOException
	 *             if an error occurs while reading.
	 */
	public static byte[] readStream(final Object from) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		transferStream(from, buffer);
		return buffer.toByteArray();
	}

	/**
	 * Transfer contents of Object from to Object to.
	 *
	 * Calls transferStream(from, to, DEFAULT_BUFFER_SIZE).
	 *
	 * @param from
	 *            streamable source.
	 * @param to
	 *            streamable target.
	 * @throws IOException
	 */
	public static void transferStream(final Object from, final Object to)
			throws IOException {
		transferStream(from, to, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Transfer contents of Object from to Object to. Uses getInputStream and
	 * getOutputStream to generate streams. Streams are closed after reading.
	 *
	 * @param from
	 *            streamable source.
	 * @param to
	 *            streamable target.
	 * @throws IOException
	 *             if thrown by calls to read/write on streams.
	 */
	public static void transferStream(final Object from, final Object to,
			final int bufferSize) throws IOException {
		InputStream in = null;
		OutputStream out = null;

		try {
			in = getInputStream(from);
			out = getOutputStream(to);
			byte[] buffer = new byte[bufferSize];
			int read;
			while ((read = in.read(buffer, 0, bufferSize)) != -1) {
				out.write(buffer, 0, read);
			}
			closeStream(in);
			closeStream(out);
		} catch (Exception e) {
			closeStream(in);
			closeStream(out);
			if (e instanceof IOException) {
				throw (IOException) e;
			}
		}
	}

	/**
	 * Close an InputStream or OutputStream.
	 *
	 * @param stream
	 *            stream to close.
	 */
	public static void closeStream(final Object stream) {
		try {
			if (stream instanceof OutputStream) {
				OutputStream out = (OutputStream) stream;
				try {
					out.flush();
				} finally {
					out.close();
				}
			} else if (stream instanceof InputStream) {
				((InputStream) stream).close();
			}
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * An InputStream that ignores calls to close.
	 *
	 * Used for methods that automatically close a stream at EOF, even though it
	 * is undesirable for the stream to be closed, such as when using a
	 * ZipInputStream.
	 *
	 * @author jmfee
	 *
	 */
	public static class UnclosableInputStream extends FilterInputStream {

		/**
		 * Create a new UnclosableInputStream object.
		 *
		 * @param in
		 *            the InputStream to wrap.
		 */
		public UnclosableInputStream(final InputStream in) {
			super(in);
		}

		/**
		 * Does not close stream.
		 */
		public void close() {
			// ignore
		}
	}

	/**
	 * An OutputStream that ignores calls to close.
	 *
	 * Used for methods that automatically close a stream, even though it is
	 * undesirable for the stream to be closed, such as when using a
	 * ZipOutputStream.
	 *
	 * @author jmfee
	 *
	 */
	public static class UnclosableOutputStream extends FilterOutputStream {

		/**
		 * Create a new UnclosableOutputStream object.
		 *
		 * @param out
		 *            the OutputStream to wrap.
		 */
		public UnclosableOutputStream(final OutputStream out) {
			super(out);
		}

		/**
		 * Flush written content, but does not close stream.
		 */
		public void close() throws IOException {
			out.flush();
			// otherwise ignore
		}
	}

}
