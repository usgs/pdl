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

	public static final String EVENT_DESCRIPTION_PROPERTY = "event-description";
	public static final String EVENT_TYPE_PROPERTY = "event-type";
	public static final String MAP_STATUS_PROPERTY = "map-status";
	public static final String MAXIMUM_LATITUDE_PROPERTY = "maximum-latitude";
	public static final String MAXIMUM_LONGITUDE_PROPERTY = "maximum-longitude";
	public static final String MINIMUM_LATITUDE_PROPERTY = "minimum-latitude";
	public static final String MINIMUM_LONGITUDE_PROPERTY = "minimum-longitude";
	public static final String PROCESS_TIMESTAMP_PROPERTY = "process-timestamp";

	private static final Logger LOGGER = Logger.getLogger(ShakeMap.class
			.getName());

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
			InputStream gridXmlIn = null;
			try {
				// parse grid.xml
				GridXMLHandler gridxmlHandler = new GridXMLHandler();
				gridXmlIn = gridxml.getInputStream();
				HashMap<String, String> grid = gridxmlHandler.parse(gridXmlIn);
				// parse through hash maps to set shakemap properties
				this.setGridXMLProperties(grid);
			} catch (Exception e) {
				// error parsing grid
				LOGGER.log(Level.WARNING, "error parsing grid.xml", e);
			} finally {
				StreamUtils.closeStream(gridXmlIn);
			}
		}

		Content infoxml = product.getContents().get(INFO_XML_ATTACHMENT);
		if (infoxml != null) {
			InputStream infoXmlIn = null;
			try {
				// parse info.xml
				InfoXMLHandler infoxmlHandler = new InfoXMLHandler();
				infoXmlIn = infoxml.getInputStream();
				HashMap<String, String> info = infoxmlHandler.parse(infoXmlIn);
				// parse through hash maps to set shakemap properties
				this.setInfoXMLProperties(info);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "error parsing info.xml", e);
			} finally {
				StreamUtils.closeStream(infoXmlIn);
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
		String depth;
		String eventDescription;
		String eventId;
		String eventSource;
		String eventSourceCode;
		String eventTime;
		String eventType;
		String latitude;
		String longitude;
		String magnitude;
		String mapStatus;
		String maximumLatitude;
		String maximumLongitude;
		String minimumLatitude;
		String minimumLongitude;
		String processTimestamp;
		String version;


		// eventId
		eventSource = gridXML.get(GridXMLHandler.EVENT_NETWORK_XML);
		eventSourceCode = gridXML.get(GridXMLHandler.EVENT_ID_XML);
		eventId = eventSource + eventSourceCode;

		if (valueIsEmpty(getEventId(), eventId))  {
			setEventId(eventSource, eventSourceCode);
		}

		// less preferred eventId (if not already set)
		eventSource = gridXML.get(GridXMLHandler.SHAKEMAPGRID_ORIGINATOR_XML);
		eventSourceCode = gridXML.get(GridXMLHandler.SHAKEMAPGRID_ID_XML);
		eventId = eventSource + eventSourceCode;

		if (valueIsEmpty(getEventId(), eventId))  {
			setEventId(eventSource, eventSourceCode);
		}


		// ShakeMap Metadata
		processTimestamp = gridXML.get(GridXMLHandler.SHAKEMAPGRID_TIMESTAMP_XML);
		if (valueIsEmpty(XmlUtils.formatDate(getProcessTimestamp()), processTimestamp)) {
			setProcessTimestamp(XmlUtils.getDate(processTimestamp));
		}

		version = gridXML.get(GridXMLHandler.SHAKEMAPGRID_VERSION_XML);
		if (valueIsEmpty(getVersion(), version)) {
			setVersion(version);
		}

		eventType = gridXML.get(GridXMLHandler.SHAKEMAPGRID_EVENT_TYPE_XML);
		if (valueIsEmpty(getEventType(), eventType)) {
			setEventType(eventType);
		}

		mapStatus = gridXML.get(GridXMLHandler.SHAKEMAPGRID_EVENT_STATUS_XML);
		if (valueIsEmpty(getMapStatus(), mapStatus)) {
			setMapStatus(mapStatus);
		}


		// ShakeMap Grid
		minimumLongitude = gridXML.get(GridXMLHandler.GRIDSPEC_LONMIN_XML);
		if (valueIsEmpty(getString(getMinimumLongitude()), minimumLongitude)) {
			setMinimumLongitude(getBigDecimal(minimumLongitude));
		}

		maximumLongitude = gridXML.get(GridXMLHandler.GRIDSPEC_LONMAX_XML);
		if (valueIsEmpty(getString(getMaximumLongitude()), maximumLongitude)) {
			setMaximumLongitude(getBigDecimal(maximumLongitude));
		}

		minimumLatitude = gridXML.get(GridXMLHandler.GRIDSPEC_LATMIN_XML);
		if (valueIsEmpty(getString(getMinimumLatitude()), minimumLatitude)) {
			setMinimumLatitude(getBigDecimal(minimumLatitude));
		}

		maximumLatitude = gridXML.get(GridXMLHandler.GRIDSPEC_LATMAX_XML);
		if (valueIsEmpty(getString(getMaximumLatitude()), maximumLatitude)) {
			setMaximumLatitude(getBigDecimal(maximumLatitude));
		}


		// Event
		latitude = gridXML.get(GridXMLHandler.EVENT_LATITUDE_XML);
		if (valueIsEmpty(getString(getLatitude()), latitude)) {
			setLatitude(getBigDecimal(latitude));
		}

		longitude = gridXML.get(GridXMLHandler.EVENT_LONGITUDE_XML);
		if (valueIsEmpty(getString(getLongitude()), longitude)) {
			setLongitude(getBigDecimal(longitude));
		}

		magnitude = gridXML.get(GridXMLHandler.EVENT_MAGNITUDE_XML);
		if (valueIsEmpty(getString(getMagnitude()), magnitude)) {
			setMagnitude(getBigDecimal(magnitude));
		}

		depth = gridXML.get(GridXMLHandler.EVENT_DEPTH_XML);
		if (valueIsEmpty(getString(getDepth()), depth)) {
			setDepth(getBigDecimal(depth));
		}

		eventTime = gridXML.get(GridXMLHandler.EVENT_TIMESTAMP_XML)
				.replace("GMT", "Z")
				.replace("UTC","Z");
		if (valueIsEmpty(XmlUtils.formatDate(getEventTime()), eventTime)) {
			setEventTime(XmlUtils.getDate(eventTime));
		}

		eventDescription = gridXML.get(GridXMLHandler.EVENT_DESCRIPTION_XML);
		if (valueIsEmpty(getEventDescription(), eventDescription)) {
			setEventDescription(eventDescription);
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
	public boolean valueIsEmpty (String productValue, String xmlValue) {
		// nothing to be set
		if (xmlValue == null) {
			return false;
		}
		// no value has been set
		if (productValue == null) {
			return true;
		}
		// value is set and values are different, log warning
		if (!productValue.equals(xmlValue)) {
			LOGGER.log(Level.FINE,
					"The ShakeMap property value: \"" + xmlValue + "\"" +
					" does not match the product value: \"" + productValue + "\".");
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
	 * Returns String value as BigDecimal
	 */
	protected BigDecimal getBigDecimal (String value) {
		if (value == null) {
			return null;
		}
		return new BigDecimal(value);
	}

	/**
	 * Returns BigDecimal value as String
	 */
	protected String getString (BigDecimal value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

}
