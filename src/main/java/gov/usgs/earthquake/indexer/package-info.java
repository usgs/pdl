/**
 * The <strong>Indexer</strong> builds a catalog of Products.
 * 
 * <p>
 * {@link Indexer} stores ProductSummaries and Events in a {@link ProductIndex}.
 * </p>
 * 
 * <br>
 * <strong>Data Objects</strong>
 * <dl>
 * <dt>{@link ProductSummary}</dt>
 * <dd>
 * The Indexer uses IndexerModules to summarize Products.
 * </dd>
 * 
 * <dt>{@link Event}</dt>
 * <dd>
 * The Indexer Associates related products into Events.
 * </dd>
 * 
 * <dt>{@link IndexerEvent}</dt>
 * <dd>
 * Represents one or more {@link IndexerChange}s made in response
 * to processing an incoming product.
 * </dd>
 * </dl>
 *
 * <br>
 * <strong>Indexer Listeners</strong>
 * <p>Listeners receive IndexerEvents whenever changes are made
 * to the index</p>
 * <dl>
 * <dt>{@link DefaultIndexerListener}</dt>
 * <dd>
 * Base class to process IndexerEvents in a separate thread.
 * </dd>
 * 
 * <dt>{@link ExternalIndexerListener}</dt>
 * <dd>
 * Executes an external process for each IndexerEvent.
 * </dd>
 * </dl>
 * 
 * <br>
 * <strong>Archive Policies</strong>
 * <p>
 * By default events and products are kept forever.
 * Configure archive policies to periodically remove old information.
 * </p>
 * 
 * <dl>
 * <dt>{@link ArchivePolicy}</dt>
 * <dd>
 * When to remove events from the ProductIndex.
 * When events are removed, any associated products are also removed.
 * </dd>
 * 
 * <dt>{@link ProductArchivePolicy}</dt>
 * <dd>
 * When to remove products from the ProductIndex, for example old versions
 * or products that did not associate to any events.
 * </dd>
 * </dl>
 */
package gov.usgs.earthquake.indexer;
