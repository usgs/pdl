/*
 * ShakemapIndexerWedge
 */
package gov.usgs.earthquake.shakemap;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import gov.usgs.earthquake.distribution.Command;
import gov.usgs.earthquake.distribution.Command.CommandTimeout;
import gov.usgs.earthquake.distribution.DefaultNotificationListener;
import gov.usgs.earthquake.distribution.NotificationListenerException;
import gov.usgs.earthquake.distribution.ProductTracker;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.DirectoryProductHandler;
import gov.usgs.earthquake.product.io.DirectoryProductSource;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;

/**
 * Legacy interface to trigger pre-Indexer ShakeMap processing.
 * 
 * The Old ShakeMap Indexer is no longer used,
 * and this class is deprecated.
 * 
 * When a shakemap product arrives, it is only processed if one of these is
 * true:
 * <ul>
 * <li>doesn't already exist</li>
 * <li>from preferred source (product source = eventsource)</li>
 * <li>from same source as before</li>
 * </ul>
 * 
 * When processing a shakemap:
 * <ol>
 * <li>remove previous version</li>
 * <li>unpack new version, if not a delete</li>
 * <li>trigger legacy indexer</li>
 * <li>send tracker update</li>
 * </ol>
 * 
 * Configurable properties:
 * <dl>
 * <dt>indexerCommand</dt>
 * <dd>The shakemap indexer command to run. Defaults to
 * <code>/home/www/vhosts/earthquake/cron/shakemap_indexer.php</code> .</dd>
 * 
 * <dt>shakemapDirectory</dt>
 * <dd>The shakemap event directory. Defaults to
 * <code>/home/www/vhosts/earthquake/htdocs/earthquakes/shakemap</code> .</dd>
 * 
 * <dt>timeout</dt>
 * <dd>How long in milliseconds the indexer is allowed to run before being
 * terminated.</dd>
 * </dl>
 */
@Deprecated()
public class ShakeMapIndexerWedge extends DefaultNotificationListener {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(ShakeMapIndexerWedge.class.getName());

	/** Translate from event source to old style shakemap source. */
	private static final Map<String, String> SOURCE_TRANSLATION_MAP = new HashMap<String, String>();
	static {
		SOURCE_TRANSLATION_MAP.put("ci", "sc");
		SOURCE_TRANSLATION_MAP.put("us", "global");
		SOURCE_TRANSLATION_MAP.put("uu", "ut");
		SOURCE_TRANSLATION_MAP.put("uw", "pn");
	}

	/** Configurable property. */
	public static final String SHAKEMAP_INDEXER_COMMAND_PROPERTY = "indexerCommand";

	/** The shakemap indexer command to execute. */
	public static final String DEFAULT_SHAKEMAP_INDEXER_COMMAND = "/home/www/vhosts/earthquake/cron/shakemap_indexer.php";

	/** Configurable property for command timeout. */
	public static final String COMMAND_TIMEOUT_PROPERTY = "timeout";

	/** Default command timeout. */
	public static final String DEFAULT_COMMAND_TIMEOUT = "100000";

	/** Configurable property for shakemap directory. */
	public static final String SHAKEMAP_DIRECTORY_PROPERTY = "shakemapDirectory";

	/** Default shakemap directory. */
	public static final String DEFAULT_SHAKEMAP_DIRECTORY = "/home/www/vhosts/earthquake/htdocs/earthquakes/shakemap";

	/** The indexer command to run. */
	private String indexerCommand = DEFAULT_SHAKEMAP_INDEXER_COMMAND;

	/** Base event directory for shakemap storage. */
	private File baseEventDirectory = new File(DEFAULT_SHAKEMAP_DIRECTORY);

	/** Timeout when running indexer command, in milliseconds. */
	private long indexerTimeout = Long.valueOf(DEFAULT_COMMAND_TIMEOUT);

	/**
	 * Create a new ShakeMapIndexerWedge.
	 * 
	 * Sets up the includeTypes list to contain "shakemap".
	 */
	public ShakeMapIndexerWedge() {
		this.getIncludeTypes().add("shakemap");
	}

