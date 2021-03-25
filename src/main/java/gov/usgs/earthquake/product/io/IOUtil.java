package gov.usgs.earthquake.product.io;

import gov.usgs.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

/**
 * Class with main method for converting from one product format to another.
 */
public class IOUtil {

	/** Argument for input file */
	public static final String INFILE_ARGUMENT = "--infile=";
	/** Argument for input format */
	public static final String INFORMAT_ARGUMENT = "--informat=";

	/** Argument for output file */
	public static final String OUTFILE_ARGUMENT = "--outfile=";
	/** Argument for output format */
	public static final String OUTFORMAT_ARGUMENT = "--outformat=";

	/** Zip format */
	public static final String ZIP_FORMAT = "zip";
	/** XML format */
	public static final String XML_FORMAT = "xml";
	/** Directory format */
	public static final String DIRECTORY_FORMAT = "directory";
	/** Binary format */
	public static final String BINARY_FORMAT = "binary";

	/**
	 * Returns a ProductHandler based on the output format
	 * @param outformat Output format
	 * @param outfile Output file
	 * @return a Product Handler
	 * @throws IOException if IO error occurs
	 */
	public static ProductHandler getProductHandler(final String outformat,
			final File outfile) throws IOException {
		ProductHandler out = null;
		if (outformat.equals(XML_FORMAT)) {
			out = new XmlProductHandler(StreamUtils.getOutputStream(outfile));
		} else if (outformat.equals(ZIP_FORMAT)) {
			out = new ZipProductHandler(StreamUtils.getOutputStream(outfile));
		} else if (outformat.equals(DIRECTORY_FORMAT)) {
			out = new DirectoryProductHandler(outfile);
		} else if (outformat.equals(BINARY_FORMAT)) {
			out = new BinaryProductHandler(StreamUtils.getOutputStream(outfile));
		} else {
			throw new IllegalArgumentException("unknown product format '"
					+ outformat + "'");
		}
		return out;
	}

	/**
	 * Returns a product source based on input format
	 * @param informat input file format
	 * @param infile input file
	 * @return a Productsource
	 * @throws IllegalArgumentException if informat argument error
	 * @throws IOException if error occurs
	 */
	public static ProductSource getProductSource(final String informat,
			final File infile) throws IllegalArgumentException, IOException {
		ProductSource in = null;
		if (informat.equals(XML_FORMAT)) {
			in = new XmlProductSource(StreamUtils.getInputStream(infile));
		} else if (informat.equals(ZIP_FORMAT)) {
			in = new ZipProductSource(infile);
		} else if (informat.equals(DIRECTORY_FORMAT)) {
			in = new DirectoryProductSource(infile);
		} else if (informat.equals(BINARY_FORMAT)) {
			in = new BinaryProductSource(StreamUtils.getInputStream(infile));
		} else {
			throw new IllegalArgumentException("unknown product format '"
					+ informat + "'");
		}
		return in;
	}

	/**
	 * Auto detect an Xml or Binary product source, that is optionally deflated.
	 *
	 * @param in
	 *            input stream containing optionally deflated xml or binary
	 *            product stream.
	 * @return ProductSource object.
	 * @throws IOException
	 *             if neither binary or xml product source is in stream.
	 */
	public static ProductSource autoDetectProductSource(final InputStream in)
			throws IOException {
		BufferedInputStream bufferedIn = autoDetectDeflate(in);
		int ch = -1;

		bufferedIn.mark(1024);
		// peek at first character in stream
		ch = bufferedIn.read();
		bufferedIn.reset();

		ProductSource productSource = null;
		// determine product format based on first character in stream
		if (((char) ch) == '<') {
			// xml format
			productSource = new XmlProductSource(bufferedIn);
		} else {
			// binary format
			productSource = new BinaryProductSource(bufferedIn);
		}

		return productSource;
	}

	/**
	 * Auto detect an optionally deflated stream.
	 *
	 * @param in
	 *            input stream containing optionally deflated xml or binary
	 *            product stream.
	 * @return ProductSource object.
	 * @throws IOException
	 *             if neither binary or xml product source is in stream.
	 */
	public static BufferedInputStream autoDetectDeflate(final InputStream in)
			throws IOException {
		// stream used to read product
		BufferedInputStream bufferedIn = new BufferedInputStream(in);

		// detect whether incoming stream is compressed
		bufferedIn.mark(1024);
		try {
			InflaterInputStream iis = new InflaterInputStream(bufferedIn);
			iis.read();
			// must be a deflated stream, reset for reading
			bufferedIn.reset();
			bufferedIn = new BufferedInputStream(new InflaterInputStream(bufferedIn));
		} catch (ZipException ze) {
			// not a deflated stream
			bufferedIn.reset();
		}

		return bufferedIn;
	}

	/**
	 * Access into IOUtil
	 * Takes arguments, gets product source and handler
	 * Streams source to handler
	 * @param args CLI args for infile, informat, outfile, outformat
	 * @throws Exception if error occurs
	 */
	public static void main(final String[] args) throws Exception {
		File infile = null;
		File outfile = null;
		String informat = null;
		String outformat = null;

		// parse arguments
		for (String arg : args) {
			if (arg.startsWith(INFILE_ARGUMENT)) {
				infile = new File(arg.replace(INFILE_ARGUMENT, ""));
			} else if (arg.startsWith(OUTFILE_ARGUMENT)) {
				outfile = new File(arg.replace(OUTFILE_ARGUMENT, ""));
			} else if (arg.startsWith(INFORMAT_ARGUMENT)) {
				informat = arg.replace(INFORMAT_ARGUMENT, "");
			} else if (arg.startsWith(OUTFORMAT_ARGUMENT)) {
				outformat = arg.replace(OUTFORMAT_ARGUMENT, "");
			}
		}

		if (infile == null || informat == null) {
			printUsage();
			System.exit(1);
		}

		if (outfile == null || outformat == null) {
			printUsage();
			System.exit(1);
		}

		ProductSource in = getProductSource(informat, infile);
		ProductHandler out = getProductHandler(outformat, outfile);
		in.streamTo(out);
	}

	/** CLI usage */
	public static void printUsage() {
		System.err
				.println("IOUtil --infile=FILE --informat=(xml|directory|zip|binary) --outfile=FILE --outformat=(xml|directory|zip|binary)");
	}

}
