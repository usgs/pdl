/*
 * Ini
 *
 * $Id$
 * $URL$
 */
package gov.usgs.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import java.util.HashMap;

/**
 * Ini is a Properties that supports sections.
 *
 * Format Rules:
 * <ul>
 * <li>Empty lines are ignored.
 * <li>Leading and trailing white space are ignored.
 * <li>Comments must be on separate lines, and begin with '#' or ';'.
 * <li>Properties are key value pairs delimited by an equals: key = value
 * <li>Section Names are on separate lines, begin with '[' and end with ']'. Any
 * whitespace around the brackets is ignored.
 * <li>Any properties before the first section are in the "null" section
 * </ul>
 *
 * Format Example:
 *
 * <pre>
 * #comment about the global
 * global = value
 *
 * # comment about this section
 * ; another comment about this section
 * [ Section Name ]
 * section = value
 * </pre>
 *
 */
public class Ini extends Properties {

	/** Serialization version. */
	private static final long serialVersionUID = 1L;

	/** Section names map to Section properties. */
	private HashMap<String, Properties> sections = new HashMap<String, Properties>();

	public static final String COMMENT_START = ";";
	public static final String ALTERNATE_COMMENT_START = "#";

	public static final String SECTION_START = "[";
	public static final String SECTION_END = "]";
	public static final String PROPERTY_DELIMITER = "=";

	/**
	 * Same as new Ini(null).
	 */
	public Ini() {
		this(null);
	}

	/**
	 * Construct a new Ini with defaults.
	 *
	 * @param properties
	 *            a Properties or Ini object with defaults. If an Ini object,
	 *            also makes a shallow copy of sections.
	 */
	public Ini(final Properties properties) {
		super(properties);

		if (properties instanceof Ini) {
			sections.putAll(((Ini) properties).getSections());
		}
	}

	/**
	 * @return the section properties map.
	 */
	public HashMap<String, Properties> getSections() {
		return sections;
	}

	/**
	 * Get a section property.
	 *
	 * @param section
	 *            the section, if null calls getProperty(key).
	 * @param key
	 *            the property name.
	 * @return value or property, or null if no matching property found.
	 */
	public String getSectionProperty(String section, String key) {
		if (section == null) {
			return getProperty(key);
		} else {
			Properties props = sections.get(section);
			if (props != null) {
				return props.getProperty(key);
			} else {
				return null;
			}
		}
	}

	/**
	 * Set a section property.
	 *
	 * @param section
	 *            the section, if null calls super.setProperty(key, value).
	 * @param key
	 *            the property name.
	 * @param value
	 *            the property value.
	 * @return any previous value for key.
	 */
	public Object setSectionProperty(String section, String key, String value) {
		if (section == null) {
			return setProperty(key, value);
		} else {
			Properties props = sections.get(section);
			if (props == null) {
				// new section
				props = new Properties();
				sections.put(section, props);
			}
			return props.setProperty(key, value);
		}
	}

	/**
	 * Read an Ini input stream.
	 *
	 * @param inStream
	 *            the input stream to read.
	 * @throws IOException
	 *         if unable to parse input stream.
	 */
	public void load(InputStream inStream) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));

		// keep track of current line number
		int lineNumber = 0;
		// line being parsed
		String line;
		// section being parsed
		Properties section = null;

		while ((line = br.readLine()) != null) {
			lineNumber = lineNumber + 1;
			line = line.trim();

			// empty line or comment
			if (line.length() == 0 || line.startsWith(COMMENT_START)
					|| line.startsWith(ALTERNATE_COMMENT_START)) {
				// ignore
				continue;
			}

			// section
			else if (line.startsWith(SECTION_START)
					&& line.endsWith(SECTION_END)) {
				// remove brackets
				line = line.replace(SECTION_START, "");
				line = line.replace(SECTION_END, "");
				line = line.trim();

				// store all properties in section
				section = new Properties();
				getSections().put(line, section);
			}

			// parse as property
			else {
				int index = line.indexOf("=");
				if (index == -1) {
					throw new IOException("Expected " + PROPERTY_DELIMITER
							+ " on line " + lineNumber + ": '" + line + "'");
				} else {
					String[] parts = line.split(PROPERTY_DELIMITER, 2);
					String key = parts[0].trim();
					String value = parts[1].trim();
					if (section != null) {
						section.setProperty(key, value);
					} else {
						setProperty(key, value);
					}
				}
			}

		}

		br.close();
	}

	/**
	 * Write an Ini format to a PrintWriter.
	 *
	 * @param props
	 *            properties to write.
	 * @param writer
	 *            the writer that writes.
	 * @param header
	 *            an optioal header that will appear in comments at the start of
	 *            the ini format.
	 * @throws IOException
	 *         if unable to write output.
	 */
	@SuppressWarnings("unchecked")
	public static void write(final Properties props, final PrintWriter writer,
			String header) throws IOException {

		if (header != null) {
			// write the header
			writer.write(new StringBuffer(COMMENT_START).append(" ").append(
					header.trim().replace("\n", "\n" + COMMENT_START + " "))
					.append("\n").toString());
		}

		// write properties
		Iterator<String> iter = (Iterator<String>) Collections.list(
				props.propertyNames()).iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			writer.write(new StringBuffer(key).append(PROPERTY_DELIMITER)
					.append(props.getProperty(key)).append("\n").toString());
		}

		// write sections
		if (props instanceof Ini) {
			Ini ini = (Ini) props;
			iter = ini.getSections().keySet().iterator();
			while (iter.hasNext()) {
				String sectionName = iter.next();
				writer.write(new StringBuffer(SECTION_START)
						.append(sectionName).append(SECTION_END).append("\n")
						.toString());
				write(ini.getSections().get(sectionName), writer, null);
			}
		}

		// flush, but don't close
		writer.flush();
	}

	/**
	 * Calls write(new PrintWriter(out), header).
	 */
	public void store(OutputStream out, String header) throws IOException {
		write(this, new PrintWriter(out), header);
	}

	/**
	 * Write properties to an OutputStream.
	 *
	 * @param out
	 *            the OutputStream used for writing.
	 */
	public void save(final OutputStream out) {
		try {
			store(out, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write properties to a PrintStream.
	 *
	 * @param out
	 *            the PrintStream used for writing.
	 */
	public void list(PrintStream out) {
		try {
			store(out, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write properties to a PrintWriter.
	 *
	 * @param out
	 *            the PrintWriter used for writing.
	 */
	public void list(PrintWriter out) {
		try {
			write(this, out, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
