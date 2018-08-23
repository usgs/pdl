package gov.usgs.earthquake.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that self-closes if the specified timeout elapses between
 * writes.
 */
public class TimeoutOutputStream extends FilterOutputStream implements Runnable {

	/** write timeout in milliseconds. */
	private final long timeout;
	/** thread that enforces timeout. */
	private final Thread timeoutThread;
	/** flag for timeoutThread to terminate. */
	private boolean closed = false;

	/**
	 * Create a TimeoutOutputStream.
	 * 
	 * @param out
	 *            the wrapped output stream.
	 * @param timeout
	 *            the timeout in milliseconds between writes. If this timeout
	 *            completes, the underlying stream will be closed.
	 */
	public TimeoutOutputStream(final OutputStream out, final long timeout) {
		super(out);
		this.timeout = timeout;
		this.timeoutThread = new Thread(this);
		this.timeoutThread.start();
	}

	@Override
	public void write(int b) throws IOException {
		// pass directly to underlying stream
		this.out.write(b);
		timeoutThread.interrupt();
	}

	@Override
	public void write(byte[] buf) throws IOException {
		// pass directly to underlying stream
		this.out.write(buf);
		timeoutThread.interrupt();
	}

	@Override
	public void write(byte[] buf, int offset, int length) throws IOException {
		// pass directly to underlying stream
		this.out.write(buf, offset, length);
		timeoutThread.interrupt();
	}

	@Override
	public void close() throws IOException {
		closed = true;
		try {
			super.close();
		} finally {
			// interrupt in case close called from outside timeoutThread
			timeoutThread.interrupt();
		}
	}

	@Override
	public void run() {
		while (!closed) {
			try {
				// wait for timeout milliseconds
				Thread.sleep(timeout);
				// timeout elapsed, close stream
				try {
					close();
				} catch (IOException e) {
				}
			} catch (InterruptedException ie) {
				// a write occured
			}
		}
	}

}
