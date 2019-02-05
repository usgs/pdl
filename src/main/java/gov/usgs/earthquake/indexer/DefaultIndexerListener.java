package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.indexer.IndexerChange.IndexerChangeType;
import gov.usgs.earthquake.product.AbstractListener;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.util.CompareUtil;
import gov.usgs.util.Config;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * DefaultIndexerListener provides a starting point from which all
 * IndexerListeners may extend.
 * 
 * As a child-class of the AbstractListener, this may be configured with all of
 * the parent parameters and also accepts the following:
 * 
 * <dl>
 * <dt>command</dt>
 * <dd>(Required) The command to execute. This must be an executable command and
 * may include arguments. Any product-specific arguments are appended at the end
 * of command.</dd>
 * 
 * <dt>storage</dt>
 * <dd>(Required) A directory used to store all products. Each product is
 * extracted into a separate directory within this directory and is referenced
 * by the --directory=/path/to/directory argument when command is executed.</dd>
 * 
 * <dt>processUnassociated</dt>
 * <dd>(Optional, Default = false) Whether or not to process unassociated
 * products. Valid values are "true" and "false".</dd>
 * 
 * <dt>processPreferredOnly</dt>
 * <dd>(Optional, Default = false) Whether or not to process only preferred
 * products of the type accepted by this listener. Valid values are "true" and
 * "false".</dd>
 * 
 * <dt>ignoreArchive</dt>
 * <dd>(Optional, Default = false) Whether or not to ignore EVENT_ARCHIVED and
 * PRODUCT_ARCHIVED indexer events. Value values are "true" and "false".</dd>
 * 
 * </dl>
 */
