package gov.usgs.earthquake.indexer;

/**
 * Description of a specific change to a {@link ProductIndex}.
 *
 * Multiple IndexerChange objects may be created, and grouped
 * into an {@link IndexerEvent}, in response to one product
 * being processed.
 */
public class IndexerChange {

	/** Enumeration of indexer event types. */
	public static enum IndexerChangeType {
		/** Enum for IndexerChangeType Event Added */
		EVENT_ADDED,
		/** Enum for IndexerChangeType Event Updated */
		EVENT_UPDATED,
		/** Enum for IndexerChangeType Event Deleted */
		EVENT_DELETED,
		/** Enum for IndexerChangeType Event Archived */
		EVENT_ARCHIVED,
		/** Enum for IndexerChangeType Event Merged */
		EVENT_MERGED,
		/** Enum for IndexerChangeType Event Split */
		EVENT_SPLIT,

		/** Enum for IndexerChangeType Product Added */
		PRODUCT_ADDED,
		/** Enum for IndexerChangeType Product Updated */
		PRODUCT_UPDATED,
		/** Enum for IndexerChangeType Product Deleted */
		PRODUCT_DELETED,
		/** Enum for IndexerChangeType Product Archived */
		PRODUCT_ARCHIVED
	};
	/** IndexerChangeType for Event Added */
	public static final IndexerChangeType EVENT_ADDED = IndexerChangeType.EVENT_ADDED;
	/** IndexerChangeType for Event Updated */
	public static final IndexerChangeType EVENT_UPDATED = IndexerChangeType.EVENT_UPDATED;
	/** IndexerChangeType for Event Deleted */
	public static final IndexerChangeType EVENT_DELETED = IndexerChangeType.EVENT_DELETED;
	/** IndexerChangeType for Event Archived */
	public static final IndexerChangeType EVENT_ARCHIVED = IndexerChangeType.EVENT_ARCHIVED;
	/** IndexerChangeType for Event Merged */
	public static final IndexerChangeType EVENT_MERGED = IndexerChangeType.EVENT_MERGED;
	/** IndexerChangeType for Event Split */
	public static final IndexerChangeType EVENT_SPLIT = IndexerChangeType.EVENT_SPLIT;

	/** IndexerChangeType for Product Added */
	public static final IndexerChangeType PRODUCT_ADDED = IndexerChangeType.PRODUCT_ADDED;
	/** IndexerChangeType for Product Updated */
	public static final IndexerChangeType PRODUCT_UPDATED = IndexerChangeType.PRODUCT_UPDATED;
	/** IndexerChangeType for Product Deleted */
	public static final IndexerChangeType PRODUCT_DELETED = IndexerChangeType.PRODUCT_DELETED;
	/** IndexerChangeType for Product Archived */
	public static final IndexerChangeType PRODUCT_ARCHIVED = IndexerChangeType.PRODUCT_ARCHIVED;

	/** Indicates the type of change that is occurring */
	private IndexerChangeType type;
	/** The event as it was before the change occurred. */
	private Event originalEvent;
	/** The event as it is after the change occurred. */
	private Event newEvent;

	/**
	 * Constructor to quickly create a new <code>IndexerChange</code> object to
	 * be added to the list of changes in a given <code>IndexerEvent</code>.
	 * Note the <code>oldEvent</code> and <code>newEvent</code> will have
	 * particular meanings depending on the given <code>type</code> of change
	 * that occurred.
	 *
	 * @param type
	 *            The type of change that occurred.
	 * @param originalEvent
	 *            The event as it was before the change occurred.
	 * @param newEvent
	 *            The event as it is after the change occurred.
	 * @see IndexerEvent
	 */
	public IndexerChange(IndexerChangeType type, Event originalEvent,
			Event newEvent) {
		this.type = type;
		this.originalEvent = originalEvent;
		this.newEvent = newEvent;
	}

	/** @return IndexerChangeType */
	public IndexerChangeType getType() {
		return this.type;
	}

	/** @return originalEvent */
	public Event getOriginalEvent() {
		return this.originalEvent;
	}

	/** @return newEvent */
	public Event getNewEvent() {
		return this.newEvent;
	}

	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append(getType().toString() + " :: ");

		if (getOriginalEvent() != null) {
			b.append(getOriginalEvent().getEventId() + " --> ");
		} else {
			b.append("null --> ");
		}

		if (getNewEvent() != null) {
			b.append(getNewEvent().getEventId());
		} else {
			b.append("null");
		}

		return b.toString();
	}
}
