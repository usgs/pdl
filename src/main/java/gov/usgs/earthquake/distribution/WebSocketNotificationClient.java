package gov.usgs.earthquake.distribution;

import javax.websocket.*;
import java.net.URI;

@ClientEndpoint
public class WebSocketNotificationClient {

  private Session session;
  private MessageHandler.Whole<String> messageHandler;

  //constructor tries to open socket on instantiation
  public WebSocketNotificationClient (URI endpoint) throws Exception{
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    container.connectToServer(this, endpoint);
  }

  @OnOpen
  public void onOpen(Session session) {
    this.session = session;
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    this.session = null;
  }

  @OnMessage
  public void onMessage(String message) {
    if (this.messageHandler != null) {
      messageHandler.onMessage(message);
    }
  }

  public void setMessageHandler(MessageHandler.Whole<String> messageHandler) {
    this.messageHandler = messageHandler;
  }

  public boolean isConnected() {
    return this.session != null;
  }

}
