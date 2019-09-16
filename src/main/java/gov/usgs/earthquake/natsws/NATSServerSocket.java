package gov.usgs.earthquake.natsws;

import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.*;

/*
Needs:
 - Track all different sessions
 - Each session has a separate connection to NATSStreaming
    - Different Client ID for each session (handled internally)
 - Notifications forwarded separately for each client
    - Might be able to do as-is
    - Translate text notifications into URLNotifications like before
    - Translate URLNotifications into new bare-bones notification
 - Handled on client side with new notification message format

 either session sending handled by a method here (called by receivers), or receivers understand sessions and
    can do the forwarding themselves
 either way, client ID assignment has to be done here, providing unique ID's
    - also doesn't just count up; reassign client ID's to new connections after a client disconnects

 Proposal: Write NATSMiddleman (or similar) class that:
    - Stores connection information & objects
    - Knows about its own session
    - OnMessage, tosses the message toward session
    - Generates unique client ID (maybe session hash?)
 This class is only responsible for:
    - Instantiating new middlemen
    - Removing them from the list if (when) session is closed
 */

@ServerWebSocket("/subscribe/{sequence}")
public class NATSServerSocket {

  // some list that keeps track of session and corresponding notification receiver equivalent

  @OnOpen
  public void onOpen(String sequence, WebSocketSession session) {

  }

  @OnClose
  public void onClose(String sequence, WebSocketSession session) {

  }

}
