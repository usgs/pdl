package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.distribution.DefaultNotificationReceiver;
import gov.usgs.earthquake.distribution.URLNotification;
import gov.usgs.earthquake.distribution.URLNotificationJSONConverter;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;
import io.nats.streaming.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO: How are host and port used? Shouldn't they be?
// they will have to be configured in the factory on startup for NATS URL
public class NATSStreamingNotificationReceiver extends DefaultNotificationReceiver implements MessageHandler {

  private static final Logger LOGGER = Logger
          .getLogger(DefaultNotificationReceiver.class.getName());

  public static String CLUSTER_ID_PROPERTY = "clusterid";
  public static String CLIENT_ID_PROPERTY = "clientid";
  public static String SUBJECT_PROPERTY = "subject";
  public static String TRACKING_FILE_NAME_PROPERTY = "trackingfile";
  public static String SEQUENCE_PROPERTY = "sequence";

  private StreamingConnection connection;
  private Subscription subscription;
  private String clusterId;
  private String clientId;
  private String subject;
  private long sequence = 0;
  private String trackingFileName;

  /**
   * Configures receiver based on included properties
   *
   * @param config
   *            The user-defined configuration
   *
   * @throws Exception If required properties are ignored
   */
  //TODO: Create defaults for properties
  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    clusterId = config.getProperty(CLUSTER_ID_PROPERTY);
    if (clusterId == null) {
      throw new ConfigurationException("[" + getName() + "] " + CLUSTER_ID_PROPERTY + " must be defined");
    }
    clientId = config.getProperty(CLIENT_ID_PROPERTY);
    if (clientId == null) {
      throw new ConfigurationException("[" + getName() + "] " + CLIENT_ID_PROPERTY + " must be defined");
    }
    subject = config.getProperty(SUBJECT_PROPERTY);
    if (subject == null) {
      throw new ConfigurationException("[" + getName() + "] " + SUBJECT_PROPERTY + " must be defined");
    }
    trackingFileName = config.getProperty(TRACKING_FILE_NAME_PROPERTY);
    if (trackingFileName == null) {
      throw new ConfigurationException("[" + getName() + "] " + TRACKING_FILE_NAME_PROPERTY + " must be defined");
    }
  }

  /**
   * Does initial tracking file management and subscribes to server
   * With a tracking file, gets the last sequence
   *
   * @throws InterruptedException
   * @throws IOException
   */
  @Override
  public void startup() throws Exception {
    super.startup();

    //Check properties if tracking file exists
    JsonObject properties = readTrackingFile();
    if (properties != null &&
        properties.getString(CLUSTER_ID_PROPERTY) == clusterId &&
        properties.getString(CLIENT_ID_PROPERTY) == clientId &&
        properties.getString(SUBJECT_PROPERTY) == subject) {
      sequence = Long.parseLong(properties.getString(SEQUENCE_PROPERTY));
    }

    // Create connection & subscription
    connection = new StreamingConnectionFactory(clusterId,clientId).createConnection();
    subscription = connection.subscribe(
      subject,
      this,
      new SubscriptionOptions.Builder().startAtSequence(sequence).build());
    // Always starts at stored sequence; initialized to 0 and overwritten by storage
  }

  /**
   * Closes subscription/connection and writes state in tracking file
   * Wraps each statement in a try/catch to ensure each step still happens
   *
   * @throws IOException
   * @throws InterruptedException
   * @throws TimeoutException
   */
  @Override
  public void shutdown() throws Exception {
    try {
      writeTrackingFile();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] failed to write to tracking file");
    }
    try {
      subscription.unsubscribe();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] failed to unsubscribe from NATS channel");
    }
    try {
      connection.close();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] failed to close NATS connection");
    }
    subscription = null;
    connection = null;
    super.shutdown();
  }

  /**
   * Writes pertinent configuration information to tracking file
   */
  private void writeTrackingFile() throws Exception{
    //TODO: Use fileUtils' writeThenMove();
    JsonObject json = Json.createObjectBuilder()
      .add(CLUSTER_ID_PROPERTY,clusterId)
      .add(CLIENT_ID_PROPERTY,clientId)
      .add(SUBJECT_PROPERTY,subject)
      .add(SEQUENCE_PROPERTY,sequence)
    .build();

    FileWriter writer = new FileWriter(trackingFileName);
    writer.write(json.toString());
    writer.close();
  }

  /**
   * Reads contents of tracking file
   *
   * @return JsonObject containing tracking file contents, or null if file doesn't exist
   */
  private JsonObject readTrackingFile() throws Exception {
    //TODO: Use fileUtils read
    JsonObject json = null;

    File trackingFile = new File(trackingFileName);
    if (trackingFile.exists()) {
      FileReader reader = new FileReader(trackingFile);
      JsonReader jsonReader = Json.createReader(reader);
      json = jsonReader.readObject();
      jsonReader.close();
      reader.close();
    }
    return json;
  }

  /**
   * Defines behavior for message receipt
   *
   * @param message
   *            The message received from the STAN server
   */
  @Override
  //TODO: Figure out how to throw exception W/O clash with superclass
  public void onMessage(Message message) {
    // parse message
    try {
      URLNotification notification = URLNotificationJSONConverter.parseJSON(new ByteArrayInputStream(message.getData()));
      // send to listeners
      receiveNotification(notification);
    } catch (Exception e) {
      //TODO: Throw exceptions instead of catching
      LOGGER.log(Level.WARNING, "[" + getName() + "]" + " exception converting message to JSON");
    }

    // set sequence, update tracking file
    sequence = message.getSequence();
    writeTrackingFile();
  }

}
