package gov.usgs.earthquake.geoserve;

/**
 * A Geoserve Place.
 * 
 * @author jmfee
 */
public class Place {

	/** geonames feature id. */
	public Long id;

	/** Admin region code. */
	public String admin1Code;

	/** Admin region name. */
	public String admin1Name;

	/** Azimuth from search in degrees. */
	public Double azimuth;
	
	/** Country code. */
	public String countryCode;

	/** Country name. */
	public String countryName;

	/** Distance from search in kilometers. */
	public Double distance;
	
	/** Feature class. (e.g. "P") */
	public String featureClass;
	
	/** Feature code. (e.g. "PPL") */
	public String featureCode;

	/** Place name. */
	public String name;

	/** Population. */
	public Long population;

	/** Latitude */
	public Double latitude;
	
	/** Longitude */
	public Double longitude;
	
	/** Elevation in meters. */
	public Double elevation;

}
