package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.earthquake.distribution.DefaultNotificationReceiver;
import gov.usgs.earthquake.distribution.URLNotification;
import gov.usgs.earthquake.distribution.URLNotificationJSONConverter;
import gov.usgs.util.Config;
import gov.usgs.util.FileUtils;
import io.nats.streaming.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NATSStreamingNotificationReceiver extends DefaultNotificationReceiver implements MessageHandler {

  private static final Logger LOGGER = Logger
          .getLogger(DefaultNotificationReceiver.class.getName());

  public static String SERVER_HOST_PROPERTY = "serverHost";
  public static String SERVER_PORT_PROPERTY = "serverPort";
  public static String CLUSTER_ID_PROPERTY = "clusterId";
  public static String CLIENT_ID_PROPERTY = "clientId";
  public static String SUBJECT_PROPERTY = "subject";
  public static String TRACKING_FILE_NAME_PROPERTY = "trackingFile";
  public static String SEQUENCE_PROPERTY = "sequence";

  //TODO: Determine default values
  public static String DEFAULT_SERVER_HOST_PROPERTY = "";
  public static String DEFAULT_SERVER_PORT_PROPERTY = "4222";
  public static String DEFAULT_CLUSTER_ID_PROPERTY = "";
  public static String DEFAULT_CLIENT_ID_PROPERTY = "";
  public static String DEFAULT_SUBJECT_PROPERTY = "";
  public static String DEFAULT_TRACKING_FILE_NAME_PROPERTY = "etc/STANReceiverInfo.json";

  private StreamingConnection connection;
  private Subscription subscription;

  private String serverHost;
  private int serverPort;
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
  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    serverHost = config.getProperty(SERVER_HOST_PROPERTY,DEFAULT_SERVER_HOST_PROPERTY);
    try {
      serverPort = Integer.parseInt(config.getProperty(SERVER_PORT_PROPERTY,DEFAULT_SERVER_PORT_PROPERTY));
    } catch (Exception e) {
      throw new ConfigurationException (SERVER_PORT_PROPERTY + " must be an integer [0-9999]");
    }
    clusterId = config.getProperty(CLUSTER_ID_PROPERTY, DEFAULT_CLUSTER_ID_PROPERTY);
    clientId = config.getProperty(CLIENT_ID_PROPERTY, DEFAULT_CLIENT_ID_PROPERTY);
    subject = config.getProperty(SUBJECT_PROPERTY, DEFAULT_SUBJECT_PROPERTY);
    trackingFileName = config.getProperty(TRACKING_FILE_NAME_PROPERTY, DEFAULT_TRACKING_FILE_NAME_PROPERTY);
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
        properties.getString(SERVER_HOST_PROPERTY) == serverHost &&
        properties.getInt(SERVER_PORT_PROPERTY) == serverPort &&
        properties.getString(CLUSTER_ID_PROPERTY) == clusterId &&
        properties.getString(CLIENT_ID_PROPERTY) == clientId &&
        properties.getString(SUBJECT_PROPERTY) == subject) {
      sequence = Long.parseLong(properties.getString(SEQUENCE_PROPERTY));
    }

    // Create connection & subscription
    StreamingConnectionFactory factory = new StreamingConnectionFactory(clusterId,clientId);
    factory.setNatsUrl("nats://" + serverHost + ":" + serverPort);
    connection = factory.createConnection();
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
  private void writeTrackingFile() throws Exception {
    JsonObject json = Json.createObjectBuilder()
      .add(SERVER_HOST_PROPERTY,serverHost)
      .add(SERVER_PORT_PROPERTY,serverPort)
      .add(CLUSTER_ID_PROPERTY,clusterId)
      .add(CLIENT_ID_PROPERTY,clientId)
      .add(SUBJECT_PROPERTY,subject)
      .add(SEQUENCE_PROPERTY,sequence)
    .build();

    FileUtils.writeFileThenMove(
      new File(trackingFileName + "_tmp"),
      new File(trackingFileName),
      json.toString().getBytes());
  }

  /**
   * Reads contents of tracking file
   *
   * @return JsonObject containing tracking file contents, or null if file doesn't exist
   */
  private JsonObject readTrackingFile() throws Exception {
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
    //writeTrackingFile(); TODO: Uncomment as soon as you figure out the exception BS
  }

  public String getServerHost() {
    return serverHost;
  }

  public void setServerHost(String serverHost) {
    this.serverHost = serverHost;
  }

  public int getServerPort() {
    return serverPort;
  }

  public void setServerPort(int serverPort) {
    this.serverPort = serverPort;
  }

  public String getClusterId() {
    return clusterId;
  }

  public void setClusterId(String clusterId) {
    this.clusterId = clusterId;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getTrackingFileName() {
    return trackingFileName;
  }

  public void setTrackingFileName(String trackingFileName) {
    this.trackingFileName = trackingFileName;
  }
}
