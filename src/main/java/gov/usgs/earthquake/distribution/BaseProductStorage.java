/*
 * BaseProductStorage
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.FilterProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.ProductHandler;
import gov.usgs.earthquake.product.io.ProductSource;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.ObjectLock;
import gov.usgs.util.StringUtils;

import java.net.URL;
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
 * Store products
 *
 * The BaseProductStorage implements the Configurable interface and can use the
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
 */
abstract public class BaseProductStorage extends DefaultConfigurable implements
		ProductStorage {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(BaseProductStorage.class.getName());

	/** Property for configured listeners */
	public static final String STORAGE_LISTENER_PROPERTY = "listeners";

	/** Property for legacyStorages. */
	public static final String LEGACY_STORAGES_PROPERTY = "legacyStorages";

	/** Locks used to make storage operations atomic. */
	protected ObjectLock<ProductId> storageLocks = new ObjectLock<ProductId>();

	private SignatureVerifier verifier = new SignatureVerifier();

	private Map<StorageListener, ExecutorService> listeners = new HashMap<StorageListener, ExecutorService>();

	/**
	 * A list of product storages used only for retrieving products, never for
	 * storing. Assists with migration between formats and other settings.
	 */
  private final ArrayList<ProductStorage> legacyStorages = new ArrayList<ProductStorage>();


	/**
	 * Define storage write format.
	 *
   * Called after write lock for id is acquired.
   *
	 * @param id
	 *     the product being stored.
	 * @return
   *     the ProductHandler.
	 */
	abstract protected ProductHandler _getProductHandler(final ProductId id) throws Exception;

  /**
   * Define storage read format.
   *
   * Called after real lock for id is acquired.
   *
   * @param id
   *      the product being stored.
   * @return the ProductSource, or null if product does not exist.
   */
  abstract protected ProductSource _getProductSource(final ProductId id) throws Exception;

  /**
   * Check whether product already in storage.
   *
   * Called after read lock for id is acquired.
   *
   * @param id
   *     the product to check.
   * @return true if exists, false otherwise
   */
  abstract protected boolean _hasProduct(final ProductId id) throws Exception;

  /**
   * Remove product from storage.
   *
   * Called after write lock for id is acquired.
   *
   * @param id
   *     the product to remove.
   */
  abstract protected void _removeProduct(final ProductId id) throws Exception;


	/**
	 * Configure this object.
	 */
  @Override
	public void configure(Config config) throws Exception {
		// Configure verifier
		verifier.configure(config);

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

			service.submit(() -> listener.onStorageEvent(event));
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
	 * Get a product from storage, loading all contents into memory.
	 *
	 * This method may cause memory problems if product contents are large.
	 *
	 * @param id
	 *            the product to retrieve.
	 * @return the loaded product.
	 * @throws Exception
	 */
	public Product getInMemoryProduct(ProductId id) throws Exception {
    acquireLock(id, false);
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
      releaseLock(id, false);
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
    this.acquireLock(id, false);
		ProductSource productSource = null;
		try {
      productSource = this._getProductSource(id);
			if (productSource == null) {
        for (ProductStorage next : this.legacyStorages) {
					try {
						productSource = next.getProductSource(id);
						if (productSource != null) {
							break;
						}
					} catch (Exception e) {
						LOGGER.log(Level.FINE, "[" + next.getName() + "] " +
								"legacy storage getProductSource exception ", e);
					}
				}
			}
		} finally {
			// release the lock no matter what
      releaseLock(id, false);
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
    acquireLock(id, false);
		boolean hasProduct = false;
		try {
      hasProduct = this._hasProduct(id);
			if (!hasProduct) {
				// primary storage doesn't have product, check legacy storages
        for (ProductStorage next : this.legacyStorages) {
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
      releaseLock(id, false);
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
    acquireLock(id, true);
		try {
      this._removeProduct(id);
      LOGGER.finer("[" + getName() + "] product removed, id=" + idString);
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
      releaseLock(id, true);
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
        releaseLock(id, true);
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
      acquireLock(id, true);
			if (hasProduct(id)) {
				throw new ProductAlreadyInStorageException("[" + getName()
						+ "] product already in storage");
			}

			// set the wrapped product output
			setProductOutput(_getProductHandler(id));
			// call the directory product output onBeginProduct method to start
			// writing the product
			super.onBeginProduct(id, status, trackerURL);
		}

		public void onEndProduct(ProductId id) throws Exception {
			// call the directory product output onEndProduct method to finish
			// writing the product
			super.onEndProduct(id);

      // DONT RELEASE THE LOCK HERE, this causes big problems on hubs...
      // See storeProductSource finally block
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
	 * @return the rejectInvalidSignatures
	 */
	public boolean isRejectInvalidSignatures() {
		return verifier.isRejectInvalidSignatures();
	}

	/**
	 * @param rejectInvalidSignatures
	 *            the rejectInvalidSignatures to set
	 */
	public void setRejectInvalidSignatures(boolean rejectInvalidSignatures) {
		verifier.setRejectInvalidSignatures(rejectInvalidSignatures);
	}

	/**
	 * @return the testSignatures
	 */
	public boolean isTestSignatures() {
		return verifier.isTestSignatures();
	}

	/**
	 * @param testSignatures
	 *            the testSignatures to set
	 */
	public void setTestSignatures(boolean testSignatures) {
		verifier.setTestSignatures(testSignatures);
	}

	/**
	 * @return the keychain
	 */
	public ProductKeyChain getKeychain() {
		return verifier.getKeychain();
	}

	/**
	 * @param keychain
	 *            the keychain to set
	 */
	public void setKeychain(ProductKeyChain keychain) {
		verifier.setKeychain(keychain);
	}

	/**
	 * @return the legacyStorages.
	 */
	public List<ProductStorage> getLegacyStorages() {
		return legacyStorages;
	}


	/**
	 * @return the storageLocks
	 */
	public ObjectLock<ProductId> getStorageLocks() {
		return storageLocks;
	}

  protected void acquireLock(final ProductId id, final boolean write) throws Exception {
    LOGGER.finest("[" + getName() + "] acquiring lock (write=" + write
        + ") for product id=" + id.toString());
    if (write) {
      storageLocks.acquireWriteLock(id);
    } else {
      storageLocks.acquireReadLock(id);
    }
    LOGGER.finest("[" + getName() + "] acquired lock (write=" + write
        + ") for product id=" + id.toString());
  }

  protected void releaseLock(final ProductId id, final boolean write) throws Exception {
    LOGGER.finest("[" + getName() + "] releasing lock (write=" + write
        + ") for product id=" + id.toString());
    if (write) {
      storageLocks.releaseWriteLock(id);
    } else {
      storageLocks.releaseReadLock(id);
    }
    LOGGER.finest("[" + getName() + "] releasing lock (write=" + write
        + ") for product id=" + id.toString());
  }

}
