package gov.usgs.earthquake.geoserve;

import java.math.BigDecimal;
import java.net.URL;

import org.json.simple.JSONObject;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the GeoserveClient class.
 * 
 * @author jmfee
 */
public class GeoserveClientTest {

	// Static convenience strings that are accessed often
	private static final String FS = System.getProperty("file.separator");
	private static final String USER_DIR = System.getProperty("user.dir");

	// This is where the product contents can be found to create a test product
	private static final String TEST_GEOSERVE_DIR = USER_DIR + FS + "etc" + FS + "testdata" + FS + "geoserve" + FS;

	/**
	 * Testing data for other classes.
	 * 
	 * @return
	 * @throws Exception
	 */
	public JSONObject getEventPlacesJSON() throws Exception {
		GeoserveClient client = new GeoserveClient();
		return client.getJSON(new URL("file://" + TEST_GEOSERVE_DIR + "event_places.json"));
	}

	/**
	 * Testing data for other classes.
	 * 
	 * @return
	 * @throws Exception
	 */
	public JSONObject getGeonamesPlacesJSON() throws Exception {
		GeoserveClient client = new GeoserveClient();
		return client.getJSON(new URL("file://" + TEST_GEOSERVE_DIR + "geonames_places.json"));
	}

	/**
	 * Testing data for other classes.
	 * 
	 * @return
	 * @throws Exception
	 */
	public JSONObject getEmptyRegionsJSON() throws Exception {
		GeoserveClient client = new GeoserveClient();
		return client.getJSON(new URL("file://" + TEST_GEOSERVE_DIR + "empty_regions.json"));
	}

	/**
	 * Testing data for other classes.
	 * 
	 * @return
	 * @throws Exception
	 */
	public JSONObject getRegionsJSON() throws Exception {
		GeoserveClient client = new GeoserveClient();
		return client.getJSON(new URL("file://" + TEST_GEOSERVE_DIR + "regions.json"));
	}

	/**
	 * Test expected URL for circle places query.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetCirclePlacesURL() throws Exception {
		GeoserveClient client = new GeoserveClient();
		PlacesQuery query = new PlacesQuery(new BigDecimal("34"), new BigDecimal("-118"), new BigDecimal("111.2"),
				new Integer(5), null, null);

		String url = client.getPlacesURL(query).toString();
		Assert.assertTrue("includes expected fragment",
				url.contains(client.placesEndpoint + "?latitude=34&limit=5&longitude=-118&maxradiuskm=111.2"));
	}

	/**
	 * Test expected URL for circle places query with zero types.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetPlacesURLWithoutTypes() throws Exception {
		GeoserveClient client = new GeoserveClient();
		PlacesQuery query = new PlacesQuery(new BigDecimal("34"), new BigDecimal("-118"), new BigDecimal("111.2"),
				new Integer(5), null, new String[] {});

		String url = client.getPlacesURL(query).toString();
		Assert.assertTrue("includes expected fragment",
				url.contains(client.placesEndpoint + "?latitude=34&limit=5&longitude=-118&maxradiuskm=111.2"));
		Assert.assertFalse("does not include types", url.contains("types="));
	}

	/**
	 * Test expected URL for event places query.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetEventPlacesURL() throws Exception {
		GeoserveClient client = new GeoserveClient();
		PlacesQuery query = PlacesQuery.getEventQuery(new BigDecimal("34"), new BigDecimal("-118"));

		String url = client.getPlacesURL(query).toString();
		Assert.assertTrue("includes expected fragment",
				url.contains(client.placesEndpoint + "?latitude=34&longitude=-118&types=event"));
	}

	/**
	 * Test expected URL for rectangle places query.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetRectanglePlacesURL() throws Exception {
		GeoserveClient client = new GeoserveClient();
		PlacesQuery query = new PlacesQuery(new BigDecimal("35"), new BigDecimal("34"), new BigDecimal("-117"),
				new BigDecimal("-118"), null, new Integer(1000));

		String url = client.getPlacesURL(query).toString();
		Assert.assertTrue("includes expected fragment", url.contains(client.placesEndpoint
				+ "?maxlatitude=35&maxlongitude=-117&minlatitude=34&minlongitude=-118&minpopulation=1000&types=geonames"));
	}

	/**
	 * Test expected URL for region query.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetRegionURL() throws Exception {
		GeoserveClient client = new GeoserveClient();
		RegionsQuery query = new RegionsQuery(new BigDecimal("34"), new BigDecimal("-118"));

		String url = client.getRegionsURL(query).toString();
		Assert.assertTrue("includes expected fragment",
				url.contains(client.regionsEndpoint + "?latitude=34&longitude=-118"));
	}

	/**
	 * Test expected URL for region query with geometry.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetRegionGeometryURL() throws Exception {
		GeoserveClient client = new GeoserveClient();
		RegionsQuery query = new RegionsQuery(new BigDecimal("34"), new BigDecimal("-118"),
				new String[] { "admin", "fe" }, true);

		String url = client.getRegionsURL(query).toString();
		Assert.assertTrue("includes expected fragment", url
				.contains(client.regionsEndpoint + "?latitude=34&longitude=-118&includeGeometry=true&types=admin,fe"));
	}

	/**
	 * Test expected URL for region query with geometry.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetRegionURLWithoutTypes() throws Exception {
		GeoserveClient client = new GeoserveClient();
		RegionsQuery query = new RegionsQuery(new BigDecimal("34"), new BigDecimal("-118"), new String[] {});

		String url = client.getRegionsURL(query).toString();
		Assert.assertTrue("includes expected fragment",
				url.contains(client.regionsEndpoint + "?latitude=34&longitude=-118"));
		Assert.assertFalse("does not include types parameter", url.contains("types="));
	}

	/**
	 * Verify getPlaces passes result of getJSON to response class.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMockPlacesResponse() throws Exception {
		final JSONObject eventPlacesJSON = getEventPlacesJSON();

		GeoserveClient client = new GeoserveClient() {
			protected JSONObject getJSON(final URL url) throws Exception {
				return eventPlacesJSON;
			}
		};

		PlacesResponse response = client
				.getPlaces(PlacesQuery.getEventQuery(new BigDecimal("1.23"), new BigDecimal("4.56")));
		Assert.assertEquals("result of getJSON is passed to places response", eventPlacesJSON, response.response);
	}

	/**
	 * Verify getREgions passes result of getJSON to response class.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMockRegionsResponse() throws Exception {
		final JSONObject regionsJSON = getRegionsJSON();

		GeoserveClient client = new GeoserveClient() {
			protected JSONObject getJSON(final URL url) throws Exception {
				return regionsJSON;
			}
		};

		RegionsResponse response = client.getRegions(new RegionsQuery(new BigDecimal("1.23"), new BigDecimal("4.56")));
		Assert.assertEquals("result of getJSON is passed to regions response", regionsJSON, response.response);
	}

}
