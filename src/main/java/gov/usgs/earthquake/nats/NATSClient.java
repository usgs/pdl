package gov.usgs.earthquake.nats;

import gov.usgs.util.Config;
import gov.usgs.util.Configurable;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

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
  private String subject;

  private StreamingConnection connection;

  @Override
  public void configure(Config config) throws Exception {
    serverHost = config.getProperty(SERVER_HOST_PROPERTY);
    serverPort = config.getProperty(SERVER_PORT_PROPERTY);
    clusterId = config.getProperty(CLUSTER_ID_PROPERTY);
    clientId = config.getProperty(CLIENT_ID_PROPERTY); //make optional (generated if not provided)
    subject = config.getProperty (SUBJECT_PROPERTY); //make optional (provide default)
  }

  @Override
  public void startup() throws Exception {
    // create connection
    StreamingConnectionFactory factory = new StreamingConnectionFactory(clusterId,clientId);
    factory.setNatsUrl("nats://" + serverHost + ":" + serverPort);
    connection = factory.createConnection();
  }

  @Override
  public void shutdown() throws Exception {
    try {
      connection.close();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "[" + getName() + "] failed to close NATS connection");
    }
    connection = null;
  }

  @Override
  public String getName() {
    return null;
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

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public StreamingConnection getConnection() {
    return connection;
  }

  public void setConnection(StreamingConnection connection) {
    this.connection = connection;
  }
}
