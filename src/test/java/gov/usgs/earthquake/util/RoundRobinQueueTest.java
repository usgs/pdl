package gov.usgs.earthquake.util;

import org.junit.Assert;
import org.junit.Test;

public class RoundRobinQueueTest {

	/**
	 * Testing class that round robins based on the first letter in a string.
	 */
	private static class FirstLetterRoundRobinQueue extends
			RoundRobinQueue<String> {

		@Override
		protected String getQueueId(final String object) {
			// first letter determines queue
			return object.substring(0, 1);
		}

	}

	@Test
	public void testRoundRobin() {
		// two items for "s" queue
		String s1 = "string 1";
		String s2 = "string 2";
		// item for "a" queue
		String s3 = "a different string";

		FirstLetterRoundRobinQueue q = new FirstLetterRoundRobinQueue();
		q.add(s1); // "s"
		q.add(s2); // "s"
		q.add(s3); // "a"
		Assert.assertEquals("q has 3 elements", 3, q.size());

		Assert.assertEquals("s1 first", s1, q.remove());
		Assert.assertEquals("s3 second", s3, q.remove());
		Assert.assertEquals("s2 third", s2, q.remove());

		Assert.assertTrue(q.isEmpty());
	}

}
