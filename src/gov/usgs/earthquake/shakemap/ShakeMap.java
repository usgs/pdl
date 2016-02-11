package gov.usgs.earthquake.shakemap;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ShakeMap class for ShakeMap products specifically.
 * 
 * This subclass of Product provides access to additional ShakeMap-specific
 * attributes and loads these attributes, as well as additional Product
 * attributes from ShakeMap source XML files.
 */
public class ShakeMap extends Product {

	private static final Logger LOGGER = Logger.getLogger(ShakeMap.class
			.getName());

	// shakemap object used for comparison
	private Product shakemap;

	// References to file content in the Product
	public static final String GRID_XML_ATTACHMENT = "download/grid.xml";
	public static final String INFO_XML_ATTACHMENT = "download/info.xml";

	// The files below have been decided to be unsupported at this time to
	// encourage
	// adoption of grid.xml by all networks.
	// public static final String GRID_XYZ_ATTACHMENT = "download/grid.xyz.zip";
	// public static final String STATIONLIST_XML_ATTACHMENT =
	// "download/stationlist.xml";
	public static final String INVISIBLE_ATTACHMENT = ".invisible";

	// A suffix added to all event codes for scenarios
	public static final String SCENARIO_ID_SUFFIX = "_se";

	// Map types
	public static final String ACTUAL = "ACTUAL";
	public static final String SCENARIO = "SCENARIO";
	public static final String TEST = "TEST";

	// ShakeMap-specific properties
	public static final String MAP_STATUS_PROPERTY = "map-status";
	public static final String EVENT_TYPE_PROPERTY = "event-type";
	public static final String PROCESS_TIMESTAMP_PROPERTY = "process-timestamp";
	public static final String EVENT_DESCRIPTION_PROPERTY = "event-description";
	public static final String MINIMUM_LONGITUDE_PROPERTY = "minimum-longitude";
	public static final String MAXIMUM_LONGITUDE_PROPERTY = "maximum-longitude";
	public static final String MINIMUM_LATITUDE_PROPERTY = "minimum-latitude";
	public static final String MAXIMUM_LATITUDE_PROPERTY = "maximum-latitude";

  // ShakeMap grid parameters
  public static final String SHAKEMAPGRID_ID = "shakemap_id";
  public static final String SHAKEMAPGRID_ORIGINATOR = "shakemap_originator";
  public static final String SHAKEMAPGRID_TIMESTAMP = "process_timestamp";
  public static final String SHAKEMAPGRID_VERSION = "shakemap_version";
  public static final String SHAKEMAPGRID_EVENT_TYPE = "shakemap_event_type";
  public static final String SHAKEMAPGRID_EVENT_STATUS = "map_status";

  // ShakeMap event parameters
  public static final String EVENT_LATITUDE = "lat";
  public static final String EVENT_LONGITUDE = "lon";
  public static final String EVENT_MAGNITUDE = "magnitude";
  public static final String EVENT_TIMESTAMP = "event_timestamp";
  public static final String EVENT_DESCRIPTION = "event_description";
  public static final String EVENT_DEPTH = "depth";

  // ShakeMap gridspec parameters
  public static final String GRIDSPEC_LONMIN = "lon_min";
  public static final String GRIDSPEC_LONMAX = "lon_max";
  public static final String GRIDSPEC_LATMIN = "lat_min";
  public static final String GRIDSPEC_LATMAX = "lat_max";

	// key in info.xml for maximum mmi
	public static final String MAXIMUM_MMI_INFO_KEY = "mi_max";
	public static final String MAXIMUM_MMI_PROPERTY = "maxmmi";

	/**
	 * @param product
	 *            the base product to be converted to a ShakeMap product
	 */
	public ShakeMap(final Product product) {
		super(product);

		// prefer grid attachment
		Content gridxml = product.getContents().get(GRID_XML_ATTACHMENT);
		if (gridxml != null) {
			try {
				// parse grid.xml
				GridXMLHandler gridxmlHandler = new GridXMLHandler();
				HashMap<String, String> grid = gridxmlHandler.parse(gridxml
						.getInputStream());
				// parse through hash maps to set shakemap properties
				this.setGridXMLProperties(grid);
			} catch (Exception e) {
				// error parsing grid
				LOGGER.log(Level.WARNING, "error parsing grid.xml", e);
			}
		}

		Content infoxml = product.getContents().get(INFO_XML_ATTACHMENT);
		if (infoxml != null) {
			try {
				// parse info.xml
				InfoXMLHandler infoxmlHandler = new InfoXMLHandler();
				HashMap<String, String> info = infoxmlHandler.parse(infoxml
						.getInputStream());
				// parse through hash maps to set shakemap properties
				this.setInfoXMLProperties(info);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "error parsing info.xml", e);
			}
		}


