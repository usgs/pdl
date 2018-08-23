package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.InputStreamContent;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.StreamUtils;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;

/**
 * Parser for binary format for product data.
 */
public class BinaryProductSource implements ProductSource {

	/** product being parsed. */
	private ProductId id;
	/** stream being parsed. */
	private InputStream in;
	/** binary io utility. */
	private BinaryIO io;

	public BinaryProductSource(final InputStream in) {
		this.in = in;
		this.io = new BinaryIO();
	}

	@Override
	public void streamTo(ProductHandler out) throws Exception {
		try {
			while (true) {
				String next = io.readString(in);

				if (next.equals(BinaryProductHandler.HEADER)) {
					// begin product
					id = ProductId.parse(io.readString(in));
					String status = io.readString(in);
					// allow tracker url to be null
					URL trackerURL = null;
					String trackerURLString = io.readString(in);
					if (!trackerURLString.equalsIgnoreCase("null")) {
						trackerURL = new URL(trackerURLString);
					}
					out.onBeginProduct(id, status, trackerURL);
				} else if (next.equals(BinaryProductHandler.PROPERTY)) {
					String name = io.readString(in);
					String value = io.readString(in);
					out.onProperty(id, name, value);
				} else if (next.equals(BinaryProductHandler.LINK)) {
					String relation = io.readString(in);
					URI href = new URI(io.readString(in));
					out.onLink(id, relation, href);
				} else if (next.equals(BinaryProductHandler.CONTENT)) {
					String path = io.readString(in);
					String contentType = io.readString(in);
					Date lastModified = io.readDate(in);
					Long length = io.readLong(in);

					// use a piped output stream to deliver content to separate
					// processing thread. this thread will continue to read
					// InputStream, transfer content to pipedOutputStream.
					// Background thread calls onContent, and reads from
					// pipedInputStream.
					PipedOutputStream pipedOut = new PipedOutputStream();
					PipedInputStream pipedIn = new PipedInputStream(pipedOut);

					final InputStreamContent content = new InputStreamContent(
							pipedIn);
					content.setContentType(contentType);
					content.setLastModified(lastModified);
					content.setLength(length);

					// background thread delivers content object to product handler
					ContentOutputThread outputThread = new ContentOutputThread(out, id, path, content);

					try {
						outputThread.start();

						// read stream content
						io.readStream(length, in, pipedOut);
					} finally {
						// done reading content, close piped stream to signal EOF.
						StreamUtils.closeStream(pipedOut);
						pipedOut = null;
						try {
							// wait for background thread to complete
							outputThread.join();
						} catch (Exception e) {
							// ignore
						}
						outputThread = null;
						content.close();
					}

				} else if (next.equals(BinaryProductHandler.SIGNATURE)) {
					String signature = io.readString(in);
					out.onSignature(id, signature);
				} else if (next.equals(BinaryProductHandler.FOOTER)) {
					out.onEndProduct(id);
					id = null;

					// end of product stream
					break;
				}
			}
		} finally {
			StreamUtils.closeStream(in);
		}
	}


	/**
	 * Free any resources associated with this source.
	 */
	@Override
	public void close() {
		StreamUtils.closeStream(in);
		in = null;
	}

}
