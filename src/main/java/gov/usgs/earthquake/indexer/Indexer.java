/*
 * Indexer
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.distribution.DefaultNotificationListener;
import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.distribution.HeartbeatListener;
import gov.usgs.earthquake.distribution.ProductAlreadyInStorageException;
import gov.usgs.earthquake.distribution.ProductStorage;
import gov.usgs.earthquake.geoserve.ANSSRegionsFactory;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.util.CompareUtil;
import gov.usgs.util.Config;
import gov.usgs.util.Configurable;
import gov.usgs.util.ExecutorTask;
import gov.usgs.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The indexer receives products from Distribution, and adds them to the
 * EventIndex.
 * 
 * This class provides the following configurable properties (in addition to
 * those inherited from DefaultNotificationListener):
 * <dl>
 * <dt>associator</dt>
 * <dd>An object that implements the Associator interface.</dd>
 * 
 * <dt>storage</dt>
 * <dd>An object that implements the ProductStorage interface.</dd>
 * 
 * <dt>index</dt>
 * <dd>An object that implements the ProductIndex interface.</dd>
 * 
 * <dt>modules</dt>
 * <dd>A comma delimited list of objects that implement the IndexerModule
 * interface</dd>
 * 
 * <dt>listeners</dt>
 * <dd>A comma delimited list of objects that implement the IndexerListener
 * interface</dd>
 * </dl>
 */
public class Indexer extends DefaultNotificationListener {

	/** Logging Utility **/
	private static final Logger LOGGER = Logger.getLogger(Indexer.class
			.getName());

	/** Preferred weight for persistent trump. */
	public static final long TRUMP_PREFERRED_WEIGHT = 100000000;

	public static final String TRUMP_PRODUCT_TYPE = "trump";
	public static final String PERSISTENT_TRUMP_PREFIX = "trump-";

	/** Property name to configure a custom associator. */
	public static final String ASSOCIATOR_CONFIG_PROPERTY = "associator";

	public static final String ASSOCIATE_USING_CURRENT_PRODUCTS_PROPERTY = "associateUsingCurrentProducts";

	public static final String DEFAULT_ASSOCIATE_USING_CURRENT_PRODUCTS = "false";

	/** Property name to configure a custom storage. */
	public static final String STORAGE_CONFIG_PROPERTY = "storage";

	/** Shortcut name to configure a file product storage. */
	public static final String STORAGE_DIRECTORY_CONFIG_PROPERTY = "storageDirectory";

	/** Property name to configure a custom index. */
	public static final String INDEX_CONFIG_PROPERTY = "index";

	/** Shortcut name to configure a sqlite index. */
	public static final String INDEXFILE_CONFIG_PROPERTY = "indexFile";

	/** Property name to configure modules. */
	public static final String MODULES_CONFIG_PROPERTY = "modules";

	/** Property name to configure listeners. */
	public static final String LISTENERS_CONFIG_PROPERTY = "listeners";

	/** Property name to configure local regions file. */
	public static final String LOCAL_REGIONS_PROPERTY = "localRegionsFile";
	/** Path to local regions file. */
	public static final String DEFAULT_LOCAL_REGIONS = "regions.json";

	/** Property name to enable search socket. */
	public static final String ENABLE_SEARCH_PROPERTY = "enableSearch";
	/** Property name for search socket port. */
	public static final String SEARCH_PORT_PROPERTY = "searchPort";
	/** Property name for search socket thread pool size. */
	public static final String SEARCH_THREADS_PROPERTY = "searchThreads";

	/** Default value whether to enable search socket. */
	public static final String DEFAULT_ENABLE_SEARCH = "false";
	/** Default port where search socket listens. */
	public static final String DEFAULT_SEARCH_PORT = "11236";
	/** Number of threads (concurrent searches) allowed. */
	public static final String DEFAULT_SEARCH_THREADS = "5";

	/** Utility used for associating products to events. */
	private Associator associator;

	/** Whether to use (false) all products or (true) current products. */
	private boolean associateUsingCurrentProducts = true;

	/** Where product contents are stored. */
	private ProductStorage productStorage;

	/** Index of stored products, and how they are related. */
	private ProductIndex productIndex;

	/** Modules provide product specific functionality. */
	private List<IndexerModule> modules = new LinkedList<IndexerModule>();

	/** Listeners listen for changes to the event index. */
	private Map<IndexerListener, ExecutorService> listeners = new HashMap<IndexerListener, ExecutorService>();

	/** Local file where regions are stored. */
	private File localRegionsFile = new File(DEFAULT_LOCAL_REGIONS);

	/** Timer for archive policy thread. */
	private Timer archiveTimer = null;

	/** Task for archive policy thread. */
	private TimerTask archiveTask = null;

	/** Whether to (false) or not (true) to run archive policies. */
	private boolean disableArchive = false;

	// -- Configurable property names -- //
	public static final String INDEX_ARCHIVE_INTERVAL_PROPERTY = "archiveInterval";
	public static final String INDEX_ARCHIVE_POLICY_PROPERTY = "archivePolicy";

	// -- Default configurable property values -- //
	private static final long INDEX_ARCHIVE_INTERVAL_DEFAULT = 300000L;

	// -- Configured member variables. Values set in configure() method. -- //
	private long archiveInterval = 0;

	private List<ArchivePolicy> archivePolicies = null;

	private SearchServerSocket searchSocket = null;

	private DefaultIndexerModule defaultModule = new DefaultIndexerModule();

	/**
	 * Default no-arg constructor. This gets called from the Configurable API.
	 * All configuration parameters are set in the "configure" method.
	 * 
	 * @throws Exception
	 *             If the JDBCProductIndex throws an exception.
	 */
	public Indexer() throws Exception {
		addModule(defaultModule);

		associator = new DefaultAssociator();
		productStorage = new FileProductStorage();
		productIndex = new JDBCProductIndex();
		archivePolicies = new LinkedList<ArchivePolicy>();
	}

	/**
	 * Returns the current associator used to associate products to one-another
	 * and products to events.
	 * 
	 * @return The current Associator.
	 */
	public Associator getAssociator() {
		return associator;
	}

	/**
	 * Sets the given associator as the current associator to associate products
	 * to one-another and products to events.
	 * 
	 * @param associator
	 *            The associator to use from this point forward.
	 */
	public void setAssociator(Associator associator) {
		this.associator = associator;
	}

	/**
	 * Returns the product storage component that is used to store products as
	 * they are received.
	 * 
	 * @return The current product storage component.
	 */
	public ProductStorage getProductStorage() {
		return productStorage;
	}

	/**
	 * Sets the current product storage component used to store products as they
	 * are received.
	 * 
	 * @param productStorage
	 *            The product storage component to use from this point forward.
	 */
	public void setProductStorage(ProductStorage productStorage) {
		this.productStorage = productStorage;
	}

	/**
	 * Returns the product index component used to index product information as
	 * it is received.
	 * 
	 * @return The current product index component.
	 */
	public ProductIndex getProductIndex() {
		return productIndex;
	}

	/**
	 * Sets the product index component used to index product information as it
	 * is received.
	 * 
	 * @param productIndex
	 *            The product index component to use from this point forward.
	 */
	public void setProductIndex(ProductIndex productIndex) {
		this.productIndex = productIndex;
	}

	/**
	 * Adds the give indexer module to the current list of modules used by the
	 * indexer to handle products.
	 * 
	 * @param toAdd
	 *            The IndexerModule to add to our list.
	 */
	public void addModule(final IndexerModule toAdd) {
		modules.add(toAdd);
	}

	/**
	 * Removes the first occurrence of the given indexer module from the current
	 * list of known modules.
	 * 
	 * @param toRemove
	 *            The module to remove.
	 * @see java.util.LinkedList#remove(Object)
	 */
	public void removeModule(final IndexerModule toRemove) {
		modules.remove(toRemove);
	}

	/**
	 * This method checks each module's support level for the given product,
	 * returning the first module with the highest support level.
	 * 
	 * @param product
	 *            the product to summarize.
	 * @return module best suited to summarize product.
	 */
	protected synchronized IndexerModule getModule(final Product product) {
		// mit is the module fetched off the iterator
		// m is the module to return
		IndexerModule mit = null, m = null;
		Iterator<IndexerModule> it = modules.iterator();

		// If there are no known modules, then null. Oops. :)
		if (it.hasNext()) {
			// Use first module so long as it exists
			m = it.next();

			// Check remaining modules if any offer greater support
			while (it.hasNext()) {
				mit = it.next();
				// We use strictly greater than (no equals)
				if (mit.getSupportLevel(product) > m.getSupportLevel(product)) {
					m = mit;
				}
			}
		}
		return m;
	}

