package gov.usgs.earthquake.geoserve;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;

import javax.json.Json;
import javax.json.JsonObject;

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

  // TODO Test this class, consider Mockito
}
