/*
 * FileProductStorage
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.DirectoryProductHandler;
import gov.usgs.earthquake.product.io.DirectoryProductSource;
import gov.usgs.earthquake.product.io.FilterProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.ProductHandler;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.FileUtils;
import gov.usgs.util.ObjectLock;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
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
 * 
 * <dt>verifySignatures</dt>
 * <dd>(Optional, default = off) Whether or not to verify signatures:
 * <dl>
 * <dt>off</dt>
 * <dd>no verification</dd>
 * 
 * <dt>test</dt>
 * <dd>test but accept invalid signatures</dd>
 * 
 * <dt>anything else</dt>
 * <dd>reject invalid signatures.</dd>
 * </dl>
 * </dd>
 * 
 * <dt>keychain</dt>
 * <dd>(Optional) List of key section names to load for signature verification.</dd>
 * </dl>
 * 
 * An attempt is made to make storage operations atomic by using read and write
 * locks. While a write operation (store or remove) is in progress, read
 * operations will block. It is possible for a remove operation to occur between
 * the time getProduct() returns and the time when product contents are actually
 * loaded from a file. Users who are concerned about this should use the
 * getInMemoryProduct() method, which holds a read lock until all product files
 * are read.
 * 
 * To override the directory structure or format, override one or more of the
 * following methods:
 * 
 * <pre>
 * String getProductPath(ProductId)
 * ProductSource getProductSourceFormat(File)
 * ProductOutput getProductHandlerFormat(File)
 * </pre>
 */
