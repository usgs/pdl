package gov.usgs.earthquake.geoserve;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.earthquake.geoserve.GeoservePlaces;
import gov.usgs.earthquake.geoserve.GeoservePlacesService;

public class GeoservePlacesServiceTest {

  private GeoservePlacesService service = null;
  private JsonObject feature =  Json.createObjectBuilder().add("feature",
      Json.createObjectBuilder().add("properties",
        Json.createObjectBuilder().add("name", "name")
          .add("country_code", "country_code")
          .add("admin1_name", "admin1_name")
          .add("country_name", "country_name")
          .add("distance", "distance")
          .add("azimuth", "azimuth")
        )
      ).build();

  @Before
  public void setUpTestEnvironment() {
    this.service = new DummyPlacesService();
  }

  @Test
  public void getNearestPlace() throws Exception {
    BigDecimal latitude = new BigDecimal("0.0");
    BigDecimal longitude = new BigDecimal("0.0");

    // nearest feature from feature collection
    JsonObject expectation = this.feature;
    Assert.assertEquals(expectation, this.service.getNearestPlace(latitude, longitude));
  }

  protected class DummyPlacesService extends GeoservePlacesService {
    @Override
    public JsonObject getEventPlaces(BigDecimal latitude, BigDecimal longitude) throws IOException, MalformedURLException {
      JsonObject eventCollection = Json.createObjectBuilder().add("features",
          Json.createArrayBuilder().add(0, feature)
        ).build();

      return eventCollection;
    }
  }
}