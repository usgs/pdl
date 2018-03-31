package gov.usgs.earthquake.geoserve;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class representing response of PlacesQuery.
 * 
 * @author jmfee
 *
 */
public class PlacesResponse {

	/** Raw JSON response */
	public final JSONObject response;

	/**
	 * Create a new PlacesResponse
	 * 
	 * @param jsonResponse
	 */
	public PlacesResponse(JSONObject jsonResponse) {
		response = jsonResponse;
	}

	/**
	 * Get event type places.
	 * 
	 * @return a list of event places, or null if not a response from an event
	 *         query.
	 */
	public List<Place> getEventPlaces() {
		try {
			JSONArray features = (JSONArray) ((JSONObject) response.get("event")).get("features");

			ArrayList<Place> places = new ArrayList<Place>(features.size());
			for (Object feature : features) {
				places.add(parsePlace((JSONObject) feature));
			}
			return places;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get geonames type places.
	 * 
	 * @return a list of geonames places, or null if not a response from a
	 *         geonames query.
	 */
	public List<Place> getGeonamesPlaces() {
		try {
			JSONArray features = (JSONArray) ((JSONObject) response.get("geonames")).get("features");

			ArrayList<Place> places = new ArrayList<Place>(features.size());
			for (Object feature : features) {
				places.add(parsePlace((JSONObject) feature));
			}
			return places;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Parse a place feature from the places response.
	 * 
	 * @param object
	 *            JSONObject representing a geojson feature.
	 * @return parsed Place object.
	 */
	protected Place parsePlace(final JSONObject object) {
		JSONArray coordinates = (JSONArray) ((JSONObject) object.get("geometry")).get("coordinates");
		JSONObject properties = (JSONObject) object.get("properties");
		Place place = new Place();

		place.id = (Long) object.get("id");

		place.latitude = parseDouble(coordinates.get(0));
		place.longitude = parseDouble(coordinates.get(1));
		place.elevation = parseDouble(coordinates.get(2));

		place.admin1Code = (String) properties.get("admin1_code");
		place.admin1Name = (String) properties.get("admin1_name");
		place.azimuth = parseDouble(properties.get("azimuth"));
		place.countryCode = (String) properties.get("country_code");
		place.countryName = (String) properties.get("country_name");
		place.distance = parseDouble(properties.get("distance"));
		place.featureClass = (String) properties.get("feature_class");
		place.featureCode = (String) properties.get("feature_code");
		place.name = (String) properties.get("name");
		place.population = (Long) properties.get("population");

		return place;
	}

	/**
	 * Utility method to always return a double, even if the JSONParser parsed a
	 * Long value.
	 * 
	 * @param object
	 *            either a Double or a Long
	 * @return a Double.
	 */
	protected Double parseDouble(final Object object) {
		try {
			return (Double) object;
		} catch (ClassCastException cce) {
			return new Double((Long) object);
		}
	}

}
