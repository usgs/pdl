package gov.usgs.earthquake.geoserve;

import java.math.BigDecimal;

/**
 * Query for places.
 * 
 * Note: - Either circle or rectangle search are allowed, but not both. - For
 * circle searches, either maxRadiusKm or limit are required - For event type
 * searches, only latitude and longitude are required.
 * 
 * @author jmfee
 * @see https://earthquake.usgs.gov/ws/geoserve/places.php
 */
public class PlacesQuery {

	public static final String GEONAMES = "geonames";
	public static final String EVENT = "event";

	/** Circle center latitude. */
	public final BigDecimal latitude;
	/** Circle center longitude */
	public final BigDecimal longitude;
	/** Circle radius in kilometers. */
	public final BigDecimal maxRadiusKm;

	/** Rectangle north extent. */
	public final BigDecimal maxLatitude;
	/** Rectangle south extent. */
	public final BigDecimal minLatitude;
	/** Rectangle east extent. */
	public final BigDecimal maxLongitude;
	/** Rectangle west extent. */
	public final BigDecimal minLongitude;

	/** Maximum number of places to return (may be fewer). */
	public final Integer limit;
	/** Minimum population for returned places. */
	public final Integer minPopulation;
	/** Search types, use the constants GEONAMES or EVENT. */
	public final String[] types;

	/**
	 * Create a new circle PlacesQuery.
	 * 
	 * Either maxRadiusKm or limit are required when types is not EVENT.
	 * 
	 * @param latitude
	 *            circle center latitude.
	 * @param longitude
	 *            circle center longitude.
	 * @param maxRadiusKm
	 *            circle radius in kilometers.
	 * @param limit
	 *            maximum number of places.
	 * @param minPopulation
	 *            minimum population.
	 * @param types
	 *            types of places to return, either GEONAMES, EVENT, or both.
	 * @see #getEventQuery(BigDecimal, BigDecimal)
	 */
	public PlacesQuery(final BigDecimal latitude, final BigDecimal longitude, final BigDecimal maxRadiusKm,
			final Integer limit, final Integer minPopulation, final String[] types) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.maxRadiusKm = maxRadiusKm;

		this.maxLatitude = null;
		this.minLatitude = null;
		this.maxLongitude = null;
		this.minLongitude = null;

		this.limit = limit;
		this.minPopulation = minPopulation;
		this.types = types;
	}

	/**
	 * Create a new rectangular PlacesQuery.
	 * 
	 * Note that only GEONAMES type queries are supported for rectangles.
	 * 
	 * @param maxLatitude
	 *            rectangle north extent.
	 * @param minLatitude
	 *            rectangle south extent.
	 * @param maxLongitude
	 *            rectangle east extent.
	 * @param minLongitude
	 *            rectangle west extent.
	 * @param limit
	 *            maximum number of places.
	 * @param minPopulation
	 *            minimum population.
	 */
	public PlacesQuery(final BigDecimal maxLatitude, final BigDecimal minLatitude, final BigDecimal maxLongitude,
			final BigDecimal minLongitude, final Integer limit, final Integer minPopulation) {
		this.latitude = null;
		this.longitude = null;
		this.maxRadiusKm = null;

		this.maxLatitude = maxLatitude;
		this.minLatitude = minLatitude;
		this.maxLongitude = maxLongitude;
		this.minLongitude = minLongitude;
		this.limit = limit;
		this.minPopulation = minPopulation;
		this.types = new String[] { GEONAMES };
	}

	/**
	 * Create a new EVENT PlacesQuery.
	 * 
	 * Returns 5 places near the given location.
	 * 
	 * @param latitude
	 *            location latitude.
	 * @param longitude
	 *            location longitude.
	 * @return
	 */
	public static PlacesQuery getEventQuery(final BigDecimal latitude, final BigDecimal longitude) {
		return new PlacesQuery(latitude, longitude, null, null, null, new String[] { EVENT });
	}

}