	/**
	 * Adds a listener to this indexer. Listeners are notified when an event is
	 * added, updated, or deleted, or when a new product arrives and is
	 * un-associated to an event.
	 * 
	 * @param toAdd
	 *            The IndexerListener to add
	 */
	public void addListener(final IndexerListener toAdd) {
		if (!listeners.containsKey(toAdd)) {
			ExecutorService listenerExecutor = Executors
					.newSingleThreadExecutor();
			listeners.put(toAdd, listenerExecutor);
		}
	}

	/**
	 * Removes a listener from this indexer.Listeners are notified when an event
	 * is added, updated, or deleted, or when a new product arrives and is
	 * un-associated to an event.
	 * 
	 * @param toRemove
	 *            The IndexerListener to remove
	 */
	public void removeListener(final IndexerListener toRemove) {
		// Remove listener from map
		ExecutorService listenerExecutor = listeners.remove(toRemove);

		if (listenerExecutor != null) {
			// Shutdown executor thread
			listenerExecutor.shutdown();
		}
	}

	/**
	 * Send an indexer event to all registered IndexerListeners.
	 * 
	 * Creates a NotificationEvent, with a reference to this object and calls
	 * each notificationListeners onNotification method in separate threads.
	 * 
	 * This method usually returns before registered NotificationListeners have
	 * completed processing a notification.
	 * 
	 * @param event
	 *            The event that occurred to trigger the notification. Note: An
	 *            IndexerEvent has a specific "type" to clarify the type of
	 *            event that occurred.
	 */
	protected synchronized void notifyListeners(final IndexerEvent event) {
		StringBuffer buf = new StringBuffer();
		Iterator<IndexerChange> changes = event.getIndexerChanges().iterator();
		while (changes.hasNext()) {
			IndexerChange change = changes.next();
			buf.append("\n").append(change.getType().toString()).append(" ");
			if (change.getOriginalEvent() == null) {
				buf.append("null");
			} else {
				buf.append(change.getOriginalEvent().getEventId());
			}
			buf.append(" => ");
			if (change.getNewEvent() == null) {
				buf.append("null");
			} else {
				buf.append(change.getNewEvent().getEventId());
			}
		}

		// Can't rely on event.getSummary because that might be null
		ProductSummary theSummary = event.getSummary();
		if (theSummary == null && event.getEvents().size() > 0) {
			theSummary = event.getEvents().get(0).getEventIdProduct();
		}

		if (theSummary != null) {
			LOGGER.log(Level.INFO, "[" + getName() + "] indexed product id="
					+ theSummary.getId().toString()
					+ ", status=" + theSummary.getStatus()
					+ buf.toString());
		} else {
			LOGGER.log(Level.FINE, "[" + getName()
					+ "] event summary was null. This probably "
					+ "means the archive policy is notifying of an archived "
					+ "event.");
		}

		Iterator<IndexerListener> it = listeners.keySet().iterator();
		while (it.hasNext()) {
			final IndexerListener listener = it.next();
			ExecutorService listenerExecutor = listeners.get(listener);
			ExecutorTask<Void> listenerTask = new ExecutorTask<Void>(
					listenerExecutor, listener.getMaxTries(),
					listener.getTimeout(), new IndexerListenerCallable(listener,
							event));
			listenerExecutor.submit(listenerTask);
		}
	}

	/**
	 * Check whether this product is in the index.
	 * 
	 * @param id
	 * @return true if product has already been indexed.
	 */
	protected boolean hasProductBeenIndexed(final ProductId id) {
		try {
			ProductIndexQuery alreadyProcessedQuery = new ProductIndexQuery();
			alreadyProcessedQuery
					.setResultType(ProductIndexQuery.RESULT_TYPE_ALL);

			// alreadyProcessedQuery.getProductIds().add(id);
			// use existing indexes
			alreadyProcessedQuery.setProductSource(id.getSource());
			alreadyProcessedQuery.setProductType(id.getType());
			alreadyProcessedQuery.setProductCode(id.getCode());
			alreadyProcessedQuery.setMinProductUpdateTime(id.getUpdateTime());
			alreadyProcessedQuery.setMaxProductUpdateTime(id.getUpdateTime());

			List<ProductSummary> existingSummary = productIndex
					.getProducts(alreadyProcessedQuery);
			if (existingSummary.size() > 0 &&
					existingSummary.get(0).getId().equals(id)) {
				// it is in the product index
				return true;
			}
		} catch (Exception wtf) {
			LOGGER.log(Level.WARNING, "[" + getName()
					+ "] exception checking if product already indexed", wtf);
		}

		// default is it hasn't been processed
		return false;
	}

	/**
	 * Override the DefaultNotificationListener accept method,
	 * to always process products that may affect event association.
	 *
	 * @param id
	 *        the product id to check.
	 * @return boolean
	 *         whether the product should be indexed.
	 */
	@Override
	public boolean accept(final ProductId id) {
		final boolean superAccept = super.accept(id);

		if (!superAccept && isIncludeActuals()) {
			// automatically accept products that affect association
			// (if processing non-scenario products)
			final String type = id.getType();
			if (Event.ORIGIN_PRODUCT_TYPE.equals(type) ||
					Event.ASSOCIATE_PRODUCT_TYPE.equals(type) ||
					Event.DISASSOCIATE_PRODUCT_TYPE.equals(type)
					|| type.startsWith(TRUMP_PRODUCT_TYPE)) {
				return true;
			}
		}

		return superAccept;
	}

	/**
	 * This method receives a product from Product Distribution and adds it to
	 * the index.
	 * 
	 * Implementation follows from Product Indexer Diagram (pg.10) of
	 * ProductIndexer.pdf document dated 09/09/2010.
	 * 
	 * Calls onProduct(product, false), which will not reprocess already
	 * processed products.
	 * 
	 * @param product
	 *            The product triggering the event.
	 * @throws Exception
	 *             if an exception occurs.
	 */
	@Override
	public synchronized void onProduct(final Product product) throws Exception {
		onProduct(product, false);
	}

