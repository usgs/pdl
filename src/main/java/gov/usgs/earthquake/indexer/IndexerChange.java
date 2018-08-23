package gov.usgs.earthquake.indexer;

public class IndexerChange {

	/** Enumeration of indexer event types. */
	public static enum IndexerChangeType {
		EVENT_ADDED, EVENT_UPDATED, EVENT_DELETED, EVENT_ARCHIVED, EVENT_MERGED, EVENT_SPLIT,

		PRODUCT_ADDED, PRODUCT_UPDATED, PRODUCT_DELETED, PRODUCT_ARCHIVED
	};

	public static final IndexerChangeType EVENT_ADDED = IndexerChangeType.EVENT_ADDED;
	public static final IndexerChangeType EVENT_UPDATED = IndexerChangeType.EVENT_UPDATED;
	public static final IndexerChangeType EVENT_DELETED = IndexerChangeType.EVENT_DELETED;
	public static final IndexerChangeType EVENT_ARCHIVED = IndexerChangeType.EVENT_ARCHIVED;
	public static final IndexerChangeType EVENT_MERGED = IndexerChangeType.EVENT_MERGED;
	public static final IndexerChangeType EVENT_SPLIT = IndexerChangeType.EVENT_SPLIT;

	public static final IndexerChangeType PRODUCT_ADDED = IndexerChangeType.PRODUCT_ADDED;
	public static final IndexerChangeType PRODUCT_UPDATED = IndexerChangeType.PRODUCT_UPDATED;
	public static final IndexerChangeType PRODUCT_DELETED = IndexerChangeType.PRODUCT_DELETED;
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

	public IndexerChangeType getType() {
		return this.type;
	}

	public Event getOriginalEvent() {
		return this.originalEvent;
	}

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
