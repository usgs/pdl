package gov.usgs.earthquake.aws;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.URLContent;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

public class ProductJson {

  public JsonObject getJsonObject(final Product product) throws Exception {
    final ProductId id = product.getId();
    return Json.createObjectBuilder()
        .add("contents", getJsonContents(product.getContents()))
        .add("geometry", getJsonGeometry(product))
        .add("id", Json.createObjectBuilder()
            .add("code", id.getCode())
            .add("source", id.getSource())
            .add("type", id.getType())
            .add("updateTime", XmlUtils.formatDate(id.getUpdateTime())))
        .add("links", getJsonLinks(product.getLinks()))
        .add("properties", getJsonProperties(product.getProperties()))
        .add("signature", product.getSignature())
        .add("status", product.getStatus())
        .add("type", "Feature")
        .build();
  }

  public Product getProduct(final JsonObject json) throws Exception {
    return null;
  }

  protected JsonArrayBuilder getJsonContents(final Map<String, Content> contents) throws Exception {
    final JsonArrayBuilder builder = Json.createArrayBuilder();
    for (final String path : contents.keySet()) {
      final Content content = contents.get(path);

      final JsonObjectBuilder jsonContent = Json.createObjectBuilder()
          .add("length", content.getLength())
          .add("modified", XmlUtils.formatDate(content.getLastModified()))
          .add("path", path)
          .add("type", content.getContentType());
      if (content instanceof URLContent) {
        jsonContent.add("url", ((URLContent) content).getURL().toString());
      } else if ("".equals(path)) {
        jsonContent.add("url", "data:" + content.getContentType() +
            ";base64," + Base64.getEncoder().encodeToString(
                StreamUtils.readStream(content.getInputStream())));
      } else {
        throw new IllegalArgumentException("Contents must be URLContent");
      }
      builder.add(jsonContent);
    }
    return builder;
  }

  protected JsonObjectBuilder getJsonGeometry(final Product product) throws Exception {
    final BigDecimal latitude = product.getLatitude();
    final BigDecimal longitude = product.getLongitude();
    final BigDecimal depth = product.getDepth();
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

  protected JsonArrayBuilder getJsonLinks(final Map<String, List<URI>> links) throws Exception {
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

  protected JsonObjectBuilder getJsonProperties(final Map<String, String> properties) throws Exception {
    final JsonObjectBuilder builder = Json.createObjectBuilder();
    for (final String name : properties.keySet()) {
      builder.add(name, properties.get(name));
    }
    return builder;
  }

}
