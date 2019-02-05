package gov.usgs.earthquake.indexer;

import java.util.HashMap;
import java.util.Map;

import gov.usgs.earthquake.product.Product;

/**
 * (Experimental) Notify external processes when preferred product change within events.
 * 
 * @author jmfee
 *
 */
public class ExternalPreferredListener extends ExternalIndexerListener {

	public static final String PREFERRED_ACTION_ARGUMENT = "--preferred-action=";

	/**
	 * Types of preferred product actions.
	 */
	public static enum PreferredAction {
		PREFERRED_ADDED,
		PREFERRED_CHANGED,
		PREFERRED_REMOVED
	};

	/**
	 * Skip parent class processing, except autoArchiving.
	 */
	@Override
	public boolean accept(final IndexerEvent event) {
		return false;
	}

	/**
	 * Handle indexer events.
	 */
	@Override
	public void onIndexerEvent(final IndexerEvent event) throws Exception {
		for (IndexerChange change : event.getIndexerChanges()) {
			if (!accept(event, change)) {
				// ignoreArchive, processOnlyWhenEventChanged checks
				continue;
			}

			Map<ProductSummary, PreferredAction> changes = getIndexerChangePreferredActions(change);
			for (ProductSummary changedProduct : changes.keySet()) {
				if (!accept(changedProduct.getId())) {
					continue;
				}

				Event changedEvent = change.getNewEvent();
				if (changedEvent == null) {
					// event archived...
					changedEvent = change.getOriginalEvent();
				}

				String command = getProductSummaryCommand(changedEvent, changedProduct);

				// indexer action
				command = command + " " +
						ExternalIndexerListener.EVENT_ACTION_ARGUMENT +
						change.getType().toString();

				// preferred product action
				command = command + " " +
						PREFERRED_ACTION_ARGUMENT +
						changes.get(changedProduct).toString();

				// pass product content as input to command
				Product product = null;
				try {
					// this product is not necessarily the product that triggered the change.
					product = storeProduct(event.getIndexer().getProductStorage().getProduct(changedProduct.getId()));
				} catch (Exception e) {
					// ignore, just leave null
				}

				runProductCommand(command, product);
			}
		}

		super.onIndexerEvent(event);
	}

	/**
	 * Compare preferred products before/after IndexerChange was applied.
	 * 
	 * @param change indexer change to evaluate.
	 * @return map of preferred products that were changed.
	 */
	public static Map<ProductSummary, PreferredAction> getIndexerChangePreferredActions(final IndexerChange change) {
		Map<ProductSummary, PreferredAction> changes = new HashMap<ProductSummary, PreferredAction>();

		// only event types
		IndexerChange.IndexerChangeType changeType = change.getType();
		if (changeType != IndexerChange.EVENT_ADDED &&
				changeType != IndexerChange.EVENT_DELETED &&
				changeType != IndexerChange.EVENT_MERGED &&
				changeType != IndexerChange.EVENT_SPLIT &&
				changeType != IndexerChange.EVENT_UPDATED) {
			return changes;
		}
		
		Map<String, ProductSummary> newProducts = getPreferredProducts(change.getNewEvent());
		Map<String, ProductSummary> originalProducts = getPreferredProducts(change.getOriginalEvent());

		// check all currently preferred products
		for (String type : newProducts.keySet()) {
			ProductSummary newProduct = newProducts.get(type);
			ProductSummary originalProduct = originalProducts.get(type);
			
			if (originalProduct == null) {
				// no product of this type previously existed
				changes.put(newProduct,
						PreferredAction.PREFERRED_ADDED);
			} else if (!newProduct.getId().equals(originalProduct.getId())) {
				// different from previous preferred product of same type
				changes.put(newProduct,
						PreferredAction.PREFERRED_CHANGED);
			}
		}
		
		for (String type : originalProducts.keySet()) {
			if (newProducts.get(type) == null) {
				// no product of this type exists anymore
				changes.put(originalProducts.get(type),
						PreferredAction.PREFERRED_REMOVED);
			}
		}
		
		return changes;
	}

	/**
	 * Get a map of preferred products from an event.
	 *
	 * @param event the event.
	 * @return preferred products, or empty hashmap if event is null.
	 */
	public static Map<String, ProductSummary> getPreferredProducts(final Event event) {
		if (event == null) {
			return new HashMap<String, ProductSummary>();
		}
		return event.getPreferredProducts();
	}

}
