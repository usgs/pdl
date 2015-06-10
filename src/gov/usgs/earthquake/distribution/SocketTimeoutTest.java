package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.util.TimeoutOutputStream;
import gov.usgs.util.StreamUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Verify read timeouts work as expected.
 */
public class SocketTimeoutTest {

	/**
	 * Test SO_TIMEOUT with a remote socket that replies before the timeout.
	 *
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testNoTimeout() throws UnknownHostException, IOException,
			InterruptedException {
		SlowResponder server = new SlowResponder(1234, 100);
		server.start();
		// wait for server to start
		Thread.sleep(50L);

		Socket socket = new Socket("127.0.0.1", 1234);
		try {
			System.err.println(new String(StreamUtils.readStream(socket
					.getInputStream())));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Didn't expect exception");
		} finally {
			socket.close();
		}

		server.join();
	}

	/**
	 * Test SO_TIMEOUT with a remote socket that replies after the timeout.
	 *
	 * @throws InterruptedException
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	@Test
	public void testTimeout() throws InterruptedException,
			UnknownHostException, IOException {
		SlowResponder server = new SlowResponder(1234, 100);
		server.start();
		// wait for server to start
		Thread.sleep(50L);

		Socket socket = new Socket("127.0.0.1", 1234);
		try {
			socket.setSoTimeout(50);
			System.err.println(new String(StreamUtils.readStream(socket
					.getInputStream())));
			Assert.fail("Expected timeout");
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			System.err.println("timeout worked");
		} finally {
			socket.close();
		}

		server.join();
	}

	/**
	 * Test SO_TIMEOUT with a remote socket that closes before sending output.
	 *
	 * @throws InterruptedException
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	@Test
	public void testShutdownWithTimeout() throws InterruptedException,
			UnknownHostException, IOException {
		ShutdownServer server = new ShutdownServer(1234, 100);
		server.start();
		// wait for server to start
		Thread.sleep(50L);

		Socket socket = new Socket("127.0.0.1", 1234);
		try {
			// set read timeout to be after server shuts down
			socket.setSoTimeout(200);
			System.err.println(new String(StreamUtils.readStream(socket
					.getInputStream())));
			Assert.fail("Expected timeout");
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			System.err.println("timeout worked");
		} finally {
			socket.close();
		}

		server.join();
	}

	@Test
	public void testSlowResponderWithTimeoutOutputStream() throws Exception {
		SlowResponder server = new SlowResponder(1234, 5000);
		server.start();
		// wait for server to start
		Thread.sleep(50L);
		byte[] tosend = ("abcdefghijklmnopqrstuvwxyz"
				+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789").getBytes();
		Socket socket = new Socket("127.0.0.1", 1234);
		socket.setSendBufferSize(100);
		long sent = 0;
		TimeoutOutputStream out = null;
		try {
			out = new TimeoutOutputStream(socket.getOutputStream(), 50);
			while (sent < 8192000) {
				// send data to server
				out.write(tosend);
				sent += tosend.length;
			}
			Assert.fail("Expected output stream to throw exception");
		} catch (Exception e) {
			System.err.println("sent " + sent + " bytes before write timeout");
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
			}
		}
		server.join();
	}

	/**
	 * Class that accepts connections, and after a delay sends output to
	 * connected socket.
	 */
	private static class SlowResponder extends Thread {
		private int port;
		private long delay;

		public SlowResponder(final int port, final long delay) {
			this.port = port;
			this.delay = delay;
		}

		@Override
		public void run() {
			ServerSocket server = null;
			Socket socket = null;
			try {
				server = new ServerSocket(this.port);
				socket = server.accept();
				System.err.println("Accepted connection from " + socket);

				// accepted, now wait
				Thread.sleep(delay);
				// and then reply
				socket.getOutputStream().write("It worked!".getBytes());
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					server.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Class that accepts connections, and after a delay shuts down without
	 * sending output to client.
	 */
	private static class ShutdownServer extends Thread {
		private int port;
		private long delay;

		public ShutdownServer(final int port, final long delay) {
			this.port = port;
			this.delay = delay;
		}

		@Override
		public void run() {
			ServerSocket server = null;
			try {
				server = new ServerSocket(this.port);
				Socket socket = server.accept();
				System.err.println("Accepted connection from " + socket);

				// accepted, now wait
				Thread.sleep(delay);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					// and then shutdown
					server.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
