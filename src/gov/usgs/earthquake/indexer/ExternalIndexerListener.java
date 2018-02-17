/*
 * ExternalIndexerListener
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.CLIProductBuilder;
import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.distribution.ExternalNotificationListener;
import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.distribution.HeartbeatListener;
import gov.usgs.earthquake.distribution.ProductAlreadyInStorageException;
import gov.usgs.earthquake.distribution.ProductStorage;
import gov.usgs.earthquake.indexer.IndexerChange.IndexerChangeType;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

import java.io.File;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ExternalIndexerListener provides a translation to a command-line interface
 * for the product indexer to speak with external, non-Java listeners.
 * 
 * As a child-class of the AbstractListener, this also accepts the following
 * configration parameters:
 * 
 * <dl>
 * <dt>command</dt>
 * <dd>(Required) The command to execute. This must be an executable command and
 * may include arguments. Any product-specific arguments are appended at the end
 * of command.</dd>
 * 
 * <dt>storage</dt>
 * <dd>(Required) A directory used to store all products. Each product is
 * extracted into a separate directory within this directory and is referenced
 * by the --directory=/path/to/directory argument when command is executed.</dd>
 * 
 * <dt>processUnassociated</dt>
 * <dd>(Optional, Default = false) Whether or not to process unassociated
 * products. Valid values are "true" and "false".</dd>
 * 
 * <dt>processPreferredOnly</dt>
 * <dd>(Optional, Default = false) Whether or not to process only preferred
 * products of the type accepted by this listener. Valid values are "true" and
 * "false".</dd>
 * 
 * <dt>autoArchive</dt>
 * <dd>(Optional, Default = false) Whether or not to archive products from
 * storage when they are archived by the indexer.</dd>
 * 
 * </dl>
 */
