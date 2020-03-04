package gov.usgs.earthquake.util;

import java.io.File;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

public class JSONTrackingFileTest {
  @Test
  public void writeTrackingFileWriteReadTest() {
    JsonObject outJson = Json.createObjectBuilder().add("entry1","abc").add("entry2",1).build();
    JSONTrackingFile trackingFile = new JSONTrackingFile(outJson, "data/testFile.json");

    JsonObject inJson = null;
    try {
      trackingFile.write();
      inJson = trackingFile.read();
    } catch (Exception e) {
      Assert.fail();
    }

    //confirm contents are correct
    Assert.assertNotNull(inJson);
    Assert.assertEquals(outJson.getString("entry1"), inJson.getString("entry1"));
    Assert.assertEquals(outJson.getInt("entry2"), inJson.getInt("entry2"));

    //clean up
    File file = new File("data/testFile.json");
    file.delete();
  }
}