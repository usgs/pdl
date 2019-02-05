package gov.usgs.earthquake.distribution;

import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.SocketAcceptor;
import gov.usgs.util.SocketListenerInterface;
import gov.usgs.util.StreamUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Telnet to this socket to get a "command prompt".
 * 
 * @author jmfee
 */
public class AdminSocketServer extends DefaultConfigurable implements
		SocketListenerInterface {

	private static final Logger LOGGER = Logger
			.getLogger(AdminSocketServer.class.getName());

	private static final int DEFAULT_THREAD_POOL_SIZE = 10;
	private static final int DEFAULT_ADMIN_PORT = 11111;

	private int port = -1;
	private int threads = -1;
	private SocketAcceptor acceptor = null;

	/** the client this server is providing stats for. */
	private ProductClient client = null;

	public AdminSocketServer() {
		this(DEFAULT_ADMIN_PORT, DEFAULT_THREAD_POOL_SIZE, null);
	}

	public AdminSocketServer(final int port, final int threads,
			final ProductClient client) {
		this.port = port;
		this.threads = threads;
		this.client = client;
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
		try {
			acceptor.stop();
		} finally {
			// shutdown no matter what
			// call DefaultNotificationReceiver shutdown last
			super.shutdown();
		}
	}

	/**
	 * Process a line of input.
	 * 
	 * @param line
	 *            input
	 * @param out
	 *            write generated output to stream
	 * @throws Exception if misconfigured or the client quits.
	 */
	protected void processLine(final String line, final OutputStream out)
			throws Exception {
		if (client == null) {
			throw new Exception("No product client configured");
		}

		String s = line.trim();
		if (s.equals("status")) {
			out.write(getStatus().getBytes());
		} else if (s.startsWith("reprocess")) {
			out.write(("Reprocess not yet supported").getBytes());
			// reprocess(out, s.replace("reprocess", "").split(" "));
		} else if (s.startsWith("search")) {
			out.write(("Search not yet supported").getBytes());
			// search(out, s.replace("search", "").split(" "));
		} else if (s.equals("quit")) {
			throw new Exception("Bye");
		} else {
			out.write(("Help:\n" + "status - show server status\n"
					+ "SOON search [source=SOURCE] [type=TYPE] [code=CODE]\n"
					+ "SOON reprocess listener=LISTENER id=PRODUCTID")
					.getBytes());
		}
	}

	private String getStatus() {
		StringBuffer buf = new StringBuffer();
		// receiver queue status
		Iterator<NotificationReceiver> iter = client.getReceivers().iterator();
		while (iter.hasNext()) {
			NotificationReceiver receiver = iter.next();
			if (receiver instanceof DefaultNotificationReceiver) {
				Map<String, Integer> status = ((DefaultNotificationReceiver) receiver)
						.getQueueStatus();
				if (status != null) {
					Iterator<String> queues = status.keySet().iterator();
					while (queues.hasNext()) {
						String queue = queues.next();
						buf.append(queue).append(" = ")
								.append(status.get(queue)).append("\n");
					}
				}
			}
		}

		String status = buf.toString();
		if (status.equals("")) {
			status = "No queues to show";
		}
		return status;
	}

	public void onSocket(Socket socket) {
		LOGGER.info("[" + getName() + "] accepted connection "
				+ socket.toString());

		InputStream in = null;
		OutputStream out = null;

		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();

			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = br.readLine()) != null) {
				processLine(line, out);
			}
		} catch (Exception ex) {
			LOGGER.log(Level.WARNING, "[" + getName()
					+ "] exception while processing socket", ex);
			// tell sender "exception"
			try {
				out.write(("Error receiving product '" + ex.getMessage() + "'")
						.getBytes());
			} catch (Exception ex2) {
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] unable to notify sender of exception", ex2);
			}
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

		LOGGER.info("[" + getName() + "] closed connection "
				+ socket.toString());
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

	public ProductClient getClient() {
		return client;
	}

	public void setClient(ProductClient client) {
		this.client = client;
	}

}
