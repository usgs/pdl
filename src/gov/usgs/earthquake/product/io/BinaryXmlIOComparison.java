package gov.usgs.earthquake.product.io;

import gov.usgs.earthquake.product.Product;
import gov.usgs.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;

/**
 * Compare io times of XML and Binary product formats.
 * 
 * All conversion is done in memory to try to balance the tests. All writes use
 * a BinaryProductSource to keep the playing field level.
 */
public class BinaryXmlIOComparison {

	public static long timeSerializeProduct(final ProductSource source,
			final ProductHandler handler) throws Exception {
		Date start = new Date();
		source.streamTo(handler);
		Date end = new Date();
		return end.getTime() - start.getTime();
	}

	public static void main(final String[] args) throws Exception {
		int numRuns = 10;
		testProductIO(
				ObjectProductHandler.getProduct(new BinaryProductSource(
						StreamUtils.getInputStream(new File(
								"etc/test_products/se082311a/us_dyfi_se082311a_1314562782198.bin")))),
				numRuns);
		testProductIO(
				ObjectProductHandler.getProduct(new BinaryProductSource(
						StreamUtils.getInputStream(new File(
								"etc/test_products/usa00040xz/us_shakemap_usa00040xz_1287260900624.bin")))),
				numRuns);
		testProductIO(
				ObjectProductHandler.getProduct(new BinaryProductSource(
						StreamUtils.getInputStream(new File(
								"etc/test_products/usa00040xz/us_losspager_usa00040xz_1287260989064.bin")))),
				numRuns);
	}

	public static void testProductIO(final Product product, int numRuns)
			throws Exception {
		System.err.println(product.getId().toString());
		testXmlReads(product, numRuns);
		testXmlWrites(product, numRuns);
		testBinaryReads(product, numRuns);
		testBinaryWrites(product, numRuns);
		System.err.println();
	}

	public static void testXmlReads(final Product product, int numReads)
			throws Exception {
		// read product into memory
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new ObjectProductSource(product).streamTo(new XmlProductHandler(baos));
		byte[] bytes = baos.toByteArray();

		long time = 0L;

		for (int i = 0; i < numReads; i++) {
			Date start = new Date();

			// parse from memory
			ObjectProductHandler.getProduct(new XmlProductSource(StreamUtils
					.getInputStream(bytes)));

			Date end = new Date();
			time += end.getTime() - start.getTime();
		}

		System.err.println("xml\tlength=" + bytes.length + ",\tnumReads="
				+ numReads + ",\tread average=" + ((double) time / numReads));
	}

	public static void testBinaryReads(final Product product, int numReads)
			throws Exception {
		// read product into memory
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new ObjectProductSource(product)
				.streamTo(new BinaryProductHandler(baos));
		byte[] bytes = baos.toByteArray();

		long time = 0L;

		for (int i = 0; i < numReads; i++) {
			Date start = new Date();

			// parse from memory
			ObjectProductHandler.getProduct(new BinaryProductSource(StreamUtils
					.getInputStream(bytes)));

			Date end = new Date();
			time += end.getTime() - start.getTime();
		}

		System.err.println("binary\tlength=" + bytes.length + ",\tnumReads="
				+ numReads + ",\tread average=" + ((double) time / numReads));
	}

	public static void testBinaryWrites(final Product product, int numWrites)
			throws Exception {
		// read product into memory
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new ObjectProductSource(product)
				.streamTo(new BinaryProductHandler(baos));
		byte[] bytes = baos.toByteArray();

		long time = 0L;

		for (int i = 0; i < numWrites; i++) {
			baos.reset();
			Date start = new Date();

			// parse from memory
			new BinaryProductSource(StreamUtils.getInputStream(bytes))
					.streamTo(new BinaryProductHandler(baos));

			Date end = new Date();
			time += end.getTime() - start.getTime();
		}

		System.err.println("binary\tlength=" + baos.toByteArray().length
				+ ",\tnumWrites=" + numWrites + ",\twrite average="
				+ ((double) time / numWrites));
	}

	public static void testXmlWrites(final Product product, int numWrites)
			throws Exception {
		// read product into memory
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new ObjectProductSource(product)
				.streamTo(new BinaryProductHandler(baos));
		byte[] bytes = baos.toByteArray();

		long time = 0L;

		for (int i = 0; i < numWrites; i++) {
			baos.reset();
			Date start = new Date();

			// parse from memory
			new BinaryProductSource(StreamUtils.getInputStream(bytes))
					.streamTo(new XmlProductHandler(baos));

			Date end = new Date();
			time += end.getTime() - start.getTime();
		}

		System.err.println("xml\tlength=" + baos.toByteArray().length
				+ ",\tnumWrites=" + numWrites + ",\twrite average="
				+ ((double) time / numWrites));
	}

}
