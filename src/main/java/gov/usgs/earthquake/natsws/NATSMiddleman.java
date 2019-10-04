package gov.usgs.earthquake.natsws;

import gov.usgs.earthquake.nats.NATSClient;
import io.micronaut.websocket.WebSocketSession;
import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import io.nats.streaming.Subscription;
import io.nats.streaming.SubscriptionOptions;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;

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

  /**
   * Forwards messages from NATSStreaming to session owner. If we expect JSON, then the message is formatted so.
   *
   * @param msg forwarded message
   */
  @Override
  public void onMessage(Message msg) {
    // get metadata
    JsonObjectBuilder builder = Json.createObjectBuilder()
      .add("sequence",msg.getSequence())
      .add("timestamp",msg.getTimestamp());

    // format according to whether we expect json or not
    if (isMessageJson) {
      JsonReader reader = Json.createReader(new ByteArrayInputStream(msg.getData()));
      JsonObject inJson = reader.readObject();
      reader.close();
      builder.add("data",inJson);
    } else {
      builder.add("data",new String(msg.getData()));
    }

    // create and forward
    JsonObject json = builder.build();
    owner.send(json);
  }
}
