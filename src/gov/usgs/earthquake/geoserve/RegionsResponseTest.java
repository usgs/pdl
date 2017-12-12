package gov.usgs.earthquake.geoserve;

import org.junit.Test;

import java.util.TimeZone;

import org.junit.Assert;

/**
 * Test the RegionsResponse class.
 * 
 * 
 * @author jmfee
 */
public class RegionsResponseTest {

	/**
	 * Test methods for a response.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRegionsResponse() throws Exception {
		RegionsResponse response = new RegionsResponse(new GeoserveClientTest().getRegionsJSON());
		Assert.assertEquals("Admin country", "United States", response.getAdminCountry());
		Assert.assertEquals("Admin iso", "USA", response.getAdminISO());
		Assert.assertEquals("Admin region", "California", response.getAdminRegion());

		Assert.assertEquals("Authoritative network", "NC", response.getAuthoritativeNetwork());

		Assert.assertEquals("FE name parses", "Northern California", response.getFERegionName());
		Assert.assertEquals("FE number parses", new Long(36), response.getFERegionNumber());

		Assert.assertEquals("TimeZone name", "America/Los_Angeles", response.getTimezoneName());
		Assert.assertEquals("TimeZone", TimeZone.getTimeZone("America/Los_Angeles"), response.getTimeZone());

		Assert.assertNull("non-existent region type returns null", response.getRegionProperties("blah"));
	}

	/**
	 * Test methods for a response when regions are missing.
	 * 
	 * @throws Exception.
	 */
	@Test
	public void testEmptyRegionsResponse() throws Exception {
		RegionsResponse response = new RegionsResponse(new GeoserveClientTest().getEmptyRegionsJSON());
		Assert.assertNull("admin country null", response.getAdminCountry());
		Assert.assertNull("admin iso null", response.getAdminISO());
		Assert.assertNull("admin region null", response.getAdminRegion());
		Assert.assertNull("authoritative network null", response.getAuthoritativeNetwork());
		Assert.assertNull("fe name null", response.getFERegionName());
		Assert.assertNull("fe number null", response.getFERegionNumber());
		Assert.assertNull("timezone name null", response.getTimezoneName());
		Assert.assertNull("timezone null", response.getTimeZone());
	}

}
