package gov.usgs.earthquake.nats;

import gov.usgs.earthquake.distribution.DefaultNotificationSender;
import gov.usgs.earthquake.distribution.Notification;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class NATSStreamingNotificationSender extends DefaultNotificationSender {

  private String clusterId;
  private String clientId;
  private String subject;
  private StreamingConnection stanConnection;

  @Override
  public void sendMessage(String message) {
    try {
      stanConnection.publish(subject, message.getBytes());
    } catch (TimeoutException te) {
      // if we lose connection to server
    } catch (InterruptedException ie) {
      // if we're interrupted when sending
    } catch (IOException ioe) {
      // if there's a problem with the message
    }
  }

  //TODO: Decide if DefaultNotificationSender should always be using URLNotifications, or if a Notification is sufficient
  // everywhere
  @Override
  public String notificationToString(Notification notification) throws Exception{
    return super.notificationToString(notification);
  }

  public void startup() throws Exception {
    stanConnection = new StreamingConnectionFactory(clusterId,clientId).createConnection();
  }

  public void shutdown() throws Exception {
    stanConnection.close();
  }
}
