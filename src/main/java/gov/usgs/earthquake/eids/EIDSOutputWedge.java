package gov.usgs.earthquake.eids;

import java.io.File;
import java.util.logging.Logger;
import gov.usgs.earthquake.distribution.DefaultNotificationListener;
import gov.usgs.earthquake.product.Product;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;

/**
 * Output received products as files in a folder.
 */
public class EIDSOutputWedge extends DefaultNotificationListener {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(EIDSOutputWedge.class.getName());

	/** String for output type of EQXML */
	public static final String OUTPUT_TYPE_EQXML = "eqxml.xml";
	/** String for output type of quakeml */
	public static final String OUTPUT_TYPE_QUAKEML = "quakeml.xml";
	/** String for output type of cube */
	public static final String OUTPUT_TYPE_CUBE = "cube.txt";

	/** Property for output directory */
	public static final String OUTPUT_DIRECTORY_PROPERTY = "directory";
	/** Property for temp directory */
	public static final String TEMP_DIRECTORY_PROPERTY = "tempDirectory";
	/** Property for file name */
	public static final String FILE_NAME_PROPERTY = "contentFile";
	/** Property for output format */
	public static final String OUTPUT_FORMAT_PROPERTY = "outputFormat";

	/** Default output directory */
	public static final File DEFAULT_DIRECTORY = new File("outputdir");
	/** Default temp directory */
	public static final File DEFAULT_TEMP_DIRECTORY = new File(
			System.getProperty("java.io.tmpdir"));
	/** Sets default output format to cube.txt */
	public static final String DEFAULT_OUTPUT_FORMAT = OUTPUT_TYPE_CUBE;

	// Local Variables
	private File directory = DEFAULT_DIRECTORY;
	private File tempDirectory = DEFAULT_TEMP_DIRECTORY;
	private String outputFormat = DEFAULT_OUTPUT_FORMAT;
	// converter object
	private LegacyConverter converter;

	/**
	 * Create a new EIDSOutputWedge.
	 *
	 * Sets up the includeTypes list to contain "origin". Override this if you
	 * want the behavior to extend past origin products.
	 */
	public EIDSOutputWedge() {
		this.getIncludeTypes().add("origin");
		converter = LegacyConverter.cubeConverter();
	}

	/**
	 * Receive a product from Product Distribution.
	 *
	 * @param product A product
	 */
	@Override
	public void onProduct(final Product product) throws Exception {
		byte[] data = converter.convert(product);

		if (data != null) {
			write(data);
		}
	}

	/**
	 * Configuration
	 */
	@Override
	public void configure(final Config config) throws Exception {
		super.configure(config);

		setDirectory(new File(config.getProperty(OUTPUT_DIRECTORY_PROPERTY,
				DEFAULT_DIRECTORY.getName())));

		setTempDirectory(new File(config.getProperty(TEMP_DIRECTORY_PROPERTY,
				DEFAULT_TEMP_DIRECTORY.getName())));

		setOutputFormat(config.getProperty(OUTPUT_FORMAT_PROPERTY,
				DEFAULT_OUTPUT_FORMAT));

	}

	/**
	 * Writes the content of the file you wish to extract to disk with a unique
	 * name and at the directory specified in configuration
	 *
	 * @param data
	 * @throws Exception
	 */
	private void write(byte[] data) throws Exception {
		String uniqueFileName = System.currentTimeMillis() + "_" + outputFormat;
		File destFile = new File(directory, uniqueFileName);

		// Handles case where filename is already in use
		// In practice this shouldn't trigger
		while (destFile.exists()) {
			uniqueFileName = System.currentTimeMillis() + "_"
					+ (int) (Math.random() * 10000) + "_" + outputFormat;
			destFile = new File(directory, uniqueFileName);

			LOGGER.info("Eqxml name not unique. Attempting to resolve as "
					+ uniqueFileName);
		}

		File srcFile = new File(tempDirectory, uniqueFileName);
		FileUtils.writeFileThenMove(srcFile, destFile, data);
	}

	/** @return directory */
	public File getDirectory() {
		return directory;
	}

	/** @return tempDirectory */
	public File getTempDirectory() {
		return tempDirectory;
	}

	/** @return outputFormat */
	public String getOutputFormat() {
		return outputFormat;
	}

	/** @return legacy converter */
	public LegacyConverter getConverter() {
		return converter;
	}

	/** @param directory file to set */
	public void setDirectory(File directory) {
		this.directory = directory;
	}

	/** @param tempDirectory file to set */
	public void setTempDirectory(File tempDirectory) {
		this.tempDirectory = tempDirectory;
	}

	/** @param outputFormat string to set */
	public void setOutputFormat(String outputFormat) {
		if (outputFormat.equals(OUTPUT_TYPE_EQXML)) {
			converter = LegacyConverter.eqxmlConverter();
		} else if (outputFormat.equals(OUTPUT_TYPE_QUAKEML)) {
			converter = LegacyConverter.quakemlConverter();
		} else if (outputFormat.equals(OUTPUT_TYPE_CUBE)) {
			converter = LegacyConverter.cubeConverter();
		} else {
			throw new IllegalArgumentException("Unknown outputFormat '"
					+ outputFormat + "'");
		}
		this.outputFormat = outputFormat;
	}

}
