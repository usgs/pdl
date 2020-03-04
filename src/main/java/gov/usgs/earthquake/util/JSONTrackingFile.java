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
  private JsonObject json;
  private File trackingFile;
  private String trackingFileName;

  public JSONTrackingFile(String fileName) {
    this(null, fileName);
  }

  public JSONTrackingFile(JsonObject json, String fileName) {
    this.json = json;
    this.trackingFileName = fileName;
    this.trackingFile = new File(fileName);
  }

  /**
   * Writes the contents of json to trackingFile
   * @throws IOException
   */
  public void write() throws IOException {
    FileUtils.writeFileThenMove(new File(trackingFileName + ".tmp"), trackingFile, json.toString().getBytes());
  }

  /**
   * Updates the json of this object, then calls write
   * @throws IOException
   */
  public void write(JsonObject json) throws IOException{
    this.json = json;
    this.write();
  }

  /**
   * Returns the contents of the tracking file, or null if the file doesn't exist
   * @throws IOException, JsonParsingException
   */
  public JsonObject read() throws Exception {
    if (trackingFile.exists()) {
      try (InputStream in = new ByteArrayInputStream(FileUtils.readFile(this.trackingFile)); JsonReader reader = Json.createReader(in)) {
        this.json = reader.readObject();
      }
    } else {
      return null;
    }
    return this.json;
  }

  public void setJson(JsonObject json) {
    this.json = json;
  }

  public JsonObject getJson() {
    return this.json;
  }

  public String getFileName() {
    return this.trackingFileName;
  }

}