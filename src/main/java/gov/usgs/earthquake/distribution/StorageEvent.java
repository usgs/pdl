package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;

import java.util.EventObject;

public class StorageEvent extends EventObject {

	/** Enumeration of <code>StorageEventType</code>s **/
	public static enum StorageEventType {
		PRODUCT_STORED, PRODUCT_REMOVED
	}

	public static final StorageEventType PRODUCT_STORED = StorageEventType.PRODUCT_STORED;
	public static final StorageEventType PRODUCT_REMOVED = StorageEventType.PRODUCT_REMOVED;

	private static final long serialVersionUID = 0x019A1A8BL;
	private ProductId id = null;
	private StorageEventType type = null;

	public StorageEvent(ProductStorage storage, ProductId id,
			StorageEventType type) {
		super(storage);
		this.id = id;
		this.type = type;
	}

	public ProductStorage getProductStorage() {
		return (ProductStorage) getSource();
	}

	public ProductId getProductId() {
		return id;
	}

	public StorageEventType getType() {
		return type;
	}
}