public class DefaultIndexerListener extends AbstractListener implements
		IndexerListener {
	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(DefaultIndexerListener.class.getName());

	public static final String PROCESS_PREFERRED_ONLY_PROPERTY = "processPreferredOnly";
	public static final String PROCESS_PREFERRED_ONLY_DEFAULT = "false";

	public static final String PROCESS_UNASSOCIATED_PROPERTY = "processUnassociated";
	public static final String PROCESS_UNASSOCIATED_DEFAULT = "true";

	public static final String PROCESS_ONLY_WHEN_EVENT_CHANGE_PROPERTY = "processOnlyWhenEventChanged";
	public static final String PROCESS_ONLY_WHEN_EVENT_CHANGE_DEFAULT = "false";

	public static final String IGNORE_ARCHIVE_PROPERTY = "ignoreArchive";
	public static final String IGNORE_ARCHIVE_DEFAULT = "true";

	/** Whether or not to process only preferred products. */
	private boolean processOnlyPreferredProducts = false;

	/** Whether or not to process unassociated products. */
	private boolean processUnassociatedProducts = true;

	/**
	 * Whether or not to process updates that don't change preferred event
	 * parameters.
	 */
	private boolean processOnlyWhenEventChanged = false;

	/** Whether or not to process archive events. */
	private boolean ignoreArchive = false;

	@Override
	public void onIndexerEvent(IndexerEvent event) throws Exception {
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
		LOGGER.info(buf.toString());
	}

	/**
	 * @param change
	 *            the indexer event that has occurred
	 * @return whether this external indexer listener handles this product type
	 * @throws Exception
	 */
	public boolean accept(IndexerEvent change) throws Exception {
		String productType = null;

		if (change.getSummary() != null) {
			ProductId productId = change.getSummary().getId();

			productType = productId.getType();

			// use default notification listener first
			if (!super.accept(productId)) {
				return false;
			}
		}

		List<Event> events = change.getEvents();
		if (!processUnassociatedProducts && events.size() == 0) {
			LOGGER.fine("[" + getName() + "] product is unassociated");
			return false;
		}

		if (processOnlyPreferredProducts && events.size() > 0) {
			// check if preferred for any event
			boolean isPreferred = false;

			// can only be a preferred product if a summary associated
			if (productType != null) {
				Iterator<Event> iter = events.iterator();
				while (iter.hasNext()) {
					Event event = iter.next();
					ProductSummary preferred = event
							.getPreferredProduct(productType);
					if (preferred != null && preferred.getId().equals(
							change.getSummary().getId())) {
						// it is the most preferred product for this event
						isPreferred = true;
						break;
					}
				}
			}

			if (!isPreferred) {
				LOGGER.fine("[" + getName()
						+ "] product is not preferred in any event");
				return false;
			}
		}

		// accept by default
		return true;
	}

	public boolean accept(IndexerEvent event, IndexerChange change)
			throws Exception {
		// check whether this is an archive indexer change
		if (ignoreArchive
				&& (change.getType() == IndexerChangeType.PRODUCT_ARCHIVED
				|| change.getType() == IndexerChangeType.EVENT_ARCHIVED)) {
			return false;
		}

		// see if preferred event parameters have changed
		if (processOnlyWhenEventChanged) {
			Event originalEvent = change.getOriginalEvent();
			Event newEvent = change.getNewEvent();
			if (originalEvent != null && newEvent != null) {
				EventSummary originalEventSummary = originalEvent.getEventSummary();
				EventSummary newEventSummary = newEvent.getEventSummary();
				if (CompareUtil.nullSafeCompare(
						originalEventSummary.getMagnitude(),
						newEventSummary.getMagnitude()) != 0) {
					// magnitude changed
				} else if (CompareUtil.nullSafeCompare(
						originalEventSummary.getLatitude(),
						newEventSummary.getLatitude()) != 0) {
					// latitude changed
				} else if (CompareUtil.nullSafeCompare(
						originalEventSummary.getLongitude(),
						newEventSummary.getLongitude()) != 0) {
					// longitude changed
				} else if (CompareUtil.nullSafeCompare(
						originalEventSummary.getDepth(),
						newEventSummary.getDepth()) != 0) {
					// depth changed
				} else if (CompareUtil.nullSafeCompare(
						originalEventSummary.getTime(),
						newEventSummary.getTime()) != 0) {
					// time changed
				} else if (originalEventSummary.isDeleted() != newEventSummary.isDeleted()) {
					// status changed
				} else {
					// preferred event parameters haven't changed
					return false;
				}
			}
		}

		// accept changes by default
		return true;
	}

	public void configure(Config config) throws Exception {
		super.configure(config);

		processOnlyPreferredProducts = Boolean.valueOf(config
				.getProperty(PROCESS_PREFERRED_ONLY_PROPERTY,
						PROCESS_PREFERRED_ONLY_DEFAULT));
		LOGGER.config("[" + getName() + "] process only preferred products = "
				+ processOnlyPreferredProducts);

		processUnassociatedProducts = Boolean.valueOf(config.getProperty(
				PROCESS_UNASSOCIATED_PROPERTY, PROCESS_UNASSOCIATED_DEFAULT));
		LOGGER.config("[" + getName() + "] process unassociated products = "
				+ processUnassociatedProducts);

		processOnlyWhenEventChanged = Boolean.valueOf(config.getProperty(
				PROCESS_ONLY_WHEN_EVENT_CHANGE_PROPERTY,
				PROCESS_ONLY_WHEN_EVENT_CHANGE_DEFAULT));
		LOGGER.config("[" + getName() + "] process only when event changed = "
				+ processOnlyWhenEventChanged);

		ignoreArchive = Boolean.valueOf(config.getProperty(
				IGNORE_ARCHIVE_PROPERTY, IGNORE_ARCHIVE_DEFAULT));
		LOGGER.config("[" + getName() + "] ignore archive changes = "
				+ ignoreArchive);
	}

	/**
	 * @return whether only preferred products are processed
	 */
	public boolean getProcessOnlyPreferredProducts() {
		return processOnlyPreferredProducts;
	}

	/**
	 * @param processOnlyPreferredProducts
	 *            whether to process ony preferred products
	 */
	public void setProcessOnlyPreferredProducts(
			final boolean processOnlyPreferredProducts) {
		this.processOnlyPreferredProducts = processOnlyPreferredProducts;
	}

	public void setProcessUnassociatedProducts(
			final boolean processUnassociatedProducts) {
		this.processUnassociatedProducts = processUnassociatedProducts;
	}

	public boolean getProcessUnassociatedProducts() {
		return processUnassociatedProducts;
	}

	public boolean isProcessOnlyWhenEventChanged() {
		return processOnlyWhenEventChanged;
	}

	public void setProcessOnlyWhenEventChanged(
			boolean processOnlyWhenEventChanged) {
		this.processOnlyWhenEventChanged = processOnlyWhenEventChanged;
	}

	public boolean isIgnoreArchive() {
		return ignoreArchive;
	}

	public void setIgnoreArchive(boolean ignoreArchive) {
		this.ignoreArchive = ignoreArchive;
	}
}
