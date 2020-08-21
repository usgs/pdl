/*
 * FileProductStorage
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.DirectoryProductHandler;
import gov.usgs.earthquake.product.io.DirectoryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.ProductHandler;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;

import java.io.File;
import java.util.logging.Logger;

/**
 * Store products in the file system.
 *
 * This implementation of ProductStorage extracts products into directories.
 *
 * The FileProductStorage implements the Configurable interface and can use the
 * following configuration parameters:
 *
 * <dl>
 * <dt>directory</dt>
 * <dd>(Optional, default = storage) The base directory where products are
 * stored. Each product is stored in a separate directory within this directory.
 * </dd>
 */
public class FileProductStorage extends BaseProductStorage {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(FileProductStorage.class.getName());

	/** Storage path property name used by Configurable interface. */
	public static final String DIRECTORY_PROPERTY_NAME = "directory";
	/** Default storage path if none is provided. */
	public static final String DEFAULT_DIRECTORY = "storage";


	/** Base directory for product storage. */
	private File baseDirectory;

	/**
	 * Create a new FileProductStorage using the default storage path.
	 */
	public FileProductStorage() {
		this(new File(DEFAULT_DIRECTORY));
	}

	/**
	 * Create a new FileProductStorage.
	 *
	 * @param baseDirectory
	 *            the base directory for all products being stored.
	 */
	public FileProductStorage(final File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	/**
	 * Configure this object.
	 *
	 * Expects a key named "directory".
	 */
	public void configure(final Config config) throws Exception {
		super.configure(config);

		final String directory = config.getProperty(DIRECTORY_PROPERTY_NAME, DEFAULT_DIRECTORY);
		baseDirectory = new File(directory);
		LOGGER.config("[" + getName() + "] using storage directory "
				+ baseDirectory.getCanonicalPath());
	}

	/**
	 * A method for subclasses to override the storage path.
	 *
	 * The returned path is appended to the base directory when storing and
	 * retrieving products.
	 *
	 * @param id the product id to convert.
	 * @return the directory used to store id.
	 */
	public String getProductPath(final ProductId id) {
		final StringBuffer buf = new StringBuffer();
		buf.append(id.getType());
		buf.append(File.separator);
		buf.append(id.getCode());
		buf.append(File.separator);
		buf.append(id.getSource());
		buf.append(File.separator);
		buf.append(id.getUpdateTime().getTime());
		return buf.toString();
	}

	/**
	 * Get the file or directory used to store a specific product.
	 *
	 * @param id which product.
	 * @return a file or directory where the product would be stored.
	 */
	public File getProductFile(final ProductId id) {
		String path = getProductPath(id);
		// remove any leading slash so path will always be within baseDirectory.
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return new File(baseDirectory, path);
	}

	@Override
	protected ProductHandler _getProductHandler(final ProductId id) throws Exception {
		return new DirectoryProductHandler(getProductFile(id));
	}

	@Override
	protected ProductSource _getProductSource(final ProductId id) throws Exception {
		final File productDirectory = getProductFile(id);
		if (!productDirectory.exists()) {
			return null;
		}
		return new DirectoryProductSource(productDirectory);
	}

	@Override
	protected boolean _hasProduct(final ProductId id) throws Exception {
		final File productDirectory = getProductFile(id);
		boolean hasProduct = productDirectory.exists();
		if (hasProduct) {
			try (ProductSource source = getProductSource(id)) {
				if (source == null) {
					hasProduct = false;
				} else if (source instanceof DirectoryProductSource) {
					hasProduct = new File(productDirectory,
							DirectoryProductHandler.PRODUCT_XML_FILENAME).exists();
				}
			}
		}
		return hasProduct;
	}

	@Override
	protected void _removeProduct(final ProductId id) throws Exception {
		File productFile = getProductFile(id);
		if (productFile.exists()) {
			// recursively delete the product directory
			FileUtils.deleteTree(productFile);
			// remove any empty parent directories
			FileUtils.deleteEmptyParents(productFile, baseDirectory);
			LOGGER.finer("[" + getName() + "] product removed, id=" + id.toString());
		}
		productFile = null;
	}

	/**
	 * Store a product in storage.
	 *
	 * Same as storeProductSource(new ObjectProductSource(product)).
	 *
	 * @param product the product to store.
	 * @return the id of the stored product.
	 */
	public ProductId storeProduct(final Product product) throws Exception {
		return storeProductSource(new ObjectProductSource(product));
	}

	/**
	 * @return the baseDirectory
	 */
	public File getBaseDirectory() {
		return baseDirectory;
	}

	/**
	 * @param baseDirectory the baseDirectory to set
	 */
	public void setBaseDirectory(final File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

}
