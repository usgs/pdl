package gov.usgs.earthquake.shakemap;

import gov.usgs.util.XmlUtils;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for ShakeMap grid.xml metadata.
 *
 * Accepts a ShakeMap object and updates the properties of that product based on
 * the product's grid.xml file.
 */
public class GridXMLHandler extends DefaultHandler {

	// ShakeMap grid parameters
		/** Element for shakemap grid */
		public static final String SHAKEMAPGRID_ELEMENT = "shakemap_grid";
		/** Shakemap grid id */
		public static final String SHAKEMAPGRID_ID = "shakemap_id";
		/** Shakemap grid originator */
		public static final String SHAKEMAPGRID_ORIGINATOR = "shakemap_originator";
		/** Shakemap grid process timestamp */
		public static final String SHAKEMAPGRID_TIMESTAMP = "process_timestamp";
		/** Shakemap grid version */
		public static final String SHAKEMAPGRID_VERSION = "shakemap_version";
		/** Shakemap grid event type */
		public static final String SHAKEMAPGRID_EVENT_TYPE = "shakemap_event_type";
		/** Shakemap grid event/map status */
		public static final String SHAKEMAPGRID_EVENT_STATUS = "map_status";

		// ShakeMap event parameters
		/** Element for event */
		public static final String EVENT_ELEMENT = "event";
		/** Event latitude */
		public static final String EVENT_LATITUDE = "lat";
		/** Event longitude */
		public static final String EVENT_LONGITUDE = "lon";
		/** Event magnitude */
		public static final String EVENT_MAGNITUDE = "magnitude";
		/** Event timestamp */
		public static final String EVENT_TIMESTAMP = "event_timestamp";
		/** Event description */
		public static final String EVENT_DESCRIPTION = "event_description";
		/** Event depth */
		public static final String EVENT_DEPTH = "depth";

		// These are new parameters used by GSM when it is contributing to a
		// different
		// network as a backup
		/** GSM Parameter when using a different network as a backup */
		public static final String EVENT_NETWORK = "event_network";
		/** GSM Parameter when using a different network as a backup */
		public static final String EVENT_ID = "event_id";

		// ShakeMap gridspec parameters
		/** Element for grid specification */
		public static final String GRIDSPEC_ELEMENT = "grid_specification";
		/** gridspec longitude min */
		public static final String GRIDSPEC_LONMIN = "lon_min";
		/** gridspec longitude max */
		public static final String GRIDSPEC_LONMAX = "lon_max";
		/** gridspec latitude min */
		public static final String GRIDSPEC_LATMIN = "lat_min";
		/** gridspec latitude max */
		public static final String GRIDSPEC_LATMAX = "lat_max";

		/** XML for SHAKEMAPGRID_ELEMENT */
		public static final String SHAKEMAPGRID_ELEMENT_XML = SHAKEMAPGRID_ELEMENT;
		/** XML for SHAKEMAPGRID_ID */
		public static final String SHAKEMAPGRID_ID_XML =  SHAKEMAPGRID_ELEMENT+ "[" + SHAKEMAPGRID_ID + "]";
		/** XML for SHAKEMAPGRID_ORIGINATOR */
		public static final String SHAKEMAPGRID_ORIGINATOR_XML = SHAKEMAPGRID_ELEMENT + "[" + SHAKEMAPGRID_ORIGINATOR + "]";
		/** XML for SHAKEMAPGRID_TIMESTAMP */
		public static final String SHAKEMAPGRID_TIMESTAMP_XML = SHAKEMAPGRID_ELEMENT + "[" + SHAKEMAPGRID_TIMESTAMP + "]";
		/** XML for SHAKEMAPGRID_VERSION */
		public static final String SHAKEMAPGRID_VERSION_XML = SHAKEMAPGRID_ELEMENT+ "[" + SHAKEMAPGRID_VERSION + "]";
		/** XML for SHAKEMAPGRID_EVENT_TYPE */
		public static final String SHAKEMAPGRID_EVENT_TYPE_XML = SHAKEMAPGRID_ELEMENT+ "[" + SHAKEMAPGRID_EVENT_TYPE + "]";
		/** XML for SHAKEMAPGRID_EVENT_STATUS */
		public static final String SHAKEMAPGRID_EVENT_STATUS_XML = SHAKEMAPGRID_ELEMENT+ "[" + SHAKEMAPGRID_EVENT_STATUS + "]";

		/** XML for EVENT_ELEMENT */
		public static final String EVENT_ELEMENT_XML = EVENT_ELEMENT;
		/** XML for EVENT_LATITUDE */
		public static final String EVENT_LATITUDE_XML = EVENT_ELEMENT + "[" + EVENT_LATITUDE + "]";
		/** XML for EVENT_LONGITUDE */
		public static final String EVENT_LONGITUDE_XML = EVENT_ELEMENT+ "[" + EVENT_LONGITUDE + "]";
		/** XML for EVENT_MAGNITUDE */
		public static final String EVENT_MAGNITUDE_XML = EVENT_ELEMENT+ "[" + EVENT_MAGNITUDE + "]";
		/** XML for EVENT_TIMESTAMP */
		public static final String EVENT_TIMESTAMP_XML = EVENT_ELEMENT+ "[" + EVENT_TIMESTAMP + "]";
		/** XML for EVENT_DESCRIPTION */
		public static final String EVENT_DESCRIPTION_XML = EVENT_ELEMENT+ "[" + EVENT_DESCRIPTION + "]";
		/** XML for EVENT_DEPTH */
		public static final String EVENT_DEPTH_XML = EVENT_ELEMENT+ "[" + EVENT_DEPTH + "]";
		/** XML for EVENT_NETWORK */
		public static final String EVENT_NETWORK_XML = EVENT_ELEMENT + "[" + EVENT_NETWORK + "]";
		/** XML for EVENT_ID */
		public static final String EVENT_ID_XML = EVENT_ELEMENT + "[" + EVENT_ID + "]";