public class FileProductStorage extends DefaultConfigurable implements
		ProductStorage {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(FileProductStorage.class.getName());

	/** Property for configured listeners */
	public static final String STORAGE_LISTENER_PROPERTY = "listeners";

	/** Storage path property name used by Configurable interface. */
	public static final String DIRECTORY_PROPERTY_NAME = "directory";
	/** Default storage path if none is provided. */
	public static final String DEFAULT_DIRECTORY = "storage";

	/** Property for whether or not to verify signatures. */
	public static final String VERIFY_SIGNATURES_PROPERTY_NAME = "verifySignatures";
	/** Don't verify signatures (Default). */
	public static final String DEFAULT_VERIFY_SIGNATURE = "off";
	/** Test signatures, but don't reject invalid. */
	public static final String TEST_VERIFY_SIGNATURE = "test";

	/** Property for whether or not to hash file paths. */
	public static final String USE_HASH_PATHS_PROPERTY = "useHashes";
	/** Do not use hashes (Default). */
	public static final boolean USE_HASH_PATHS_DEFAULT = false;

	/** Property for a list of keys. */
	public static final String KEYCHAIN_PROPERTY_NAME = "keychain";

	/** Property for a file of keys. */
	public static final String KEYCHAIN_FILE_PROPERTY_NAME = "keychainFile";

	/** Property for legacyStorages. */
	public static final String LEGACY_STORAGES_PROPERTY = "legacyStorages";

	/** Base directory for product storage. */
	private File baseDirectory;

	private boolean useHashes = USE_HASH_PATHS_DEFAULT;

	/** Locks used to make storage operations atomic. */
	private ObjectLock<ProductId> storageLocks = new ObjectLock<ProductId>();

	/**
	 * @return the storageLocks
	 */
	public ObjectLock<ProductId> getStorageLocks() {
		return storageLocks;
	}

	/** Whether or not to reject invalid signatures. */
	private boolean rejectInvalidSignatures = false;

	/** If not rejecting invalid signatures, test them anyways. */
	private boolean testSignatures = false;

	/** Keys used when testing signatures. */
	private ProductKeyChain keychain;

	private Map<StorageListener, ExecutorService> listeners = new HashMap<StorageListener, ExecutorService>();

	/**
	 * A list of product storages used only for retrieving products, never for
	 * storing. Assists with migration between formats and other settings.
	 */
	private final ArrayList<ProductStorage> legacyStorages = new ArrayList<ProductStorage>();

	/**
	 * Create this digest once, and clone it later. Only used if
	 * <code>useHashed</code> is set to <code>true</code>.
	 */
	private static final MessageDigest SHA_DIGEST;
	static {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA");
		} catch (Exception e) {
			LOGGER.warning("Unable to create SHA Digest for HashFileProductStorage");
			digest = null;
		}
		SHA_DIGEST = digest;
	}

	/**
	 * This is chosen because 16^3 = 4096 &lt; 32000, which is the ext3
	 * subdirectory limit.
	 */
	public static final int DIRECTORY_NAME_LENGTH = 3;

	private SignatureVerifier verifier = new SignatureVerifier();

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
	public void configure(Config config) throws Exception {
		String directory = config.getProperty(DIRECTORY_PROPERTY_NAME,
				DEFAULT_DIRECTORY);
		baseDirectory = new File(directory);
		LOGGER.config("[" + getName() + "] using storage directory "
				+ baseDirectory.getCanonicalPath());

		String verifySignatures = config
				.getProperty(VERIFY_SIGNATURES_PROPERTY_NAME);
		if (verifySignatures != null) {
			if (verifySignatures.equals(TEST_VERIFY_SIGNATURE)) {
				testSignatures = true;
				LOGGER.config("[" + getName() + "] test message signatures");
			} else if (!verifySignatures.equals(DEFAULT_VERIFY_SIGNATURE)) {
				rejectInvalidSignatures = true;
				LOGGER.config("[" + getName() + "] reject invalid signatures");
			}

			String keyNames = config.getProperty(KEYCHAIN_PROPERTY_NAME);
			if (keyNames != null) {
				LOGGER.config("[" + getName() + "] using product keys "
						+ keyNames);
				keychain = new ProductKeyChain(keyNames, Config.getConfig());
			} else {
				String keychainFileName = config
						.getProperty(KEYCHAIN_FILE_PROPERTY_NAME);
				if (keychainFileName != null) {
					Config keychainConfig = new Config();
					InputStream keychainFileInputStream = StreamUtils.getInputStream(
							new File(keychainFileName));
					try {
						keychainConfig.load(keychainFileInputStream);
					} finally {
						StreamUtils.closeStream(keychainFileInputStream);
					}
					keyNames = keychainConfig
							.getProperty(KEYCHAIN_PROPERTY_NAME);
					keychain = new ProductKeyChain(keyNames, keychainConfig);
				} else {
					LOGGER.warning("[" + getName()
							+ "] no product keys configured");
					keychain = new ProductKeyChain();
				}
			}

			verifier.setTestSignatures(testSignatures);
			verifier.setRejectInvalidSignatures(rejectInvalidSignatures);
			verifier.setKeychain(keychain);
		}

		// Set up our configured listeners
		Iterator<String> listenerIter = StringUtils.split(
				config.getProperty(STORAGE_LISTENER_PROPERTY), ",").iterator();
		while (listenerIter.hasNext()) {
			String listenerName = listenerIter.next();
			try {
				StorageListener listener = (StorageListener) Config.getConfig()
						.getObject(listenerName);
				addStorageListener(listener);
			} catch (Exception ccx) {
				throw new ConfigurationException("[" + getName()
						+ "] listener \"" + listenerName
						+ "\" was not properly configured. " + ccx.getMessage());
			}
		}

		// load legacy storages
		Iterator<String> legacyIter = StringUtils.split(
				config.getProperty(LEGACY_STORAGES_PROPERTY), ",").iterator();
		while (legacyIter.hasNext()) {
			String legacyName = legacyIter.next();
			try {
				ProductStorage legacyStorage = (ProductStorage) Config
						.getConfig().getObject(legacyName);
				legacyStorages.add(legacyStorage);
			} catch (Exception e) {
				throw new ConfigurationException("[" + getName()
						+ "] legacy storage '" + legacyName
						+ "' not properly configured. " + e.getMessage());
			}
		}
	}

	@Override
	public synchronized void notifyListeners(final StorageEvent event) {
		Iterator<StorageListener> listenerIter = listeners.keySet().iterator();
		while (listenerIter.hasNext()) {
			final StorageListener listener = listenerIter.next();
			LOGGER.finer("[" + getName() + "] listener :: "
					+ listener.getClass().getCanonicalName());
			final ExecutorService service = listeners.get(listener);

			service.submit(new Runnable() {

				public void run() {
					listener.onStorageEvent(event);
				}
			});
		}
	}

	@Override
	public void addStorageListener(final StorageListener listener) {
		LOGGER.finest("[" + getName() + "] adding listener :: "
				+ listener.getClass().getCanonicalName());
		if (!listeners.containsKey(listener)) {
			ExecutorService service = Executors.newSingleThreadExecutor();
			listeners.put(listener, service);
		}
	}

	@Override
	public void removeStorageListener(final StorageListener listener) {
		ExecutorService service = listeners.remove(listener);

		if (service != null) {
			service.shutdown();
		}
	}

	/**
	 * A method for subclasses to override the storage path.
	 * 
	 * The returned path is appended to the base directory when storing and
	 * retrieving products.
	 * 
	 * @param id
	 *            the product id to convert.
	 * @return the directory used to store id.
	 */
	public String getProductPath(final ProductId id) {

		if (useHashes) {
			return getHashedProductPath(id);
		} else {
			return getNormalProductPath(id);
		}
	}

	protected String getHashedProductPath(final ProductId id) {
		try {
			MessageDigest digest;
			synchronized (SHA_DIGEST) {
				digest = ((MessageDigest) SHA_DIGEST.clone());
			}

			String hexDigest = toHexString(digest.digest(id.toString()
					.getBytes()));

			StringBuffer buf = new StringBuffer();
			// start with product type, to give idea of available products and
			// disk usage when looking at filesystem
			buf.append(id.getType());

			// sub directories based on hash
			int length = hexDigest.length();
			for (int i = 0; i < length; i += DIRECTORY_NAME_LENGTH) {
				String part;
				if (i + DIRECTORY_NAME_LENGTH < length) {
					part = hexDigest.substring(i, i + DIRECTORY_NAME_LENGTH);
				} else {
					part = hexDigest.substring(i);
				}
				buf.append(File.separator);
				buf.append(part);
			}

			return buf.toString();
		} catch (CloneNotSupportedException e) {
			// fall back to parent class
			return getNormalProductPath(id);
		}
	}

	/**
	 * Convert an array of bytes into a hex string. The string will always be
	 * twice as long as the input byte array, because bytes < 0x10 are zero
	 * padded.
	 * 
	 * @param bytes
	 *            byte array to convert to hex.
	 * @return hex string equivalent of input byte array.
	 */
	private String toHexString(final byte[] bytes) {
		StringBuffer buf = new StringBuffer();
		int length = bytes.length;
		for (int i = 0; i < length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				buf.append('0');
			}
			buf.append(hex);
		}
		return buf.toString();
	}

	public String getNormalProductPath(final ProductId id) {
		StringBuffer buf = new StringBuffer();
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
	 * A method for subclasses to override the storage format.
	 * 
	 * When overriding this method, the method getProductSourceFormat should
	 * also be overridden.
	 * 
	 * @param file
	 *            a file that should be converted into a ProductHandler.
	 * @return the ProductHandler.
	 */
	protected ProductHandler getProductHandlerFormat(final File file)
			throws Exception {
		return new DirectoryProductHandler(file);
	}

	/**
	 * A method for subclasses to override the storage format.
	 * 
	 * When overriding this method, the method getProductHandlerFormat should
	 * also be overridden.
	 * 
	 * @param file
	 *            a file that should be converted into a ProductSource.
	 * @return the ProductSource.
	 */
	protected ProductSource getProductSourceFormat(final File file)
			throws Exception {
		return new DirectoryProductSource(file);
	}

	/**
	 * Get the file or directory used to store a specific product.
	 * 
	 * @param id
	 *            which product.
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

	/**
	 * Get a product from storage.
	 * 
	 * Calls the getProductSource method, and uses ObjectProductHandler to
	 * convert the ProductSource into a Product.
	 * 
	 * @param id
	 *            the product to retrieve.
	 * @return the product, or null if not in this storage.
	 */
	public Product getProduct(ProductId id) throws Exception {
		ProductSource source = getProductSource(id);
		if (source == null) {
			return null;
		} else {
			return ObjectProductHandler.getProduct(source);
		}
	}

	/**
	 * Get a product from storage, loading all file contents into memory.
	 * 
	 * This method may cause memory problems if product contents are large.
	 * 
	 * @param id
	 *            the product to retrieve.
	 * @return the loaded product.
	 * @throws Exception
	 */
	public Product getInMemoryProduct(ProductId id) throws Exception {
		LOGGER.finest("[" + getName() + "] acquiring read lock for product id="
				+ id.toString());
		storageLocks.acquireReadLock(id);
		LOGGER.finest("[" + getName() + "] acquired read lock for product id="
				+ id.toString());
		try {
			// load product
			Product product = getProduct(id);
			// convert all contents to ByteContent
			Map<String, Content> contents = product.getContents();
			Iterator<String> iter = contents.keySet().iterator();
			while (iter.hasNext()) {
				String path = iter.next();
				contents.put(path, new ByteContent(contents.get(path)));
			}
			// product content is all in memory
			return product;
		} finally {
			LOGGER.finest("[" + getName()
					+ "] releasing read lock for product id=" + id.toString());
			storageLocks.releaseReadLock(id);
			LOGGER.finest("[" + getName()
					+ "] released write lock for product id=" + id.toString());
		}
	}

	/**
	 * Get a ProductSource from storage.
	 * 
	 * @param id
	 *            the product to retrieve.
	 * @return a ProductSource for the product, or null if not in this storage.
	 */
	public ProductSource getProductSource(ProductId id) throws Exception {
		ProductSource productSource = null;

		LOGGER.finest("[" + getName() + "] acquiring read lock for product id="
				+ id.toString());
		// acquire lock in case storage operation in progress
		storageLocks.acquireReadLock(id);
		LOGGER.finest("[" + getName() + "] acquired read lock for product id="
				+ id.toString());
		try {
			File productFile = getProductFile(id);
			if (productFile.exists()) {
				productSource = getProductSourceFormat(productFile);
			}
			if (productSource == null) {
				Iterator<ProductStorage> legacyIter = legacyStorages.iterator();
				while (legacyIter.hasNext()) {
					ProductStorage next = legacyIter.next();
					try {
						productSource = next.getProductSource(id);
						if (productSource != null) {
							break;
						}
					} catch (Exception e) {
						LOGGER.log(Level.FINE, "[" + getName() + "] " +
								"legacy storage getProductSource exception ", e);
					}
				}
			}
		} finally {
			// release the lock no matter what
			LOGGER.finest("[" + getName()
					+ "] releasing read lock for product id=" + id.toString());
			storageLocks.releaseReadLock(id);
			LOGGER.finest("[" + getName()
					+ "] released read lock for product id=" + id.toString());
		}

		return productSource;
	}

	/**
	 * Check whether a product exists in storage.
	 * 
	 * @param id
	 *            the product to check.
	 * @return true if the product exists, false otherwise.
	 */
	public boolean hasProduct(ProductId id) throws Exception {
		boolean hasProduct = false;

		LOGGER.finest("[" + getName() + "] acquiring read lock for product id="
				+ id.toString());
		// acquire lock in case storage operation in progress
		storageLocks.acquireReadLock(id);
		LOGGER.finest("[" + getName() + "] acquired read lock for product id="
				+ id.toString());
		try {
			File productDirectory = getProductFile(id);
			hasProduct = productDirectory.exists();
			if (hasProduct) {
				// be a little more detailed...
				ProductSource source = getProductSource(id);
				if (source == null) {
					hasProduct = false;
				} else if (source instanceof DirectoryProductSource) {
					// not sure how we would get here
					// FileNotFound exception appears in logs...
					hasProduct = (new File(productDirectory,
							DirectoryProductHandler.PRODUCT_XML_FILENAME)
							.exists());
				}
				if (source != null) {
					source.close();
				}
			}

			if (!hasProduct) {
				// primary storage doesn't have product, check legacy storages
				Iterator<ProductStorage> legacyIter = legacyStorages.iterator();
				while (legacyIter.hasNext()) {
					ProductStorage next = legacyIter.next();
					try {
						if (next.hasProduct(id)) {
							return true;
						}
					} catch (Exception e) {
						LOGGER.log(Level.FINE, "[" + getName()
								+ "] legacy storage hasProduct exception ", e);
					}
				}
			}
		} finally {
			LOGGER.finest("[" + getName()
					+ "] releasing read lock for product id=" + id.toString());
			// release lock no matter what
			storageLocks.releaseReadLock(id);
			LOGGER.finest("[" + getName()
					+ "] released read lock for product id=" + id.toString());
		}

		return hasProduct;
	}

	/**
	 * Remove a product from storage.
	 * 
	 * @param id
	 *            product to remove.
	 */
	public void removeProduct(ProductId id) throws Exception {
		String idString = id.toString();
		LOGGER.finest("[" + getName()
				+ "] acquiring write lock for product id=" + idString);
		// acquire lock in case storage operation in progress
		storageLocks.acquireWriteLock(id);
		LOGGER.finest("[" + getName() + "] acquired write lock for product id="
				+ idString);
		try {
			File productFile = getProductFile(id);
			if (productFile.exists()) {
				// recursively delete the product directory
				FileUtils.deleteTree(productFile);
				// remove any empty parent directories
				FileUtils.deleteEmptyParents(productFile, baseDirectory);
				LOGGER.finer("[" + getName() + "] product removed, id=" + idString);
			}
			productFile = null;
			// remove from any legacy storages
			Iterator<ProductStorage> legacyIter = legacyStorages.iterator();
			while (legacyIter.hasNext()) {
				ProductStorage next = legacyIter.next();
				try {
					next.removeProduct(id);
				} catch (Exception e) {
					LOGGER.log(Level.FINE, "[" + getName()
							+ "] legacy storage remove exception ", e);
				}
			}
		} finally {
			LOGGER.finest("[" + getName()
					+ "] releasing write lock for product id=" + idString);
			// release lock no matter what
			storageLocks.releaseWriteLock(id);
			LOGGER.finest("[" + getName()
					+ "] released write lock for product id=" + idString);
		}

		// Notify listeners
		notifyListeners(new StorageEvent(this, id, StorageEvent.PRODUCT_REMOVED));
	}

	/**
	 * Store a product in storage.
	 * 
	 * Same as storeProductSource(new ObjectProductSource(product)).
	 * 
	 * @param product
	 *            the product to store.
	 * @return the id of the stored product.
	 */
	public ProductId storeProduct(Product product) throws Exception {
		return storeProductSource(new ObjectProductSource(product));
	}

	/**
	 * Store a ProductSource to storage.
	 * 
	 * If any exceptions occur while storing a product (other than the product
	 * already existing in storage) the incompletely stored product is removed.
	 * 
	 * @param source
	 *            the ProductSource to store.
	 * @return the id of the stored product.
	 */
	public ProductId storeProductSource(ProductSource source) throws Exception {
		StorageProductOutput output = new StorageProductOutput();
		// output acquires the storageLock during onBeginProduct, once the
		// product id is known.
		try {
			source.streamTo(output);

			ProductId id = output.getProductId();
			LOGGER.finer("[" + getName() + "] product stored id=" + id
					+ ", status=" + output.getStatus());

			verifier.verifySignature(getProduct(id));

		} catch (Exception e) {
			if (!(e instanceof ProductAlreadyInStorageException)
					&& !(e.getCause() instanceof ProductAlreadyInStorageException)) {
				if (e instanceof InvalidSignatureException) {
					// suppress stack trace for invalid signature
					LOGGER.warning(e.getMessage()
							+ ", removing incomplete product");
				} else {
					LOGGER.log(
							Level.WARNING,
							"["
									+ getName()
									+ "] exception while storing product, removing incomplete product",
							e);
				}
				try {
					// remove incompletely stored product.
					removeProduct(output.getProductId());
				} catch (Exception e2) {
					// ignore
					LOGGER.log(Level.WARNING, "[" + getName()
							+ "] exception while removing incomplete product",
							e2);
				}
			}
			throw e;
		} finally {
			// DO RELEASE THE WRITE LOCK HERE

			// This leads to thread sync problems in
			// SearchResponseXmlProductSource, because xml events were sent in
			// one thread, leading to acquisition of a write lock, while this
			// method was called in a separate thread and attempted to release
			// the write lock.

			// However, not releasing the lock here leads to other problems when
			// hubs are receiving products via multiple receivers.

			ProductId id = output.getProductId();

			if (id != null) {
				// release the write lock
				LOGGER.finest("[" + getName()
						+ "] releasing write lock for product id="
						+ id.toString());
				storageLocks.releaseWriteLock(id);
				LOGGER.finest("[" + getName()
						+ "] released write lock for product id="
						+ id.toString());
			}

			// close underlying handler
			output.close();
			output.setProductOutput(null);

			source.close();
		}

		ProductId id = output.getProductId();
		// Notify our storage listeners
		StorageEvent event = new StorageEvent(this, id,
				StorageEvent.PRODUCT_STORED);
		notifyListeners(event);

		return id;
	}

	/**
	 * Used when storing products.
	 * 
	 * When onBeginProduct is called with the ProductId being stored, a
	 * DirectoryProductOutput is created which manages storage.
	 */
	private class StorageProductOutput extends FilterProductHandler {

		/** The stored product id. */
		private ProductId id;

		/** The stored product status. */
		private String status;

		/**
		 * Construct a new StorageProductOutput.
		 */
		public StorageProductOutput() {
		}

		/**
		 * @return the product id that was stored.
		 */
		public ProductId getProductId() {
			return id;
		}

		/**
		 * @return the product status that was stored.
		 */
		public String getStatus() {
			return status;
		}

		/**
		 * The productID is stored and can be found using getProductId().
		 */
		public void onBeginProduct(ProductId id, String status, URL trackerURL)
				throws Exception {
			// save the product id for later
			this.id = id;
			this.status = status;

			// acquire write lock for product
			LOGGER.finest("[" + getName()
					+ "] acquiring write lock for product id=" + id.toString());
			storageLocks.acquireWriteLock(id);
			// keep track that we have write lock
			LOGGER.finest("[" + getName()
					+ "] acquired write lock for product id=" + id.toString());
			if (hasProduct(id)) {
				throw new ProductAlreadyInStorageException("[" + getName()
						+ "] product already in storage");
			}

			// set the wrapped product output
			setProductOutput(getProductHandlerFormat(getProductFile(id)));
			// call the directory product output onBeginProduct method to start
			// writing the product
			super.onBeginProduct(id, status, trackerURL);
		}

		public void onEndProduct(ProductId id) throws Exception {
			// call the directory product output onEndProduct method to finish
			// writing the product
			super.onEndProduct(id);

			// DONT RELEASE THE LOCK HERE, this causes bigger problems on
			// hubs...

			// release the write lock
			// LOGGER.finest("Releasing write lock for product id=" +
			// id.toString());
			// storageLocks.releaseWriteLock(id);
			// keep track that we no longer have write lock
			// this.haveWriteLock = false;
			// LOGGER.finest("Released write lock for product id=" +
			// id.toString());
		}
	}

	/**
	 * Called at client shutdown to free resources.
	 */
	public void shutdown() throws Exception {
		// Remove all our listeners. Doing this will also shut down the
		// ExecutorServices
		Iterator<StorageListener> listenerIter = listeners.keySet().iterator();
		while (listenerIter.hasNext()) {
			removeStorageListener(listenerIter.next());
			// Maybe we should call "listener.shutdown()" here as well?
		}

		// shutdown any legacy storages
		Iterator<ProductStorage> legacyIter = legacyStorages.iterator();
		while (legacyIter.hasNext()) {
			ProductStorage next = legacyIter.next();
			try {
				next.shutdown();
			} catch (Exception e) {
				LOGGER.log(Level.FINE, "[" + getName()
						+ "] legacy storage shutdown exception ", e);
			}
		}
	}

	/**
	 * Called after client configuration to begin processing.
	 */
	public void startup() throws Exception {
		// startup any legacy storages
		Iterator<ProductStorage> legacyIter = legacyStorages.iterator();
		while (legacyIter.hasNext()) {
			ProductStorage next = legacyIter.next();
			try {
				next.startup();
			} catch (Exception e) {
				LOGGER.log(Level.FINE, "[" + getName()
						+ "] legacy storage startup exception ", e);
			}
		}
	}

	/**
	 * @return the baseDirectory
	 */
	public File getBaseDirectory() {
		return baseDirectory;
	}

	/**
	 * @param baseDirectory
	 *            the baseDirectory to set
	 */
	public void setBaseDirectory(File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	/**
	 * @return the rejectInvalidSignatures
	 */
	public boolean isRejectInvalidSignatures() {
		return rejectInvalidSignatures;
	}

	/**
	 * @param rejectInvalidSignatures
	 *            the rejectInvalidSignatures to set
	 */
	public void setRejectInvalidSignatures(boolean rejectInvalidSignatures) {
		this.rejectInvalidSignatures = rejectInvalidSignatures;
		verifier.setRejectInvalidSignatures(rejectInvalidSignatures);
	}

	/**
	 * @return the testSignatures
	 */
	public boolean isTestSignatures() {
		return testSignatures;
	}

	/**
	 * @param testSignatures
	 *            the testSignatures to set
	 */
	public void setTestSignatures(boolean testSignatures) {
		this.testSignatures = testSignatures;
		verifier.setTestSignatures(testSignatures);
	}

	/**
	 * @return the keychain
	 */
	public ProductKeyChain getKeychain() {
		return keychain;
	}

	/**
	 * @param keychain
	 *            the keychain to set
	 */
	public void setKeychain(ProductKeyChain keychain) {
		this.keychain = keychain;
		verifier.setKeychain(keychain);
	}

	/**
	 * @return the legacyStorages.
	 */
	public List<ProductStorage> getLegacyStorages() {
		return legacyStorages;
	}

}
