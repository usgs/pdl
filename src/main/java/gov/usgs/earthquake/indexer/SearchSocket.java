package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Client side of search socket interface.
 */
public class SearchSocket {

	/** The remote host to connect. */
	private final InetAddress host;

	/** The remote port to connect. */
	private final int port;

	/**
	 * Construct a new SearchSocket.
	 * 
	 * @param host
	 *            the remote host.
	 * @param port
	 *            the remote port.
	 */
	public SearchSocket(final InetAddress host, final int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * Send a search request, converting the response to a java object.
	 * 
	 * @param request
	 *            the request to send.
	 * @param storage
	 *            where received products are stored.
	 * @return the response.
	 * @throws Exception
	 */
	public SearchResponse search(final SearchRequest request, final FileProductStorage storage) throws Exception {
		final PipedInputStream pipedIn = new PipedInputStream();
		final PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);

		// parse response in background, while searching
		ResponseParserThread thread = new ResponseParserThread(pipedIn, storage);
		thread.start();

		// start search, sending response xml to response parser thread
		search(request, pipedOut);

		// wait for parsing to complete
		thread.join();

		// either return parsed object, or raise parse exception
		if (thread.getSearchResponse() != null) {
			return thread.getSearchResponse();
		} else {
			throw thread.getParseError();
		}
	}

	/**
	 * Send a search request, writing the response to an outputstream.
	 * 
	 * @param request
	 *            the request to send.
	 * @param responseOut
	 *            the outputstream to write.
	 * @throws Exception
	 */
	public void search(final SearchRequest request,
			final OutputStream responseOut) throws Exception {
		Socket socket = null;
		DeflaterOutputStream out = null;
		InputStream in = null;

		try {
			// connect to the configured endpoint
			socket = new Socket(host, port);

			// send the request as compressed xml
			out = new DeflaterOutputStream(new BufferedOutputStream(
					socket.getOutputStream()));
			SearchXML.toXML(request,
					new StreamUtils.UnclosableOutputStream(out));

			// must finish and flush to complete Deflater stream
			out.finish();
			out.flush();

			// now read response
			in = new InflaterInputStream(new BufferedInputStream(
					socket.getInputStream()));
			StreamUtils.transferStream(in, responseOut);
		} finally {
			// make sure socket is closed
			try {
				socket.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * Thread used for parsing search response in background.
	 */
	private static class ResponseParserThread extends Thread {

		/** Input stream being parsed. */
		private InputStream in = null;

		/** Storage where received products are stored. */
		private FileProductStorage storage = null;

		/** The parsed search response. */
		private SearchResponse searchResponse = null;

		/** Parse error, if one happened. */
		private Exception parseError = null;

		public ResponseParserThread(final InputStream in, final FileProductStorage storage) {
			this.in = in;
			this.storage = storage;
		}

		public void run() {
			try {
				searchResponse = SearchXML.parseResponse(in, storage);
			} catch (Exception e) {
				searchResponse = null;
				parseError = e;
			}
		}

		public SearchResponse getSearchResponse() {
			return searchResponse;
		}

		public Exception getParseError() {
			return parseError;
		}
	}

}
