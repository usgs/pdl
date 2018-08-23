package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.ProcessTimeoutException;
import gov.usgs.util.StringUtils;
import gov.usgs.util.TimeoutProcess;
import gov.usgs.util.TimeoutProcessBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReplicationStorageListener extends DefaultStorageListener {

	private static final Logger LOGGER = Logger
			.getLogger(ReplicationStorageListener.class.getName());

	/**
	 * Name of the property specifying whether to use archive flag on the
	 * replication.
	 */
	public static final String ARCHIVE_FLAG_PROPERTY = "archiveSync";

	/**
	 * Name of the property specifying the replication command on the host
	 * system.
	 */
	public static final String REPL_CMD_PROPERTY = "rsync";

	/**
	 * Name of property indicating how many times the replication should be
	 * attempted before considering it a failure.
	 */
	public static final String REPL_MAX_TRIES_PROPERTY = "maxTries";

	/**
	 * Name of the property specifying how long to wait for the replication to
	 * complete successfully.
	 */
	public static final String REPL_TIMEOUT_PROPERTY = "timeout";

	/**
	 * Name of property specifying to which hosts the storage should be
	 * replicated.
	 */
	public static final String REPL_HOSTS_PROPERTY = "targetHosts";

	/** Default. Use archiving. */
	private static final boolean ARCHIVE_FLAG_DEFAULT = true;

	/** Default replication command */
	private static final String REPL_CMD_DEFAULT = "rsync";

	/** Default number of times to try replication. */
	private static final int REPL_MAX_TRIES_DEFAULT = 1;

	/** Default replication timeout (milliseconds). */
	private static final long REPL_TIMEOUT_DEFAULT = 30000L;

	/** Default replication hosts. None. */
	private static final Map<String, ExecutorService> REPL_HOSTS_DEFAULT = new HashMap<String, ExecutorService>();

	private boolean archiveFlag = ARCHIVE_FLAG_DEFAULT;
	private String replCmd = REPL_CMD_DEFAULT;
	private int replMaxTries = REPL_MAX_TRIES_DEFAULT;
	private long replTimeout = REPL_TIMEOUT_DEFAULT;
	private Map<String, ExecutorService> replHosts = REPL_HOSTS_DEFAULT;

	/**
	 * Default constructor used when this object is instantiated via
	 * configuration.
	 */
	public ReplicationStorageListener() {
	}

	public ReplicationStorageListener(final boolean archiveFlag,
			String replCmd, final long replTimeout, final List<String> replHosts) {
		this.archiveFlag = archiveFlag;
		this.replCmd = replCmd;
		this.replTimeout = replTimeout;
		setReplHosts(replHosts);
	}

	protected void setReplHosts(List<String> replHosts) {
		this.replHosts = new HashMap<String, ExecutorService>();
		Iterator<String> replHostsIter = replHosts.iterator();
		while (replHostsIter.hasNext()) {
			String replHost = replHostsIter.next();
			ExecutorService service = Executors.newSingleThreadExecutor();
			this.replHosts.put(replHost, service);
		}
	}

	@Override
	public void configure(Config config) {

		// -- Configure the archive flag property
		try {
			String useArchive = config.getProperty(ARCHIVE_FLAG_PROPERTY);
			if ("TRUE".equalsIgnoreCase(useArchive)) {
				archiveFlag = true;
			} else {
				archiveFlag = false;
			}
		} catch (Exception ex) {
			LOGGER.warning("[" + getName()
					+ "] replicationStorageListener::Archive flag " + ""
					+ "misconfigured. Using default.");
			archiveFlag = ARCHIVE_FLAG_DEFAULT;
		}

		// -- Configure the replication command property
		try {
			replCmd = config.getProperty(REPL_CMD_PROPERTY);
			if (replCmd == null || "".equals(replCmd)) {
				replCmd = REPL_CMD_DEFAULT;
			}
		} catch (Exception ex) {
			LOGGER.warning("[" + getName()
					+ "] replicationStorageListener::Exception "
					+ "configuring replication command. (" + ex.getMessage()
					+ ")");
		}

		// -- Configure the replication max tries property
		try {
			replMaxTries = Integer.parseInt(config
					.getProperty(REPL_MAX_TRIES_PROPERTY));
		} catch (NumberFormatException npx) {
			LOGGER.warning("[" + getName()
					+ "] replicationStorageListener::Bad value for "
					+ "replication max tries. Using default.");
			replTimeout = REPL_MAX_TRIES_DEFAULT;
		} catch (NullPointerException npx) {
			// User didn't configure timeout. Just use default; no warning.
			replTimeout = REPL_MAX_TRIES_DEFAULT;
		}

		// -- Configure the replication timeout property
		try {
			replTimeout = Long.parseLong(config
					.getProperty(REPL_TIMEOUT_PROPERTY));
		} catch (NumberFormatException npx) {
			LOGGER.warning("[" + getName()
					+ "] replicationStorageListener::Bad value for "
					+ "replication timeout. Using default.");
			replTimeout = REPL_TIMEOUT_DEFAULT;
		} catch (NullPointerException npx) {
			// User didn't configure timeout. Just use default; no warning.
			replTimeout = REPL_TIMEOUT_DEFAULT;
		}

		// -- Configure the replication hosts property
		try {
			setReplHosts(StringUtils.split(
					config.getProperty(REPL_HOSTS_PROPERTY), ","));
		} catch (Exception ex) {
			LOGGER.warning("["
					+ getName()
					+ "] replicationStorageListener::No replication hosts configured.");
			replHosts = REPL_HOSTS_DEFAULT;
		}
	}

	@Override
	public void onProductStored(StorageEvent event) throws Exception {
		if (!(event.getProductStorage() instanceof FileProductStorage)) {
			return; // Can't replicate a non-file product storage
		}
		LOGGER.info("[" + getName() + "] product stored. Replicating. ("
				+ event.getProductId().toString() + ")");
		syncProductContents((FileProductStorage) event.getProductStorage(),
				event.getProductId(), false);
		LOGGER.info("[" + getName() + "] product replicated to remote. ("
				+ event.getProductId().toString() + ")");
	}

	@Override
	public void onProductRemoved(StorageEvent event) throws Exception {
		if (!(event.getProductStorage() instanceof FileProductStorage)) {
			return; // Can't replicate a non-file product storage
		}

		LOGGER.info("[" + getName() + "] product removed. Replicating. ("
				+ event.getProductId().toString() + ")");
		syncProductContents((FileProductStorage) event.getProductStorage(),
				event.getProductId(), true);
		LOGGER.info("[" + getName() + "] product removal replicated. ("
				+ event.getProductId().toString() + ")");
	}

	protected void syncProductContents(FileProductStorage storage,
			ProductId id, boolean deleting) throws IOException {

		final File baseDir = storage.getBaseDirectory();
		final String path = storage.getProductPath(id);

		Iterator<String> replHostsIter = replHosts.keySet().iterator();
		while (replHostsIter.hasNext()) {
			final String replHost = replHostsIter.next();
			final ExecutorService service = replHosts.get(replHost);
			service.submit(new ReplicationTask(createReplicationCommand(
					baseDir, path, replHost, deleting), baseDir, replMaxTries,
					replTimeout, service));
		}
	}

	/**
	 * Create the replication command.
	 * 
	 * @param baseDir
	 *            The directory from which replication will be executed.
	 * @param path
	 *            The path of the content to replicate
	 * @param host
	 *            The host string to which content should be replicated. Format
	 *            = user@host:path
	 * @param deleting
	 *            Flag whether this should be a deleting replication or not
	 * 
	 * @return The command and arguments as a list suitable for a
	 *         <code>ProcessBuilder</code>.
	 * @throws IOException 
	 */
	protected List<String> createReplicationCommand(final File baseDir,
			final String path, final String host, final boolean deleting) throws IOException {

		// Make sure we are replicating a directory that actually exists
		File source = new File(baseDir, path);

		while (!source.exists() && !source.getParentFile().equals(baseDir)) {
			source = source.getParentFile();
		}

		// StringBuffer command = new StringBuffer(replCmd);
		List<String> command = new ArrayList<String>();
		command.add(replCmd);

		if (archiveFlag) {
			command.add("-a");
		}

		command.add("-vz");
		command.add("--relative");
		command.add("-e");
		command.add("ssh -o ConnectTimeout=5");

		if (deleting) {
			// To do a delete we must sync the parent directory and then
			// explicitly include the original target directory and exclude
			// everything else.
			command.add("--delete");
			command.add("--include='" + source.getName() + "**'");
			command.add("--exclude='*'");
			source = source.getParentFile();
		} else {

		}

		command.add("."
				+ source.getCanonicalPath().replace(baseDir.getCanonicalPath(),
						""));

		command.add(host);

		return command;
	}

	protected class ReplicationTask extends Thread {

		// Command to execute
		private List<String> command = null;
		// String representation of command
		private String cmdStr = null;
		// Working directory from where to execute the command
		private File cwd = null;
		// Number of times to try replication
		private int numTries = 1;
		// How long to let the command try for
		private long timeout = 1000L;
		// Executor service to repeat this task if appropriate
		private ExecutorService service = null;

		public ReplicationTask(final List<String> command, final File cwd,
				final int numTries, final long timeout,
				final ExecutorService service) {
			this.command = command;
			this.cwd = cwd;
			this.timeout = timeout;
			this.numTries = numTries;
			this.service = service;

			// Command string for easier viewing
			StringBuffer buf = new StringBuffer();
			Iterator<String> iter = command.iterator();
			while (iter.hasNext()) {
				buf.append(iter.next()).append(" ");
			}
			this.cmdStr = buf.toString().trim();

		}

		public void run() {
			try {
				TimeoutProcessBuilder builder = new TimeoutProcessBuilder(
						timeout, command);
				builder.directory(cwd);
				TimeoutProcess process = builder.start();
				int exitStatus = process.waitFor();

				LOGGER.info("[" + getName() + "] command \"" + cmdStr
						+ "\" exited with status [" + exitStatus + "]");
				if (exitStatus != 0) {
					LOGGER.info("[" + getName() + "] command \"" + cmdStr
							+ "\" error output: " + new String(process.errorOutput()));
				}
			} catch (ProcessTimeoutException cex) {

				StringBuffer message = new StringBuffer();
				message.append("[" + getName() + "] command \"").append(cmdStr)
						.append("\" timed out.");

				if (numTries > 0) {
					// Try again
					message.append(" Trying again.");
					service.submit(this);
				} else {
					message.append(" Not retrying.");
				}
				LOGGER.warning(message.toString());
			} catch (IOException iox) {
				LOGGER.log(Level.WARNING, iox.getMessage(), iox);
			} catch (InterruptedException iex) {
				LOGGER.warning(iex.getMessage());
			}
		}
	}
}
