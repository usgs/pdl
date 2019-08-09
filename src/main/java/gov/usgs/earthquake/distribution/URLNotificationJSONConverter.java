package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.XmlUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class URLNotificationJSONConverter {

  public static final String ATTRIBUTE_PRODUCT_ID = "id";
  public static final String ATTRIBUTE_SOURCE = "source";
  public static final String ATTRIBUTE_TYPE = "type";
  public static final String ATTRIBUTE_CODE = "code";
  public static final String ATTRIBUTE_UPDATE_TIME = "updatetime";
  public static final String ATTRIBUTE_TRACKER_URL = "trackerURL";
  public static final String ATTRIBUTE_EXPIRES = "expires";
  public static final String ATTRIBUTE_URL = "url";

  public static String toJSON(final URLNotification notification) {
    //id
    ProductId id = notification.getProductId();

    JsonObject json = Json.createObjectBuilder()
      .add(ATTRIBUTE_PRODUCT_ID,Json.createObjectBuilder()
        .add(ATTRIBUTE_SOURCE,id.getSource())
        .add(ATTRIBUTE_TYPE,id.getType())
        .add(ATTRIBUTE_CODE,id.getCode())
        .add(ATTRIBUTE_UPDATE_TIME, XmlUtils.formatDate(id.getUpdateTime())))
      .add(ATTRIBUTE_TRACKER_URL,notification.getTrackerURL().toString())
      .add(ATTRIBUTE_EXPIRES,XmlUtils.formatDate(notification.getExpirationDate()))
      .add(ATTRIBUTE_URL,notification.getProductURL().toString())
      .build();

    return json.toString();
  }

  public static URLNotification parseJSON(final InputStream message) throws Exception{
    JsonReader jsonReader = Json.createReader(message);
    JsonObject json = jsonReader.readObject();
    jsonReader.close();

    JsonObject idJson = json.getJsonObject(ATTRIBUTE_PRODUCT_ID);

    ProductId id = new ProductId(
      idJson.getString(ATTRIBUTE_SOURCE),
      idJson.getString(ATTRIBUTE_TYPE),
      idJson.getString(ATTRIBUTE_CODE),
      XmlUtils.getDate(idJson.getString(ATTRIBUTE_UPDATE_TIME)));

    return new URLNotification(
            id,
            XmlUtils.getDate(json.getString(ATTRIBUTE_EXPIRES)),
            new URL(json.getString(ATTRIBUTE_TRACKER_URL)),
            new URL(json.getString(ATTRIBUTE_URL)));
  }

  public static void main(String[] args) throws Exception{
    URLNotification testNotification = new URLNotification(new ProductId("testSource","testType","testCode"), new Date(),
            new URL("http://localhost/tracker/"), new URL("http://localhost/product/"));

    String JSON = toJSON(testNotification);

    System.out.println(JSON);

    URLNotification outNotification = parseJSON(new ByteArrayInputStream(JSON.getBytes()));

    System.out.println("Notifications " + ((testNotification.equals(outNotification))?"are":"are not") + " equivalent");
  }
}
