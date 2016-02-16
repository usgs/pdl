package gov.usgs.earthquake.shakemap;

import java.io.File;
import java.util.HashMap;


import org.junit.Assert;
import org.junit.Test;

import gov.usgs.util.StreamUtils;
import gov.usgs.earthquake.shakemap.GridXMLHandler;

import org.junit.Before;

public class GridXMLHandlerTest {
	
  private GridXMLHandler module = null;

  @Before
  public void setUpTestEnvironment() throws Exception {
    module = new GridXMLHandler();

  }
  /*
   * Test GridXMLHandler for correct output
   */
  @Test
  public void testParseMethod() throws Exception {
    HashMap<String, String> map = module.parse(StreamUtils.getInputStream(new File(
        "etc/test_products/us20004z5b/us_shakemap_us20004z5b_1455071675669_grid.xml")));

    Assert.assertEquals("-71.619800", map.get("event[lon]"));
    Assert.assertEquals("COQUIMBO, CHILE", map.get("event[event_description]"));
    Assert.assertEquals("2016-02-10T00:33:05UTC", map.get("event[event_timestamp]"));
    Assert.assertEquals("6.3", map.get("event[magnitude]"));
    Assert.assertEquals("us", map.get("event[event_network]"));
    Assert.assertEquals("-33.214000", map.get("grid_specification[lat_min]"));
    Assert.assertEquals("-30.634000", map.get("event[lat]"));
    Assert.assertEquals("us20004z5b", map.get("event[event_id]"));
    Assert.assertEquals("us20004z5b", map.get("shakemap_grid[shakemap_id]"));
    Assert.assertEquals("2", map.get("shakemap_grid[shakemap_version]"));
    Assert.assertEquals("31.53", map.get("event[depth]"));
    Assert.assertEquals("-68.619800", map.get("grid_specification[lon_max]"));
    Assert.assertEquals("2016-02-10T02:33:29Z", map.get("shakemap_grid[process_timestamp]"));
    Assert.assertEquals("us", map.get("shakemap_grid[shakemap_originator]"));
    Assert.assertEquals("ACTUAL", map.get("shakemap_grid[shakemap_event_type]"));
    Assert.assertEquals("-28.054000", map.get("grid_specification[lat_max]"));
    Assert.assertEquals("-74.619800", map.get("grid_specification[lon_min]"));
    Assert.assertEquals("RELEASED", map.get("shakemap_grid[map_status]"));
  }
}
