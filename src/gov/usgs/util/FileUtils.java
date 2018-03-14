/*
 * FileUtils
 *
 * $Id$
 * $HeadURL$
 */
package gov.usgs.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;

/**
 * File input, output, content type, and delete utilities.
 * 
 * @author jmfee
 * 
 */
public class FileUtils {

	/** For looking up file mime types. */
	private static final FileNameMap MIMETYPES = URLConnection.getFileNameMap();

	/**
	 * Get a file mime type based on its filename.
	 * 
	 * Calls getContentType(file.getName()).
	 * 
	 * @param file
	 * @return String mime type.
	 */
	public static String getContentType(final File file) {
		return getContentType(file.getName());
	}

	/**
	 * Get a file mime type based on its file path extension.
	 * 
	 * Uses URLConnection.getFileNameMap().
	 * 
	 * @param filename
	 *            file path.
	 * @return String mime type.
	 */
	public static String getContentType(final String filename) {
		return MIMETYPES.getContentTypeFor(filename);
	}

	/**
	 * Read file contents into a byte array.
	 * 
	 * @param file
	 *            file to read.
	 * @return byte array of file content.
	 * @throws IOException
	 *             if an error occurs while reading.
	 */
	public static byte[] readFile(final File file) throws IOException {
		return StreamUtils.readStream(file);
	}

	/**
	 * Write a file's content.
	 * 
	 * @param file
	 *            file to write.
	 * @param content
	 *            content to write to file.
	 */
	public static void writeFile(final File file, final byte[] content)
			throws IOException {
		StreamUtils.transferStream(content, file);
	}

	/**
	 * Write a file's content atomically.
	 * 
	 * @param tempfile
	 *            where file is written.
	 * @param file
	 *            where tempfile is moved after writing.
	 * @param content
	 *            file content to write.
	 * @throws IOException
	 *             if any errors occur.
	 */
	public static void writeFileThenMove(final File tempfile, final File file,
			final byte[] content) throws IOException {
		writeFile(tempfile, content);

		File parent = file.getAbsoluteFile().getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}
		tempfile.renameTo(file);
	}

	/**
	 * Delete all files recursively within tree.
	 * 
	 * @param path
	 *            root of tree to delete.
	 */
	public static void deleteTree(final File path) {
		if (path.exists()) {
			if (path.isDirectory()) {
				File[] files = path.listFiles();
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						deleteTree(files[i]);
					} else {
						files[i].delete();
					}
				}
				files = null;
			} else {
				path.delete();
			}
		}

		deleteEmptyParents(path);
	}

	/**
	 * Delete path and any empty parent directories.
	 * 
	 * @param path
	 *            directory to start in.
	 */
	public static void deleteEmptyParents(final File path) {
		deleteEmptyParents(path, null);
	}

	/**
	 * Delete path and any empty parent directories up to the stopAt point.
	 * 
	 * @param path
	 *            direcotry to start in
	 * @param stopAt
	 *            directory to stop at
	 */
	public static void deleteEmptyParents(final File path, final File stopAt) {
		File parent = path;
		// Loop while parent is not null and we are not yet at the stopAt point
		while (parent != null && !parent.equals(stopAt)) {
			try {
				parent.delete();
				parent = parent.getParentFile();
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
	}

	/**
	 * Extracts a resource file from within the executing JAR and copies it to
	 * the file system.
	 * 
	 * @param rsFile
	 *            Absolute file path (relative to JAR root) of the file to
	 *            extract.
	 * @param fsFile
	 *            File path where to place the extracted file. An absolute path
	 *            here is relative to the file system root. Relative paths for
	 *            this value are relative to the CWD.
	 */
	public static void extractResourceFile(String rsFile, String fsFile) {
		InputStream is = FileUtils.class.getResourceAsStream(rsFile);
		try {
			FileOutputStream os = new FileOutputStream(fsFile);
			try {
				byte[] buf = new byte[1024];
				while (true) {
					int len = is.read(buf);
					if (len < 0) {
						break;
					}
					os.write(buf, 0, len);
				} // END: while

				// Do we have a logger yet?
				// System.err.println("Copied " + rsFile + " from jar file to " +
				// fsFile+" in the file system.  ("+totalCopied+" bytes).");
			} finally {
				StreamUtils.closeStream(os);
			}
		} catch (FileNotFoundException fnf) {
			// Logger?
			System.err.println(fnf.getMessage() + " -- 1");
		} catch (IOException iox) {
			// Logger?
			System.err.println(iox.getMessage() + " -- 2");
		} catch (NullPointerException npx) {
			// A dummy catch
		} finally {
			StreamUtils.closeStream(is);
		}
	}

}
