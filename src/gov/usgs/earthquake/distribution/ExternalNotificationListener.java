/*
 * ExternalNotificationListener
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

import java.io.File;

import java.net.URI;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An external process that is called when new products arrive.
 * 
 * The ExternalNotificationListener implements the Configurable interface and
 * can use the following configuration parameters:
 * 
 * <dl>
 * <dt>command</dt>
 * <dd>(Required) The command to execute. This must be an executable command and
 * may include arguments. Any product specific arguments are appended at the end
 * of command.</dd>
 * 
 * <dt>storage</dt>
 * <dd>(Required) A directory used to store all products. Each product is
 * extracted into a separate directory within this directory and is referenced
 * by the --directory=/path/to/directory argument when command is executed.</dd>
 * </dl>
 * 
 */
public class ExternalNotificationListener extends DefaultNotificationListener {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(ExternalNotificationListener.class.getName());

	/** Configuration parameter for storage directory product. */
	public static final String STORAGE_NAME_PROPERTY = "storage";

	/** Configuration parameter for command. */
	public static final String COMMAND_PROPERTY = "command";

	/** Argument used to pass signature to external process. */
	public static final String SIGNATURE_ARGUMENT = "--signature=";

	private static final String STORAGE_DIRECTORY_PROPERTY = "storageDirectory";

	/** Where products are stored in extracted form. */
	private FileProductStorage storage;

	/** Command that is executed after a product is stored. */
	private String command;

	/**
	 * Construct a new ExternalNotificationListener.
	 * 
	 * The listener must be configured with a FileProductStorage and command to
	 * function.
	 */
	public ExternalNotificationListener() {
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
		String storageDirectory = config
				.getProperty(STORAGE_DIRECTORY_PROPERTY);
		if (storageName == null && storageDirectory == null) {
			throw new ConfigurationException("[" + getName()
					+ "] 'storage' is a required configuration property.");
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
					+ storageDirectory + "'");
			storage = new FileProductStorage(new File(storageDirectory));
		}
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
	 * Append product arguments to the base command.
	 * 
	 * @param product
	 *            the product used to generate arguments.
	 * @return command as a string.
	 * @throws Exception
	 */
	public String getProductCommand(final Product product) throws Exception {
		StringBuffer buf = new StringBuffer(command);

		ProductId id = product.getId();

		// get path to product in storage, should be a directory
		File productDirectory = storage.getProductFile(id);

		buf.append(" ").append(CLIProductBuilder.DIRECTORY_ARGUMENT)
				.append(productDirectory.getCanonicalPath());

		buf.append(" ").append(CLIProductBuilder.TYPE_ARGUMENT)
				.append(id.getType());
		buf.append(" ").append(CLIProductBuilder.CODE_ARGUMENT)
				.append(id.getCode());
		buf.append(" ").append(CLIProductBuilder.SOURCE_ARGUMENT)
				.append(id.getSource());
		buf.append(" ").append(CLIProductBuilder.UPDATE_TIME_ARGUMENT)
				.append(XmlUtils.formatDate(id.getUpdateTime()));
		buf.append(" ").append(CLIProductBuilder.STATUS_ARGUMENT)
				.append(product.getStatus());
		if (product.isDeleted()) {
			buf.append(" ").append(CLIProductBuilder.DELETE_ARGUMENT);
		}

		if (product.getTrackerURL() != null) {
			buf.append(" ").append(CLIProductBuilder.TRACKER_URL_ARGUMENT)
					.append(product.getTrackerURL().toString());
		}

		Map<String, String> props = product.getProperties();
		Iterator<String> iter = props.keySet().iterator();
		while (iter.hasNext()) {
			String name = iter.next();
			buf.append(" \"").append(CLIProductBuilder.PROPERTY_ARGUMENT)
					.append(name).append("=")
					.append(props.get(name).replace("\"", "\\\"")).append("\"");
		}

		Map<String, List<URI>> links = product.getLinks();
		iter = links.keySet().iterator();
		while (iter.hasNext()) {
			String relation = iter.next();
			Iterator<URI> iter2 = links.get(relation).iterator();
			while (iter2.hasNext()) {
				buf.append(" ").append(CLIProductBuilder.LINK_ARGUMENT)
						.append(relation).append("=")
						.append(iter2.next().toString());
			}
		}

		Content content = product.getContents().get("");
		if (content != null) {
			buf.append(" ").append(CLIProductBuilder.CONTENT_ARGUMENT);
			buf.append(" ").append(CLIProductBuilder.CONTENT_TYPE_ARGUMENT)
					.append(content.getContentType());
		}

		if (product.getSignature() != null) {
			buf.append(" ").append(SIGNATURE_ARGUMENT)
					.append(product.getSignature());
		}

		return buf.toString();
	}

