package gov.usgs.earthquake.distribution;

import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receives notifications from an arbitrary web socket.
 */
public class WebSocketNotificationReceiver extends DefaultNotificationReceiver implements WebSocketListener {

  public static final Logger LOGGER = Logger
          .getLogger(WebSocketNotificationReceiver.class.getName());

  public static final String SERVER_HOST_PROPERTY = "serverHost";
  public static final String SERVER_PORT_PROPERTY = "serverPort";
  public static final String SEQUENCE_PROPERTY = "sequence";
  public static final String TIMESTAMP_PROPERTY = "timestamp";
  public static final String TRACKING_FILE_NAME_PROPERTY = "trackingFileName";

  public static final String DEFAULT_SERVER_HOST = "http://www.google.com";
  public static final String DEFAULT_SERVER_PORT = "4222";
  public static String DEFAULT_TRACKING_FILE_NAME = "data/WebSocketReceiverInfo.json";

  public static final String ATTRIBUTE_DATA = "data";

  private String serverHost;
  private String serverPort;
  private String trackingFileName;
  private WebSocketClient client;
  private String sequence = "0";


  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    serverHost = config.getProperty(SERVER_HOST_PROPERTY, DEFAULT_SERVER_HOST);
    serverPort = config.getProperty(SERVER_PORT_PROPERTY, DEFAULT_SERVER_PORT);
    trackingFileName = config.getProperty(TRACKING_FILE_NAME_PROPERTY, DEFAULT_TRACKING_FILE_NAME);
  }

  /**
   * Reads a sequence from a tracking file if it exists. Otherwise, starting sequence is 0.
   * Connects to web socket
   * @throws Exception
   */
  @Override
  public void startup() throws Exception{
    super.startup();

    //read sequence from tracking file if other parameters agree
    JsonObject json = readTrackingFile();
    if (json != null && json.getString(SERVER_HOST_PROPERTY) == serverHost && json.getString(SERVER_PORT_PROPERTY) == serverPort) {
      sequence = json.getString(SEQUENCE_PROPERTY);
    }

    //open websocket
    client = new WebSocketClient(new URI(serverHost + "sequence/" + sequence + ":" + serverPort), this);
  }

  /**
   * Closes web socket
   * @throws Exception
   */
  @Override
  public void shutdown() throws Exception{
    //close socket
    client.shutdown();
    super.shutdown();
  }

  /**
   * Writes tracking file to disc, storing latest sequence
   * @throws Exception
   */
  public void writeTrackingFile() throws Exception {
    JsonObject json = Json.createObjectBuilder()
            .add(SERVER_HOST_PROPERTY,serverHost)
            .add(SERVER_PORT_PROPERTY,serverPort)
            .add(SEQUENCE_PROPERTY,sequence)
            .build();

    FileUtils.writeFileThenMove(
            new File(trackingFileName + "_tmp"),
            new File(trackingFileName),
            json.toString().getBytes());
  }

  /**
   * Reads tracking file from disc
   * @return  JsonObject tracking file
   * @throws Exception
   */
  public JsonObject readTrackingFile() throws Exception {
    JsonObject json = null;

    File trackingFile = new File(trackingFileName);
    if (trackingFile.exists()) {
      InputStream contents = new ByteArrayInputStream(FileUtils.readFile(trackingFile));
      JsonReader jsonReader = Json.createReader(contents);
      json = jsonReader.readObject();
      jsonReader.close();
    }
    return json;
  }

  /**
   * Message handler function passed to WebSocketClient
   * Parses the message as JSON, receives the contained URL notification, and writes the tracking file.
   * @param message
   */
  @Override
  public void onMessage(String message) {
    InputStream in = null;
    try {
      //parse input as json
      in = StreamUtils.getInputStream(message);
      JsonReader reader = Json.createReader(in);
      JsonObject json = reader.readObject();
      reader.close();

      //convert to URLNotification and receive
      JsonObject dataJson = json.getJsonObject(ATTRIBUTE_DATA);
      URLNotification notification = URLNotificationJSONConverter.parseJSON(dataJson);
      receiveNotification(notification);

      //send heartbeat
      HeartbeatListener.sendHeartbeatMessage(getName(), "nats notification timestamp", json.getString(TIMESTAMP_PROPERTY));

      //write tracking file
      sequence = json.getString(SEQUENCE_PROPERTY);
      writeTrackingFile();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] exception while processing URLNotification ", e);
    } finally {
      StreamUtils.closeStream(in);
    }

  }
}