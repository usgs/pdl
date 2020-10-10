package gov.usgs.earthquake.geoserve;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GeoserveRegionsServiceTest {

  private GeoserveRegionsService service = null;
  private String feName = "fe region name";
  private JsonObject feRegion =  Json.createObjectBuilder().add("features",
      Json.createArrayBuilder().add(0,
        Json.createObjectBuilder().add("properties",
          Json.createObjectBuilder().add("name", feName)
            .add("number", 123)
          )
        )
      ).build();

  @Before
  public void setUpTestEnvironment() {
    this.service = new DummyRegionsService();
  }

  @Test
  public void getNearestPlace() throws Exception {
    BigDecimal latitude = new BigDecimal("0.0");
    BigDecimal longitude = new BigDecimal("0.0");

    Assert.assertEquals(this.feName, this.service.getFeRegionName(latitude, longitude));
  }

  protected class DummyRegionsService extends GeoserveRegionsService {
    @Override
    public JsonObject getFeRegion(BigDecimal latitude, BigDecimal longitude) throws IOException, MalformedURLException {
      return feRegion;
    }
  }
}