	/**
	 * Receive a product and add it to the index. Optionally, reprocessing a
	 * product that has already been processed.
	 * 
	 * @param product
	 *            The product triggering the event.
	 * @param force
	 *            Whether to reprocess products that have already been processed
	 *            (true), or skip (false).
	 * @throws Exception
	 */
	public synchronized void onProduct(final Product product,
			final boolean force) throws Exception {
		ProductId id = product.getId();

		// The notification to be sent when we are finished with this product
		IndexerEvent notification = new IndexerEvent(this);
		notification.setIndex(getProductIndex());

		// -------------------------------------------------------------------//
		// -- Step 1: Store product
		// -------------------------------------------------------------------//
		try {
			LOGGER.finest("[" + getName() + "] storing product id="
					+ id.toString());
			productStorage.storeProduct(product);
			LOGGER.finest("[" + getName() + "] stored product id="
					+ id.toString());
		} catch (ProductAlreadyInStorageException paise) {
			LOGGER.finer("["
					+ getName()
					+ "] product already in indexer storage, checking if indexed");
			if (force) {
				LOGGER.finer("[" + getName()
						+ "] force=true skipping check, (re)process product");
			} else if (hasProductBeenIndexed(id)) {
				LOGGER.fine("[" + getName() + "] product already indexed "
						+ product.getId());
				// don't reindex for now
				return;
			}
		}

		// -------------------------------------------------------------------//
		// -- Step 2: Use product module to summarize product
		// -------------------------------------------------------------------//

		LOGGER.finer("[" + getName() + "] summarizing product id="
				+ id.toString());
		// Find best available indexer module
		IndexerModule module = getModule(product);

		// Use this module to summarize the product
		ProductSummary productSummary = module.getProductSummary(product);
		notification.setSummary(productSummary);

		// -------------------------------------------------------------------//
		// -- Step 3: Add product summary to the product index
		// -------------------------------------------------------------------//

		LOGGER.finest("[" + getName() + "] beginning index transaction");
		// Start the product index transaction, only proceed if able
		productIndex.beginTransaction();

		try {
			LOGGER.finer("[" + getName() + "] finding previous version");
			// Check index for previous version of this product
			ProductSummary prevSummary = getPrevProductVersion(productSummary);
			boolean redundantProduct = isRedundantProduct(prevSummary, productSummary);

			LOGGER.finer("[" + getName() + "] finding previous event");
			Event prevEvent = null;
			if (!redundantProduct) {
				// Skip association queries and use existing product association
				// performed in next branch (should be associated already if
				// "redundant").

				// Check index for existing event candidate
				prevEvent = getPrevEvent(productSummary, true);
			}

			// may be an update/delete to a product that previously associated
			// to an event, even though this product isn't associating on its
			// own
			if (prevSummary != null && prevEvent == null) {
				// see if prevSummary associated with an event
				ProductIndexQuery prevEventQuery = new ProductIndexQuery();
				prevEventQuery.getProductIds().add(prevSummary.getId());
				if (associateUsingCurrentProducts) {
					prevEventQuery.setResultType(ProductIndexQuery.RESULT_TYPE_CURRENT);
				}
				List<Event> prevEvents = productIndex.getEvents(prevEventQuery);
				if (prevEvents.size() != 0) {
					// just use first (there can really only be one).
					prevEvent = prevEvents.get(0);
				}
			}

			// special handling to allow trump products to associate based on
			// a product link. Not used when eventsource/eventsourcecode set.
			if (prevEvent == null
					&& productSummary.getId().getType().equals(TRUMP_PRODUCT_TYPE)
					&& productSummary.getLinks().containsKey("product")
					&& !productSummary.getStatus().equalsIgnoreCase(
							Product.STATUS_DELETE)) {
				// see if we can associate via another product
				ProductIndexQuery otherEventQuery = new ProductIndexQuery();
				otherEventQuery.getProductIds().add(
						ProductId.parse(productSummary.getLinks()
								.get("product").get(0).toString()));
				if (associateUsingCurrentProducts) {
					otherEventQuery.setResultType(ProductIndexQuery.RESULT_TYPE_CURRENT);
				}
				List<Event> prevEvents = productIndex
						.getEvents(otherEventQuery);
				if (prevEvents.size() != 0) {
					// just use first (there can really only be one).
					prevEvent = prevEvents.get(0);
				}
			}

			// Add the summary to the index
			LOGGER.finer("[" + getName() + "] adding summary to index");
			if (prevSummary != null && prevSummary.equals(productSummary)) {
				// implied force=true, prevEvent!=null

				// remove the previous version of this product summary
				// so the new one can take its place
				if (prevEvent != null) {
					productIndex.removeAssociation(prevEvent, prevSummary);
				} else {
					LOGGER.fine("[" + getName()
							+ "] reprocessing unassociated summary");
				}
				productIndex.removeProductSummary(prevSummary);
			}
			productSummary = productIndex.addProductSummary(productSummary);

			Event event = null;
			if (prevEvent == null) {
				// No existing event, try to create one and associate
				event = createEvent(productSummary);
				if (event != null) {
					LOGGER.finer("[" + getName() + "] created event indexid="
							+ event.getIndexId());
					event.log(LOGGER);
				} else {
					LOGGER.finer("[" + getName()
							+ "] unable to create event for product.");
				}
			} else {
				LOGGER.finer("[" + getName()
						+ "] found existing event indexid="
						+ prevEvent.getIndexId());
				prevEvent.log(LOGGER);

				// Existing event found associate to it
				event = productIndex.addAssociation(prevEvent, productSummary);
			}

			// Can't split or merge a non-existent event
			if (prevEvent != null && event != null) {
				LOGGER.finer("[" + getName() + "] checking for event splits");
				// Check for event splits
				notification.addIndexerChanges(checkForEventSplits(
						productSummary, prevEvent, event));
			}

			// Is this a problem??? split may modify the event, and then
			// the unmodified version of that event is passed to merge???
			// If this is a problem, checkForEventSplits and checkForEventMerges
			// could be modified to accept the notification object (and add
			// changes to it) and return the potentially modified object by
			// reference.

			if (event != null) {
				LOGGER.finer("[" + getName() + "] checking for event merges");
				// Check for event merges
				notification.addIndexerChanges(checkForEventMerges(
						productSummary, prevEvent, event));
			}

			// see if this is a trump product that needs special processing.
			event = checkForTrump(event, productSummary, prevSummary);

			// Set our notification indexer changes if not set yet
			if (notification.getIndexerChanges().size() == 0) {
				if (prevEvent == null && event != null) {
					// No previous event, so event added.
					notification.addIndexerChange(new IndexerChange(
							IndexerChange.EVENT_ADDED, prevEvent, event));
				} else if (prevEvent != null && event != null) {
					// Previous existed so event updated.
					notification.addIndexerChange(new IndexerChange(event
							.isDeleted() ? IndexerChange.EVENT_DELETED
							: IndexerChange.EVENT_UPDATED, prevEvent, event));
				} else if (prevEvent == null && event == null) {
					// No event existed or could be created.

					if (prevSummary == null) {
						// No previous summary, product added.
						notification.addIndexerChange(new IndexerChange(
								IndexerChange.PRODUCT_ADDED, null, null));
					} else {
						// Previous summary existed. Product updated.
						notification
								.addIndexerChange(new IndexerChange(
										productSummary.isDeleted() ? IndexerChange.PRODUCT_DELETED
												: IndexerChange.PRODUCT_UPDATED,
										null, null));
					}
				}
			}

			LOGGER.finer("[" + getName()
					+ "] updating event summary parameters");
			// update preferred event parameters in index
			productIndex.eventsUpdated(notification.getEvents());

			LOGGER.finer("[" + getName() + "] committing transaction");
			// Commit our changes to the index (after updating summary attrs)
			productIndex.commitTransaction();

			try {
				LOGGER.fine("[" + getName() + "] notifying listeners");
				// ---------------------------------------------------------//
				// -- Step 5: Notify listeners with our indexer event
				// ---------------------------------------------------------//
				notifyListeners(notification);
			} catch (Exception e) {
				// this doesn't affect success of index transaction...
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] exception while notifying listeners", e);
			}

			// send heartbeat info
			HeartbeatListener.sendHeartbeatMessage(getName(),
					"indexed product", id.toString());
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "[" + getName()
					+ "] rolling back transaction", e);
			// just rollback since it wasn't successful
			productIndex.rollbackTransaction();

			// send heartbeat info
			HeartbeatListener.sendHeartbeatMessage(getName(),
					"index exception", id.toString());
			// send heartbeat info
			HeartbeatListener.sendHeartbeatMessage(getName(),
					"index exception class", e.getClass().getName());

