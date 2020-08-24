package gov.usgs.earthquake.aws;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import gov.usgs.earthquake.indexer.Event;
import gov.usgs.earthquake.indexer.ProductSummary;


public class EventJson {

  public JsonObject getJsonObject(final Event event) throws Exception {
    final ProductSummary originProduct = event.getPreferredOriginProduct();
    final Map<String, String> originProperties = originProduct.getProperties();
    final String status = event.isDeleted()
        ? "deleted"
        : originProperties.getOrDefault("review-status", "automatic");

    return Json.createObjectBuilder()
        .add("geometry", this.getJsonGeometry(event))
        .add("id", event.getEventId())
        .add("properties",
            Json.createObjectBuilder()
                .add("magnitude", event.getMagnitude())
                .add("net", event.getSource())
                .add("netid", event.getSourceCode())
                .add("products", getJsonProducts(event.getProducts()))
                .add("status", status)
        )
        .add("type", "Feature")
        .build();
  }

  public JsonObject getJsonGeometry(final Event event) throws Exception {
    BigDecimal depth = event.getDepth();
    BigDecimal latitude = event.getLatitude();
    BigDecimal longitude = event.getLongitude();
    if (latitude == null && longitude == null) {
      return null;
    }
    return Json.createObjectBuilder()
        .add("type", "Point")
        .add("coordinates",
            Json.createArrayBuilder()
                .add(longitude)
                .add(latitude)
                .add(depth))
        .build();
  }

  public JsonArray getJsonIds(final Event event, final boolean includeDeleted) throws Exception {
    JsonArrayBuilder ids = Json.createArrayBuilder();
    Map<String, List<String>> allEventCodes = event.getAllEventCodes(includeDeleted);
    for (final String net : allEventCodes.keySet()) {
      for (final String netid : allEventCodes.get(net)) {
        ids.add(net + netid);
      }
    }
    return ids.build();
  }

  public JsonObject getJsonProducts(final Map<String, List<ProductSummary>> products) throws Exception {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    for (final String type : products.keySet()) {
      JsonArrayBuilder typeProducts = Json.createArrayBuilder();
      for (final ProductSummary summary : products.get(type)) {
        typeProducts.add(new ProductSummaryJson().getJsonObject(summary));
      }
      builder.add(type, typeProducts);
    }
    return builder.build();
  }

}