public class ExternalIndexerListener extends DefaultIndexerListener implements
		IndexerListener {

	private static final Logger LOGGER = Logger
			.getLogger(ExternalIndexerListener.class.getName());

	public static final String EVENT_ACTION_ARGUMENT = "--action=";
	public static final String EVENT_IDS_ARGUMENT = "--eventids=";

	public static final String PREFERRED_ID_ARGUMENT = "--preferred-eventid=";
	public static final String PREFERRED_EVENTSOURCE_ARGUMENT = "--preferred-eventsource=";
	public static final String PREFERRED_EVENTSOURCECODE_ARGUMENT = "--preferred-eventsourcecode=";
	public static final String PREFERRED_MAGNITUDE_ARGUMENT = "--preferred-magnitude=";
	public static final String PREFERRED_LONGITUDE_ARGUMENT = "--preferred-longitude=";
	public static final String PREFERRED_LATITUDE_ARGUMENT = "--preferred-latitude=";
	public static final String PREFERRED_DEPTH_ARGUMENT = "--preferred-depth=";
	public static final String PREFERRED_ORIGIN_TIME_ARGUMENT = "--preferred-eventtime=";
	/** Configuration parameter for storage directory product. */
	public static final String STORAGE_NAME_PROPERTY = "storage";

	/** Short circuit to directly configure storage directory. */
	public static final String STORAGE_DIRECTORY_PROPERTY = "storageDirectory";

	/** Configuration parameter for command. */
	public static final String COMMAND_PROPERTY = "command";

	/** Configuration parameter for autoArchive. */
	public static final String AUTO_ARCHIVE_PROPERTY = "autoArchive";
	public static final String AUTO_ARCHIVE_DEFAULT = "true";

	/** Argument used to pass signature to external process. */
	public static final String SIGNATURE_ARGUMENT = "--signature=";

	/** Where products are stored in extracted form. */
	private FileProductStorage storage;

	/** Command that is executed after a product is stored. */
	private String command;

	/** Archive products from listener storage when archived by indexer. */
	private boolean autoArchive = false;

	/**
	 * Construct a new ExternalIndexerListener object
	 * 
	 * The listener must be configured with a FileProductStorage and a command
	 * to function.
	 */
	public ExternalIndexerListener() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.usgs.earthquake.indexer.IndexerListener#onIndexerEvent(gov.usgs.
	 * earthquake.indexer.IndexerEvent)
	 */
	public void onIndexerEvent(IndexerEvent change) throws Exception {
		// Only handle products that are specifically included, unless there are
		// no specified inclusions, and do not handle products that are
		// specifically excluded.
		if (accept(change)) {
			Product product = null;
			// store product first
			try {
				product = change.getProduct();
				if (product != null) {
					getStorage().storeProduct(change.getProduct());
					product = getStorage().getProduct(product.getId());
				} else {
					LOGGER.finer("[" + getName()
							+ "] Change product is null. Probably archiving.");
				}
			} catch (ProductAlreadyInStorageException paise) {
				LOGGER.info("[" + getName() + "] product already in storage");
				// keep going anyways
			}

			for (Iterator<IndexerChange> changeIter = change
					.getIndexerChanges().iterator(); changeIter.hasNext();) {
				IndexerChange indexerChange = changeIter.next();

				// check if we should process this change
				if (!accept(change, indexerChange)) {
					continue;
				}

				// build command
				final String indexerCommand = getProductSummaryCommand(change,
						indexerChange);

				// execute
				LOGGER.info("[" + getName() + "] running command "
						+ indexerCommand);
				final Process process = Runtime.getRuntime().exec(
						indexerCommand);

				// Stream content over stdin if it exists
				if (product != null) {
					Content content = product.getContents().get("");
					if (content != null) {
						StreamUtils.transferStream(content.getInputStream(),
								process.getOutputStream());
					}
				}

				// Close the output stream
				StreamUtils.closeStream(process.getOutputStream());

				Timer commandTimer = new Timer();
				if (this.getTimeout() > 0) {
					// Schedule process destruction for commandTimeout
					// milliseconds in the future
					commandTimer.schedule(new TimerTask() {
						public void run() {
							LOGGER.warning("[" + getName()
									+ "] command timeout '" + indexerCommand
									+ "', destroying process.");
							process.destroy();
						}
					}, this.getTimeout());
				}

				// Wait for process to complete
				process.waitFor();
				// Cancel the timer if it was not triggered
				commandTimer.cancel();
				LOGGER.info("[" + getName() + "] command '" + indexerCommand
						+ "' exited with status '" + process.exitValue() + "'");

				// send heartbeat info
				HeartbeatListener.sendHeartbeatMessage(getName(), "command",
						indexerCommand);
				HeartbeatListener.sendHeartbeatMessage(getName(), "exit value",
						Integer.toString(process.exitValue()));
			}
		}

		if (autoArchive) {
			Iterator<IndexerChange> changeIter = change.getIndexerChanges()
					.iterator();
			ProductStorage storage = getStorage();
			while (changeIter.hasNext()) {
				IndexerChange nextChange = changeIter.next();
				if (nextChange.getType() == IndexerChangeType.PRODUCT_ARCHIVED) {
					// one product being archived
					if (change.getSummary() != null) {
						ProductId productId = change.getSummary().getId();
						LOGGER.log(Level.FINER,
								"[" + getName() + "] auto archiving product "
										+ productId.toString());
						storage.removeProduct(productId);
					}
				} else if (nextChange.getType() == IndexerChangeType.EVENT_ARCHIVED) {
					// all products on event being archived
					Event changeEvent = nextChange.getOriginalEvent();
					LOGGER.log(Level.FINER,
							"[" + getName() + "] auto archiving event "
									+ changeEvent.getEventId() + " products");
					Iterator<ProductSummary> productIter = changeEvent
							.getAllProductList().iterator();
					while (productIter.hasNext()) {
						ProductId productId = productIter.next().getId();
						LOGGER.log(Level.FINER,
								"[" + getName() + "] auto archiving product "
										+ productId.toString());
						storage.removeProduct(productId);
					}
				}
			}
		}
	}


	/**
	 * Get the product command and add the indexer arguments to it.
	 * 
	 * @param change
	 *            The IndexerEvent received by the ExternalIndexerListener
	 * @return the command to execute with its arguments as a string
	 * @throws Exception
	 */
	public String getProductSummaryCommand(IndexerEvent change,
			IndexerChange indexerChange) throws Exception {
		ProductSummary summary = change.getSummary();

		Event event = indexerChange.getNewEvent();
		// When archiving events include event information
		if (event == null && indexerChange.getType() == IndexerChangeType.EVENT_ARCHIVED) {
			event = indexerChange.getOriginalEvent();
		}

		String command = getProductSummaryCommand(event, summary);

		// Tells external indexer what type of index event occurred.
		command = command + " " +
				ExternalIndexerListener.EVENT_ACTION_ARGUMENT +
				indexerChange.getType().toString();

		return command;
	}

	/**
	 * Get the command for a specific event and summary.
	 *
	 * @param event
	 * @param summary
	 * @return command line arguments as a string.
	 * @throws Exception
	 */
	public String getProductSummaryCommand(Event event, ProductSummary summary) throws Exception {
		StringBuffer indexerCommand = new StringBuffer(getCommand());

		if (event != null) {
			EventSummary eventSummary = event.getEventSummary();
			indexerCommand.append(" ")
					.append(ExternalIndexerListener.PREFERRED_ID_ARGUMENT)
					.append(eventSummary.getId());
			indexerCommand
					.append(" ")
					.append(ExternalIndexerListener.PREFERRED_EVENTSOURCE_ARGUMENT)
					.append(eventSummary.getSource());
			indexerCommand
					.append(" ")
					.append(ExternalIndexerListener.PREFERRED_EVENTSOURCECODE_ARGUMENT)
					.append(eventSummary.getSourceCode());
			Map<String, List<String>> eventids = event.getAllEventCodes(true);
			Iterator<String> sourceIter = eventids.keySet().iterator();
			indexerCommand.append(" ").append(EVENT_IDS_ARGUMENT);
			while (sourceIter.hasNext()) {
				String source = sourceIter.next();
				Iterator<String> sourceCodeIter = eventids.get(source).iterator();
				while (sourceCodeIter.hasNext()) {
					String sourceCode = sourceCodeIter.next();
					indexerCommand.append(source).append(sourceCode);
					if (sourceCodeIter.hasNext() || sourceIter.hasNext()) {
						indexerCommand.append(",");
					}
				}
			}

			indexerCommand.append(" ").append(PREFERRED_MAGNITUDE_ARGUMENT)
					.append(eventSummary.getMagnitude());
			indexerCommand.append(" ").append(PREFERRED_LATITUDE_ARGUMENT)
					.append(eventSummary.getLatitude());
			indexerCommand.append(" ").append(PREFERRED_LONGITUDE_ARGUMENT)
					.append(eventSummary.getLongitude());
			indexerCommand.append(" ").append(PREFERRED_DEPTH_ARGUMENT)
					.append(eventSummary.getDepth());
			String eventTime = null;
			if (event.getTime() != null) {
				eventTime = XmlUtils.formatDate(event.getTime());
			}
			indexerCommand.append(" ").append(PREFERRED_ORIGIN_TIME_ARGUMENT)
					.append(eventTime);
		}

		if (summary != null) {

			File productDirectory = getStorage().getProductFile(summary.getId());
			if (productDirectory.exists()) {
				// Add the directory argument
				indexerCommand.append(" ")
						.append(CLIProductBuilder.DIRECTORY_ARGUMENT)
						.append(productDirectory.getCanonicalPath());
			}

			// Add arguments from summary
			indexerCommand.append(" ").append(CLIProductBuilder.TYPE_ARGUMENT)
					.append(summary.getType());
			indexerCommand.append(" ").append(CLIProductBuilder.CODE_ARGUMENT)
					.append(summary.getCode());
			indexerCommand.append(" ").append(CLIProductBuilder.SOURCE_ARGUMENT)
					.append(summary.getSource());
			indexerCommand.append(" ")
					.append(CLIProductBuilder.UPDATE_TIME_ARGUMENT)
					.append(XmlUtils.formatDate(summary.getUpdateTime()));
			indexerCommand.append(" ").append(CLIProductBuilder.STATUS_ARGUMENT)
					.append(summary.getStatus());
			if (summary.isDeleted()) {
				indexerCommand.append(" ")
						.append(CLIProductBuilder.DELETE_ARGUMENT);
			}

			// Add optional tracker URL argument
			if (summary.getTrackerURL() != null) {
				indexerCommand.append(" ")
						.append(CLIProductBuilder.TRACKER_URL_ARGUMENT)
						.append(summary.getTrackerURL());
			}

			// Add property arguments
			Map<String, String> props = summary.getProperties();
			Iterator<String> iter = props.keySet().iterator();
			while (iter.hasNext()) {
				String name = iter.next();
				indexerCommand.append(" \"")
						.append(CLIProductBuilder.PROPERTY_ARGUMENT).append(name)
						.append("=").append(props.get(name).replace("\"", "\\\""))
						.append("\"");
			}

			// Add link arguments
			Map<String, List<URI>> links = summary.getLinks();
			iter = links.keySet().iterator();
			while (iter.hasNext()) {
				String relation = iter.next();
				Iterator<URI> iter2 = links.get(relation).iterator();
				while (iter2.hasNext()) {
					indexerCommand.append(" ")
							.append(CLIProductBuilder.LINK_ARGUMENT)
							.append(relation).append("=")
							.append(iter2.next().toString());
				}
			}
		}

		Product product = null;
		try {
			product = getStorage().getProduct(summary.getId());
		} catch (Exception e) {
			// when archiving product may not exist
			LOGGER.log(
					Level.FINE,
					"Exception retreiving product from storage, probably archiving",
					e);
		}
		if (product != null) {
			// Can only add these arguments if there is a product
			Content content = product.getContents().get("");
			if (content != null) {
				indexerCommand.append(" ").append(
						CLIProductBuilder.CONTENT_ARGUMENT);
				indexerCommand.append(" ")
						.append(CLIProductBuilder.CONTENT_TYPE_ARGUMENT)
						.append(content.getContentType());
			}

			if (product.getSignature() != null) {
				indexerCommand
						.append(" ")
						.append(ExternalNotificationListener.SIGNATURE_ARGUMENT)
						.append(product.getSignature());
			}

		}

		return indexerCommand.toString();
	}

	/**
	 * Configure an ExternalNotificationListener using a Config object.
	 * 
	 * @param config
	 *            the config containing a
	 */
	public void configure(Config config) throws Exception {
		super.configure(config);

		command = config.getProperty(COMMAND_PROPERTY);
		if (command == null) {
			throw new ConfigurationException("[" + getName()
					+ "] 'command' is a required configuration property");
		}
		LOGGER.config("[" + getName() + "] command is '" + command + "'");

		// storage references an object in the global configuration
		String storageName = config.getProperty(STORAGE_NAME_PROPERTY);
		String directoryName = config.getProperty(STORAGE_DIRECTORY_PROPERTY);
		if (storageName == null && directoryName == null) {
			throw new ConfigurationException("[" + getName()
					+ "] one of 'storage' or 'storageDirectory' is required");
		}

		if (storageName != null) {
			LOGGER.config("[" + getName() + "] loading FileProductStorage '"
					+ storageName + "'");
			storage = (FileProductStorage) Config.getConfig().getObject(
					storageName);
			if (storage == null) {
				throw new ConfigurationException("[" + getName()
						+ "] unable to load FileProductStorage '" + storageName
						+ "'");
			}
		} else {
			LOGGER.config("[" + getName() + "] using storage directory '"
					+ directoryName + "'");
			storage = new FileProductStorage(new File(directoryName));
			storage.setName(getName() + "-storage");
		}

		autoArchive = Boolean.valueOf(config.getProperty(AUTO_ARCHIVE_PROPERTY,
				AUTO_ARCHIVE_DEFAULT));
		LOGGER.config("[" + getName() + "] autoArchive = " + autoArchive);
	}

	/**
	 * Called when client is shutting down.
	 */
	public void shutdown() throws Exception {
		super.shutdown();
		// TODO: make current process a member and kill process?
		// or find way of detaching so client process can exit but product
		// process can complete?
		storage.shutdown();
	}

	/**
	 * Called after client has been configured and should begin processing.
	 */
	public void startup() throws Exception {
		// no background threads to start or objects to create
		storage.startup();
		super.startup();
	}

	/**
	 * @return the storage
	 */
	public FileProductStorage getStorage() {
		return storage;
	}

	/**
	 * @param storage
	 *            the storage to set
	 */
	public void setStorage(FileProductStorage storage) {
		this.storage = storage;
	}

	/**
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @param command
	 *            the command to set
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * @return the autoArchive
	 */
	public boolean isAutoArchive() {
		return autoArchive;
	}

	/**
	 * @param autoArchive
	 *            the autoArchive to set
	 */
	public void setAutoArchive(boolean autoArchive) {
		this.autoArchive = autoArchive;
	}

}