		/** XML for GRIDSPEC_ELEMENT */
		public static final String GRIDSPEC_ELEMENT_XML = GRIDSPEC_ELEMENT;
		/** XML for GRIDSPEC_LONMIN */
		public static final String GRIDSPEC_LONMIN_XML = GRIDSPEC_ELEMENT+ "[" + GRIDSPEC_LONMIN + "]";
		/** XML for GRIDSPEC_LONMAX */
		public static final String GRIDSPEC_LONMAX_XML = GRIDSPEC_ELEMENT+ "[" + GRIDSPEC_LONMAX + "]";
		/** XML for GRIDSPEC_LATMIN */
		public static final String GRIDSPEC_LATMIN_XML = GRIDSPEC_ELEMENT+ "[" + GRIDSPEC_LATMIN + "]";
		/** XML for GRIDSPEC_LATMAX */
		public static final String GRIDSPEC_LATMAX_XML = GRIDSPEC_ELEMENT+ "[" + GRIDSPEC_LATMAX + "]";

	// ShakeMap griddata parameters
	/** Element for Shakemap griddata */
	public static final String GRIDDATA_ELEMENT = "grid_data";
	/** Shakemap griddata parameter to stop parsing before */
	public static final String STOP_PARSING_BEFORE_GRIDDATA =
			"Stop parsing before grid data.";

	private HashMap<String, String> grid = new HashMap<String, String>();

	/**
	 * Construct a new SAX Handler for an grid.xml document.
	 */
	public GridXMLHandler() {}

	/**
	 * @param in
	 *            - the file or stream to parse
	 * @return the ShakeMap associated with this XML handler
	 * @throws Exception if error occurs
	 */
	public HashMap<String, String> parse(final Object in) throws Exception {
		try {
			XmlUtils.parse(in, this);
		} catch (Exception se) {
			if (!se.getMessage().equals(STOP_PARSING_BEFORE_GRIDDATA)) {
				throw se;
			}
		}
		return this.grid;
	}

	/**
	 * @return the parsed info.
	 */
	public HashMap<String, String> getInfo() {
		return this.grid;
	}


	/**
	 * Read grid, event, and gridspec element attributes.
	 *
     * @param uri namespace of element.
     * @param localName name of element.
     * @param qName qualified name of element.
     * @param attributes element attributes.
	 * @throws SAXException when griddata element is reached, to stop parsing.
	 */
	public final void startElement(final String uri, final String localName,
			final String qName, final Attributes attributes)
			throws SAXException {

		if (localName != null && GRIDDATA_ELEMENT.equals(localName)) {
			// don't read the whole grid
			throw new SAXException(STOP_PARSING_BEFORE_GRIDDATA);
		}

		if (localName.equals(SHAKEMAPGRID_ELEMENT)) {
			this.grid.put(SHAKEMAPGRID_ORIGINATOR_XML, attributes.getValue(SHAKEMAPGRID_ORIGINATOR));
			this.grid.put(SHAKEMAPGRID_ID_XML, attributes.getValue(SHAKEMAPGRID_ID));
			this.grid.put(SHAKEMAPGRID_TIMESTAMP_XML, attributes.getValue(SHAKEMAPGRID_TIMESTAMP));
			this.grid.put(SHAKEMAPGRID_VERSION_XML, attributes.getValue(SHAKEMAPGRID_VERSION));
			this.grid.put(SHAKEMAPGRID_EVENT_TYPE_XML, attributes.getValue(SHAKEMAPGRID_EVENT_TYPE));
			this.grid.put(SHAKEMAPGRID_EVENT_STATUS_XML, attributes.getValue(SHAKEMAPGRID_EVENT_STATUS));
		} else if (localName.equals(EVENT_ELEMENT)) {
			this.grid.put(EVENT_LATITUDE_XML, attributes.getValue(EVENT_LATITUDE));
			this.grid.put(EVENT_LONGITUDE_XML, attributes.getValue(EVENT_LONGITUDE));
			this.grid.put(EVENT_MAGNITUDE_XML , attributes.getValue(EVENT_MAGNITUDE));
			this.grid.put(EVENT_DEPTH_XML, attributes.getValue(EVENT_DEPTH));
			this.grid.put(EVENT_TIMESTAMP_XML, attributes.getValue(EVENT_TIMESTAMP));
			this.grid.put(EVENT_DESCRIPTION_XML, attributes.getValue(EVENT_DESCRIPTION));
			this.grid.put(EVENT_NETWORK_XML, attributes.getValue(EVENT_NETWORK));
			this.grid.put(EVENT_ID_XML, attributes.getValue(EVENT_ID));
		} else if (localName.equals(GRIDSPEC_ELEMENT)) {
			this.grid.put(GRIDSPEC_LONMIN_XML, attributes.getValue(GRIDSPEC_LONMIN));
			this.grid.put(GRIDSPEC_LATMIN_XML, attributes.getValue(GRIDSPEC_LATMIN));
			this.grid.put(GRIDSPEC_LONMAX_XML, attributes.getValue(GRIDSPEC_LONMAX));
			this.grid.put(GRIDSPEC_LATMAX_XML, attributes.getValue(GRIDSPEC_LATMAX));
		}
	}
}
