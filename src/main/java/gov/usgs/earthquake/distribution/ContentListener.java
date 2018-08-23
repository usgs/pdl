package gov.usgs.earthquake.distribution;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;

/**
 * A listener that listens for a specific content path.
 * 
 * This is intended for users who wish to output specific pieces of product
 * content, such as "quakeml.xml", for products that otherwise meet their
 * configured NotificationListener criteria.
 */
public class ContentListener extends DefaultNotificationListener {

	private static final Logger LOGGER = Logger.getLogger(ContentListener.class
			.getName());

	/** configuration property for includePaths. */
	public static final String OUTPUT_DIRECTORY_PROPERTY = "outputDirectory";
	public static final String TEMP_DIRECTORY_PROPERTY = "tempDirectory";
	public static final String OUTPUT_FORMAT_PROPERTY = "outputFormat";

	public static final String DEFAULT_OUTPUT_FORMAT = "SOURCE_TYPE_CODE_UPDATETIME_PATH";

	/** Directory where content is output. */
	private File outputDirectory = null;

	/**
	 * Temporary directory where content is written before moving to
	 * outputDirectory, defaults to system temp directory when null.
	 */
	private File tempDirectory = null;

	/** Output format for files inside outputDirectory. */
	private String outputFormat = DEFAULT_OUTPUT_FORMAT;

	public ContentListener() {
	}

	@Override
	public void configure(final Config config) throws Exception {
		super.configure(config);

		if (getIncludePaths().size() == 0) {
			throw new ConfigurationException("[" + getName()
					+ "] ContentListener requires 'includePaths' be non-empty");
		}

		outputDirectory = new File(
				config.getProperty(OUTPUT_DIRECTORY_PROPERTY));
		LOGGER.config("[" + getName() + "] output directory = "
				+ outputDirectory);

		String tempDirectoryString = config
				.getProperty(TEMP_DIRECTORY_PROPERTY);
		if (tempDirectoryString != null) {
			tempDirectory = new File(tempDirectoryString);
		}
		LOGGER.config("[" + getName() + "] temp directory = " + tempDirectory);

		outputFormat = config.getProperty(OUTPUT_FORMAT_PROPERTY,
				DEFAULT_OUTPUT_FORMAT);
		LOGGER.config("[" + getName() + "] output format = " + outputFormat);
	}

	@Override
	public void onProduct(final Product product) throws Exception {
		Map<String, Content> contents = product.getContents();
		Iterator<String> iter = getIncludePaths().iterator();
		while (iter.hasNext()) {
			String path = iter.next();
			Content content = contents.get(path);
			if (content != null) {
				// product has content at this path
				writeContent(product.getId(), path, content);
			}
		}
	}

	/**
	 * Generate an output path based on product id and content path.
	 * 
	 * @param id
	 *            the product id.
	 * @param path
	 *            the content path.
	 * @return relative path to write content within output directory.
	 */
	protected String getOutputPath(final ProductId id, final String path) {
		return outputFormat
				.replace("SOURCE", id.getSource())
				.replace("TYPE", id.getType())
				.replace("CODE", id.getCode())
				.replace("UPDATETIME",
						Long.toString(id.getUpdateTime().getTime()))
				.replace("PATH", path);
	}

	/**
	 * Output a product content that was in includePaths.
	 * 
	 * @param id
	 *            the product id.
	 * @param path
	 *            the content path.
	 * @param content
	 *            the content.
	 * @throws Exception
	 *             when unable to output the content.
	 */
	protected void writeContent(final ProductId id, final String path,
			final Content content) throws Exception {
		String outputPath = getOutputPath(id, path);
		if (tempDirectory != null && !tempDirectory.exists()) {
			// make sure parent directories exist
			tempDirectory.mkdirs();
		}
		File tempFile = File.createTempFile(outputPath, null, tempDirectory);

		File outputFile = new File(outputDirectory, outputPath);
		File outputFileParent = outputFile.getParentFile();
		if (!outputFileParent.exists()) {
			// make sure parent directories exist
			outputFileParent.mkdirs();
		}

		// write, then move into place
		InputStream in = null;
		OutputStream out = null;
		try {
			if (!tempFile.getParentFile().exists()) {
				tempFile.getParentFile().mkdirs();
			}
			if (!outputFile.getParentFile().exists()) {
				outputFile.getParentFile().mkdirs();
			}
			in = content.getInputStream();
			out = StreamUtils.getOutputStream(tempFile);
			// write to temp file
			StreamUtils.transferStream(in, out);
			// move to output file
			tempFile.renameTo(outputFile);
		} finally {
			StreamUtils.closeStream(in);
			StreamUtils.closeStream(out);
			// clean up temp file if it wasn't renamed
			if (tempFile.exists()) {
				tempFile.delete();
			}
		}
	}

	/**
	 * @return the outputDirectory
	 */
	public File getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * @param outputDirectory
	 *            the outputDirectory to set
	 */
	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	/**
	 * @return the tempDirectory
	 */
	public File getTempDirectory() {
		return tempDirectory;
	}

	/**
	 * @param tempDirectory
	 *            the tempDirectory to set
	 */
	public void setTempDirectory(File tempDirectory) {
		this.tempDirectory = tempDirectory;
	}

	/**
	 * @return the outputFormat
	 */
	public String getOutputFormat() {
		return outputFormat;
	}

	/**
	 * @param outputFormat
	 *            the outputFormat to set
	 */
	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

}
