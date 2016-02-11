package gov.usgs.earthquake.shakemap;

import gov.usgs.util.XmlUtils;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * An XML Handler to load necessary product information from grid.xml
 * 
 * Accepts a ShakeMap object and updates the properties of that product based on
 * the product's grid.xml file.
 */
public class GridXMLHandler extends DefaultHandler {
	
	// ShakeMap grid parameters
		public static final String SHAKEMAPGRID_ELEMENT = "shakemap_grid";
		public static final String SHAKEMAPGRID_ID = "shakemap_id";
		public static final String SHAKEMAPGRID_ORIGINATOR = "shakemap_originator";
		public static final String SHAKEMAPGRID_TIMESTAMP = "process_timestamp";
		public static final String SHAKEMAPGRID_VERSION = "shakemap_version";
		public static final String SHAKEMAPGRID_EVENT_TYPE = "shakemap_event_type";
		public static final String SHAKEMAPGRID_EVENT_STATUS = "map_status";

		// ShakeMap event parameters
		public static final String EVENT_ELEMENT = "event";
		public static final String EVENT_LATITUDE = "lat";
		public static final String EVENT_LONGITUDE = "lon";
		public static final String EVENT_MAGNITUDE = "magnitude";
		public static final String EVENT_TIMESTAMP = "event_timestamp";
		public static final String EVENT_DESCRIPTION = "event_description";
		public static final String EVENT_DEPTH = "depth";
		// These are new parameters used by GSM when it is contributing to a
		// different
		// network as a backup
		public static final String EVENT_NETWORK = "event_network";
		public static final String EVENT_ID = "event_id";
		
		// ShakeMap gridspec parameters
		public static final String GRIDSPEC_ELEMENT = "grid_specification";
		public static final String GRIDSPEC_LONMIN = "lon_min";
		public static final String GRIDSPEC_LONMAX = "lon_max";
		public static final String GRIDSPEC_LATMIN = "lat_min";
		public static final String GRIDSPEC_LATMAX = "lat_max";
			
		public static final String SHAKEMAPGRID_ELEMENT_XML = SHAKEMAPGRID_ELEMENT;
		public static final String SHAKEMAPGRID_ID_XML =  SHAKEMAPGRID_ELEMENT+ "[" + SHAKEMAPGRID_ID + "]";
		public static final String SHAKEMAPGRID_ORIGINATOR_XML = SHAKEMAPGRID_ELEMENT + "[" + SHAKEMAPGRID_ORIGINATOR + "]";
		public static final String SHAKEMAPGRID_TIMESTAMP_XML = SHAKEMAPGRID_ELEMENT + "[" + SHAKEMAPGRID_TIMESTAMP + "]";
		public static final String SHAKEMAPGRID_VERSION_XML = SHAKEMAPGRID_ELEMENT+ "[" + SHAKEMAPGRID_VERSION + "]";
		public static final String SHAKEMAPGRID_EVENT_TYPE_XML = SHAKEMAPGRID_ELEMENT+ "[" + SHAKEMAPGRID_EVENT_TYPE + "]";
		public static final String SHAKEMAPGRID_EVENT_STATUS_XML = SHAKEMAPGRID_ELEMENT+ "[" + SHAKEMAPGRID_EVENT_STATUS + "]";

		public static final String EVENT_ELEMENT_XML = EVENT_ELEMENT;
		public static final String EVENT_LATITUDE_XML = EVENT_ELEMENT + "[" + EVENT_LATITUDE + "]";
		public static final String EVENT_LONGITUDE_XML = EVENT_ELEMENT+ "[" + EVENT_LONGITUDE + "]";
		public static final String EVENT_MAGNITUDE_XML = EVENT_ELEMENT+ "[" + EVENT_MAGNITUDE + "]";
		public static final String EVENT_TIMESTAMP_XML = EVENT_ELEMENT+ "[" + EVENT_TIMESTAMP + "]";
		public static final String EVENT_DESCRIPTION_XML = EVENT_ELEMENT+ "[" + EVENT_DESCRIPTION + "]";
		public static final String EVENT_DEPTH_XML = EVENT_ELEMENT+ "[" + EVENT_DEPTH + "]";
		public static final String EVENT_NETWORK_XML = EVENT_ELEMENT + "[" + EVENT_NETWORK + "]";
		public static final String EVENT_ID_XML = EVENT_ELEMENT + "[" + EVENT_ID + "]";
		
		public static final String GRIDSPEC_ELEMENT_XML = GRIDSPEC_ELEMENT;
		public static final String GRIDSPEC_LONMIN_XML = GRIDSPEC_ELEMENT+ "[" + GRIDSPEC_LONMIN + "]";
		public static final String GRIDSPEC_LONMAX_XML = GRIDSPEC_ELEMENT+ "[" + GRIDSPEC_LONMAX + "]";
		public static final String GRIDSPEC_LATMIN_XML = GRIDSPEC_ELEMENT+ "[" + GRIDSPEC_LATMIN + "]";
		public static final String GRIDSPEC_LATMAX_XML = GRIDSPEC_ELEMENT+ "[" + GRIDSPEC_LATMAX + "]";
			
	// ShakeMap griddata parameters
	public static final String GRIDDATA_ELEMENT = "grid_data";
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
	 * @throws Exception
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
