package gov.usgs.earthquake.natsws;

import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.*;

import javax.xml.bind.annotation.XmlType;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manages concurrent message sessions with NATSMiddlemen
 * Responsible for keeping a map of connections and instantiating, removing middleman instances
 */
@ServerWebSocket("/subscribe/{sequence}")
public class NATSServerSocket {

  private static Logger LOGGER = Logger.getLogger(NATSServerSocket.class.getName());

  public static String HOST_PROPERTY = "host";
  public static String PORT_PROPERTY = "port";
  public static String CLUSTER_ID_PROPERTY = "clusterId";
  public static String SUBJECT_PROPERTY = "subject";
  public static String MESSAGE_TYPE_PROPERTY = "type";

  public static String DEFAULT_HOST = "localhost";
  public static String DEFAULT_PORT = "4222";
  public static String DEFAULT_CLUSTER_ID = "usgs";
  public static String DEFAULT_SUBJECT = "anss.pdl.realtime";
  public static String DEFAULT_MESSAGE_TYPE = "json";

  private Map<String,NATSMiddleman> natsConnections = new HashMap<>();
  private String host;
  private String port;
  private String clusterId;
  private String subject;
  private String messageType;

  //TODO: Should this be done here, or in NATSMiddleman?
  // argument: here, so it isn't done on instantiation of each NATSMiddleman
  public NATSServerSocket() {
    host = System.getenv(HOST_PROPERTY);
    if (host == null) {
      host = DEFAULT_HOST;
    }
    port = System.getenv(PORT_PROPERTY);
    if (port == null) {
      port = DEFAULT_PORT;
    }
    clusterId = System.getenv(CLUSTER_ID_PROPERTY);
    if (clusterId == null) {
      clusterId = DEFAULT_CLUSTER_ID;
    }
    subject = System.getenv(SUBJECT_PROPERTY);
    if (subject == null) {
      subject = DEFAULT_SUBJECT;
    }
    messageType = System.getenv(MESSAGE_TYPE_PROPERTY);
    if (messageType == null) {
      messageType = DEFAULT_MESSAGE_TYPE;
    }

  }

  /**
   * Creates new NATSMiddlemen and adds them to map
   *
   * @param sequence The session's desired starting sequence
   * @param session The opened session
   */
  @OnOpen
  public void onOpen(String sequence, WebSocketSession session) {
    // make sure sequence is valid
    int intSequence = 0;
    try {
      intSequence = Integer.parseInt(sequence);
    } catch (Exception e) {
      session.close(CloseReason.INVALID_FRAME_PAYLOAD_DATA);
      return;
    }

    // create a new middleman
    NATSMiddleman newMiddleman = new NATSMiddleman(
      host,
      port,
      clusterId,
      subject,
      intSequence,
      messageType == "json",
      session);

    // start up middleman
    try {
      newMiddleman.startup();
    } catch (Exception e) {
      session.close(CloseReason.INTERNAL_ERROR);
      return;
    }

    // add middleman to set
    natsConnections.put(session.getId(),newMiddleman);
  }

  /**
   * Removes NATSMiddleman from map and closes connection
   *
   * @param sequence sequence included on startup (not used)
   * @param session closed session
   */
  @OnClose
  public void onClose(String sequence, WebSocketSession session) {
    // get middleman
    NATSMiddleman middleman = natsConnections.remove(session.getId());

    // make sure middleman exists before shutting down (in case it was never created due to error)
    if (middleman != null) {
      try {
        middleman.shutdown();
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Failed to close NATS connection for session " + session.getId());
        e.printStackTrace();
      }
    }

  }

}
