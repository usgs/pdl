package gov.usgs.earthquake.distribution;

import javax.websocket.*;
import java.net.URI;

@ClientEndpoint
//TODO: Figure out how pings are handled - should be default
public class WebSocketClient {

  private Session session;
  private WebSocketListener listener;

  //constructor tries to open socket on instantiation
  public WebSocketClient(URI endpoint, WebSocketListener listener) throws Exception {
    this.listener = listener;

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
  public void onMessage(String message){
    listener.onMessage(message);
  }

  public void shutdown() throws Exception{
    this.session.close();
  }

  public void setListener(WebSocketListener listener) {
    this.listener = listener;
  }

  public boolean isConnected() {
    return this.session != null && this.session.isOpen();
  }

}
