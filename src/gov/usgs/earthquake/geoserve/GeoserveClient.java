package gov.usgs.earthquake.geoserve;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import gov.usgs.util.StreamUtils;
import gov.usgs.util.StringUtils;

/**
 * Client for the Geoserve Web Service.
 * 
 * 
 * Examples:
 * 
 * <pre>
 * PlacesQuery eventPlacesQuery = PlacesQuery.getEventQuery(
 * 		new BigDecimal("34"), new BigDecimal("-118"));
 * List<Place> eventPlaces = new GeoserveClient().getPlaces(eventPlacesQuery).getEventPlaces();
 * for (Place place : eventPlaces) {
 *   System.out.println(place.name);
 * }
 * </pre>
 * 
 * <pre>
 * RegionQuery regionsQuery = new RegionsQuery(new BigDecimal("34"), new BigDecimal("-118"));
 * RegionsResponse regions = new GeoserveClient().getRegions(regionsQuery);
 * System.out.println(regions.getAdminRegion());
 * System.out.println(regions.getAdminCountry());
 * </pre>
 * 
 * @author jmfee
 */
public class GeoserveClient {

	/** Point to Earthquake web site instance by default. */
	public static final String DEFAULT_GEOSERVE_URL = "https://earthquake.usgs.gov/ws/geoserve/";

	/** Base URL for geoserve service. */
	public final URL geoserveURL;

	/** Places endpoint relative to geoserve URL. */
	public String placesEndpoint = "places.json";

	/** Regions endpoint relative to geoserve URL. */
	public String regionsEndpoint = "regions.json";

	/** Expected JSON charset. */
	public String charset = "UTF-8";

	/** Socket connect timeout. */
	public int connectTimeout = 5;
	/** Socket read timeout. */
	public int readTimeout = 5;

	/**
	 * Create a GeoserveClient with the default URL.
	 * 
	 * @throws MalformedURLException
	 */
	public GeoserveClient() throws MalformedURLException {
		this(new URL(DEFAULT_GEOSERVE_URL));
	}

	/**
	 * Create a GeoserveClient with a custom geoserve URL.
	 * 
	 * @param geoserveURL
	 *            custom geoserve url.
	 */
	public GeoserveClient(final URL geoserveURL) {
		this.geoserveURL = geoserveURL;
	}

	/**
	 * Query geoserve for region information.
	 * 
	 * @param query
	 *            query parameters.
	 * @return region information.
	 * @throws Exception
	 */
	public RegionsResponse getRegions(RegionsQuery query) throws Exception {
		return new RegionsResponse(getJSON(getRegionsURL(query)));
	}

	/**
	 * Query geoserve for place information.
	 * 
	 * @param query
	 *            query parameters.
	 * @return place information.
	 * @throws Exception
	 */
	public PlacesResponse getPlaces(PlacesQuery query) throws Exception {
		return new PlacesResponse(getJSON(getPlacesURL(query)));
	}

	/**
	 * Get URL for places query.
	 * 
	 * @param query
	 *            query to execute
	 * @return URL including query parameters
	 * @throws Exception
	 */
	protected URL getPlacesURL(final PlacesQuery query) throws Exception {
		LinkedList<Object> params = new LinkedList<Object>();
		if (query.latitude != null) {
			params.add("latitude=" + query.latitude.toString());
		}
		if (query.limit != null) {
			params.add("limit=" + query.limit.toString());
		}
		if (query.longitude != null) {
			params.add("longitude=" + query.longitude.toString());
		}
		if (query.maxLatitude != null) {
			params.add("maxlatitude=" + query.maxLatitude.toString());
		}
		if (query.maxLongitude != null) {
			params.add("maxlongitude=" + query.maxLongitude.toString());
		}
		if (query.maxRadiusKm != null) {
			params.add("maxradiuskm=" + query.maxRadiusKm.toString());
		}
		if (query.minLatitude != null) {
			params.add("minlatitude=" + query.minLatitude.toString());
		}
		if (query.minLongitude != null) {
			params.add("minlongitude=" + query.minLongitude.toString());
		}
		if (query.minPopulation != null) {
			params.add("minpopulation=" + query.minPopulation.toString());
		}
		if (query.types != null && query.types.length > 0) {
			params.add("types=" + StringUtils.join(Arrays.asList((Object[]) query.types), ","));
		}

		URL url = new URL(geoserveURL, placesEndpoint + "?" + StringUtils.join(params, "&"));
		return url;
	}

	/**
	 * Build URL for a region query.
	 * 
	 * @param query
	 *            query to execute.
	 * @return URL including query parameters
	 * @throws Exception
	 */
	protected URL getRegionsURL(final RegionsQuery query) throws Exception {
		LinkedList<Object> params = new LinkedList<Object>();
		params.add("latitude=" + query.latitude.toString());
		params.add("longitude=" + query.longitude.toString());
		if (query.includeGeometry) {
			params.add("includeGeometry=true");
		}
		if (query.types != null && query.types.length > 0) {
			params.add("types=" + StringUtils.join(Arrays.asList((Object[]) query.types), ","));
		}

		URL url = new URL(geoserveURL, regionsEndpoint + "?" + StringUtils.join(params, "&"));
		return url;
	}

	/**
	 * Read a URL and parse JSON.
	 * 
	 * @param url
	 *            url with a JSON response.
	 * @return parsed JSONObject
	 * @throws Exception
	 */
	protected JSONObject getJSON(final URL url) throws Exception {
		InputStream stream = StreamUtils.getURLInputStream(url, connectTimeout, readTimeout);
		JSONParser parser = new JSONParser();

		try {
			return (JSONObject) parser.parse(new BufferedReader(new InputStreamReader(stream, charset)));
		} finally {
			StreamUtils.closeStream(stream);
		}
	}

}
