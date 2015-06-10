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
		Content source = product.getContents().get(GRID_XML_ATTACHMENT);
		if (source != null) {
			GridXMLHandler handler = new GridXMLHandler(this);
			InputStream in = null;
			try {
				in = source.getInputStream();
				handler.parse(in);
			} catch (Exception e) {
				// error parsing grid
				throw new IllegalArgumentException(e);
			} finally {
				StreamUtils.closeStream(in);
			}
		}

		Content infoxml = product.getContents().get(INFO_XML_ATTACHMENT);
		if (infoxml != null) {
			try {
				// parse info.xml
				InfoXMLHandler infoxmlHandler = new InfoXMLHandler();
				HashMap<String, String> info = infoxmlHandler.parse(infoxml
						.getInputStream());

				// read maxmmi from info.xml
				if (info.containsKey(MAXIMUM_MMI_INFO_KEY)) {
					this.getProperties().put(MAXIMUM_MMI_PROPERTY,
							info.get(MAXIMUM_MMI_INFO_KEY));
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "error parsing infoxml", e);
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
		return new BigDecimal(getProperties().get(MINIMUM_LONGITUDE_PROPERTY));
	}

	/**
	 * @param minimumLongitude
	 *            the minimum longitude to set
	 */
	public void setMinimumLongitude(BigDecimal minimumLongitude) {
		getProperties().put(ShakeMap.MINIMUM_LONGITUDE_PROPERTY,
				minimumLongitude.toPlainString());
	}

	/**
	 * @return the maximum longitude boundary of this ShakeMap
	 */
	public BigDecimal getMaximumLongitude() {
		return new BigDecimal(getProperties().get(MAXIMUM_LONGITUDE_PROPERTY));
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
		return new BigDecimal(getProperties().get(MINIMUM_LATITUDE_PROPERTY));
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
		return new BigDecimal(getProperties().get(MAXIMUM_LATITUDE_PROPERTY));
	}

	/**
	 * @param maximumLatitude
	 *            the maximum latitude to set
	 */
	public void setMaximumLatitude(BigDecimal maximumLatitude) {
		getProperties().put(MAXIMUM_LATITUDE_PROPERTY,
				maximumLatitude.toPlainString());
	}

}
