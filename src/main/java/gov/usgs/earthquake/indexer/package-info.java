/**
 * The Indexer builds a catalog of Products.
 *
 * {@link Indexer} implements the
 * {@link gov.usgs.earthquake.distribution.NotificationListener}
 * interface to receive PDL products.
 * 
 * It stores products, uses {@link IndexerModule}s to create
 * {@link ProductSummary}s, and builds a catalog of {@link Event}s
 * stored in a {@link ProductIndex}.  After making changes to the index
 * {@link IndexerListener}s receive an {@link IndexerEvent} with
 * one or more {@link IndexerChange}s descrbing what changed.
 *
 * Notable IndexerModules
 * <ul>
 * <li>{@link DefaultIndexerModule}</li>
 * <li>{@link gov.usgs.earthquake.momenttensor.MTIndexerModule}</li>
 * <li>{@link gov.usgs.earthquake.shakemap.ShakeMapIndexerModule}</li>
 * </ul>
 * 
 * API Users can subclass the {@link DefaultIndexerListener} to process
 * {@link IndexerEvent}s.  The {@link ExternalIndexerListener} can be
 * configured to run external processes for each change.
 */
package gov.usgs.earthquake.indexer;
