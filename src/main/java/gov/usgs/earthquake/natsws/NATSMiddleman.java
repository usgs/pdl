package gov.usgs.earthquake.natsws;

import gov.usgs.earthquake.nats.NATSClient;
import io.micronaut.websocket.WebSocketSession;
import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import io.nats.streaming.Subscription;
import io.nats.streaming.SubscriptionOptions;

/**
 * Responsible for subscribing to a single channel and forwarding messages
 */
public class NATSMiddleman implements MessageHandler {

  private NATSClient client;
  private String subject;
  private int sequence;
  private Subscription subscription;
  private boolean isMessageJson;
  private WebSocketSession owner;

  public NATSMiddleman(String host, String port, String clusterId, String subject, int sequence, boolean isMessageJson, WebSocketSession owner) {
    client = new NATSClient(host,port,clusterId,owner.getId());
    this.subject = subject;
    this.sequence = sequence;
    this.isMessageJson = isMessageJson;
    this.owner = owner;
  }

  /**
   * Starts a NATSStreaming connection using the NATSClient and subscribes to a channel
   *
   * @throws Exception if establishment of connection fails
   */
  public void startup() throws Exception {
    client.startup();
    subscription = client.getConnection().subscribe(subject,this, new SubscriptionOptions.Builder().startAtSequence(sequence).build());
  }

  /**
   * Closes connection to NATSStreaming
   *
   * @throws Exception if close fails for some reason
   */
  public void shutdown() throws Exception {
    subscription.close();
    client.shutdown();
  }

  //TODO: Format as JSON if message is json
  //TODO: Include (formatted as JSON):
  //  - Sequence
  //  - Timestamp
  //  - Data
  /**
   * Forwards messages from NATSStreaming to session owner
   *
   * @param msg forwarded message
   */
  @Override
  public void onMessage(Message msg) {
    // forward message
    owner.send(new String(msg.getData()));
  }
}
