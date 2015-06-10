package gov.usgs.earthquake.shakemap;

import java.math.BigDecimal;
import gov.usgs.util.XmlUtils;
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

	// ShakeMap griddata parameters
	public static final String GRIDDATA_ELEMENT = "grid_data";
	public static final String STOP_PARSING_BEFORE_GRIDDATA = "Stop parsing before grid data.";

	// The ShakeMap object parsed by this handler.
	private ShakeMap shakemap;
	// Whether we need to parse an event id (only if the product doesn't already
	// have one).
	private boolean needEventId = false;

	/**
	 * @param shakemap
	 *            the ShakeMap product to load
	 */
	public GridXMLHandler(ShakeMap shakemap) {
		this.shakemap = shakemap;
		if (shakemap.getEventId() == null) {
			needEventId = true;
		}
	}

	/**
	 * @return the ShakeMap associated to this XML handler
	 */
	public ShakeMap getShakemap() {
		return shakemap;
	}

	/**
	 * @param shakemap
	 *            the ShakeMap product to associate to this handler.
	 */
	public void setShakemap(ShakeMap shakemap) {
		this.shakemap = shakemap;
	}

	/**
	 * 
	 * @param in
	 *            the file or stream to parse
	 * @return the ShakeMap associated with this XML handler
	 * @throws Exception
	 */
	public ShakeMap parse(final Object in) throws Exception {
		try {
			XmlUtils.parse(in, this);
		} catch (Exception se) {
			if (!se.getMessage().equals(STOP_PARSING_BEFORE_GRIDDATA)) {
				throw se;
			}
		}
		return getShakemap();
	}

	/**
	 * @param uri
	 *            the uri for this element
	 * @param localName
	 *            the local name for this element
	 * @param qName
	 *            the fully-qualified name for this element
	 * @param attributes
	 *            the attributes of the element
	 */
	public final void startElement(final String uri, final String localName,
			final String qName, final Attributes attributes)
			throws SAXException {

		if (localName != null && GRIDDATA_ELEMENT.equals(localName)) {
			// don't read the whole grid
			throw new SAXException(STOP_PARSING_BEFORE_GRIDDATA);
		}

		if (localName.equals(SHAKEMAPGRID_ELEMENT)) {
			if (needEventId) {
				shakemap.setEventId(
						attributes.getValue(SHAKEMAPGRID_ORIGINATOR),
						attributes.getValue(SHAKEMAPGRID_ID));
			}
			shakemap.setProcessTimestamp(XmlUtils.getDate(attributes
					.getValue(SHAKEMAPGRID_TIMESTAMP)));
			shakemap.setVersion(attributes.getValue(SHAKEMAPGRID_VERSION));
			shakemap.setEventType(attributes.getValue(SHAKEMAPGRID_EVENT_TYPE));
			shakemap.setMapStatus(attributes
					.getValue(SHAKEMAPGRID_EVENT_STATUS));
		} else if (localName.equals(EVENT_ELEMENT)) {
			shakemap.setLatitude(new BigDecimal(attributes
					.getValue(EVENT_LATITUDE)));
			shakemap.setLongitude(new BigDecimal(attributes
					.getValue(EVENT_LONGITUDE)));
			shakemap.setMagnitude(new BigDecimal(attributes
					.getValue(EVENT_MAGNITUDE)));
			shakemap.setDepth(new BigDecimal(attributes.getValue(EVENT_DEPTH)));
			String dateString = attributes.getValue(EVENT_TIMESTAMP).replace(
					"GMT", "Z");
			shakemap.setEventTime(XmlUtils.getDate(dateString));
			shakemap.setEventDescription(attributes.getValue(EVENT_DESCRIPTION));

			if (needEventId) {
				// A more accurate way of labeling events used by GSM.
				// This should be okay here because it will always appear after
				// the
				// SHAKEMAPGRID_ELEMENT, and will only override the above when
				// present
				String network = attributes.getValue(EVENT_NETWORK);
				String networkid = attributes.getValue(EVENT_ID);
				if (network != null && networkid != null) {
					shakemap.setEventId(network, networkid);
				}
			}
		} else if (localName.equals(GRIDSPEC_ELEMENT)) {
			shakemap.setMinimumLongitude(new BigDecimal(attributes
					.getValue(GRIDSPEC_LONMIN)));
			shakemap.setMinimumLatitude(new BigDecimal(attributes
					.getValue(GRIDSPEC_LATMIN)));
			shakemap.setMaximumLongitude(new BigDecimal(attributes
					.getValue(GRIDSPEC_LONMAX)));
			shakemap.setMaximumLatitude(new BigDecimal(attributes
					.getValue(GRIDSPEC_LATMAX)));
		}
	}

}
