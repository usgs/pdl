package gov.usgs.earthquake.util;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class RoundRobinBlockingQueueTest {

	/**
	 * Testing class that round robins based on the first letter in a string.
	 */
	private static class FirstLetterRoundRobinBlockingQueue extends
			RoundRobinBlockingQueue<String> {

		@Override
		protected String getQueueId(final String object) {
			// first letter determines queue
			return object.substring(0, 1);
		}

	}

	private final FirstLetterRoundRobinBlockingQueue q = new FirstLetterRoundRobinBlockingQueue();

	private class Putter extends Thread {
		@Override
		public void run() {
			try {
				// PART 1
				// wait 100 ms before first put
				Thread.sleep(100);
				q.put("abc123");

				// PART 2
				Thread.sleep(100);
				// (now at ~200ms)
				q.put("def456");

				// PART 3
				Thread.sleep(100);
				// (now at ~300ms)
				q.put("ghi789");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class Taker extends Thread {
		@Override
		public void run() {
			try {
				// PART 1
				String polled = q.poll(75, TimeUnit.MILLISECONDS);
				Assert.assertNull("polling times out before put", polled);

				polled = q.poll(50, TimeUnit.MILLISECONDS);
				Assert.assertNotNull("after put, polling doesn't time out",
						polled);

				// PART 2 (start at ~125ms)
				String took = q.take();
				Assert.assertNotNull("take blocks until after put", took);

				// PART 3
				try {
					q.remove();
					Assert.fail("remove should throw NoSuchElementException when empty");
				} catch (NoSuchElementException nsee) {
				}
				polled = q.poll();
				Assert.assertNull("poll should return null when empty", polled);

				took = q.take();
				Assert.assertNotNull("third object taken", took);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void takeBlocksUntilPut() throws InterruptedException {
		Putter putter = new Putter();
		Taker taker = new Taker();

		// start taking before putting
		taker.start();
		putter.start();

		putter.join();
		taker.join();
	}

}