			throw e;
		}

	}

	/**
	 * Check whether two products are redundant, meaning would not affect event
	 * associations and indexer can skip split/merge steps.
	 * 
	 * @param previous previous version of product.
	 * @param current current version of product.
	 * @return true if products are equivalent for association purposes.
	 */
	private boolean isRedundantProduct(final ProductSummary previous, final ProductSummary current) {
		if (previous == null || previous.equals(current)
				|| !previous.getId().isSameProduct(current.getId())) {
			return false;
		}
		if (previous.getPreferredWeight() == current.getPreferredWeight()
				&& CompareUtil.nullSafeCompare(previous.getStatus(),
						current.getStatus()) == 0
				&& CompareUtil.nullSafeCompare(previous.getEventDepth(),
						current.getEventDepth()) == 0
				&& CompareUtil.nullSafeCompare(previous.getEventLatitude(),
						current.getEventLatitude()) == 0
				&& CompareUtil.nullSafeCompare(previous.getEventLongitude(),
						current.getEventLongitude()) == 0
				&& CompareUtil.nullSafeCompare(previous.getEventMagnitude(),
						current.getEventMagnitude()) == 0
				&& CompareUtil.nullSafeCompare(previous.getEventSource(),
						current.getEventSource()) == 0
				&& CompareUtil.nullSafeCompare(previous.getEventSourceCode(),
						current.getEventSourceCode()) == 0
				&& CompareUtil.nullSafeCompare(previous.getEventTime(),
						current.getEventTime()) == 0) {
			// these are the properties that would influence indexer associations
			// or preferred event properties.
			return true;
		}
		return false;
	}

	/**
	 * Check for, and handle incoming trump products.
	 * 
	 * Handles version specific trump products, calls
	 * {@link #checkForPersistentTrump(Event, ProductSummary, ProductSummary)}
	 * to handle "persistent" trump products.
	 * 
	 * VERSION SPECIFIC TRUMP
	 * 
	 * Version specific trump products include:
	 * - a link with relation "product" that is a product id urn.
	 * - a property "weight" that defines the new preferred weight.
	 * 
	 * Finds the associated product, resummarizes. If trump is deleted, product
	 * is associated as is. If trump is not deleted, set's preferred weight
	 * before reassociating.
	 * 
	 * Preconditions:
	 * <ul>
	 * <li>The "trump" type product must associate with the correct event on its
	 * own. The admin pages accomplish this by sending
	 * eventsource/eventsourcecode of the associated product. This means that no
	 * product without an eventsource/eventsourcecode property may be trumped.</li>
	 * </ul>
	 * 
	 * @param event
	 *            current event being updated.
	 * @param productSummary
	 *            product summary associated with event.
	 * @return updated event, or same event if not updated.
	 * @throws Exception
	 * @see {@link #checkForPersistentTrump(Event, ProductSummary, ProductSummary)}
	 */
	private Event checkForTrump(Event event, ProductSummary productSummary,
			ProductSummary prevSummary) throws Exception {
		if (event == null) {
			return event;
		}

		String type = productSummary.getId().getType();
		if (type.equals(TRUMP_PRODUCT_TYPE)) {
			// version specific trump
			ProductId trumpedId = null;
			ProductSummary trumpedSummary = null;
			if (productSummary.isDeleted()) {
				// deleting version specific trump
				// reset preferred weight of reference product
				// (is is a link in previous version of trump)
				trumpedId = getTrumpedProductId(prevSummary);
				if (trumpedId == null) {
					LOGGER.warning("Unable to process trump delete, "
							+ "missing 'product' link from previous version");
					return event;
				}
				trumpedSummary = getProductSummaryById(trumpedId);
				if (trumpedSummary == null) {
					// no matching product in index, possibly already deleted
					return event;
				}
				// resummarize product
				event = resummarizeProduct(event, trumpedSummary);
			} else {
				// updating product weight
				trumpedId = getTrumpedProductId(productSummary);
				Long weight = Long.valueOf(productSummary.getProperties().get(
						"weight"));
				if (trumpedId == null || weight == null) {
					LOGGER.warning("Invalid trump, "
							+ "missing 'product' link or 'weight' property");
					return event;
				}
				trumpedSummary = getProductSummaryById(trumpedId);
				if (trumpedSummary == null) {
					// no matching product in index, possibly already deleted
					LOGGER.info("Unable to process trump, " + "product '"
							+ trumpedId.toString() + "' not found");
					return event;
				}
				event = setSummaryWeight(event, trumpedSummary, weight);
			}
		} else {
			return checkForPersistentTrump(event, productSummary);
		}

		return event;
	}

	/**
	 * Check for, and handle persistent trump products.
	 *
	 * PERSISTENT TRUMP
	 *
	 * Persistent trump products include:
	 * - a type "trump-PRODUCT" where PRODUCT is the type of product receiving
	 * trump.
	 * - a property "trump-source" that is the source of product receiving trump.
	 * - a property "trump-code" that is the code of product receiving trump.
	 *
	 * Steps:
	 *
	 * 1) Find preferred persistent trump product for product being associated
	 * 		(may be a persistent trump product being associated)
	 * 2) If a non-trump product being associated,
	 * 		stop processing if not affected by trump.
	 * 3) set TRUMP_PREFERRED_WEIGHT on product referenced by preferred
	 * 		persistent trump product; resummarize any other product that has
	 * 		TRUMP_PREFERRED_WEIGHT.
	 *
	 * Preconditions:
	 * <ul>
	 * <li>The "trump" type product must associate with the correct event on its
	 * own. The admin pages accomplish this by sending
	 * eventsource/eventsourcecode of the associated product.</li>
	 * </ul>
	 *
	 * @param event
	 *            current event being updated.
	 * @param productSummary
	 *            product summary associated with event.
	 * @return updated event, or same event if not updated.
	 * @throws Exception
	 */
	private Event checkForPersistentTrump(final Event event,
			final ProductSummary productSummary) throws Exception {
		Event updatedEvent = event;

		// the type of product currently being indexed
		String type = productSummary.getType();
		// the "trump-TYPE" product type
		String persistentTrumpType = null;
		// whether productSummary is a persistent trump product
		boolean associatingTrump = false;
		// the product source receiving trump
		String trumpSource = null;
		// the product type receiving trump
		String trumpType = null;
		// the product code receiving trump
		String trumpCode = null;

		// determine persistentTrumpType and trumpType
		if (type.startsWith(PERSISTENT_TRUMP_PREFIX)) {
			// incoming trump product
			persistentTrumpType = type;
			trumpType = type.replace(PERSISTENT_TRUMP_PREFIX, "");
			associatingTrump = true;
			// always set persistent trump preferred weight to 1,
			// so most recent updateTime is most preferred
			updatedEvent = setSummaryWeight(updatedEvent, productSummary, 1L);
		} else {
			// incoming product, possibly affected by existing trump
			persistentTrumpType = PERSISTENT_TRUMP_PREFIX + type;
			trumpType = type;
		}
		
		// find active persistent trump product for type
		ProductSummary persistentTrump = updatedEvent.getPreferredProduct(
				persistentTrumpType);
		if (persistentTrump != null) {
			trumpSource = persistentTrump.getProperties().get("trump-source");
			trumpCode = persistentTrump.getProperties().get("trump-code");
		}

		// if a non-trump product is coming in,
		// only continue processing if it is affected by persistentTrump.
		// (otherwise weights should already be set)
		if (!associatingTrump &&
				!(productSummary.getSource().equals(trumpSource)
				&& productSummary.getCode().equals(trumpCode))) {
			// not affected by trump
			return event;
		}

		// update products affected by trump
		List<ProductSummary> products = updatedEvent.getProducts(trumpType);
		if (products != null) {
			for (ProductSummary summary : products) {
				if (summary.getSource().equals(trumpSource)
						&& summary.getCode().equals(trumpCode)) {
					// add trump to product
					updatedEvent = setSummaryWeight(updatedEvent, summary,
							TRUMP_PREFERRED_WEIGHT);
				} else if (summary.getPreferredWeight() == TRUMP_PREFERRED_WEIGHT) {
					// remove trump from previously trumped product.
					updatedEvent = resummarizeProduct(updatedEvent, summary);
				}
			}			
		}
		// return updated event
		return updatedEvent;
	}

	/**
	 * Get the productId referred to by a trump product.
	 * 
	 * @param trumpSummary
	 *            trump product with reference to product id.
	 * @return product id, or null if unable to parse product id.
	 */
	protected ProductId getTrumpedProductId(final ProductSummary trumpSummary) {
		try {
			// use 'product' link from previous summary
			ProductId trumpedId = ProductId.parse(trumpSummary.getLinks().get(
					"product").get(0).toString());
			return trumpedId;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get a product summary object using its product id.
	 * 
	 * @param id
	 *            id to find.
	 * @return matching product summary or null.
	 * @throws Exception
	 */
	protected ProductSummary getProductSummaryById(final ProductId id)
			throws Exception {
		ProductIndexQuery query = new ProductIndexQuery();
		query.getProductIds().add(id);
		List<ProductSummary> summaries = productIndex.getProducts(query);
		if (summaries.size() > 0) {
			return summaries.get(0);
		}
		return null;
	}

	/**
	 * Update a product summary weight
	 * 
	 * @param event
	 *            the event.
	 * @param summary
	 *            the summary.
	 * @param preferredWeight
	 *            the weight to set.
	 * @return event with updated summary.
	 * @throws Exception
	 */
	protected Event setSummaryWeight(Event event, ProductSummary summary,
			final Long preferredWeight) throws Exception {
		if (summary.getPreferredWeight() == preferredWeight) {
			// already set
			return event;
		}

		LOGGER.info("Setting product preferred weight "
				+ summary.getId().toString() + ", weight "
				+ summary.getPreferredWeight() + " (old) => " + preferredWeight
				+ " (new)");

		// remove existing summary from event
		event = productIndex.removeAssociation(event, summary);
		productIndex.removeProductSummary(summary);
		// set custom weight
		summary.setPreferredWeight(preferredWeight);
		// add updated summary to event
		summary = productIndex.addProductSummary(summary);
		event = productIndex.addAssociation(event, summary);
		// return updated event
		return event;
	}

	/**
	 * Resummarize a product within an event.
	 * 
	 * @param event
	 *            the event.
	 * @param summary
	 *            the summary.
	 * @return event with updated summary.
	 * @throws Exception
	 */
	protected Event resummarizeProduct(final Event event,
			final ProductSummary summary) throws Exception {
		Event updatedEvent = null;
		ProductSummary updatedSummary = null;
		// remove existing summary from event
		updatedEvent = productIndex.removeAssociation(event, summary);
		productIndex.removeProductSummary(summary);
		// use module to summarize original product
		Product product = productStorage.getProduct(summary.getId());
		if (product == null) {
			throw new Exception("Unable to resummarize product, "
					+ "product not in storage " + summary.getId().toString());
		}
		updatedSummary = getModule(product).getProductSummary(product);
		LOGGER.info("Resummarizing product " + summary.getId().toString()
				+ ", weight " + summary.getPreferredWeight() + " (old) => "
				+ updatedSummary.getPreferredWeight() + " (new)");
		// add updated summary to event
		updatedSummary = productIndex.addProductSummary(updatedSummary);
		updatedEvent = productIndex
				.addAssociation(updatedEvent, updatedSummary);
		// return updated event
		return updatedEvent;
	}

	/**
	 * Check for event splits (and split them if needed).
	 * 
	 * @param summary
	 *            the summary the indexer is currently processing.
	 * @param originalEvent
	 *            the event before the indexer made any changes.
	 * @param updatedEvent
	 *            the event after the indexer made any changes.
	 * @return List of changes made during this method.
	 * @throws Exception
	 */
	protected synchronized List<IndexerChange> checkForEventSplits(
			final ProductSummary summary, final Event originalEvent,
			final Event updatedEvent) throws Exception {
		List<IndexerChange> changes = new ArrayList<IndexerChange>();

		// save reference so we can check later if this has changed
		Event splitEvent = updatedEvent;

		// ## 1) Split event into sub events
		Map<String, Event> subEvents = splitEvent.getSubEvents();
		if (subEvents.size() == 1) {
			// still only one event, cannot split
			return changes;
		}

		// the original eventid before the indexer started processing this
		// update (may have already changed in updatedEvent).
		String originalEventId = originalEvent.getEventId();

		// ## 2) See if sub events still associate
		// list of events that actually are split
		List<Event> alreadySplit = new ArrayList<Event>();

		// see how sub events associate compared to this event (since the
		// original event is the one that should be "UPDATED")
		Event originalSubEvent = subEvents.remove(originalEventId);

		Iterator<String> subEventsIter = new ArrayList<String>(
				subEvents.keySet()).iterator();
		while (subEventsIter.hasNext()) {
			String nextEventId = subEventsIter.next();
			Event nextEvent = subEvents.get(nextEventId);

			if (!originalSubEvent.isAssociated(nextEvent, associator)) {
				// not associated, so split
				splitEvent = splitEvents(splitEvent, nextEvent);

				// see if associated to any that already split
				Iterator<Event> alreadySplitIter = alreadySplit.iterator();
				while (alreadySplitIter.hasNext()) {
					Event alreadySplitEvent = alreadySplitIter.next();
					if (alreadySplitEvent.isAssociated(nextEvent, associator)) {
						// need to merge with event that already split

						// will need reference to alreadySplitEvent, so keep
						// reference to merged event in nextEvent
						nextEvent = mergeEvents(alreadySplitEvent, nextEvent);
						// remove original already split
						alreadySplit.remove(alreadySplitEvent);
						// add merged nextEvent
						alreadySplit.add(nextEvent);
						// signal that nextEvent was already added to
						// alreadySplit
						nextEvent = null;
						// associated, and one at a time, so stop checking
						break;
					}
				}

				if (nextEvent != null) {
					// wasn't merged with an already split event
					alreadySplit.add(nextEvent);
				}
			}
		}
		if (alreadySplit.size() == 0) {
			// didn't split any events...
			return changes;
		}

		// ## 3) Build list of Indexer changes that actually happened.
		String splitEventId = splitEvent.getEventId();

		if (!originalEventId.equalsIgnoreCase(splitEventId)) {
			LOGGER.warning("[" + getName() + "] eventid (" + splitEventId
					+ ") no longer matches original (" + originalEventId
					+ ") after split.");
		}

		// first notify about updated original event
		changes.add(new IndexerChange(
				(splitEvent.isDeleted() ? IndexerChange.EVENT_DELETED
						: IndexerChange.EVENT_UPDATED), originalEvent,
				splitEvent));

		// now notify about all events that split from original event
		Iterator<Event> alreadySplitIter = alreadySplit.iterator();
		while (alreadySplitIter.hasNext()) {
			Event alreadySplitEvent = alreadySplitIter.next();
			changes.add(new IndexerChange(IndexerChange.EVENT_SPLIT, null,
					alreadySplitEvent));
		}

		// done
		return changes;
	}

	/**
	 * Removes the leaf event (and all its products) from the root event. This
	 * method modifies the runtime objects as well as updating the index DB.
	 * 
	 * @param root
	 *            The root event from which all leaf products will be removed
	 * @param leaf
	 *            The event (with products) that will be removed from the root
	 * @return copy of root without the products that have been removed. The
	 *         indexId property of leaf is updated to its new value.
	 * @throws Exception
	 */
	protected synchronized Event splitEvents(final Event root, final Event leaf)
			throws Exception {
		Event updated = root;
		Iterator<ProductSummary> leafProducts = leaf.getProductList()
				.iterator();

		// assign leaf indexId by reference
		Event insertedLeafEvent = productIndex.addEvent(leaf);
		leaf.setIndexId(insertedLeafEvent.getIndexId());

		while (leafProducts.hasNext()) {
			ProductSummary product = leafProducts.next();
			if (updated != null) {
				updated = productIndex.removeAssociation(updated, product);
			}
			// leaf already has the product in its list, not returning anyways.
			productIndex.addAssociation(leaf, product);
		}

		return updated;
	}

	/**
	 * Merges the child event (and all its products) into the target event. If
	 * the child event attempts to merge in a product that is the same as one
	 * already associated to the target event, the child version of the product
	 * takes precedence. Note: This only applies when the target and child
	 * product have the same type, code, source, and update time; i.e. the
	 * products are duplicates. This method modifies the runtime objects as well
	 * as the index DB. The child event is then deleted.
	 * 
	 * @param target
	 *            The target event into which the child is merged.
	 * @param child
	 *            The child event to be merged into the target.
	 * @throws Exception
	 */
	protected synchronized Event mergeEvents(final Event target,
			final Event child) throws Exception {
		Iterator<ProductSummary> childProducts = child.getProductList()
				.iterator();
		Event updatedEvent = target;
		Event updatedChild = child;

		while (childProducts.hasNext()) {
			ProductSummary product = childProducts.next();
			productIndex.removeAssociation(child, product);
			updatedChild = productIndex
					.removeAssociation(updatedChild, product);
			updatedEvent = productIndex.addAssociation(updatedEvent, product);
		}

		productIndex.removeEvent(updatedChild);

		return updatedEvent;
	}

	/**
	 * Check and merge any nearby events or previously unassociated products
	 * that now associate.
	 * 
	 * @param summary
	 *            the summary currently being processed by the indexer.
	 * @param originalEvent
	 *            the event before any changes.
	 * @param updatedEvent
	 *            the event after the summary was associated.
	 * @return list of any merge type changes.
	 * @throws Exception
	 */
	protected synchronized List<IndexerChange> checkForEventMerges(
			final ProductSummary summary, final Event originalEvent,
			final Event updatedEvent) throws Exception {
		List<IndexerChange> changes = new ArrayList<IndexerChange>();
		Event mergedEvent = updatedEvent;

		// ## 1) Check for nearby events
		if (originalEvent != null) {
			// only if the event was not just created, because otherwise this
			// product would have associated to an existing event

			// build the query
			EventSummary mergedSummary = mergedEvent.getEventSummary();
			ProductIndexQuery nearbyEvents = associator.getLocationQuery(
					mergedSummary.getTime(), mergedSummary.getLatitude(),
					mergedSummary.getLongitude());
			if (associateUsingCurrentProducts && nearbyEvents != null) {
				nearbyEvents.setResultType(ProductIndexQuery.RESULT_TYPE_CURRENT);
			}

			LOGGER.finer("[" + getName() + "] searching for nearby events");
			// do the search
			Iterator<Event> events = productIndex.getEvents(nearbyEvents)
					.iterator();
			LOGGER.finer("[" + getName()
					+ "] search for nearby events complete");
			while (events.hasNext()) {
				Event foundEvent = events.next();
				if (foundEvent.getIndexId().equals(mergedEvent.getIndexId())) {
					// found the event currently being checked for merges,
					// ignore
					continue;
				} else if (mergedEvent.isAssociated(foundEvent, associator)) {
					// event associates to another event, merge them
					mergedEvent = mergeEvents(mergedEvent, foundEvent);
					changes.add(new IndexerChange(IndexerChange.EVENT_MERGED,
							foundEvent, null));
				}
			}
		}

		// ## 2) Now look for products that were previously unassociated, but
		// that can now associate because of the event id of the incoming
		// product
		// (if the event already had this id, these products would already be
		// associated...)
		String source = summary.getEventSource();
		String sourceCode = summary.getEventSourceCode();
		// without this check, all unassociated products would be added...(BAD)
		if (source != null && sourceCode != null) {
			// build the query
			ProductIndexQuery unassociatedProducts = new ProductIndexQuery();
			unassociatedProducts.setEventSource(source);
			unassociatedProducts.setEventSourceCode(sourceCode);

			// run the query
			LOGGER.finer("[" + getName()
					+ "] searching for unassociated products");
			Iterator<ProductSummary> summaries = productIndex
					.getUnassociatedProducts(unassociatedProducts).iterator();
			LOGGER.finer("[" + getName()
					+ "] search for unassociated products complete");
			// add associations
			while (summaries.hasNext()) {
				mergedEvent = productIndex.addAssociation(mergedEvent,
						summaries.next());
			}
		}

		// ## 2.5) Check for merge by associate product
		// only need to check when associate product is first added
		// THIS IMPLEMENTATION ASSUMES: both events exist when associate product
		// is sent. Search for existing event (during getPrevEvent) does not
		// search associate products othereventsource or othereventsourcecode
		// properties.
		if (summary.getType().equals(Event.ASSOCIATE_PRODUCT_TYPE)
				&& !summary.isDeleted()) {
			String otherEventSource = summary.getProperties().get(
					Event.OTHEREVENTSOURCE_PROPERTY);
			String otherEventSourceCode = summary.getProperties().get(
					Event.OTHEREVENTSOURCECODE_PROPERTY);

			if (otherEventSource == null || otherEventSourceCode == null) {
				LOGGER.warning(Event.ASSOCIATE_PRODUCT_TYPE
						+ " product without " + Event.OTHEREVENTSOURCE_PROPERTY
						+ " or " + Event.OTHEREVENTSOURCECODE_PROPERTY
						+ " properties, ignoring");
			} else {
				// search for associated event
				ProductIndexQuery associateQuery = new ProductIndexQuery();
				associateQuery.setEventSource(otherEventSource);
				associateQuery.setEventSourceCode(otherEventSourceCode);

				if (associateUsingCurrentProducts) {
					associateQuery.setResultType(ProductIndexQuery.RESULT_TYPE_CURRENT);
				}

				LOGGER.finer("[" + getName()
						+ "] searching for associated event");
				// do the search
				Iterator<Event> events = productIndex.getEvents(associateQuery)
						.iterator();
				LOGGER.finer("[" + getName()
						+ "] search for associated event complete");
				while (events.hasNext()) {
					Event foundEvent = events.next();
					if (foundEvent.getIndexId()
							.equals(mergedEvent.getIndexId())) {
						// found the event currently being checked for merges,
						// ignore
						continue;
					} else if (mergedEvent.isAssociated(foundEvent)) {
						// event associates to another event, merge them
						mergedEvent = mergeEvents(mergedEvent, foundEvent);
						changes.add(new IndexerChange(
								IndexerChange.EVENT_MERGED, foundEvent, null));
					}
				}
			}
		}

		// ## 4) Check if the event has changed during this method
		if (mergedEvent != updatedEvent) {
			// something has changed, so add an IndexerChange
			if (originalEvent == null) {
				// no previous event, it was added (although unassociated
				// products were associated)
				changes.add(new IndexerChange(IndexerChange.EVENT_ADDED, null,
						mergedEvent));
			} else {
				// may have merged with other events, or associated unassociated
				// products. Other changes represent the merges, so just
				// indicate update/delete.
				changes.add(new IndexerChange(
						(mergedEvent.isDeleted() ? IndexerChange.EVENT_DELETED
								: IndexerChange.EVENT_UPDATED), originalEvent,
						mergedEvent));
			}
		}

		return changes;
	}

	protected synchronized ProductSummary getPrevProductVersion(
			ProductSummary summary) throws Exception {
		ProductSummary prevSummary = null;
		List<ProductSummary> candidateSummaries = null;
		ProductIndexQuery query = new ProductIndexQuery();

		// Set type, code and source
		query.setProductType(summary.getType());
		query.setProductCode(summary.getCode());
		query.setProductSource(summary.getSource());

		// Query the index (first look for associated products)
		candidateSummaries = productIndex.getProducts(query);

		if (candidateSummaries == null || candidateSummaries.size() == 0) {
			// No summaries found associated to events, try unassociated.
			candidateSummaries = productIndex.getUnassociatedProducts(query);
		}

		if (candidateSummaries != null && candidateSummaries.size() > 0) {
			prevSummary = candidateSummaries.get(0);
			if (candidateSummaries.size() != 1) {
				LOGGER.warning(
						"[" + getName() + "] " + summary.getId().toString() +
						": More than one existing summary is claiming to be most recent.");
			}
		}
		return prevSummary;
	}

	/**
	 * Associate products are processed during
	 * {@link #checkForEventMerges(ProductSummary, Event, Event)} and are
	 * ignored during this method.
	 * 
	 * @see Associator#getSearchRequest(ProductSummary)
	 * @see Associator#chooseEvent(List, ProductSummary)
	 * 
	 * @param summary
	 * @return Event to which a productSummary is associated, or null if not
	 *         found.
	 * @throws Exception
	 */
	protected synchronized Event getPrevEvent(ProductSummary summary)
			throws Exception {
		return getPrevEvent(summary, false);
	}

	/**
	 * Find an existing event that summary should associate with.
	 * 
	 * @param summary the previous event.
	 * @param associating whether associating (vs archiving).
	 * @return previous event, or null if none found.
	 * @throws Exception
	 */
	protected synchronized Event getPrevEvent(ProductSummary summary,
			boolean associating) throws Exception {
		Event prevEvent = null;
		List<Event> candidateEvents = null;

		SearchRequest request = associator.getSearchRequest(summary);

		if (associating && associateUsingCurrentProducts) {
			for (SearchQuery query : request.getQueries()) {
				query.getProductIndexQuery().setResultType(
						ProductIndexQuery.RESULT_TYPE_CURRENT);
			}
		}

		SearchResponse response = search(request);
		if (response != null) {
			candidateEvents = response.getEvents();
		}

		if (candidateEvents != null && candidateEvents.size() > 0) {
			// Found some events. Find best match.
			prevEvent = associator.chooseEvent(candidateEvents, summary);
		}

		return prevEvent;
	}

	/*
	 * protected IndexerEvent createIndexerEvent(ProductSummary prevSummary,
	 * Event prevEvent, ProductSummary summary, Event event) { IndexerType type
	 * = null; IndexerEvent indexerEvent = new IndexerEvent(this);
	 * 
	 * // ---------------------------------- // Determine the type if
	 * IndexerEvent // ----------------------------------
	 * 
	 * if (summary.getStatus() == Product.STATUS_DELETE) { type =
	 * IndexerEvent.PRODUCT_DELETED; if (event != null) { // Since we have an
	 * event, this is now an EVENT_UPDATED type type =
	 * IndexerEvent.EVENT_UPDATED;
	 * 
	 * // Check if all products on event are deleted. if
	 * (event.getProductList().size() == 0) { type = IndexerEvent.EVENT_DELETED;
	 * } } } else { // Product was not a "DELETE" status. Must be an added or
	 * updated. if (prevEvent == null && event != null) { type =
	 * IndexerEvent.EVENT_ADDED; } else if (prevEvent != null && event != null)
	 * { type = IndexerEvent.EVENT_UPDATED; } else if (prevSummary == null &&
	 * summary != null) { type = IndexerEvent.PRODUCT_ADDED; } else if
	 * (prevSummary != null && summary != null) { type =
	 * IndexerEvent.PRODUCT_UPDATED; }
	 * 
	 * if (summary == null) { // Not sure how this happens.
	 * LOGGER.warning("Trying to notify of a null summary."); } }
	 * 
	 * // Set parameters indexerEvent.setEventType(type);
	 * indexerEvent.setOldEvent(prevEvent); indexerEvent.setSummary(summary);
	 * indexerEvent.setEvent(event);
	 * 
	 * return indexerEvent; }
	 */
	/**
	 * Loads parent, specific, and dependent configurations; in that order.
	 */
	@Override
	public synchronized void configure(Config config) throws Exception {
		// -- Load parent configurations -- //
		super.configure(config);

		// reads properties from same config section
		defaultModule.getSignatureVerifier().configure(config);

		// -- Load specific configurations -- //
		String associatorName = config.getProperty(ASSOCIATOR_CONFIG_PROPERTY);
		if (associatorName != null) {
			associator = (Associator) Config.getConfig().getObject(
					associatorName);
		}

		String storageName = config.getProperty(STORAGE_CONFIG_PROPERTY);
		String storageDirectory = config
				.getProperty(STORAGE_DIRECTORY_CONFIG_PROPERTY);
		if (storageName != null) {
			LOGGER.config("[" + getName() + "] loading ProductStorage '"
					+ storageName + "'");
			productStorage = (ProductStorage) Config.getConfig().getObject(
					storageName);
			if (productStorage == null) {
				throw new ConfigurationException("[" + getName()
						+ "] ProductStorage '" + storageName
						+ "' is not properly configured");
			}
		} else if (storageDirectory != null) {
			LOGGER.config("[" + getName() + "] using storage directory '"
					+ storageDirectory + "'");
			productStorage = new FileProductStorage(new File(storageDirectory));
		} else {
			productStorage.configure(config);
		}

		String indexName = config.getProperty(INDEX_CONFIG_PROPERTY);
		String indexFileName = config.getProperty(INDEXFILE_CONFIG_PROPERTY);
		if (indexName != null) {
			LOGGER.config("[" + getName() + "] loading ProductIndex '"
					+ indexName + "'");
			productIndex = (ProductIndex) Config.getConfig().getObject(
					indexName);
			if (productIndex == null) {
				throw new ConfigurationException("[" + getName()
						+ "] ProductIndex '" + indexName
						+ "' is not properly configured");
			}
		} else if (indexFileName != null) {
			LOGGER.config("[" + getName() + "] using sqlite product index '"
					+ indexFileName + "'");
			productIndex = new JDBCProductIndex(indexFileName);
		} else {
			productIndex.configure(config);
		}

		// How often to check for expired products
		String archivePolicy = config
				.getProperty(INDEX_ARCHIVE_POLICY_PROPERTY);
		if (archivePolicy != null) {
			Iterator<String> iter = StringUtils.split(archivePolicy, ",")
					.iterator();
			while (iter.hasNext()) {
				String policyName = iter.next();

				LOGGER.config("[" + getName() + "] loading ArchivePolicy '"
						+ policyName + "'");
				ArchivePolicy policy = (ArchivePolicy) Config.getConfig()
						.getObject(policyName);
				if (policy == null) {
					throw new ConfigurationException("[" + getName()
							+ "] ArchivePolicy '" + policyName
							+ "' is not configured properly");
				}

				// Only use archive policies that are valid
				if (policy.isValidPolicy()) {
					archivePolicies.add(policy);
				} else {
					LOGGER.warning("[" + getName() + "] ArchivePolicy '"
							+ policyName + "' is not valid");
				}
			}
		}

		// How often should the archive policies be run
		String buffer = config.getProperty(INDEX_ARCHIVE_INTERVAL_PROPERTY);
		if (buffer != null) {
			archiveInterval = Long.parseLong(buffer);
		} else {
			// Use default age
			archiveInterval = INDEX_ARCHIVE_INTERVAL_DEFAULT;
		}
		LOGGER.config("[" + getName() + "] archive interval is '"
				+ archiveInterval + "'");

		// Always use at least a default indexer module
		String moduleNames = config.getProperty(MODULES_CONFIG_PROPERTY);
		if (moduleNames != null) {
			Iterator<String> modules = StringUtils.split(moduleNames, ",")
					.iterator();
			while (modules.hasNext()) {
				String moduleName = modules.next();
				if (moduleName.equals("")) {
					continue;
				}
				LOGGER.config("[" + getName() + "] loading indexer module '"
						+ moduleName + "'");
				IndexerModule module = (IndexerModule) Config.getConfig()
						.getObject(moduleName);
				if (module == null) {
					throw new ConfigurationException("[" + getName()
							+ "] indexer module '" + moduleName
							+ "' is not configured properly");
				}
				addModule(module);
			}
		} else {
			LOGGER.config("[" + getName() + "] no indexer modules configured.");
		}

		String listenerNames = config.getProperty(LISTENERS_CONFIG_PROPERTY);
		if (listenerNames != null) {
			Iterator<String> listeners = StringUtils.split(listenerNames, ",")
					.iterator();
			while (listeners.hasNext()) {
				String listenerName = listeners.next();
				if (listenerName.equals("")) {
					continue;
				}
				LOGGER.config("[" + getName() + "] loading indexer listener '"
						+ listenerName + "'");
				IndexerListener listener = (IndexerListener) Config.getConfig()
						.getObject(listenerName);
				if (listener == null) {
					throw new ConfigurationException("[" + getName()
							+ "] indexer listener '" + listenerName
							+ "' is not configured properly");
				}
				addListener(listener);
			}
		} else {
			LOGGER.config("[" + getName()
					+ "] no indexer listeners configured.");
		}

		String localRegions = config.getProperty(LOCAL_REGIONS_PROPERTY,
				DEFAULT_LOCAL_REGIONS);
		this.localRegionsFile = new File(localRegions);
		LOGGER.config("[" + getName() + "] Local regions file: "
				+ this.localRegionsFile);

		String enableSearch = config.getProperty(ENABLE_SEARCH_PROPERTY,
				DEFAULT_ENABLE_SEARCH);
		if (Boolean.valueOf(enableSearch)) {
			searchSocket = new SearchServerSocket();
			searchSocket.setIndex(this);

			int searchPort = Integer.parseInt(config.getProperty(
					SEARCH_PORT_PROPERTY, DEFAULT_SEARCH_PORT));
			searchSocket.setPort(searchPort);

			int searchThreads = Integer.parseInt(config.getProperty(
					SEARCH_THREADS_PROPERTY, DEFAULT_SEARCH_THREADS));
			searchSocket.setThreads(searchThreads);

			LOGGER.config("[" + getName()
					+ "] SearchServerSocket running at localhost:" + searchPort
					+ ", with " + searchThreads + " threads");
		}
		// -- Load dependent configurations -- //

		associateUsingCurrentProducts = Boolean.valueOf(
				config.getProperty(ASSOCIATE_USING_CURRENT_PRODUCTS_PROPERTY,
				DEFAULT_ASSOCIATE_USING_CURRENT_PRODUCTS));
		LOGGER.config("[" + getName() + "] associateUsingCurrentProducts = "
				+ associateUsingCurrentProducts);
	}

	/**
	 * Shuts down the Indexer. The parent shutdown method is called and then all
	 * executor services (from listeners) are shutdown in sequence.
	 */
	@Override
	public synchronized void shutdown() throws Exception {
		// -- Shut down dependent processes -- //
		try {
			productIndex.shutdown();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "[" + getName()
					+ "] exception shutting down product index", e);
		}
		productStorage.shutdown();

		// ExecutorServices tied to known listeners.
		Iterator<IndexerListener> iter = listeners.keySet().iterator();
		while (iter.hasNext()) {
			IndexerListener listener = iter.next();
			try {
				listeners.get(listener).shutdown();
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "[" + getName()
						+ "] exception shutting down listener executor", e);
			}
			if (listener instanceof Configurable) {
				try {
					((Configurable) listener).shutdown();
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "[" + getName()
							+ "] exception shutting down listener", e);
				}
			}
		}

		Iterator<IndexerModule> modules = this.modules.iterator();
		while (modules.hasNext()) {
			IndexerModule module = modules.next();
			if (module instanceof Configurable) {
				try {
					((Configurable) module).shutdown();
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "[" + getName()
							+ "] exception shutting down module", e);
				}
			}
		}
		// -- Shut down our own specific processes -- //

		// Shut down our timers if they exist
		if (archiveTask != null) {
			archiveTask.cancel();
			archiveTask = null;
		}
		if (archiveTimer != null) {
			archiveTimer.cancel();
			archiveTimer = null;
		}

		if (searchSocket != null) {
			searchSocket.shutdown();
		}
		// -- Call parent shutdown method -- //
		super.shutdown();
	}

	/**
	 * Starts up the necessary parent, specific, and dependent processes, in
	 * that order.
	 */
	@Override
	public synchronized void startup() throws Exception {
		// -- Call parent startup method -- //
		super.startup();

		// -- Start up our own specific processes -- //

		// -- Start dependent processes -- //
		// ExecutorServices tied to known listeners.
		Iterator<IndexerListener> iter = listeners.keySet().iterator();
		while (iter.hasNext()) {
			IndexerListener listener = iter.next();
			if (listener instanceof Configurable) {
				((Configurable) listener).startup();
			}
		}

		// configure regions factory before modules
		ANSSRegionsFactory factory = ANSSRegionsFactory.getFactory(false);
		factory.setLocalRegions(localRegionsFile);
		factory.startup();

		Iterator<IndexerModule> modules = this.modules.iterator();
		while (modules.hasNext()) {
			IndexerModule module = modules.next();
			if (module instanceof Configurable) {
				((Configurable) module).startup();
			}
		}

		// ProductIndex
		productStorage.startup();
		productIndex.startup();

		// Cleanup thread to purge old products
		if (archivePolicies.size() > 0) {
			// Instantiate a timer object
			archiveTimer = new Timer();
			// Instantiate the task object
			archiveTask = new TimerTask() {
				public void run() {
					try {
						int[] counts = purgeExpiredProducts();
						LOGGER.info(String
								.format("["
										+ getName()
										+ "] purged %d expired events and %d expired unassociated products.",
										counts[0], counts[1]));
					} catch (Exception ex) {
						LOGGER.log(Level.WARNING, "[" + getName()
								+ "] indexer cleanup thread threw exception",
								ex);
					}
				}
			};
			// Run archiver immediately at startup, then at regular intervals
			archiveTimer.schedule(archiveTask, 0L, archiveInterval);
		}

		if (searchSocket != null) {
			searchSocket.startup();
		}
	}

	/**
	 * Checks the index for content that match a configured archive policy.
	 * Events are checked first and matched events are removed along with all
	 * their products. Listeners are notified of each archived event with an
	 * EVENT_ARCHIVED type. Unassociated products are checked next, matched
	 * unassociated products are archived and listeners are notified with
	 * PRODUCT_ARCHIVE type.
	 * 
	 * Note: Product "age" is determined by when the earthquake for that product
	 * occurred and does not reflect how long the product has actually been in
	 * the index.
	 * 
	 * @see #archivePolicies
	 */
	public synchronized int[] purgeExpiredProducts() throws Exception {
		int[] counts = { 0, 0 };
		ProductIndexQuery query = null;
		ArchivePolicy policy = null;

		if (isDisableArchive()) {
			LOGGER.info("Archiving disabled");
			return counts;
		}

		for (int i = 0; i < archivePolicies.size(); i++) {
			policy = archivePolicies.get(i);
			query = policy.getIndexQuery();

			if (!(policy instanceof ProductArchivePolicy)) {
				// -- Purge expired events for this policy -- //
				LOGGER.fine("[" + getName()
						+ "] running event archive policy (" + policy.getName()
						+ ")");
				try {
					// Get a list of those events
					List<Event> expiredEvents = productIndex.getEvents(query);

					// Loop over list of expired events and remove each one
					Iterator<Event> eventIter = expiredEvents.iterator();
					while (eventIter.hasNext()) {
						Event event = eventIter.next();

						LOGGER.info("[" + getName() + "] archiving event "
								+ event.getEventId());
						event.log(LOGGER);

						productIndex.beginTransaction();
						try {
							removeEvent(event);

							// Notify of the event archived
							IndexerEvent notification = new IndexerEvent(this);
							notification.setSummary(null);
							notification.addIndexerChange(new IndexerChange(
									IndexerChange.EVENT_ARCHIVED, event, null));
							notifyListeners(notification);

							++counts[0];
							productIndex.commitTransaction();
						} catch (Exception e) {
							LOGGER.log(Level.WARNING, "[" + getName()
									+ "] exception archiving event "
									+ event.getEventId() + ", rolling back", e);
							productIndex.rollbackTransaction();
						}
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "[" + getName()
							+ "] exception running event archive policy ("
							+ policy.getName() + ") ", e);
				}
			}

			if (policy instanceof ProductArchivePolicy) {
				ProductArchivePolicy productPolicy = (ProductArchivePolicy) policy;

				// -- Purge expired products for this policy -- //
				LOGGER.fine("[" + getName()
						+ "] running product archive policy ("
						+ policy.getName() + ")");

				try {
					// Get a list of those products
					List<ProductSummary> expiredProducts;

					if (productPolicy.isOnlyUnassociated()) {
						expiredProducts = productIndex
								.getUnassociatedProducts(query);
					} else {
						expiredProducts = productIndex.getProducts(query);
					}

					// Loop over list of expired products and remove each one
					Iterator<ProductSummary> productIter = expiredProducts
							.iterator();
					while (productIter.hasNext()) {
						ProductSummary product = productIter.next();

						LOGGER.info("[" + getName() + "] archiving product "
								+ product.getId().toString());
						productIndex.beginTransaction();
						try {
							removeSummary(product);
	
							// Notify of the product archived
							IndexerEvent notification = new IndexerEvent(this);
							notification.setSummary(product);
							notification.addIndexerChange(new IndexerChange(
									IndexerChange.PRODUCT_ARCHIVED, null, null));
							notifyListeners(notification);
	
							++counts[1];
							productIndex.commitTransaction();
						} catch (Exception e) {
							LOGGER.log(Level.WARNING, "[" + getName()
									+ "] exception archiving event "
									+ product.getId().toString() + ", rolling back", e);
							productIndex.rollbackTransaction();
						}
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "[" + getName()
							+ "] exception running product archive policy ("
							+ policy.getName() + ")", e);
				}

			}
		}

		return counts;
	}

	/**
	 * Removes the given event from the Indexer ProductIndex and ProductStorage.
	 * 
	 * @param event
	 * @throws Exception
	 *             If errors occur while removing the event
	 */
	protected synchronized void removeEvent(Event event) throws Exception {
		// Removing an "event" from storage is really just removing all its
		// associated products
		List<ProductSummary> summaries = event.getAllProductList();
		Iterator<ProductSummary> summaryIter = summaries.iterator();
		while (summaryIter.hasNext()) {
			ProductSummary summary = summaryIter.next();
			// Remove product from storage
			productStorage.removeProduct(summary.getId());
			// Remove product summary from index
			productIndex.removeProductSummary(summary);
		}

		// Remove from index
		productIndex.removeEvent(event);
	}

	/**
	 * Removes the given summary from the Indexer ProductIndex and
	 * ProductStorage.
	 * 
	 * @param summary
	 * @throws Exception
	 *             If errors occur while removing the summary
	 */
	protected synchronized void removeSummary(ProductSummary summary)
			throws Exception {

		Event event = getPrevEvent(summary);
		if (event != null) {
			List<ProductSummary> eventProducts = event.getAllProductList();
			if (eventProducts != null && eventProducts.size() == 1
					&& eventProducts.get(0).getId().equals(summary.getId())) {
				// last product for the event
				removeEvent(event);
				// product is already removed by removeEvent
				return;
			}
		}

		// Remove product from storage
		productStorage.removeProduct(summary.getId());
		// Remove product summary from index
		productIndex.removeProductSummary(summary);

		// if product was associated to event need to update index
		if (event != null) {
			// remove the product from the event
			event.removeProduct(summary);

			// update event table
			ArrayList<Event> events = new ArrayList<Event>();
			events.add(event);
			productIndex.eventsUpdated(events);
		}
	}

	/**
	 * Tries to create an event based on information in the given summary. If
	 * successful, the summary is associated to the newly created event. Note:
	 * The summary must have been externally added to the ProductIndex before
	 * this method can be called.
	 * 
	 * A product summary must have non-null (id) source and code, (location)
	 * latitude and longitude, and (time) time, in order to have the minimum 
	 * properties required to create a new event.
	 * 
	 * @param summary
	 *            The product summary serving as the basis for the new event.
	 * @return The event that is created, added and associated or null if the
	 *         given summary can not be used to create a new event.
	 * @throws Exception
	 *             If the ProductIndex.addEvent throws an exception or if the
	 *             ProductIndex.addAssociation throws an exception. This may
	 *             happen if this method is called before the summary is added
	 *             to the ProductIndex.
	 */
	private synchronized Event createEvent(ProductSummary summary)
			throws Exception {
		if (Event.productHasOriginProperties(summary)) {
			Event event = productIndex.addEvent(new Event());
			return productIndex.addAssociation(event, summary);
		} else {
			return null;
		}
	}

	/**
	 * Search for products in this index.
	 * 
	 * @param request
	 *            the search request.
	 * @return the search response.
	 * @throws Exception
	 */
	public synchronized SearchResponse search(SearchRequest request)
			throws Exception {
		SearchResponse response = new SearchResponse();

		// Execute each query
		Iterator<SearchQuery> iter = request.getQueries().iterator();
		while (iter.hasNext()) {
			SearchQuery query = iter.next();

			if (query instanceof EventsSummaryQuery) {
				List<EventSummary> eventSummaries = new LinkedList<EventSummary>();
				Iterator<Event> events = productIndex.getEvents(
						query.getProductIndexQuery()).iterator();
				// convert events to event summaries
				while (events.hasNext()) {
					Event event = events.next();
					eventSummaries.add(event.getEventSummary());
				}
				((EventsSummaryQuery) query).setResult(eventSummaries);
			}

			else if (query instanceof EventDetailQuery) {
				List<Event> events = productIndex.getEvents(query
						.getProductIndexQuery());
				((EventDetailQuery) query).setResult(events);
			}

			else if (query instanceof ProductsSummaryQuery) {
				List<ProductSummary> products = productIndex.getProducts(query
						.getProductIndexQuery());
				((ProductsSummaryQuery) query).setResult(products);
			}

			else if (query instanceof ProductDetailQuery) {
				List<Product> products = new LinkedList<Product>();
				Iterator<ProductId> ids = query.getProductIndexQuery()
						.getProductIds().iterator();
				// fetch products from storage
				while (ids.hasNext()) {
					ProductId id = ids.next();
					Product product = productStorage.getProduct(id);
					if (product != null) {
						products.add(product);
					}
				}
				((ProductDetailQuery) query).setResult(products);
			}

			response.addResult(query);
		}

		return response;
	}

	public boolean isDisableArchive() {
		return disableArchive;
	}

	public void setDisableArchive(boolean disableArchive) {
		this.disableArchive = disableArchive;
	}

	/**
	 * @return the archiveInterval
	 */
	public long getArchiveInterval() {
		return archiveInterval;
	}

	/**
	 * @param archiveInterval
	 *            the archiveInterval to set
	 */
	public void setArchiveInterval(long archiveInterval) {
		this.archiveInterval = archiveInterval;
	}

	/**
	 * @return the archivePolicies
	 */
	public List<ArchivePolicy> getArchivePolicies() {
		return archivePolicies;
	}

}
