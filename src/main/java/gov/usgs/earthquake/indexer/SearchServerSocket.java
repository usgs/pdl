package gov.usgs.earthquake.indexer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.SocketAcceptor;
import gov.usgs.util.SocketListenerInterface;
import gov.usgs.util.StreamUtils;

/**
 * Server side of socket search interface.
 */
public class SearchServerSocket extends DefaultConfigurable implements
		SocketListenerInterface {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(SearchServerSocket.class.getName());

	/** The configuration property used for listen port. */
	public static final String SEARCH_PORT_PROPERTY = "port";

	/** The default listen port, as a string. */
	public static final String DEFAULT_SEARCH_PORT = "11236";

	/** The configuration property used for listen thread count. */
	public static final String THREAD_POOL_SIZE_PROPERTY = "threads";

	/** The default number of threads, as a string. */
	public static final String DEFAULT_THREAD_POOL_SIZE = "10";

	/** The configuration property used to reference a ProductIndex. */
	public static final String PRODUCT_INDEXER_PROPERTY = "indexer";

	/** The configuration property used to reference a URLProductStorage. */
	public static final String PRODUCT_STORAGE_PROPERTY = "storage";

	/** The port to bind. */
	private int port = -1;

	/** The number of threads to use. */
	private int threads = -1;

	/** The server socket accept thread. */
	private SocketAcceptor acceptor;

	/** The indexer that will be searched. */
	private Indexer indexer;

	/**
	 * Construct a new SearchServerSocket using defaults.
	 */
	public SearchServerSocket() {
		this.port = Integer.parseInt(DEFAULT_SEARCH_PORT);
		this.threads = Integer.parseInt(DEFAULT_THREAD_POOL_SIZE);
	}

	/**
	 * Method to perform search.
	 * 
	 * Calls Indexer.search(SearchRequest). Simplifies testing.
	 * 
	 * @param request
	 *            the search to execute.
	 * @return the search response.
	 * @throws Exception
	 */
	protected SearchResponse search(final SearchRequest request)
			throws Exception {
		return indexer.search(request);
	}

	/**
	 * This method is called each time a SearchSocket connects.
	 */
	@Override
	public void onSocket(Socket socket) {
		LOGGER.info("[" + getName() + "] accepted search connection "
				+ socket.toString());

		InputStream in = null;
		DeflaterOutputStream out = null;

		try {
			in = socket.getInputStream();
			in = new InflaterInputStream(new BufferedInputStream(
					new StreamUtils.UnclosableInputStream(in)));
			// read request
			SearchRequest request = SearchXML
					.parseRequest(new StreamUtils.UnclosableInputStream(in));

			// do search
			SearchResponse response = this.search(request);

			// send response
			out = new DeflaterOutputStream(new BufferedOutputStream(
					socket.getOutputStream()));
			SearchXML.toXML(response, new StreamUtils.UnclosableOutputStream(
					out));

			// finish compression
			out.finish();
			out.flush();
		} catch (Exception ex) {
			LOGGER.log(Level.WARNING, "[" + getName()
					+ "] exception while processing search", ex);
		} finally {
			StreamUtils.closeStream(in);
			StreamUtils.closeStream(out);

			try {
				socket.shutdownInput();
			} catch (Exception e) {
				// ignore
			}
			try {
				socket.shutdownOutput();
			} catch (Exception e) {
				// ignore
			}

			try {
				socket.close();
			} catch (Exception e) {
				// ignore
			}
		}

		LOGGER.info("[" + getName() + "] closed search connection "
				+ socket.toString());
	}

	@Override
	public void configure(Config config) throws Exception {
		port = Integer.parseInt(config.getProperty(SEARCH_PORT_PROPERTY,
				DEFAULT_SEARCH_PORT));
		LOGGER.config("[" + getName() + "] search port is " + port);

		threads = Integer.parseInt(config.getProperty(
				THREAD_POOL_SIZE_PROPERTY, DEFAULT_THREAD_POOL_SIZE));
		LOGGER.config("[" + getName() + "] number of threads is " + threads);

		String indexerName = config.getProperty(PRODUCT_INDEXER_PROPERTY);
		if (indexerName == null) {
			throw new ConfigurationException("[" + getName() + "] '"
					+ PRODUCT_INDEXER_PROPERTY
					+ "' is a required configuration property");
		}
		LOGGER.config("[" + getName() + "] loading indexer '" + indexerName
				+ "'");
		indexer = (Indexer) Config.getConfig().getObject(indexerName);
		if (indexer == null) {
			throw new ConfigurationException("[" + getName() + "] indexer '"
					+ indexerName + "' is not configured properly");
		}
	}

	@Override
	public void shutdown() throws Exception {
		// stop accepting connections
		acceptor.stop();
		acceptor = null;
	}

	@Override
	public void startup() throws Exception {
		ServerSocket socket = new ServerSocket(port);
		socket.setReuseAddress(true);
		acceptor = new SocketAcceptor(socket, this,
				Executors.newFixedThreadPool(threads));
		// start accepting connections via socket
		acceptor.start();
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public Indexer getIndexer() {
		return indexer;
	}

	public void setIndex(Indexer indexer) {
		this.indexer = indexer;
	}

}