	/**
	 * Receive a ShakeMap from Product Distribution.
	 * 
	 * @param product
	 *            a shakemap type product.
	 */
	@Override
	public void onProduct(final Product product) throws Exception {
		ProductId productId = product.getId();

		// convert this product to a ShakeMap product, which has more
		// information
		ShakeMap shakemap = new ShakeMap(product);

		// get the legacy directory
		File legacyDirectory = getEventDirectory(shakemap);

		// check for a previous version of this shakemap
		if (legacyDirectory.exists()) {
			LOGGER.info("Shakemap directory exists "
					+ legacyDirectory.getCanonicalPath());

			try {
				ShakeMap previousShakemap = new ShakeMap(
						ObjectProductHandler
								.getProduct(new DirectoryProductSource(
										legacyDirectory)));
				ProductId previousId = previousShakemap.getId();

				// same version?
				if (productId.equals(previousId)) {
					// already have this version of shakemap
					LOGGER.info("Shakemap already processed "
							+ productId.toString());
					return;
				} else {
					LOGGER.info("Shakemap is different, previous is "
							+ previousId.toString());
				}

				if (!productId.getSource().equals(shakemap.getEventSource())
						&& !productId.getSource().equals(previousId.getSource())) {
					// incoming is not from preferred source
					LOGGER.info("Skipping non-preferred shakemap, previous source='"
							+ previousId.getSource()
							+ "' incoming source='"
							+ productId.getSource()
							+ "'");
					return;
				}
			} catch (Exception e) {
				// unable to load as a product, may be just a shakemap directory
				// received via rsync instead of a shakemap product directory

				if (!productId.getSource().equals(shakemap.getEventSource())) {
					// only process if product source matches event source
					LOGGER.info("Shakemap directory already exists, skipping non-preferred source '"
							+ productId.getSource() + "'");
					return;
				}
			}

			// remove previous version
			FileUtils.deleteTree(legacyDirectory);
		}

		// passed filtering, so do what the product says
		String source = translateShakeMapSource(shakemap.getEventSource());
		String code = shakemap.getEventSourceCode();
		boolean delete = false;

		if (!shakemap.isDeleted()) {
			// write the original product, not the modified ShakeMap product.
			new ObjectProductSource(product)
					.streamTo(new DirectoryProductHandler(legacyDirectory));
		} else {
			// need to delete the shakemap, everywhere
			// the indexer will handle the everywhere part...
			delete = true;
		}

		// run the indexer to update shakemap pages
		int exitCode = runIndexer(source, code, delete);
		if (exitCode == 0) {
			new ProductTracker(product.getTrackerURL()).productIndexed(
					this.getName(), productId);
		} else {
			throw new NotificationListenerException("[" + getName()
					+ "] command exited with status " + exitCode);
		}
	}

	/**
	 * Run the shakemap indexer.
	 * 
	 * If network and code are omitted, all events are updated.
	 * 
	 * @param network
	 *            the network to update.
	 * @param code
	 *            the code to update.
	 * @param delete
	 *            whether indexer is handling a delete (true) or update (false).
	 * @return -1 if indexer does not complete within max(1, getAttemptCount())
	 *         times, or exit code if indexer completes.
	 * @throws IOException
	 */
	public int runIndexer(final String network, final String code,
			final boolean delete) throws Exception {
		// build indexer command
		StringBuffer updateCommand = new StringBuffer(indexerCommand);
		if (network != null && code != null) {
			updateCommand.append(" --network=").append(network);
			updateCommand.append(" --code=").append(code);
			if (delete) {
				updateCommand.append(" --delete");
			}
		}

		// now run command
		String productCommand = updateCommand.toString();

		Command command = new Command();
		command.setCommand(productCommand);
		command.setTimeout(indexerTimeout);
		try {
			LOGGER.fine("[" + getName() + "] running command '"
					+ productCommand + "'");
			command.execute();

			int exitCode = command.getExitCode();
			LOGGER.info("[" + getName() + "] command '" + productCommand
					+ "' exited with status '" + exitCode + "'");
			return exitCode;
		} catch (CommandTimeout ct) {
			LOGGER.warning("[" + getName() + "] command '" + productCommand
					+ "' timed out");
			return -1;
		}
	}

	/**
	 * Get the directory for a particular shakemap.
	 * 
	 * @param shakemap
	 *            the shakemap to find a directory for.
	 * @return the shakemap directory.
	 */
	public File getEventDirectory(final ShakeMap shakemap) throws Exception {
		String source = translateShakeMapSource(shakemap.getEventSource());
		String code = shakemap.getEventSourceCode();

		return new File(baseEventDirectory, source + "/shake/" + code);
	}

	/**
	 * Translate from an event source to the old style shakemap source.
	 * 
	 * Driven by the SOURCE_TRANSLATION_MAP.
	 * 
	 * @param eventSource
	 *            the event network.
	 * @return the shakemap network.
	 */
	public String translateShakeMapSource(final String eventSource) {
		if (SOURCE_TRANSLATION_MAP.containsKey(eventSource)) {
			return SOURCE_TRANSLATION_MAP.get(eventSource);
		}
		return eventSource;
	}

	/**
	 * Configure this shakemap indexer.
	 */
	@Override
	public void configure(final Config config) throws Exception {
		super.configure(config);

		baseEventDirectory = new File(config.getProperty(
				SHAKEMAP_DIRECTORY_PROPERTY, DEFAULT_SHAKEMAP_DIRECTORY));
		LOGGER.config("Shakemap event directory "
				+ baseEventDirectory.getCanonicalPath());

		indexerCommand = config.getProperty(SHAKEMAP_INDEXER_COMMAND_PROPERTY,
				DEFAULT_SHAKEMAP_INDEXER_COMMAND);
		LOGGER.config("Shakemap indexer command " + indexerCommand);

		indexerTimeout = Long.valueOf(config.getProperty(
				COMMAND_TIMEOUT_PROPERTY, DEFAULT_COMMAND_TIMEOUT));
		LOGGER.config("Shakemap indexer command timeout " + indexerTimeout);
	}

}