		/*
		 * else { // At this time we are disabling all non-grid.xml
		 * functionality // as all shakemaps sent in should have a grid.xml
		 * file.
		 * 
		 * //otherwise try gridXYZ (has most) + stationlist (has depth) source =
		 * product.getContents().get(GRID_XYZ_ATTACHMENT); if (source != null) {
		 * GridXYZHandler handler = new GridXYZHandler(this); try {
		 * handler.parse(source.getInputStream()); } catch (Exception e) {
		 * //error parsing gridxyz throw new IllegalArgumentException(e); } }
		 * 
		 * source = product.getContents().get(STATIONLIST_XML_ATTACHMENT); if
		 * (source != null) { StationlistXMLHandler handler = new
		 * StationlistXMLHandler(this); try {
		 * handler.parse(source.getInputStream()); } catch (Exception e) {
		 * //error parsing stationlist throw new IllegalArgumentException(e); }
		 * } }
		 */
	}

	/**
	 * @param gridXML
	 *            shakemap properties hash keyed by grid.xml attribute name
	 */
	public void setGridXMLProperties (HashMap<String, String> gridXML) {



		// metadata
		if (valueIsEmpty(EVENTSOURCE_PROPERTY, gridXML.get(SHAKEMAPGRID_ORIGINATOR)) &&
				valueIsEmpty(EVENTSOURCECODE_PROPERTY, gridXML.get(SHAKEMAPGRID_ID))) {
			setEventId(
					gridXML.get(SHAKEMAPGRID_ORIGINATOR),
					gridXML.get(SHAKEMAPGRID_ID)
				);
		}

		if (valueIsEmpty(SHAKEMAPGRID_TIMESTAMP, gridXML.get(SHAKEMAPGRID_TIMESTAMP))) {
			setProcessTimestamp(XmlUtils.getDate(
					gridXML.get(SHAKEMAPGRID_TIMESTAMP)));
		}

		if (valueIsEmpty(VERSION_PROPERTY, gridXML.get(SHAKEMAPGRID_VERSION))) {
			setVersion(gridXML.get(SHAKEMAPGRID_VERSION));
		}

		if (valueIsEmpty(EVENT_TYPE_PROPERTY, gridXML.get(SHAKEMAPGRID_EVENT_TYPE))) {
			setEventType(gridXML.get(SHAKEMAPGRID_EVENT_TYPE));
		}

		if (valueIsEmpty(MAP_STATUS_PROPERTY, gridXML.get(SHAKEMAPGRID_EVENT_STATUS))) {
			setMapStatus(gridXML.get(SHAKEMAPGRID_EVENT_STATUS));
		}


		// Shakemap
		if (valueIsEmpty(MAP_STATUS_PROPERTY, gridXML.get(MAP_STATUS_PROPERTY))) {
			setMapStatus(gridXML.get(MAP_STATUS_PROPERTY));
		}

		if (valueIsEmpty(EVENT_TYPE_PROPERTY, gridXML.get(EVENT_TYPE_PROPERTY))) {
			setEventType(gridXML.get(EVENT_TYPE_PROPERTY));
		}

		if (valueIsEmpty(PROCESS_TIMESTAMP_PROPERTY, gridXML.get(PROCESS_TIMESTAMP_PROPERTY))) {
			setProcessTimestamp(XmlUtils.getDate(
					gridXML.get(PROCESS_TIMESTAMP_PROPERTY)));
		}

		if (valueIsEmpty(EVENT_DESCRIPTION_PROPERTY, gridXML.get(EVENT_DESCRIPTION_PROPERTY))) {
			setEventDescription(gridXML.get(EVENT_DESCRIPTION_PROPERTY));
		}

		if (valueIsEmpty(MINIMUM_LONGITUDE_PROPERTY, gridXML.get(MINIMUM_LONGITUDE_PROPERTY))) {
			setMinimumLongitude(getBigDecimal(gridXML.get(MINIMUM_LONGITUDE_PROPERTY)));
		}

		if (valueIsEmpty(MAXIMUM_LONGITUDE_PROPERTY, gridXML.get(MAXIMUM_LONGITUDE_PROPERTY))) {
			setMaximumLongitude(getBigDecimal(gridXML.get(MAXIMUM_LONGITUDE_PROPERTY)));
		}

		if (valueIsEmpty(MINIMUM_LATITUDE_PROPERTY, gridXML.get(MINIMUM_LATITUDE_PROPERTY))) {
			setMinimumLatitude(getBigDecimal(gridXML.get(MINIMUM_LATITUDE_PROPERTY)));
		}

		if (valueIsEmpty(MAXIMUM_LATITUDE_PROPERTY, gridXML.get(MAXIMUM_LATITUDE_PROPERTY))) {
			setMaximumLatitude(getBigDecimal(gridXML.get(MAXIMUM_LATITUDE_PROPERTY)));
		}


		// Event
		if (valueIsEmpty(EVENT_LATITUDE, gridXML.get(EVENT_LATITUDE))) {
			setLatitude(getBigDecimal(gridXML.get(EVENT_LATITUDE)));
		}

		if (valueIsEmpty(EVENT_LONGITUDE, gridXML.get(EVENT_LONGITUDE))) {
			setLongitude(getBigDecimal(gridXML.get(EVENT_LONGITUDE)));
		}

		if (valueIsEmpty(EVENT_MAGNITUDE, gridXML.get(EVENT_MAGNITUDE))) {
			setMagnitude(getBigDecimal(gridXML.get(EVENT_MAGNITUDE)));
		}

		if (valueIsEmpty(EVENT_DEPTH, gridXML.get(EVENT_DEPTH))) {
			setDepth(getBigDecimal(gridXML.get(EVENT_DEPTH)));
		}

		if (valueIsEmpty(EVENT_TIMESTAMP, gridXML.get(EVENT_TIMESTAMP))) {
			setEventTime(XmlUtils.getDate(gridXML.get(EVENT_TIMESTAMP)
					.replace("GMT", "Z")
					.replace("UTC","Z")
				));
		}

		if (valueIsEmpty(EVENT_DESCRIPTION, gridXML.get(EVENT_DESCRIPTION))) {
			setEventDescription(gridXML.get(EVENT_DESCRIPTION));
		}


		// Grid Spec
		if (valueIsEmpty(GRIDSPEC_LONMIN, gridXML.get(GRIDSPEC_LONMIN))) {
			setMinimumLongitude(getBigDecimal(gridXML.get(GRIDSPEC_LONMIN)));
		}

		if (valueIsEmpty(GRIDSPEC_LATMIN, gridXML.get(GRIDSPEC_LATMIN))) {
			setMinimumLatitude(getBigDecimal(gridXML.get(GRIDSPEC_LATMIN)));
		}

		if (valueIsEmpty(GRIDSPEC_LONMAX, gridXML.get(GRIDSPEC_LONMAX))) {
			setMaximumLongitude(getBigDecimal(gridXML.get(GRIDSPEC_LONMAX)));
		}

		if (valueIsEmpty(GRIDSPEC_LATMAX, gridXML.get(GRIDSPEC_LATMAX))) {
			setMaximumLatitude(getBigDecimal(gridXML.get(GRIDSPEC_LATMAX)));
		}

	};

	/**
	 * @param infoXML
	 *            shakemap properties hash keyed by info.xml attribute name
	 */
	public void setInfoXMLProperties (HashMap<String, String> infoXML) {
		// read maxmmi from info.xml
		if (infoXML.containsKey(MAXIMUM_MMI_INFO_KEY)) {
			this.getProperties().put(MAXIMUM_MMI_PROPERTY,
					infoXML.get(MAXIMUM_MMI_INFO_KEY));
		}
	};

	/**
	 * @param property,
	 *            the property to check on the PDL object
	 * @return if the shakemap property is already set
	 */
	public boolean valueIsEmpty (String property, String value) {
		// nothing to be set
		if (value == null) {
			return false;
		}
		// no value has been set
		if (getProperties().get(property) == null) {
			return true;
		}
		// value are different, log warning
		if (!getProperties().get(property).equals(value)) {
			LOGGER.log(Level.FINE,
					"The ShakeMap property \"" + property + "\" does not match PDL value.");
		}
		return false;
	}

	/**
	 * @param mapStatus
	 *            the map status to set
	 */
	public void setMapStatus(String mapStatus) {
		getProperties().put(MAP_STATUS_PROPERTY, mapStatus);
	}

	/**
	 * @return the status of this map
	 */
	public String getMapStatus() {
		return getProperties().get(MAP_STATUS_PROPERTY);
	}

	/**
	 * @param eventType
	 *            the event type to set
	 */
	public void setEventType(String eventType) {
		getProperties().put(EVENT_TYPE_PROPERTY, eventType);
	}

	/**
	 * @return the event type of this product as defined in ShakeMap
	 */
	public String getEventType() {
		return getProperties().get(EVENT_TYPE_PROPERTY);
	}

	/**
	 * @param processTimestamp
	 *            the process timestamp to set
	 */
	public void setProcessTimestamp(Date processTimestamp) {
		getProperties().put(PROCESS_TIMESTAMP_PROPERTY,
				XmlUtils.formatDate(processTimestamp));
	}

	/**
	 * @return the process timestamp of this ShakeMap
	 */
	public Date getProcessTimestamp() {
		return XmlUtils
				.getDate(getProperties().get(PROCESS_TIMESTAMP_PROPERTY));
	}

	/**
	 * @return the event description text for this ShakeMap
	 */
	public String getEventDescription() {
		return getProperties().get(EVENT_DESCRIPTION_PROPERTY);
	}

	/**
	 * @param eventDescription
	 *            the event description to set
	 */
	public void setEventDescription(String eventDescription) {
		getProperties().put(EVENT_DESCRIPTION_PROPERTY, eventDescription);
	}

	/**
	 * @return the minimum longitude boundary of this ShakeMap
	 */
	public BigDecimal getMinimumLongitude() {
		return getBigDecimal(getProperties().get(MINIMUM_LONGITUDE_PROPERTY));
	}

	/**
	 * @param minimumLongitude
	 *            the minimum longitude to set
	 */
	public void setMinimumLongitude(BigDecimal minimumLongitude) {
		getProperties().put(MINIMUM_LONGITUDE_PROPERTY,
				minimumLongitude.toPlainString());
	}

	/**
	 * @return the maximum longitude boundary of this ShakeMap
	 */
	public BigDecimal getMaximumLongitude() {
		return getBigDecimal(getProperties().get(MAXIMUM_LONGITUDE_PROPERTY));
	}

	/**
	 * @param maximumLongitude
	 *            the maximum longitude to set
	 */
	public void setMaximumLongitude(BigDecimal maximumLongitude) {
		getProperties().put(MAXIMUM_LONGITUDE_PROPERTY,
				maximumLongitude.toPlainString());
	}

	/**
	 * @return the minimum latitude boundary of this ShakeMap
	 */
	public BigDecimal getMinimumLatitude() {
		return getBigDecimal(getProperties().get(MINIMUM_LATITUDE_PROPERTY));
	}

	/**
	 * @param minimumLatitude
	 *            the minimum latitude to set
	 */
	public void setMinimumLatitude(BigDecimal minimumLatitude) {
		getProperties().put(MINIMUM_LATITUDE_PROPERTY,
				minimumLatitude.toPlainString());
	}

	/**
	 * @return the maximum latitude boundary of this ShakeMap
	 */
	public BigDecimal getMaximumLatitude() {
		return getBigDecimal(getProperties().get(MAXIMUM_LATITUDE_PROPERTY));
	}

	/**
	 * @param maximumLatitude
	 *            the maximum latitude to set
	 */
	public void setMaximumLatitude(BigDecimal maximumLatitude) {
		getProperties().put(MAXIMUM_LATITUDE_PROPERTY,
				maximumLatitude.toPlainString());
	}

	/**
	 * Returns string value as BigDecimal
	 */
	protected BigDecimal getBigDecimal (String value) {
		if (value == null) {
			return null;
		}
		return new BigDecimal(value);
	}

}
