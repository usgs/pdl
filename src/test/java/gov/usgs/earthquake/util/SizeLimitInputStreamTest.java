package gov.usgs.earthquake.util;

import gov.usgs.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class SizeLimitInputStreamTest {

	private static byte[] TEST_BYTES = ("abcdefghijklmnopqrstuvwxyz"
			+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789").getBytes();

	@Test
	public void testExceedLimit() throws IOException {
		SizeLimitInputStream limited = new SizeLimitInputStream(
				new ByteArrayInputStream(TEST_BYTES), TEST_BYTES.length - 2);
		try {
			StreamUtils.readStream(limited);
			Assert.fail("expected exception");
		} catch (IOException ioe) {
			if (!ioe.getMessage().contains("more than size limit")) {
				throw ioe;
			}
		}
	}

	@Test
	public void testAtLimit() throws IOException {
		SizeLimitInputStream atLimit = new SizeLimitInputStream(
				new ByteArrayInputStream(TEST_BYTES), TEST_BYTES.length);
		StreamUtils.readStream(atLimit);
	}
}
