package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.ProductTracker;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Callable object for deferred indexer listener notification.
 */
public class IndexerListenerCallable implements Callable<Void> {

	public static final Logger LOGGER = Logger
			.getLogger(IndexerListenerCallable.class.getName());

	private final IndexerListener listener;
	private final IndexerEvent event;

	/**
	 * Get a callable object for deferred listener notification.
	 * 
	 * @param listener
	 *            the listener to notify
	 * @param event
	 *            the notification to send
	 */
	public IndexerListenerCallable(final IndexerListener listener,
			final IndexerEvent event) {
		this.listener = listener;
		this.event = event;
	}

	public Void call() throws Exception {
		try {
			listener.onIndexerEvent(event);
			return null;
		} catch (Exception e) {
			ProductSummary summary = event.getSummary();

			LOGGER.log(Level.WARNING, "["
					+ event.getIndexer().getName()
					+ "] listener ("
					+ listener.getName()
					+ ") threw exception"
					+ (summary != null ? ", for product id="
							+ summary.getId().toString() : ""), e);

			// track exception
			if (summary != null) {
				new ProductTracker(summary.getTrackerURL()).exception(listener
						.getClass().getCanonicalName(), summary.getId(), e);
			}

			// but rethrow for outside handling
			throw e;
		}
	}

}
