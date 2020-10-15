package gov.usgs.earthquake.aws;

import java.net.URL;

import javax.json.Json;

import org.junit.Test;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.JsonProduct;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.protocolhandlers.data.Handler;

public class JsonNotificationTest {
  static {
    Handler.register();
  }

  public static final String JSON_NOTIFICATION_URL = "data:;base64,"
      + "eyJjb250ZW50cyI6W3sibGVuZ3RoIjoyNSwibW9kaWZpZWQiOiIyMDIwLTEwLTEzVDIwOj"
      + "AyOjUwLjAwMFoiLCJwYXRoIjoidGVzdC50eHQiLCJzaGEyNTYiOiJyU212TzI0Uk5xSndE"
      + "NkN5eFQyT3VHeWZ1M0w4WmR1d2Qrd1FpVU1HZnRJPSIsInR5cGUiOiJ0ZXh0L3BsYWluIi"
      + "widXJsIjoiaHR0cHM6Ly9hbnNzLXBkbC1scC1kZXZlbG9wbWVudC1yYnVja2V0LTVvMjkw"
      + "OW45MGI5bC5zMy5kdWFsc3RhY2sudXMtd2VzdC0yLmFtYXpvbmF3cy5jb20vcHJvZHVjdH"
      + "Rlc3QtcHJvZHVjdC9qZjEyMy9qZi8xNjAyNzI0Mjk4MjQ5L3Rlc3QudHh0In1dLCJnZW9t"
      + "ZXRyeSI6bnVsbCwiaWQiOnsiY29kZSI6ImpmMTIzIiwic291cmNlIjoiamYiLCJ0eXBlIj"
      + "oidGVzdC1wcm9kdWN0IiwidXBkYXRlVGltZSI6IjIwMjAtMTAtMTVUMDE6MTE6MzguMjQ5"
      + "WiJ9LCJsaW5rcyI6W10sInByb3BlcnRpZXMiOnsicGRsLWNsaWVudC12ZXJzaW9uIjoiVm"
      + "Vyc2lvbiAyLjUuMSAyMDIwLTA2LTI1IiwidGVzdHByb3AiOiJ0ZXN0dmFsdWUifSwic2ln"
      + "bmF0dXJlIjoiTUMwQ0ZRQ0JGRVRoTlN3TlhLOFk2b2gvZDA0dm53bi94UUlVRms1NXJTcm"
      + "lOZFRzNGovR3daUUV5SzkwTE5vPSIsInNpZ25hdHVyZVZlcnNpb24iOiJ2MiIsInN0YXR1"
      + "cyI6IlVQREFURSIsInR5cGUiOiJGZWF0dXJlIn0=";

  @Test
  public void parseProductURL() throws Exception {
    byte[] data = StreamUtils.readStream(new URL(JSON_NOTIFICATION_URL));
    System.err.println(new String(data));

    final Product product = new JsonProduct().getProduct(
        Json.createReader(StreamUtils.getInputStream(new URL(JSON_NOTIFICATION_URL))).readObject());
    System.err.println(product.getId().toString());
  }
}
