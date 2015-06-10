/*
 * IndexerListener
 * 
 * $Id: IndexerListener.java 22335 2014-11-17 15:52:47Z jmfee $
 * $URL: https://ghttrac.cr.usgs.gov/websvn/ProductDistribution/trunk/src/gov/usgs/earthquake/indexer/IndexerListener.java $
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.util.Configurable;

/**
 * Listen for notifications that the index has changed.
 */
public interface IndexerListener extends Configurable {

	/**
	 * This method is called when the indexer makes a change to the
	 * ProductIndex.
	 * 
	 * @param change
	 *            description of the change.
	 */
	public void onIndexerEvent(final IndexerEvent change) throws Exception;

	/**
	 * An indexer that generates a IndexerEvent will attempt to deliver the
	 * event at most this many times, if the listener throws an Exception while
	 * processing.
	 * 
	 * @return A value of less than one means never attempt to deliver.
	 */
	public int getMaxTries();

	/**
	 * A IndexerListener has this many milliseconds to process an event before
	 * being interrupted.
	 * 
	 * @return number of milliseconds before timing out. A value of 0 or less
	 *         means never time out.
	 */
	public long getTimeout();

}
