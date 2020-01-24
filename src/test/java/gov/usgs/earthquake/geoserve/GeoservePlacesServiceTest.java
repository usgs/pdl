package gov.usgs.earthquake.geoserve;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GeoservePlacesServiceTest {

  private GeoservePlacesService service = null;

  @Before
  public void setUpTestEnvironment() {
    service = new GeoservePlacesService();
  }

  @Test
  public void azimuthToDirection() {
    Assert.assertEquals("S", service.azimuthToDirection(0));
    Assert.assertEquals("W", service.azimuthToDirection(90));
    Assert.assertEquals("N", service.azimuthToDirection(180));
    Assert.assertEquals("E", service.azimuthToDirection(270));

    Assert.assertEquals("S", service.azimuthToDirection(-0));
    Assert.assertEquals("E", service.azimuthToDirection(-90));
    Assert.assertEquals("N", service.azimuthToDirection(-180));
    Assert.assertEquals("W", service.azimuthToDirection(-270));
  }

  @Test
  public void formatEventTitle() {
    int distance = 0;
    double azimuth = 0.0;

    // OUS location
    String expectation = String.format("0 km S of name, country_name");
    JsonObject feature = Json.createObjectBuilder()
        .add("properties",
            Json.createObjectBuilder().add("name", "name").add("country_code", "country_code")
                .add("admin1_name", "admin1_name").add("country_name", "country_name").add("distance", distance)
                .add("azimuth", azimuth))
        .build();
    Assert.assertEquals(expectation, service.formatEventTitle(feature));

    // US location
    expectation = String.format("0 km S of name, admin1_name");
    feature = Json.createObjectBuilder()
        .add("properties",
            Json.createObjectBuilder().add("name", "name").add("country_code", "us").add("admin1_name", "admin1_name")
                .add("country_name", "country_name").add("distance", distance).add("azimuth", azimuth))
        .build();
    Assert.assertEquals(expectation, service.formatEventTitle(feature));
  }
}