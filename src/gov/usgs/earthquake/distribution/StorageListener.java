package gov.usgs.earthquake.distribution;

import gov.usgs.util.Configurable;

public interface StorageListener extends Configurable {

	/**
	 * Called when a <code>ProductStorage</code> is changed by a
	 * <code>Product</code>.
	 * 
	 * @param event
	 *            Contains storage change details
	 */
	public void onStorageEvent(StorageEvent event);
}
