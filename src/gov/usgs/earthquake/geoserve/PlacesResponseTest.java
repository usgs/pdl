package gov.usgs.earthquake.geoserve;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test the PlacesResponse class.
 */
public class PlacesResponseTest {

	/**
	 * Test parsing of Event Places response.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEventPlacesResponse() throws Exception {
		PlacesResponse response = new PlacesResponse(new GeoserveClientTest().getEventPlacesJSON());

		List<Place> eventPlaces = response.getEventPlaces();
		List<Place> geonamesPlaces = response.getGeonamesPlaces();

		Assert.assertNull("geonames places returns null", geonamesPlaces);

		Assert.assertEquals("event places includes 5 places", 5, eventPlaces.size());

		// first place in list.
		Place place = eventPlaces.get(0);
		Assert.assertEquals("place id", new Long(5328563L), place.id);
		Assert.assertEquals("place admin1 code", "CA", place.admin1Code);
		Assert.assertEquals("place admin1 name", "California", place.admin1Name);
		Assert.assertEquals("place azimuth", new Double(50), place.azimuth);
		Assert.assertEquals("place country code", "US", place.countryCode);
		Assert.assertEquals("place country name", "United States", place.countryName);
		Assert.assertEquals("place distance", new Double(381.945), place.distance);
		Assert.assertEquals("place feature class", "P", place.featureClass);
		Assert.assertEquals("place feature code", "PPL", place.featureCode);
		Assert.assertEquals("place name", "Big Sur", place.name);
		Assert.assertEquals("place population", new Long(1000), place.population);
		Assert.assertEquals("place latitude", new Double(-121.80745), place.latitude);
		Assert.assertEquals("place longitude", new Double(36.27024), place.longitude);
		Assert.assertEquals("place elevation", new Double(44), place.elevation);
	}

	/**
	 * Test parsing of Geonames Places response.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGeonamesPlacesResponse() throws Exception {
		PlacesResponse response = new PlacesResponse(new GeoserveClientTest().getGeonamesPlacesJSON());

		List<Place> eventPlaces = response.getEventPlaces();
		List<Place> geonamesPlaces = response.getGeonamesPlaces();

		Assert.assertEquals("geonames places includes 5 places", 5, geonamesPlaces.size());

		Assert.assertNull("event places return null", eventPlaces);

		// first place in list.
		Place place = geonamesPlaces.get(0);
		Assert.assertEquals("place id", new Long(5357499L), place.id);
		Assert.assertEquals("place admin1 code", "CA", place.admin1Code);
		Assert.assertEquals("place admin1 name", "California", place.admin1Name);
		Assert.assertEquals("place azimuth", new Double(335.3), place.azimuth);
		Assert.assertEquals("place country code", "US", place.countryCode);
		Assert.assertEquals("place country name", "United States", place.countryName);
		Assert.assertEquals("place distance", new Double(21.442), place.distance);
		Assert.assertEquals("place feature class", "P", place.featureClass);
		Assert.assertEquals("place feature code", "PPLA2", place.featureCode);
		Assert.assertEquals("place name", "Hollister", place.name);
		Assert.assertEquals("place population", new Long(34928), place.population);
		Assert.assertEquals("place latitude", new Double(-121.4016), place.latitude);
		Assert.assertEquals("place longitude", new Double(36.85245), place.longitude);
		Assert.assertEquals("place elevation", new Double(88), place.elevation);
	}

}
