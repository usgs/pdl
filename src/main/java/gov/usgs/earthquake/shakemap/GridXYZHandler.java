/**
 * This class is currently not in use, but kept for posterity and the ability
 * to add this support back in if it is deemed necessary.
 */
package gov.usgs.earthquake.shakemap;

import java.util.zip.ZipInputStream;

import gov.usgs.util.StreamUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.math.BigDecimal;

import java.text.SimpleDateFormat;

/**
 * Parser for ShakeMap grid.xyz metadata.
 *
 * id magnitude latitude longitude month day year hour:minute:second timezone
 * lonMin latMin lonMax latMax (Process time: dow month day hour:minute:second
 * year) eventDescription 2009232_290541 4.2 41.94 -114.09 AUG 20 2009 06:44:11
 * GMT -115.327 41.0306 -112.844 42.8806 (Process time: Wed Aug 19 23:55:49
 * 2009) 73.1 miles NE of WELLS-NV
 */
public class GridXYZHandler {

	public static final SimpleDateFormat EVENT_TIMESTAMP_FORMAT = new SimpleDateFormat(
			"MMM dd yyyy HH:mm:ss zzz");
	public static final SimpleDateFormat PROCESS_TIMESTAMP_FORMAT = new SimpleDateFormat(
			"'(Process time: 'EEE MMM dd HH:mm:ss yyyy')'");

	private ShakeMap shakemap;

	public GridXYZHandler(ShakeMap shakemap) {
		this.shakemap = shakemap;
	}

	public ShakeMap getShakemap() {
		return shakemap;
	}

	public void setShakemap(ShakeMap shakemap) {
		this.shakemap = shakemap;
	}

	/**
	 * Read first line of grid.xyz file and set properties on ShakeMap object.
	 *
	 * @param in the grid.xyz input stream.
	 */
	public void parse(final InputStream in) throws Exception {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new ZipInputStream(in)));
			String firstLine = br.readLine();
			br.close();

			String parts[] = firstLine.split(" ");
			// id
			shakemap.setEventSourceCode(parts[0]);

			// magnitude
			shakemap.setMagnitude(new BigDecimal(parts[1]));
			// latitude
			shakemap.setLatitude(new BigDecimal(parts[2]));
			// longitude
			shakemap.setLongitude(new BigDecimal(parts[3]));

			// month day year hour:minute:second timezone
			String[] eventTimestampParts = new String[5];
			System.arraycopy(parts, 4, eventTimestampParts, 0, 5);
			shakemap.setEventTime(EVENT_TIMESTAMP_FORMAT.parse(join(" ",
					eventTimestampParts)));

			// lonMin
			shakemap.setMinimumLongitude(new BigDecimal(parts[9]));
			// latMin
			shakemap.setMinimumLatitude(new BigDecimal(parts[10]));
			// lonMax
			shakemap.setMaximumLongitude(new BigDecimal(parts[11]));
			// latMax
			shakemap.setMaximumLatitude(new BigDecimal(parts[12]));

			// (Process time: dow month day hour:minute:second year)
			String[] processTimestampParts = new String[7];
			System.arraycopy(parts, 13, processTimestampParts, 0, 7);
			shakemap.setProcessTimestamp(PROCESS_TIMESTAMP_FORMAT.parse(join(" ",
					processTimestampParts)));

			String eventDescription = "";
			for (int i = 20; i < parts.length; i++) {
				eventDescription += parts[i] + " ";
			}
			shakemap.setEventDescription(eventDescription.trim());
		} finally {
			StreamUtils.closeStream(in);
		}
	}

	protected String join(final String delimeter, final String[] parts) {
		StringBuffer buf = new StringBuffer();
		if (parts == null) {
			return "";
		}

		buf.append(parts[0]);
		for (int i = 1; i < parts.length; i++) {
			buf.append(delimeter);
			buf.append(parts[i]);
		}
		return buf.toString();
	}

}
