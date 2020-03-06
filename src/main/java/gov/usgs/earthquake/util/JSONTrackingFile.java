package gov.usgs.earthquake.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import gov.usgs.util.FileUtils;

/**
 * Represents a tracking file in JSON.
 */
public class JSONTrackingFile {
  private String trackingFileName;

  public JSONTrackingFile(String fileName) {
    this(null, fileName);
  }

  public JSONTrackingFile(JsonObject json, String fileName) {
    this.trackingFileName = fileName;
  }

  /**
   * Writes the contents of json to trackingFile
   * @throws IOException
   */
  public void write(JsonObject json) throws IOException {
    FileUtils.writeFileThenMove(new File(trackingFileName + ".tmp"), new File(trackingFileName), json.toString().getBytes());
  }

  /**
   * Returns the contents of the tracking file, or null if the file doesn't exist
   * @throws IOException, JsonParsingException
   */
  public JsonObject read() throws Exception {
    JsonObject json = null;
    File trackingFile = new File(trackingFileName);
    if (trackingFile.exists()) {
      try (InputStream in = new ByteArrayInputStream(FileUtils.readFile(trackingFile)); JsonReader reader = Json.createReader(in)) {
        json = reader.readObject();
      }
    }
    return json;
  }

  public String getTrackingFileName() {
    return this.trackingFileName;
  }

  public void setTrackingFileName(String trackingFileName) {
    this.trackingFileName = trackingFileName;
  }

}