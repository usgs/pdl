package gov.usgs.earthquake.aws;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.XmlUtils;

public class ProductSummaryJson {

  public JsonObject getJsonObject(final ProductSummary summary) throws Exception {
    final ProductId id = summary.getId();
    return Json.createObjectBuilder()
        .add("geometry", getJsonGeometry(summary))
        .add("id", Json.createObjectBuilder()
            .add("code", id.getCode())
            .add("source", id.getSource())
            .add("type", id.getType())
            .add("updateTime", XmlUtils.formatDate(id.getUpdateTime())))
        .add("links", getJsonLinks(summary.getLinks()))
        .add("properties", getJsonProperties(summary.getProperties()))
        .add("preferredWeight", summary.getPreferredWeight())
        .add("status", summary.getStatus())
        .add("type", "Feature")
        .build();
  }

  public JsonObjectBuilder getJsonGeometry(final ProductSummary summary) throws Exception {
    final BigDecimal latitude = summary.getEventLatitude();
    final BigDecimal longitude = summary.getEventLongitude();
    final BigDecimal depth = summary.getEventDepth();
    if (latitude != null || longitude != null) {
      return Json.createObjectBuilder()
          .add("type", "Point")
          .add("coordinates", Json.createArrayBuilder()
              .add(longitude)
              .add(latitude)
              .add(depth));
    }
    return null;
  }

  public JsonArrayBuilder getJsonLinks(final Map<String, List<URI>> links) throws Exception {
    final JsonArrayBuilder builder = Json.createArrayBuilder();
    for (final String relation : links.keySet()) {
      final List<URI> relationLinks = links.get(relation);
      for (final URI uri : relationLinks) {
        builder.add(Json.createObjectBuilder()
            .add("relation", relation)
            .add("uri", uri.toString()));
      }
    }
    return builder;
  }

  public JsonObjectBuilder getJsonProperties(final Map<String, String> properties) throws Exception {
    final JsonObjectBuilder builder = Json.createObjectBuilder();
    for (final String name : properties.keySet()) {
      builder.add(name, properties.get(name));
    }
    return builder;
  }

}
