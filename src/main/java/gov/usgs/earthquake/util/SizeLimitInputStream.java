package gov.usgs.earthquake.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Stream that only allows a certain number of bytes to be read.
 * 
 * Current implementation only tracks read bytes, and ignores any mark or reset
 * calls.
 */
public class SizeLimitInputStream extends FilterInputStream {

	/** Maximum number of bytes to read. */
	private long limit;
	/** Number of bytes already read. */
	private long read = 0L;

	/**
	 * Construct a new SizeLimitInputStream.
	 * 
	 * @param in
	 *            stream to limit.
	 * @param limit
	 *            maximum number of bytes allowed to read.
	 */
	public SizeLimitInputStream(InputStream in, final long limit) {
		super(in);
		this.limit = limit;
	}

	/**
	 * Read one byte.
	 */
	@Override
	public int read() throws IOException {
		int b = in.read();
		if (b != -1) {
			read++;
			checkLimit();
		}
		return b;
	}

	/**
	 * Read into an array of bytes.
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * Read into an array of bytes.
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int total = in.read(b, off, len);
		read += total;
		checkLimit();
		return total;
	}

	/**
	 * Check how many bytes have been read.
	 * 
	 * @throws IOException
	 *             if more bytes than the limit allows have been read.
	 */
	protected void checkLimit() throws IOException {
		if (read > limit) {
			throw new IOException("Read more than size limit (" + limit
					+ ") bytes");
		}
	}

}
