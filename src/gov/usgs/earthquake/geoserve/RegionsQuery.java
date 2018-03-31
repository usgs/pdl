package gov.usgs.earthquake.geoserve;

import java.math.BigDecimal;

/**
 * Query for region information.
 * 
 * @author jmfee
 */
public class RegionsQuery {

	/** search location latitude. */
	public final BigDecimal latitude;

	/** search location longitude. */
	public final BigDecimal longitude;

	/** whether to include geometry in response. */
	public final boolean includeGeometry;

	/** restrict the types of regions returned. */
	public final String[] types;

	/**
	 * Create a new RegionsQuery for a point.
	 * 
	 * @param latitude
	 *            search location latitude.
	 * @param longitude
	 *            search location longitude.
	 */
	public RegionsQuery(final BigDecimal latitude, final BigDecimal longitude) {
		this(latitude, longitude, null);
	}

	/**
	 * Create a new RegionsQuery with a custom list of types.
	 * 
	 * @param latitude
	 *            search location latitude.
	 * @param longitude
	 *            search location longitude.
	 * @param types
	 *            types of regions to search.
	 */
	public RegionsQuery(final BigDecimal latitude, final BigDecimal longitude, final String[] types) {
		this(latitude, longitude, types, false);
	}

	/**
	 * Create a new Regions Query with a custom list of types and
	 * includeGeometery setting.
	 * 
	 * @param latitude
	 *            search location latitude.
	 * @param longitude
	 *            search location longitude.
	 * @param types
	 *            types of regions to search.
	 * @param includeGeometry
	 *            whether to include geometry in response.
	 */
	public RegionsQuery(final BigDecimal latitude, final BigDecimal longitude, final String[] types,
			final boolean includeGeometry) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.types = types;
		this.includeGeometry = includeGeometry;
	}

}
