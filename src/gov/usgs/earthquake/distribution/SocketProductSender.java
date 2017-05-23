/*
 * SocketProductSender
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.BinaryIO;
import gov.usgs.earthquake.product.io.BinaryProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.earthquake.util.TimeoutOutputStream;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Send Products to SocketProductReceivers.
 * 
 * The SocketProductSender implements the Configurable interface and uses the
 * following configuration parameters:
 * 
 * <dl>
 * <dt>host</dt>
 * <dd>(Required) The IP address or hostname of a SocketProductReceiver.</dd>
 * 
 * <dt>port</dt>
 * <dd>(Optional, default=11235) The port on host of a SocketProductReceiver</dd>
 * </dl>
 * 
 * @author jmfee
 * 
 */
public class SocketProductSender extends DefaultConfigurable implements
		ProductSender {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(SocketProductSender.class.getName());

	public static final String SENDER_HOST_PROPERTY = "host";
	public static final String SENDER_PORT_PROPERTY = "port";

	/** The default port number for SocketProductReceivers. */
	public static final String DEFAULT_SENDER_PORT = "11235";

	public static final String CONNECT_TIMEOUT_PROPERTY = "connectTimeout";
	public static final String DEFAULT_CONNECT_TIMEOUT = "15000";

	public static final String READ_TIMEOUT_PROPERTY = "readTimeout";
	public static final String DEFAULT_READ_TIMEOUT = "15000";

	public static final String WRITE_TIMEOUT_PROPERTY = "writeTimeout";
	public static final String DEFAULT_WRITE_TIMEOUT = "-1";

	/** Property name to configure binary or xml format. */
	public static final String BINARY_FORMAT_PROPERTY = "binaryFormat";
	/** Default value for whether to use binary format. */
	public static final String BINARY_FORMAT_DEFAULT = "false";

	/** Property name to configure deflate compression. */
	public static final String ENABLE_DEFLATE_PROPERTY = "enableDeflate";
	/** Default value for whether to use deflate compression. */
	public static final String ENABLE_DEFLATE_DEFAULT = "true";

	public static final String DEFLATE_LEVEL_PROPERTY = "deflateLevel";
	public static final String DEFLATE_LEVEL_DEFAULT = "1";

	public static final String ENABLE_PDL_PROTOCOL_PROPERTY = "enablePdlProtocol";
	public static final String DEFAULT_ENABLE_PDL_PROTOCOL = "true";

	public static final byte[] PROTOCOL_HEADER = { 'P', 'D', 'L' };
	public static final String PROTOCOL_VERSION_0_1 = "v0.1";
	public static final String UNKNOWN_PRODUCT = "Unknown product";
	public static final String ALREADY_HAVE_PRODUCT = "Already have product";
	public static final String RECEIVE_ERROR = "Error receiving product";

	/** Whether to store in binary format (true), or xml format (false). */
	private boolean binaryFormat = false;

	/** Whether to deflate product sent over the wire. */
	private boolean enableDeflate = true;

	/** Compression level when deflating products. */
	private int deflateLevel = 1;

	private boolean enablePdlProtocol = true;

	/** The remote hostname or ip address. */
	private String host = null;
	/** The remote port. */
	private int port = -1; // -1 is invalid. This better be overridden.
	/** How long to wait before connecting, in milliseconds. */
	private int connectTimeout = 15000;
	/** How long to block while reading, before timing out. */
	private int readTimeout = 15000;
	/** How long to block while writing, before timing out. */
	private int writeTimeout = -1;

	private Socket socket = null;

	/**
	 * Construct a new ProductSender.
	 * 
	 * @param host
	 * @param port
	 */
	public SocketProductSender(final String host, final int port) {
		this(host, port, Integer.parseInt(DEFAULT_CONNECT_TIMEOUT));
	}

	public SocketProductSender(final String host, final int port,
			final int connectTimeout) {
		this(host, port, connectTimeout,
				Integer.parseInt(DEFAULT_READ_TIMEOUT), Integer
						.parseInt(DEFAULT_WRITE_TIMEOUT));
	}

	public SocketProductSender(final String host, final int port,
			final int connectTimeout, final int readTimeout,
			final int writeTimeout) {
		this.host = host;
		this.port = port;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.writeTimeout = writeTimeout;
	}

	/** Empty constructor for configurable interface. */
	public SocketProductSender() {
	}

	/**
	 * Construct a new ProductSender using a Config object.
	 * 
	 * @param config
	 * @throws Exception
	 */
	public SocketProductSender(Config config) throws Exception {
		configure(config);
	}

	/**
	 * Implement the ProductSender interface.
	 * 
	 * Connects to host:port and sends a Deflaterped xml encoded Product. There
	 * is no direct response over the socket at this time.
	 * 
	 * Updates may be retrieved from a ProductTracker.
	 */
	public void sendProduct(Product product) throws Exception {
		BinaryIO io = new BinaryIO();
		boolean sendProduct = true;
		String status = null;
		ObjectProductSource productSource = null;
		InputStream in = null;
		OutputStream out = null;
		try {
			socket = new Socket();
			socket.setSoTimeout(readTimeout);
			socket.connect(new InetSocketAddress(host, port), connectTimeout);
			LOGGER.info("[" + getName() + "] sending product to "
					+ socket.toString());

			productSource = new ObjectProductSource(product);

			in = new BufferedInputStream(socket.getInputStream());
			out = new BufferedOutputStream(socket.getOutputStream());
			if (writeTimeout > 0) {
				out = new TimeoutOutputStream(out, writeTimeout);
			}

			if (enablePdlProtocol) {
				LOGGER.fine("[" + getName() + "] using protocol version "
						+ PROTOCOL_VERSION_0_1);

				// flag to receiver for "PDL" protocol
				out.write(PROTOCOL_HEADER);
				io.writeString(PROTOCOL_VERSION_0_1, out);
				io.writeString(product.getId().toString(), out);
				out.flush();

				status = io.readString(in);
				if (ALREADY_HAVE_PRODUCT.equals(status)) {
					sendProduct = false;
				} else if (UNKNOWN_PRODUCT.equals(status)) {
					// hub doesn't have this product, send
				} else {
					// unexpected reply, don't consider it success
					throw new Exception("Unexpected hub reply '" + status + "'");
				}
			} else {
				LOGGER.fine("[" + getName() + "] not using PDL protocol");
			}

			if (sendProduct) {
				if (enableDeflate) {
					out = new DeflaterOutputStream(out, new Deflater(
							deflateLevel));
				}

				// make sure product handler doesn't close stream before done
				OutputStream productOut = new StreamUtils.UnclosableOutputStream(
						out);
				if (binaryFormat) {
					productSource
							.streamTo(new BinaryProductHandler(productOut));
				} else {
					productSource.streamTo(new XmlProductHandler(productOut));
				}

				// deflate requires "finish"
				if (enableDeflate) {
					((DeflaterOutputStream) out).finish();
				}

				// flush buffered output stream to socket
				out.flush();
				// mark end of stream for server (for xml parser)
				socket.shutdownOutput();

				// finished sending, now get status from server
				if (enablePdlProtocol) {
					// the new way
					status = io.readString(in);
				} else {
					// the old way
					status = new BufferedReader(new InputStreamReader(
							socket.getInputStream())).readLine();
				}
			}

			LOGGER.info("[" + getName() + "] send complete "
					+ socket.toString() + " response=\"" + status + "\"");
		} catch (SocketTimeoutException ste) {
			throw new Exception("Error sending to " + host
					+ ", connect or read timeout", ste);
		} catch (UnknownHostException uhe) {
			throw new Exception("Unknown host " + host
					+ ", check that DNS is properly configured", uhe);
		} catch (SocketException se) {
			if (!enablePdlProtocol) {
				// check the old way
				try {
					// possible that hub already has product
					status = new BufferedReader(new InputStreamReader(
							socket.getInputStream())).readLine();
					if (status.equals("Product already received")) {
						// hub already has product
						LOGGER.info("[" + getName()
								+ "] hub already has product");
						return;
					}
				} catch (Exception e) {
					// ignore, already have an exception
					e.printStackTrace();
				}
			}
			throw new Exception("Error sending to " + host
					+ ", possible write timeout", se);
		} catch (Exception e) {
			throw new Exception("[" + getName() + "] error sending to " + host,
					e);
		} finally {
			try {
				out.close();
			} catch (Exception ignore) {
			}
			socket.close();
			socket = null;
		}

		if (status != null && status.startsWith("Error")) {
			throw new Exception("[" + getName() + "] error sending to " + host
					+ ", message=" + status);
		}
	}

	/**
	 * Reads the host and port from config.
	 * 
	 * @param config
	 *            a Config object with host and port properties.
	 */
	public void configure(Config config) throws Exception {
		host = config.getProperty(SENDER_HOST_PROPERTY);
		if (host == null) {
			throw new ConfigurationException("[" + getName()
					+ "] 'host' is a required configuration property");
		}
		LOGGER.config("[" + getName() + "] host is '" + host + "'");

		port = Integer.parseInt(config.getProperty(SENDER_PORT_PROPERTY,
				DEFAULT_SENDER_PORT).trim());
		LOGGER.config("[" + getName() + "] port is '" + port + "'");

		connectTimeout = Integer.parseInt(config.getProperty(
				CONNECT_TIMEOUT_PROPERTY, DEFAULT_CONNECT_TIMEOUT));
		LOGGER.config("[" + getName() + "] connectTimeout is '"
				+ connectTimeout + "'");

		readTimeout = Integer.parseInt(config.getProperty(
				READ_TIMEOUT_PROPERTY, DEFAULT_READ_TIMEOUT));
		LOGGER.config("[" + getName() + "] readTimeout is '" + readTimeout
				+ "'");

		writeTimeout = Integer.parseInt(config.getProperty(
				WRITE_TIMEOUT_PROPERTY, DEFAULT_WRITE_TIMEOUT));
		LOGGER.config("[" + getName() + "] writeTimeout is '" + writeTimeout
				+ "'");

		binaryFormat = Boolean.valueOf(config.getProperty(
				BINARY_FORMAT_PROPERTY, BINARY_FORMAT_DEFAULT));
		LOGGER.config("[" + getName() + "] using "
				+ (binaryFormat ? "binary" : "xml") + " format");

		enableDeflate = Boolean.valueOf(config.getProperty(
				ENABLE_DEFLATE_PROPERTY, ENABLE_DEFLATE_DEFAULT));
		LOGGER.config("[" + getName() + "] enableDeflate is " + enableDeflate);

		deflateLevel = Integer.valueOf(config.getProperty(
				DEFLATE_LEVEL_PROPERTY, DEFLATE_LEVEL_DEFAULT));
		LOGGER.config("[" + getName() + "] deflateLevel is " + deflateLevel);

		enablePdlProtocol = Boolean.valueOf(config.getProperty(
				ENABLE_PDL_PROTOCOL_PROPERTY, DEFAULT_ENABLE_PDL_PROTOCOL));
		LOGGER.config("[" + getName() + "] enablePdlProtocol is "
				+ enablePdlProtocol);
	}

	/**
	 * Makes sure the socket is closed.
	 */
	public void shutdown() throws Exception {
		if (socket != null) {
			if (!socket.isOutputShutdown()) {
				try {
					socket.getOutputStream().flush();
					socket.getOutputStream().close();
				} catch (IOException iox) { /* Ignore */
				}
			}
			if (!socket.isClosed()) {
				try {
					socket.close();
				} catch (IOException iox) { /* Ignore */
				}
			}
		}
	}

	/**
	 * Does nothing, a socket is opened each time a product is sent.
	 */
	public void startup() throws Exception {
		// Nothing to do for startup...
	}

	/**
	 * @return the binaryFormat
	 */
	public boolean isBinaryFormat() {
		return binaryFormat;
	}

	/**
	 * @param binaryFormat
	 *            the binaryFormat to set
	 */
	public void setBinaryFormat(boolean binaryFormat) {
		this.binaryFormat = binaryFormat;
	}

	/**
	 * @return the enableDeflate
	 */
	public boolean isEnableDeflate() {
		return enableDeflate;
	}

	/**
	 * @param enableDeflate
	 *            the enableDeflate to set
	 */
	public void setEnableDeflate(boolean enableDeflate) {
		this.enableDeflate = enableDeflate;
	}

	/**
	 * @return the deflateLevel
	 */
	public int getDeflateLevel() {
		return deflateLevel;
	}

	/**
	 * @param deflateLevel
	 *            the deflateLevel to set
	 */
	public void setDeflateLevel(int deflateLevel) {
		this.deflateLevel = deflateLevel;
	}

	/**
	 * @return the enablePdlProtocol
	 */
	public boolean isEnablePdlProtocol() {
		return enablePdlProtocol;
	}

	/**
	 * @param enablePdlProtocol
	 *            the enablePdlProtocol to set
	 */
	public void setEnablePdlProtocol(boolean enablePdlProtocol) {
		this.enablePdlProtocol = enablePdlProtocol;
	}

	/**
	 * @return the connectTimeout
	 */
	public int getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * @param connectTimeout
	 *            the connectTimeout to set
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * @return the readTimeout
	 */
	public int getReadTimeout() {
		return readTimeout;
	}

	/**
	 * @param readTimeout
	 *            the readTimeout to set
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * @return the writeTimeout
	 */
	public int getWriteTimeout() {
		return writeTimeout;
	}

	/**
	 * @param writeTimeout
	 *            the writeTimeout to set
	 */
	public void setWriteTimeout(int writeTimeout) {
		this.writeTimeout = writeTimeout;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

}
