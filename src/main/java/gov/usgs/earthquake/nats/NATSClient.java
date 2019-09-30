package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.ConfigurationException;
import gov.usgs.util.Config;
import gov.usgs.util.Configurable;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages conserved NATS Streaming connection properties.
 * Written so several concurrent connections can be created.
 */
public class NATSClient implements Configurable {

  public static Logger LOGGER  = Logger
          .getLogger(NATSClient.class.getName());

  public static String SERVER_HOST_PROPERTY = "serverHost";
  public static String SERVER_PORT_PROPERTY = "serverPort";
  public static String CLUSTER_ID_PROPERTY = "clusterId";
  public static String CLIENT_ID_PROPERTY = "clientId";
  public static String SUBJECT_PROPERTY = "subject";

  private String serverHost;
  private String serverPort;
  private String clusterId;
  private String clientId;
  private String clientIdSuffix;

  private StreamingConnection connection;

  public NATSClient() {
    this("localhost","4222","test-cluster",Long.toString(Thread.currentThread().getId()));
  }

  public NATSClient(String serverHost, String serverPort, String clusterId, String clientIdSuffix) {
    // try to generate a unique ID; use suffix only if fail
    this.serverHost = serverHost;
    this.serverPort = serverPort;
    this.clusterId = clusterId;
    this.clientIdSuffix = clientIdSuffix;
  }

  /**
   * Configures the required and optional parameters to connect to NATS Streaming server
   *
   * @param config
   *            the Config to load.
   * @throws Exception
   */
  @Override
  public void configure(Config config) throws Exception {
    // required parameters
    serverHost = config.getProperty(SERVER_HOST_PROPERTY);
    if (serverHost == null) {
      throw new ConfigurationException(SERVER_HOST_PROPERTY + " is a required parameter");
    }

    serverPort = config.getProperty(SERVER_PORT_PROPERTY);
    if (serverHost == null) {
      throw new ConfigurationException(SERVER_PORT_PROPERTY + " is a required parameter");
    }

    clusterId = config.getProperty(CLUSTER_ID_PROPERTY);
    if (serverHost == null) {
      throw new ConfigurationException(CLUSTER_ID_PROPERTY + " is a required parameter");
    }

    clientId = config.getProperty(CLIENT_ID_PROPERTY);
  }

  /**
   * Starts connection to NATS streaming server
   * @throws Exception If something goes wrong when connecting to NATS streaming server
   */
  @Override
  public void startup() throws Exception {
    // generate client ID if we don't have one
    if (clientId == null) {
      clientId = generateClientId(clientIdSuffix);
    }

    // create connection
    StreamingConnectionFactory factory = new StreamingConnectionFactory(clusterId,clientId);
    factory.setNatsUrl("nats://" + serverHost + ":" + serverPort);
    connection = factory.createConnection();
  }

  /**
   * Safely closes connection to NATS Streaming server
   */
  @Override
  public void shutdown() {
    try {
      connection.close();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] failed to close NATS connection");
    }
    connection = null;
  }

  /**
   * Creates a client ID based on the host IP and MAC address
   *
   * @param suffix
   *    Suffix to add to generated ID
   * @return clientId
   * @throws Exception if there's an issue accessing IP or MAC addresses, or can't do sha1 hash
   */
  private static String generateClientId(String suffix) throws Exception{
    // get mac address
    InetAddress host = InetAddress.getLocalHost();
    NetworkInterface net = NetworkInterface.getByInetAddress(host);
    byte[] macRaw =  net.getHardwareAddress();

    // do a sha1 hash
    MessageDigest digest = MessageDigest.getInstance("SHA-1");
    digest.reset();
    digest.update(macRaw);
    String sha1 = String.format("%040x", new BigInteger(1, digest.digest()));

    // create client id
    String clientId = host.getHostAddress().replace('.','-') + '_' + sha1+ '_' + suffix;

    return clientId;
  }

  @Override
  public String getName() {
    return NATSClient.class.getName();
  }

  @Override
  public void setName(String string) {

  }

  public String getServerHost() {
    return serverHost;
  }

  public void setServerHost(String serverHost) {
    this.serverHost = serverHost;
  }

  public String getServerPort() {
    return serverPort;
  }

  public void setServerPort(String serverPort) {
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

  public StreamingConnection getConnection() {
    return connection;
  }

  public void setConnection(StreamingConnection connection) {
    this.connection = connection;
  }
}
