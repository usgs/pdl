package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.FileContent;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.BinaryProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


public class DeflateComparison {

	/**
	 * Deflate an input stream.
	 * 
	 * @param level
	 *            deflate level.
	 * @param in
	 *            input stream to deflate.
	 * @return output length in bytes.
	 * @throws IOException
	 */
	public long deflateStream(final int level, final InputStream in)
			throws IOException {
		CountingOutputStream cos = new CountingOutputStream();
		DeflaterOutputStream dos = new DeflaterOutputStream(cos, new Deflater(
				level));
		StreamUtils.transferStream(in, new StreamUtils.UnclosableOutputStream(
				dos));
		dos.finish();
		dos.close();
		return cos.getTotalBytes();
	}

	/**
	 * Transfer an input stream.
	 * 
	 * @param in
	 *            input stream to transfer.
	 * @return output length in bytes.
	 * @throws IOException
	 */
	public long transferStream(final InputStream in) throws IOException {
		CountingOutputStream cos = new CountingOutputStream();
		StreamUtils.transferStream(in, cos);
		return cos.getTotalBytes();
	}

	/**
	 * Test different compression levels and speeds for a file.
	 * 
	 * Reads file into memory to avoid disk io overhead.
	 * 
	 * @param file
	 *            file to test.
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void testFile(final File file) throws IllegalArgumentException,
			IOException {
		// read into memory to avoid disk io overhead
		byte[] fileContent = StreamUtils.readStream(StreamUtils
				.getInputStream(file));
		testByteArray(file.getCanonicalPath(), fileContent);
	}

	/**
	 * Test different compression levels and speeds for a byte array.
	 * 
	 * @param content
	 *            content to test.
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void testByteArray(final String name, final byte[] content)
			throws IOException {
		Date start;
		long totalBytes = content.length;

		System.err.println(name + ", length = " + totalBytes + " bytes");

		System.err.println("no compression");
		start = new Date();
		long noCompression = transferStream(new ByteArrayInputStream(content));
		long noCompressionTime = new Date().getTime() - start.getTime();
		formatResult(totalBytes, noCompression, noCompressionTime);

		System.err.println("default compression (-1)");
		start = new Date();
		long deflateDefault = deflateStream(Deflater.DEFAULT_COMPRESSION,
				new ByteArrayInputStream(content));
		long deflateDefaultTime = new Date().getTime() - start.getTime();
		formatResult(totalBytes, deflateDefault, deflateDefaultTime);

		System.err.println("best speed (1)");
		start = new Date();
		long deflateBestSpeed = deflateStream(Deflater.BEST_SPEED,
				new ByteArrayInputStream(content));
		long deflateBestSpeedTime = new Date().getTime() - start.getTime();
		formatResult(totalBytes, deflateBestSpeed, deflateBestSpeedTime);

		System.err.println("best compression (9)");
		start = new Date();
		long deflateBestCompression = deflateStream(Deflater.BEST_COMPRESSION,
				new ByteArrayInputStream(content));
		long deflateBestCompressionTime = new Date().getTime()
				- start.getTime();
		formatResult(totalBytes, deflateBestCompression,
				deflateBestCompressionTime);
	}

	protected void formatResult(final long totalBytes,
			final long transferredBytes, final long elapsedTime) {
		long savedBytes = totalBytes - transferredBytes;
		System.err.printf("\t%.3fs, %.1f%% reduction (%d fewer bytes)%n",
				elapsedTime / 1000.0,
				100 * (savedBytes / ((double) totalBytes)), savedBytes);
	}

	/**
	 * An output stream that counts how many bytes are written, and otherwise
	 * ignores output.
	 */
	private static class CountingOutputStream extends OutputStream {
		// number of bytes output
		private long totalBytes = 0L;

		public long getTotalBytes() {
			return totalBytes;
		}

		@Override
		public void write(byte[] b) {
			totalBytes += b.length;
		}

		@Override
		public void write(byte[] b, int offset, int length) {
			totalBytes += length;
		}

		@Override
		public void write(int arg0) throws IOException {
			totalBytes++;
		}

	}

	/**
	 * A main method for accessing tests using custom files.
	 * 
	 * @param args
	 *            a list of files or directorys to include in compression
	 *            comparison.
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("Usage: DeflateComparison FILE [FILE ...]");
			System.err
					.println("where FILE is a file or directory to include in comparison");
			System.exit(1);
		}

		Product product = new Product(new ProductId("test", "test", "test"));
		try {
			product.setTrackerURL(new URL("http://localhost/tracker"));
		} catch (Exception e) {
			// ignore
		}

		// treat all arguments as files or directories to be added as content
		for (String arg : args) {
			File file = new File(arg);
			if (!file.exists()) {
				System.err.println(file.getCanonicalPath() + " does not exist");
				System.exit(1);
			}

			if (file.isDirectory()) {
				product.getContents().putAll(
						FileContent.getDirectoryContents(file));
			} else {
				product.getContents()
						.put(file.getName(), new FileContent(file));
			}
		}

		// convert product to byte array in binary format
		System.err.println("Reading files into memory");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new ObjectProductSource(product)
				.streamTo(new BinaryProductHandler(baos));
		new DeflateComparison().testByteArray("product contents",
				baos.toByteArray());
	}

}
