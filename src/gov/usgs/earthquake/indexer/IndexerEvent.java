/*
 * IndexerEvent
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.product.Product;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * A description of a change to a ProductIndex.
 * 
 * IndexerEvents are created by the Indexer, and sent to IndexerListeners.
 */
public class IndexerEvent extends EventObject {

	/** Serialization ID. */
	private static final long serialVersionUID = 1L;

	/** The index that was changed. */
	private ProductIndex index;

	/** The product that triggered this change. */
	private ProductSummary summary;

	/**
	 * A Vector object is used here because it provides synchronized
	 * (thread-safe) access to its elements. This is important since this event
	 * will be sent asynchronously to many listeners (potentially).
	 */
	private Vector<IndexerChange> indexerChanges = null;

	/**
	 * Construct a new IndexerEvent.
	 * 
	 * @param source
	 *            the indexer that made the change.
	 */
	public IndexerEvent(final Indexer source) {
		super(source);
		// Initial capacity = 5, capacity increment = 5;
		this.indexerChanges = new Vector<IndexerChange>(5, 5);
	}

	public Indexer getIndexer() {
		return (Indexer) getSource();
	}

	public ProductIndex getIndex() {
		return this.index;
	}

	public void setIndex(ProductIndex index) {
		this.index = index;
	}

	public ProductSummary getSummary() {
		return this.summary;
	}

	public void setSummary(ProductSummary summary) {
		this.summary = summary;
	}

	public void addIndexerChange(IndexerChange change) {
		if (change != null) {
			this.indexerChanges.add(change);
		}
	}

	public void addIndexerChanges(List<IndexerChange> changes) {
		if (changes == null) {
			return;
		}

		Iterator<IndexerChange> iterator = changes.iterator();
		while (iterator.hasNext()) {
			addIndexerChange(iterator.next());
		}
	}

	public Vector<IndexerChange> getIndexerChanges() {
		return this.indexerChanges;
	}

	/**
	 * Convenience method to retrieve Product from Indexer storage.
	 * 
	 * @return Product object corresponding to ProductSummary.
	 * @throws Exception
	 */
	public Product getProduct() throws Exception {
		if (summary == null) {
			return null;
		}
		return getIndexer().getProductStorage().getProduct(summary.getId());
	}

	/**
	 * Retrieve a distinct list of events that were changed as part of this
	 * IndexerEvent.
	 * 
	 * @return list of events
	 */
	public List<Event> getEvents() {
		// map from eventid to event
		Map<Long, Event> events = new HashMap<Long, Event>();

		// iterate over all changes, and place new event into map.

		// more recent changes occur later in list, and thus only the
		// latest update to an event will appear (if the event was
		// changed more than once).
		Iterator<IndexerChange> iter = indexerChanges.iterator();
		while (iter.hasNext()) {
			IndexerChange change = iter.next();
			Event event = change.getNewEvent();
			if (event != null) {
				// not an UNASSOCIATED_PRODUCT, which doesn't have an associated
				// event
				events.put(event.getIndexId(), event);
			}
		}

		// extract distinct events from map
		return new LinkedList<Event>(events.values());
	}

}
