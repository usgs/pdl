/*
 * DirectoryPoller
 *
 * $Id$
 * $HeadURL$
 */
package gov.usgs.util;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Monitor a directory for files, notifying FileListenerInterfaces.
 * 
 * Implementers of the FileListenerInterface should process files before
 * returning, because these files may move or disappear.
 */
public class DirectoryPoller {

	/** Timer schedules polling frequency. */
	private Timer timer = new Timer();

	/** Directory to watch. */
	private final File pollDirectory;

	/** Directory to store files in. */
	private final File storageDirectory;

	/** Notification of files. */
	private List<FileListenerInterface> listeners = new LinkedList<FileListenerInterface>();

	/**
	 * Create a DirectoryPoller.
	 * 
	 * @param pollDirectory
	 *            directory that is polled for new files.
	 * @param storageDirectory
	 *            directory where polled files are moved. When null, polled
	 *            files are deleted after calling listeners.
	 */
	public DirectoryPoller(final File pollDirectory, final File storageDirectory) {
		if (!pollDirectory.exists()) {
			pollDirectory.mkdirs();
		}
		this.pollDirectory = pollDirectory;

		if (storageDirectory != null && !storageDirectory.exists()) {
			storageDirectory.mkdirs();
		}
		this.storageDirectory = storageDirectory;
	}

	public File getPollDirectory() {
		return this.pollDirectory;
	}

	public File getStorageDirectory() {
		return this.storageDirectory;
	}

	public void addFileListener(final FileListenerInterface listener) {
		listeners.add(listener);
	}

	public void removeFileListener(final FileListenerInterface listener) {
		listeners.remove(listener);
	}

	/**
	 * Start polling in a background thread.
	 * 
	 * Any previously scheduled polling is stopped before starting at this
	 * frequency. This schedules using fixed-delay (time between complete polls)
	 * as opposed to fixed-rate (how often to start polling).
	 * 
	 * @param frequencyInMilliseconds
	 *            how often to poll.
	 */
	public void start(final long frequencyInMilliseconds) {
		if (timer != null) {
			// already started
			stop();
		}

		timer = new Timer();
		timer.schedule(new PollTask(), 0L, frequencyInMilliseconds);
	}

	/**
	 * Stop any currently scheduled polling.
	 */
	public void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * The Polling Task. Notifies all listeners then either deletes or moves the
	 * file to storage.
	 * 
	 * @author jmfee
	 * 
	 */
	protected class PollTask extends TimerTask {
		public void run() {
			// get files from poll directory
			File[] files = pollDirectory.listFiles();
			for (File file : files) {
				// send file to listeners
				notifyListeners(file);
				// move file to storage
				moveToStorage(file);
			}
		}
	}

	/**
	 * Notify all listeners that files exist and need to be processed.
	 * 
	 * @param file
	 */
	public void notifyListeners(final File file) {
		Iterator<FileListenerInterface> iter = new LinkedList<FileListenerInterface>(
				listeners).iterator();
		while (iter.hasNext()) {
			try {
				iter.next().onFile(file);
			} catch (Exception e) {
				// keep notifying other listeners
			}
		}
	}

	/**
	 * Move a file from polldir to storage directory. Attempts to move file into
	 * storage directory. The file is not moved if no storage directory was
	 * specified, or if the file no longer exists.
	 * 
	 * @param file
	 *            file to move.
	 */
	private void moveToStorage(final File file) {
		if (storageDirectory == null) {
			// nowhere to move, just delete
			file.delete();
			return;
		}

		if (!file.exists()) {
			// was already removed, done
			return;
		}

		// build a filename that doesn't exist
		String fileName = file.getName();
		File storageFile = new File(storageDirectory, fileName);
		if (storageFile.exists()) {
			fileName = new Date().getTime() + "_" + fileName;
			storageFile = new File(storageDirectory, fileName);
		}
		// rename file
		file.renameTo(storageFile);
	}

}
