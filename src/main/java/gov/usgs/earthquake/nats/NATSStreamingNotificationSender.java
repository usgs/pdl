package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.*;
import gov.usgs.util.Config;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NATSStreamingNotificationSender extends DefaultNotificationSender {

  private static final Logger LOGGER = Logger
          .getLogger(DefaultNotificationSender.class.getName());

  public static String CLUSTER_ID_PROPERTY = "clusterid";
  public static String CLIENT_ID_PROPERTY = "clientid";
  public static String SUBJECT_PROPERTY = "subject";

  public static String DEFAULT_CLUSTER_ID_PROPERTY = "";
  public static String DEFAULT_SUBJECT_PROPERTY = "";

  private String clusterId;
  private String clientId;
  private String subject;
  private StreamingConnection stanConnection;

  @Override
  public void configure(Config config) throws Exception{
    super.configure(config);

    clusterId = config.getProperty(CLUSTER_ID_PROPERTY, DEFAULT_CLUSTER_ID_PROPERTY);
    subject = config.getProperty(SUBJECT_PROPERTY, DEFAULT_SUBJECT_PROPERTY);
  }

  @Override
  public void sendNotification(final Notification notification) throws Exception {
    String message = URLNotificationJSONConverter.toJSON((URLNotification) notification);
    try {
      stanConnection.publish(subject, message.getBytes());
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] exception publishing NATSStreaming notification:");
      throw e;
    }
  }

  /**
   * Creates a client ID based on the host IP and MAC address
   *
   * @return clientId
   * @throws Exception if there's an issue accessing IP or MAC addresses, or can't do sha1 hash
   */
  private static String generateClientId() throws Exception {
    // get mac address
    InetAddress host = InetAddress.getLocalHost();
    NetworkInterface net = NetworkInterface.getByInetAddress(host);
    String mac = "";
    byte[] macRaw =  net.getHardwareAddress();
    for (int i = 0; i < macRaw.length; i++) {
      mac += String.format("%02x",macRaw[i]);
      mac += ':';
    }
    mac = mac.substring(0,mac.length()-1);

    // do a sha1 hash
    MessageDigest digest = MessageDigest.getInstance("SHA-1");
    digest.reset();
    digest.update(mac.getBytes("utf8"));
    String sha1 = String.format("%040x", new BigInteger(1, digest.digest()));

    // create client id
    String clientId = host.getHostAddress().replace('.','-') + '_' + sha1;

    return clientId;
  }

  @Override
  public void startup() throws Exception {
    super.startup();

    // generate client ID
    try {
      clientId = generateClientId();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] could not generate client ID. Is this machine connected to the internet?");
      throw e;
    }

    StreamingConnectionFactory factory = new StreamingConnectionFactory(clusterId,clientId);
    factory.setNatsUrl("nats://" + this.serverHost + ":" + this.serverPort);
    stanConnection = factory.createConnection();
  }

  @Override
  public void shutdown() throws Exception {
    try {
      stanConnection.close();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] failed to close NATS connection");
    }
    stanConnection = null;
    super.shutdown();
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
}