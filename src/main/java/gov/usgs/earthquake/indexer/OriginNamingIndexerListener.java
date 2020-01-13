package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.geoserve.GeoservePlacesService;

import javax.json.JsonObject;
import java.util.Map;

/**
 * Names origin products (events) based on calls to the Geoserve webservice
 */
public class OriginNamingIndexerListener extends ReliableIndexerListener{

  @Override
  protected void onBeforeProcessThreadStart() throws Exception {
    //Do database call to update lastIndexId
  }

  @Override
  public void processProduct(final ProductSummary product) throws Exception {
    // only operate on origin products
    if (product.getType() != "origin") return;

    // get required info to do Geoserve call
    Map<String,String> properties = product.getProperties();
    double latitude = Double.parseDouble(properties.get("latitude"));
    double longitude = Double.parseDouble(properties.get("longitude"));
    double maxRadiusKm = 500; //TODO: figure out what this needs to be, and make configurable
    double limit = 1;
    double minPopulation = 10000;

    // do Geoserve call
    GeoservePlacesService service = new GeoservePlacesService();
    JsonObject response = service.getPlaces("latitude=" + latitude + "&longitude=" + longitude + "&maxradiuskm=" + maxRadiusKm + "&limit=" + limit + "&minpopulation=" + minPopulation);

    double direction;
    String textDirection;


    String title = response.getString("distance") + "km " + "";

  }

  @Override
  protected void onProcessException(ProductSummary product, Exception e) throws Exception {

  }
}
