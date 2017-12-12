package gov.usgs.earthquake.geoserve;

import java.util.TimeZone;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Response for a RegionsQuery.
 * 
 * @author jmfee
 */
public class RegionsResponse {

	/** Raw JSON response. */
	public final JSONObject response;

	/**
	 * Create a new RegionsResopnse.
	 */
	public RegionsResponse(JSONObject jsonResponse) {
		response = jsonResponse;
	}

	/**
	 * @return admin region "country" property, or null.
	 */
	public String getAdminCountry() {
		try {
			return (String) getRegionProperties("admin").get("country");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return admin region "iso" property, or null.
	 */
	public String getAdminISO() {
		try {
			return (String) getRegionProperties("admin").get("iso");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return admin region "region" property, or null.
	 */
	public String getAdminRegion() {
		try {
			return (String) getRegionProperties("admin").get("region");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return authoritative region "network" property, or null.
	 */
	public String getAuthoritativeNetwork() {
		try {
			return (String) getRegionProperties("authoritative").get("network");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return fe region "name" property, or null.
	 */
	public String getFERegionName() {
		try {
			return (String) getRegionProperties("fe").get("name");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return fe region "number" property, or null.
	 */
	public Long getFERegionNumber() {
		try {
			return (Long) getRegionProperties("fe").get("number");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return timezone region "timezone" property, or null.
	 */
	public String getTimezoneName() {
		try {
			return (String) getRegionProperties("timezone").get("timezone");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return TimeZone for timezone region "timezone" property, or null.
	 */
	public TimeZone getTimeZone() {
		try {
			return TimeZone.getTimeZone(getTimezoneName());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Find first matching region by type.
	 * 
	 * @param type
	 *            type of region
	 * @return matching object, or null.
	 */
	public JSONObject getRegion(final String type) {
		try {
			JSONObject obj = response;
			obj = (JSONObject) obj.get(type);
			obj = (JSONObject) ((JSONArray) obj.get("features")).get(0);
			return obj;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Find first matching region's properties property.
	 * 
	 * @param type
	 *            type of region.
	 * @return JSONObject representing properties, or null.
	 */
	public JSONObject getRegionProperties(final String type) {
		try {
			JSONObject obj = getRegion(type);
			obj = (JSONObject) obj.get("properties");
			return obj;
		} catch (Exception e) {
			return null;
		}
	}

}
