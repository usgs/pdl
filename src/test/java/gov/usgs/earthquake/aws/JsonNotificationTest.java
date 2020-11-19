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
      + "ew0KICAiY29udGVudHMiOiBbDQogICAgew0KICAgICAgImxlbmd0aCI6IDI1LA0KICAgIC"
      + "AgIm1vZGlmaWVkIjogIjIwMjAtMTAtMTNUMjA6MDI6NTAuMDAwWiIsDQogICAgICAicGF0"
      + "aCI6ICJ0ZXN0LnR4dCIsDQogICAgICAic2hhMjU2IjogInJTbXZPMjRSTnFKd0Q2Q3l4VD"
      + "JPdUd5ZnUzTDhaZHV3ZCt3UWlVTUdmdEk9IiwNCiAgICAgICJ0eXBlIjogInRleHQvcGxh"
      + "aW4iLA0KICAgICAgInVybCI6ICJodHRwczovL3Rlc3Rkb21haW4vcHJvZHVjdC90ZXN0LX"
      + "Byb2R1Y3QvamYxMjMvamYvMTYwMjcyNDI5ODI0OS90ZXN0LnR4dCINCiAgICB9DQogIF0s"
      + "DQogICJnZW9tZXRyeSI6IG51bGwsDQogICJpZCI6IHsNCiAgICAiY29kZSI6ICJqZjEyMy"
      + "IsDQogICAgInNvdXJjZSI6ICJqZiIsDQogICAgInR5cGUiOiAidGVzdC1wcm9kdWN0IiwN"
      + "CiAgICAidXBkYXRlVGltZSI6ICIyMDIwLTEwLTE1VDAxOjExOjM4LjI0OVoiDQogIH0sDQ"
      + "ogICJsaW5rcyI6IFtdLA0KICAicHJvcGVydGllcyI6IHsNCiAgICAicGRsLWNsaWVudC12"
      + "ZXJzaW9uIjogIlZlcnNpb24gMi41LjEgMjAyMC0wNi0yNSIsDQogICAgInRlc3Rwcm9wIj"
      + "ogInRlc3R2YWx1ZSINCiAgfSwNCiAgInNpZ25hdHVyZSI6ICJNQzBDRlFDQkZFVGhOU3dO"
      + "WEs4WTZvaC9kMDR2bnduL3hRSVVGazU1clNyaU5kVHM0ai9Hd1pRRXlLOTBMTm89IiwNCi"
      + "AgInNpZ25hdHVyZVZlcnNpb24iOiAidjIiLA0KICAic3RhdHVzIjogIlVQREFURSIsDQog"
      + "ICJ0eXBlIjogIkZlYXR1cmUiDQp9";

  @Test
  public void parseProductURL() throws Exception {
    byte[] data = StreamUtils.readStream(new URL(JSON_NOTIFICATION_URL));
    System.err.println(new String(data));

    final Product product = new JsonProduct().getProduct(
        Json.createReader(StreamUtils.getInputStream(new URL(JSON_NOTIFICATION_URL))).readObject());
    System.err.println(product.getId().toString());
  }
}