	/**
	 * Split a command string into a command array.
	 * 
	 * This version uses a StringTokenizer to split arguments. Quoted arguments
	 * are supported (single or double), with quotes removed before passing to
	 * runtime. Double quoting arguments will preserve quotes when passing to
	 * runtime.
	 * 
	 * @param command
	 *            command to run.
	 * @return Array of arguments suitable for passing to
	 *         Runtime.exec(String[]).
	 */
	protected static String[] splitCommand(final String command) {
		List<String> arguments = new LinkedList<String>();
		String currentArgument = null;

		// use a tokenizer because that's how Runtime.exec does it currently...
		StringTokenizer tokens = new StringTokenizer(command);
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken();

			if (currentArgument == null) {
				currentArgument = token;
			} else {
				// continuing previous argument, that was split on whitespace
				currentArgument = currentArgument + " " + token;
			}

			if (currentArgument.startsWith("\"")) {
				// double quoted argument
				if (currentArgument.endsWith("\"")) {
					// that has balanced quotes
					// remove quotes and add argument
					currentArgument = currentArgument.substring(1,
							currentArgument.length() - 1);
				} else {
					// unbalanced quotes, keep going
					continue;
				}
			} else if (currentArgument.startsWith("'")) {
				// single quoted argument
				if (currentArgument.endsWith("'")) {
					// that has balanced quotes
					// remove quotes and add argument
					currentArgument = currentArgument.substring(1,
							currentArgument.length() - 1);
				} else {
					// unbalanced quotes, keep going
					continue;
				}
			}

			arguments.add(currentArgument);
			currentArgument = null;
		}

		if (currentArgument != null) {
			// weird, but add argument anyways
			arguments.add(currentArgument);
		}

		return arguments.toArray(new String[] {});
	}

	/**
	 * Call the external process for this product.
	 * 
	 * @param product
	 * @throws Exception
	 */
	public void onProduct(final Product product) throws Exception {
		// store product
		try {
			storage.storeProduct(product);
		} catch (ProductAlreadyInStorageException e) {
			LOGGER.info("[" + getName() + "] product already in storage "
					+ product.getId().toString());
		}

		// now run command
		String productCommand = null;
		Process process = null;
		int exitValue = -1;

		try {
			productCommand = getProductCommand(product);
			LOGGER.info("[" + getName() + "] running command " + productCommand);
			process = Runtime.getRuntime().exec(productCommand);

			// inline product content, may or may not be null
			Content content = product.getContents().get("");
			if (content != null) {
				StreamUtils.transferStream(content.getInputStream(),
						process.getOutputStream());
			} else {
				// need to close process stdin either way
				StreamUtils.closeStream(process.getOutputStream());
			}

			// TODO: log/capture process input/error streams

			exitValue = process.waitFor();
		} catch (Exception e) {
			if (process != null) {
				// make sure to kill zombies
				process.destroy();
			}

			// signal that process did not exit normally
			exitValue = -1;

			// give subclasses chance to handle exception
			commandException(product, productCommand, e);
		}

		// if process exited normally
		if (exitValue != -1) {
			// give subclasses chance to handle exitValue, which may be non-zero
			commandComplete(product, productCommand, exitValue);
		}
	}

	/**
	 * Called when the command finishes executing normally.
	 * 
	 * This implementation throws a NotificationListenerException if the
	 * exitValue is non-zero.
	 * 
	 * @param product
	 *            the product being processed.
	 * @param command
	 *            the generated command, as a string.
	 * @param exitValue
	 *            the exit status of the process.
	 * @throws Exception
	 *             When re-notification should occur, based on maxTries, or none
	 *             if done.
	 */
	public void commandComplete(final Product product, final String command,
			final int exitValue) throws Exception {
		LOGGER.info("[" + getName() + "] command '" + command
				+ "' exited with status '" + exitValue + "'");

		// send heartbeat info
		HeartbeatListener.sendHeartbeatMessage(getName(), "command", command);
		HeartbeatListener.sendHeartbeatMessage(getName(), "exit value",
				Integer.toString(exitValue));

		if (exitValue != 0) {
			throw new NotificationListenerException("[" + getName()
					+ "] command exited with status " + exitValue);
		}
	}

	/**
	 * Called when an exception occurs while running command.
	 * 
	 * This implementation throws a NotificationListenerException with exception
	 * as the cause.
	 * 
	 * @param product
	 *            product being processed
	 * @param productCommand
	 *            command that was built
	 * @param exception
	 *            exception that was thrown during execution. This will be an
	 *            InterruptedException if the process timed out.
	 * @throws Exception
	 *             When re-notification should occur, based on maxTries, or none
	 *             if done.
	 */
	public void commandException(final Product product,
			final String productCommand, final Exception exception)
			throws Exception {
		if (exception instanceof InterruptedException) {
			LOGGER.warning("[" + getName() + "] command '" + productCommand
					+ "' timed out");
		} else {
			LOGGER.log(Level.WARNING, "[" + getName()
					+ "] exception running command '" + productCommand + "'",
					exception);
		}

		// send heartbeat info
		HeartbeatListener.sendHeartbeatMessage(getName(), "exception",
				productCommand);
		HeartbeatListener.sendHeartbeatMessage(getName(), "exception class",
				exception.getClass().getName());

		throw new NotificationListenerException(exception);
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

}
