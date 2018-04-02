/*
 * SocketProductReceiver
 */
package gov.usgs.earthquake.distribution;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.util.Config;
import gov.usgs.util.SocketAcceptor;
import gov.usgs.util.SocketListenerInterface;

/**
 * Receive Products directly via a Socket.
 * 
 * The received products are sent using a SocketProductSender.
 * 
 * A SocketProductReceiver receives products directly and notifies listeners of
 * received notifications.
 * 
 * These are typically used on hubs with an EIDSNotificationSender or
 * RelayProductReceiver.
 * 
 * The NotificationReceiver uses a NotificationIndex to track received
 * notifications, and a ProductStorage to store retrieved products.
 * 
 * The DefaultNotificationReceiver implements the Configurable interface and
 * uses the following configuration parameters:
 * 
 * Each listener has a separate queue of notifications. Each listener is
 * allocated one thread to process notifications from this queue.
 */
public class SocketProductReceiver extends DefaultNotificationReceiver
		implements SocketListenerInterface {

	private static final String THREAD_POOL_SIZE_PROPERTY = "threads";

	private static final String DEFAULT_THREAD_POOL_SIZE = "10";

	private static final String PRODUCT_PORT_PROPERTY = "port";

	private static final String DEFAULT_PRODUCT_PORT = "11235";
	
	private static final String SIZE_LIMIT_PROPERTY = "sizeLimit";
	
	private static final String DEFAULT_SIZE_LIMIT = "-1";


	private static final Logger LOGGER = Logger
			.getLogger(SocketProductReceiver.class.getName());

	private int port = -1;
	private int threads = -1;
	private long sizeLimit = -1;

	private SocketAcceptor acceptor = null;

	public SocketProductReceiver() throws Exception {
		super();
		this.port = Integer.parseInt(DEFAULT_PRODUCT_PORT);
		this.threads = Integer.parseInt(DEFAULT_THREAD_POOL_SIZE);
		this.sizeLimit = Long.parseLong(DEFAULT_SIZE_LIMIT);
	}

	public SocketProductReceiver(Config config) throws Exception {
		this();
		configure(config);
	}

	public void configure(Config config) throws Exception {
		super.configure(config);
		this.port = Integer.parseInt(config.getProperty(PRODUCT_PORT_PROPERTY,
				DEFAULT_PRODUCT_PORT));
		LOGGER.config("[" + getName() + "]  port is '" + this.port + "'");

		this.threads = Integer.parseInt(config.getProperty(
				THREAD_POOL_SIZE_PROPERTY, DEFAULT_THREAD_POOL_SIZE));
		LOGGER.config("[" + getName() + "]  number of threads is '"
				+ this.threads + "'");

		this.sizeLimit = Long.parseLong(config.getProperty(
				SIZE_LIMIT_PROPERTY, DEFAULT_SIZE_LIMIT));
		LOGGER.config("[" + getName() + "] size limite is '"
				+ this.sizeLimit + "'");
	}

	public void startup() throws Exception {
		// call DefaultNotificationReceiver startup first
		super.startup();

		ServerSocket socket = new ServerSocket(port);
		socket.setReuseAddress(true);
		acceptor = new SocketAcceptor(socket, this,
				Executors.newFixedThreadPool(threads));
		// start accepting connections via socket
		acceptor.start();
	}

	public void shutdown() throws Exception {
		// stop accepting connections
		acceptor.stop();

		// call DefaultNotificationReceiver shutdown last
		super.shutdown();
	}

	public void onSocket(Socket socket) {
		LOGGER.info("[" + getName() + "] accepted connection "
				+ socket.toString());

		try {
			new SocketProductReceiverHandler(this, socket).run();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
					"[" + getName() + "] uncaught exception processing "
					+ socket.toString(), e);
		} finally {
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

		LOGGER.info("[" + getName() + "] closed connection "
				+ socket.toString());
	}

	protected String storeAndNotify(final ProductSource source)
			throws Exception {
		Notification notification = storeProductSource(source);
		if (notification != null) {
			// note in log file
			String message = "[" + getName() + "] received product '"
					+ notification.getProductId().toString() + "'\n";

			// product successfully stored, and notification index updated.
			new ProductTracker(notification.getTrackerURL()).productReceived(
					this.getName(), notification.getProductId());

			// notify listeners of newly available product.
			notifyListeners(notification);

			return message;
		} else {
			throw new Exception("[" + getName()
					+ "] unknown error, no notification generated");
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public long getSizeLimit() {
		return sizeLimit;
	}

	public void setSizeLimit(long sizeLimit) {
		this.sizeLimit = sizeLimit;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

}
