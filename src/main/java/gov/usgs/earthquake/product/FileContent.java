/*
 * FileContent
 */
package gov.usgs.earthquake.product;

import gov.usgs.util.StreamUtils;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

/**
 * Content stored in a file.
 */
public class FileContent extends AbstractContent {

	/** Used to look up file types. */
	private static MimetypesFileTypeMap SYSTEM_MIME_TYPES = new MimetypesFileTypeMap();

	/** Explicit list of file extensions with standard mime types. */
	private static Map<String, String> MIME_TYPES = new HashMap<String, String>();
	static {
		MIME_TYPES.put("atom", "application/atom+xml");
		MIME_TYPES.put("css", "text/css");
		MIME_TYPES.put("gif", "image/gif");
		MIME_TYPES.put("gz", "application/gzip");
		MIME_TYPES.put("html", "text/html");
		MIME_TYPES.put("jpg", "image/jpeg");
		MIME_TYPES.put("js", "text/javascript");
		MIME_TYPES.put("json", "application/json");
		MIME_TYPES.put("kml", "application/vnd.google-earth.kml+xml");
		MIME_TYPES.put("kmz", "application/vnd.google-earth.kmz");
		MIME_TYPES.put("pdf", "application/pdf");
		MIME_TYPES.put("png", "image/png");
		MIME_TYPES.put("ps", "application/postscript");
		MIME_TYPES.put("txt", "text/plain");
		MIME_TYPES.put("xml", "application/xml");
		MIME_TYPES.put("zip", "application/zip");
	}

	/** The actual content. */
	private File content;

	/**
	 * Construct a new FileContent that does not use a nested path. same as new
	 * FileContent(file, file.getParentFile());
	 * 
	 * @param file
	 *            the source of content.
	 */
	public FileContent(final File file) {
		this.content = file;
		this.setLastModified(new Date(file.lastModified()));
		this.setLength(file.length());
		this.setContentType(getMimeType(file));
	}

	/**
	 * Construct a new FileContent from a URLContent for legacy products
	 * 
	 * @param urlc
	 *            the source of content.
	 * @throws URISyntaxException
	 */
	public FileContent(final URLContent urlc) throws URISyntaxException {
		super(urlc);
		this.content = new File(urlc.getURL().toURI());
	}

	/**
	 * Convert a Content to a file backed content.
	 * 
	 * The file written is new File(baseDirectory, content.getPath()).
	 * 
	 * @param content
	 *            the content that will be converted to a file.
	 * @param toWrite
	 *            the file where content is written.
	 */
	public FileContent(final Content content, final File toWrite)
			throws IOException {
		super(content);

		// this file content object will use the newly created file
		this.content = toWrite;

		// make sure the parent directory exists
		File parent = toWrite.getCanonicalFile().getParentFile();
		if (!parent.isDirectory()) {
			parent.mkdirs();
		}

		// save handle to stream to force it closed
		OutputStream out = null;
		InputStream in = null;
		try {
			in = content.getInputStream();
			out = StreamUtils.getOutputStream(toWrite);
			// write the file
			StreamUtils.transferStream(in, out);
		} finally {
			// force the stream closed
			StreamUtils.closeStream(in);
			StreamUtils.closeStream(out);
		}

		// update modification date in filesystem
		toWrite.setLastModified(content.getLastModified().getTime());

		// verify file length
		Long length = getLength();
		if (length > 0) {
			if (!length.equals(toWrite.length())) {
				throw new IOException("Written file length ("
						+ toWrite.length()
						+ ") does not match non-zero content length (" + length
						+ ")");
			}
		}

		// length may still be <= 0 if content was input stream.
		setLength(toWrite.length());
	}

	/**
	 * @return an InputStream for the wrapped content.
	 */
	public InputStream getInputStream() throws IOException {
		return StreamUtils.getInputStream(content);
	}

	/**
	 * @return the wrapped file.
	 */
	public File getFile() {
		return content;
	}

	/**
	 * Search a directory for files. This is equivalent to
	 * getDirectoryContents(directory, directory).
	 * 
	 * @param directory
	 *            the directory to search.
	 * @return a map of relative paths to FileContent objects.
	 * @throws IOException
	 */
	public static Map<String, FileContent> getDirectoryContents(
			final File directory) throws IOException {
		File absDirectory = directory.getCanonicalFile();
		return getDirectoryContents(absDirectory, absDirectory);
	}

	/**
	 * Search a directory for files. The path to files relative to baseDirectory
	 * is used as a key in the returned map.
	 * 
	 * @param directory
	 *            the directory to search.
	 * @param baseDirectory
	 *            the directory used to compute relative paths.
	 * @return a map of relative paths to FileContent objects.
	 * @throws IOException
	 */
	public static Map<String, FileContent> getDirectoryContents(
			final File directory, final File baseDirectory) throws IOException {
		Map<String, FileContent> contents = new HashMap<String, FileContent>();

		// compute the base path once, and escape the pattern being matched
		String basePath = Pattern.quote(baseDirectory.getCanonicalPath()
				+ File.separator);

		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				// recurse into sub directory
				contents.putAll(getDirectoryContents(file.getCanonicalFile(),
						baseDirectory.getCanonicalFile()));
			} else {
				String path = file.getCanonicalPath().replaceAll(basePath, "");
				contents.put(path, new FileContent(file));
			}
		}

		return contents;
	}

	/**
	 * This implementation calls defaultGetMimeType, and exists so subclasses
	 * can override.
	 * 
	 * @param file
	 *            file to check.
	 * @return corresponding mime type.
	 */
	public String getMimeType(final File file) {
		return defaultGetMimeType(file);
	}

	/**
	 * Check a local list of mime types, and fall back to MimetypeFileTypesMap
	 * if not specified.
	 * 
	 * @param file
	 *            file to check.
	 * @return corresponding mime type.
	 */
	protected static String defaultGetMimeType(final File file) {
		String name = file.getName();
		int index = name.lastIndexOf('.');
		if (index != -1) {
			String extension = name.substring(index + 1);
			if (MIME_TYPES.containsKey(extension)) {
				return MIME_TYPES.get(extension);
			}
		}
		return SYSTEM_MIME_TYPES.getContentType(file);
	}

	/**
	 * Free any resources associated with this content.
	 */
	public void close() {
		// nothing to free
	}

}
