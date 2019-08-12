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
public class NATSStreamingNotificationReceiver extends DefaultNotificationReceiver {

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
  private File trackingFile;

  /**
   * Configures receiver based on included properties
   *
   * @param config
   *            The user-defined configuration
   *
   * @throws Exception If required properties are ignored
   */
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

    // Managing tracking file
    trackingFile = new File(trackingFileName);
    if (trackingFile.exists()) {
      // If exists, grab last sequence from it
      FileReader reader = new FileReader(trackingFile);
      JsonReader jsonReader = Json.createReader(reader);
      JsonObject json = jsonReader.readObject();
      jsonReader.close();
      reader.close();

      sequence = json.getInt(SEQUENCE_PROPERTY);
      // Ignore other properties, will be overwritten by current config
    }
    // Tracking file created on shutdown, so we don't worry about that here

    // Create connection & subscription
    connection = new StreamingConnectionFactory(clusterId,clientId).createConnection();
    subscription = connection.subscribe(
      subject,
      new STANMessageHandler(),
      new SubscriptionOptions.Builder().startAtSequence(sequence).build());
    // Always starts at stored sequence; initialized to 0 and overwritten by storage
  }

  /**
   * Closes subscription/connection and writes state in tracking file
   *
   * @throws IOException
   * @throws InterruptedException
   * @throws TimeoutException
   */
  @Override
  public void shutdown() throws Exception {
    writeTrackingFile();
    subscription.unsubscribe();
    connection.close();
    super.shutdown();
  }

  /**
   * Writes pertinent configuration information to tracking file
   */
  private void writeTrackingFile() {
    JsonObject json = Json.createObjectBuilder()
      .add(CLUSTER_ID_PROPERTY,clusterId)
      .add(CLIENT_ID_PROPERTY,clientId)
      .add(SUBJECT_PROPERTY,subject)
      .add(SEQUENCE_PROPERTY,sequence)
    .build();

    try {
      FileWriter writer = new FileWriter(trackingFileName);
      writer.write(json.toString());
      writer.close();
    } catch (IOException e) {
      // handle IO exception
    }
  }



  /**
   * Member class handling NATS Streaming messages.
   * Can be defined/instantiated inline, but done here for clarity
   */
  private class STANMessageHandler implements MessageHandler{

    //TODO: Determine if we need more robust exception handling with message
    /**
     * Defines behavior for message receipt
     *
     * @param message
     *            The message received from the STAN server
     */
    public void onMessage(Message message) {
      // increment sequence
      sequence++;

      // parse message
      String messageString = message.getData().toString();
      try {
        URLNotification notification = URLNotificationJSONConverter.parseJSON(StreamUtils.getInputStream(messageString));
        // send to listeners
        receiveNotification(notification);
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "[" + getName() + "]" + " exception converting message to JSON");
      }
    }

  }
}
