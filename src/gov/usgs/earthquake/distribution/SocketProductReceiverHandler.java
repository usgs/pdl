package gov.usgs.earthquake.distribution;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.BinaryIO;
import gov.usgs.earthquake.product.io.IOUtil;
import gov.usgs.earthquake.util.SizeLimitInputStream;
import gov.usgs.util.ObjectLock;
import gov.usgs.util.StreamUtils;

public class SocketProductReceiverHandler implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(SocketProductReceiverHandler.class.getName());

	public static final int PDL_PROTOCOL_BUFFER = 1024;

	protected final BinaryIO io = new BinaryIO();
	protected final SocketProductReceiver receiver;
	protected final Socket socket;
	protected String protocolVersion;


	public SocketProductReceiverHandler(final SocketProductReceiver receiver, final Socket socket) {
		this.receiver = receiver;
		this.socket = socket;
	}
	
	public void acquireWriteLock(final ProductId id) {
		try {
			ObjectLock<ProductId> storageLocks = ((FileProductStorage) receiver.getProductStorage())
					.getStorageLocks();
			storageLocks.acquireWriteLock(id);
		} catch (Exception e) {
			// ignore
		}
	}

	public void releaseWriteLock(final ProductId id) {
		if (id == null) {
			return;
		}

		try {
			ObjectLock<ProductId> storageLocks = ((FileProductStorage) receiver.getProductStorage())
					.getStorageLocks();
			storageLocks.releaseWriteLock(id);
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * Read PDL protocol version.
	 *
	 * @param in input stream to read
	 * @return version, or null if not the PDL protocol.
	 *
	 * @throws IOException
	 */
	public String readProtocolVersion(final InputStream in) throws IOException {
		String version = null;
		if (in.read() == 'P' && in.read() == 'D' && in.read() == 'L') {
			try {
				version = io.readString(in, PDL_PROTOCOL_BUFFER);
			} catch (IOException e) {
				if (e.getMessage().contains("maxLength")) {
					throw new IOException("bad protocol version");
				} else {
					throw e;
				}
			}
		}
		return version;
	}

	/**
	 * Read ProductId protocol version.
	 *
	 * @param in input stream to read
	 * @return version, or null if not the PDL protocol.
	 *
	 * @throws IOException
	 */
	public String readProductId(final InputStream in) throws IOException {
		String version = null;
		if (in.read() == 'P' && in.read() == 'D' && in.read() == 'L') {
			try {
				version = io.readString(in, PDL_PROTOCOL_BUFFER);
			} catch (IOException e) {
				if (e.getMessage().contains("maxLength")) {
					throw new IOException("bad protocol version");
				} else {
					throw e;
				}
			}
		}
		return version;
	}

	@Override
	public void run() {
		BufferedInputStream in = null;
		InputStream productIn = null;
		OutputStream out = null;
		ProductId productId = null;

		try {
			socket.setSoTimeout(receiver.getReadTimeout());
			in = new BufferedInputStream(socket.getInputStream());
			out = socket.getOutputStream();

			in.mark(PDL_PROTOCOL_BUFFER);

			protocolVersion = readProtocolVersion(in);
			if (protocolVersion == null) {
				LOGGER.fine("[" + receiver.getName() + "] not using PDL protocol "
						+ socket);
				in.reset();
			} else {
				LOGGER.fine("[" + receiver.getName() + "] protocol version '"
						+ protocolVersion + "' " + socket);

				// got a version, see if it's supported
				if (SocketProductSender.PROTOCOL_VERSION_0_1.equals(protocolVersion)) {
					// product id is only message
					String productIdString;
					try {
						productIdString = io.readString(in, 1024);
					} catch (IOException e) {
						if (e.getMessage().contains("maxLength")) {
							throw new IOException("version too long");
						} else {
							throw e;
						}
					}
					productId = ProductId.parse(productIdString);

					acquireWriteLock(productId);
					if (receiver.getProductStorage().hasProduct(productId)) {
						// have product, don't send
						sendString(out, SocketProductSender.ALREADY_HAVE_PRODUCT);
						return;
					} else {
						// don't have product
						sendString(out, SocketProductSender.UNKNOWN_PRODUCT);
						out.flush();
					}
				} else {
					throw new IOException("unsupported protocol version");
				}
			}

			// read product
			productIn = in;
			if (receiver.getSizeLimit() > 0) {
				productIn = new SizeLimitInputStream(in, receiver.getSizeLimit());
			}
			String status = receiver.storeAndNotify(IOUtil
					.autoDetectProductSource(new StreamUtils.UnclosableInputStream(
							productIn)));
			LOGGER.info(status + " from " + socket.toString());

			try {
				// tell sender "success"
				if (protocolVersion == null) {
					out.write(status.getBytes());
				} else {
					io.writeString(status, out);
				}
				out.flush();
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "[" + receiver.getName()
						+ "] unable to notify sender of success", ex);
			}
		} catch (Exception ex) {
			sendException(out, ex);
		} finally {
			releaseWriteLock(productId);

			StreamUtils.closeStream(in);
			StreamUtils.closeStream(out);
		}
	}

	/**
	 * Send an exception to the user.
	 *
	 * @param out output stream where exception message is written
	 * @param e exception to write
	 */
	public void sendException(final OutputStream out, final Exception e) {
		try {
			if (e instanceof ProductAlreadyInStorageException
					|| e.getCause() instanceof ProductAlreadyInStorageException) {
				LOGGER.info("[" + receiver.getName() + "] product from "
						+ socket.toString() + " already in storage");
				sendString(out, SocketProductSender.ALREADY_HAVE_PRODUCT);
			} else {
				// tell sender "exception"
				LOGGER.log(Level.WARNING, "[" + receiver.getName()
						+ "] exception while processing socket", e);
				sendString(out, SocketProductSender.RECEIVE_ERROR +
						" '" + e.getMessage() + "'");
			}
		} catch (Exception e2) {
			//ignore
		}
	}

	/**
	 * Send a string to the user.
	 *
	 * @param out output stream where exception message is written
	 * @param str string to write
	 */
	public void sendString(final OutputStream out, final String str) throws IOException {
		try {
			if (protocolVersion == null) {
				out.write(str.getBytes());
			} else {
				io.writeString(str, out);
			}
			out.flush();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "[" + receiver.getName() +
					"] unable to send message '" + str + "' to " +
					socket.toString(), e);
			throw e;
		}
	}

}
