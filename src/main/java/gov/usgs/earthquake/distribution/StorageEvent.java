package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;

import java.util.EventObject;

public class StorageEvent extends EventObject {

	/** Enumeration of <code>StorageEventType</code>s **/
	public static enum StorageEventType {
		/** StorageEventType enum for stored */
		PRODUCT_STORED,
		/** StorageEventType enum for removed */
		PRODUCT_REMOVED
	}

	/** Variable of StorageEventType, for the PRODUCT_STORED enum */
	public static final StorageEventType PRODUCT_STORED = StorageEventType.PRODUCT_STORED;
	/** Variable of StorageEventType, for the PRODUCT_REMOVED enum */
	public static final StorageEventType PRODUCT_REMOVED = StorageEventType.PRODUCT_REMOVED;

	private static final long serialVersionUID = 0x019A1A8BL;
	private ProductId id = null;
	private StorageEventType type = null;

	/**
	 * Construct a new StorageEvent
	 * @param storage ProductStorage
	 * @param id ProductId
	 * @param type StorageEventType
	 */
	public StorageEvent(ProductStorage storage, ProductId id,
			StorageEventType type) {
		super(storage);
		this.id = id;
		this.type = type;
	}

	/** @return ProductStorage */
	public ProductStorage getProductStorage() {
		return (ProductStorage) getSource();
	}

	/** @return Product ID */
	public ProductId getProductId() {
		return id;
	}

	/** @return StorageEventType */
	public StorageEventType getType() {
		return type;
	}
}
