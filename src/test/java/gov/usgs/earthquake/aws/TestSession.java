package gov.usgs.earthquake.aws;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.MessageHandler.Partial;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;


/**
 * Test session provides testing basic remote to capture sendText calls.
 */
public class TestSession implements Session {

  TestBasicRemote testBasicRemote = new TestBasicRemote();

	@Override
	public Basic getBasicRemote() {
		return testBasicRemote;
	}

	@Override
	public String getId() {
		return "test session";
	}

  public String waitForBasicSendText(final long timeoutMillis) throws InterruptedException {
    return testBasicRemote.waitForSendText(timeoutMillis);
  }

  // other interface methods not implemented at this time.

	@Override
	public WebSocketContainer getContainer() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public <T> void addMessageHandler(Class<T> clazz, Whole<T> handler) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public <T> void addMessageHandler(Class<T> clazz, Partial<T> handler) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Set<MessageHandler> getMessageHandlers() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void removeMessageHandler(MessageHandler handler) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String getProtocolVersion() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String getNegotiatedSubprotocol() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public List<Extension> getNegotiatedExtensions() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean isSecure() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean isOpen() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public long getMaxIdleTimeout() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void setMaxIdleTimeout(long milliseconds) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void setMaxBinaryMessageBufferSize(int length) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public int getMaxBinaryMessageBufferSize() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void setMaxTextMessageBufferSize(int length) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public int getMaxTextMessageBufferSize() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Async getAsyncRemote() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void close() throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void close(CloseReason closeReason) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public URI getRequestURI() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Map<String, List<String>> getRequestParameterMap() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String getQueryString() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Map<String, String> getPathParameters() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Map<String, Object> getUserProperties() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Principal getUserPrincipal() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Set<Session> getOpenSessions() {
		throw new RuntimeException("Not implemented");
	}

}
