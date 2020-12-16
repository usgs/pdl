package gov.usgs.earthquake.aws;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import javax.websocket.EncodeException;
import javax.websocket.RemoteEndpoint.Basic;

/**
 * TestBasicRemote captures data sent via sendText calls.
 */
public class TestBasicRemote implements Basic {

	private static final Logger LOGGER = Logger.getLogger(TestBasicRemote.class.getName());

  public String lastSendText = null;
  private Object sendTextSync = new Object();

	@Override
	public void sendText(String text) throws IOException {
		LOGGER.info("sendText called with " + text);
    synchronized(sendTextSync) {
      lastSendText = text;
      sendTextSync.notifyAll();
    }
	}

  public String waitForSendText(final long timeoutMillis) throws InterruptedException {
    synchronized(sendTextSync) {
      if (lastSendText == null) {
        sendTextSync.wait(timeoutMillis);
      }
      return lastSendText;
    }
  };

  // other methods in interface are not used at this time

	@Override
	public void setBatchingAllowed(boolean allowed) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean getBatchingAllowed() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void flushBatch() throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void sendPing(ByteBuffer applicationData) throws IOException, IllegalArgumentException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void sendPong(ByteBuffer applicationData) throws IOException, IllegalArgumentException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void sendBinary(ByteBuffer data) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void sendText(String partialMessage, boolean isLast) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void sendBinary(ByteBuffer partialByte, boolean isLast) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public OutputStream getSendStream() throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Writer getSendWriter() throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void sendObject(Object data) throws IOException, EncodeException {
		throw new RuntimeException("Not implemented");
	}

}
