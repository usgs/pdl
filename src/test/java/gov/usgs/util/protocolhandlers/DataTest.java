package gov.usgs.util.protocolhandlers;

import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;

import gov.usgs.util.StreamUtils;
import gov.usgs.util.protocolhandlers.data.Handler;

public class DataTest {

  static {
    // register data protocol handler
    Handler.register();
  }

  @Test
  public void testBase64DataURL() throws Exception {
    final String expected = "hello world";
    final String base64 = Base64.getEncoder().encodeToString(expected.getBytes());

    // create base64 encoded data url
    URL url = new URL("data:text/plain;base64," + base64);
    // open connection and check content type
    URLConnection connection = url.openConnection();
    Assert.assertEquals("text/plain", connection.getContentType());
    // check data decodes
    byte[] data = StreamUtils.readStream(connection.getInputStream());
    Assert.assertEquals("hello world", new String(data));
    // check url remains base64 encoded
    Assert.assertEquals("data:text/plain;base64," + base64, url.toString());
  }

  @Test
  public void testDataURL() throws Exception {
    final String expected = "hello world";
    // create non-base64 encoded data url
    URL url = new URL("data:," + expected);
    // open connection and check content type
    URLConnection connection = url.openConnection();
    Assert.assertNull(connection.getContentType());
    // check data parses
    byte[] data = StreamUtils.readStream(url);
    Assert.assertEquals(expected, new String(data));
    // check toString
    Assert.assertEquals("data:," + expected, url.toString());
  }

  @Test
  public void testEmptyURL() throws Exception {
    URL url = new URL("data:,");
    // check data is empty
    URLConnection connection = url.openConnection();
    Assert.assertEquals(0, connection.getContentLength());
    byte[] data = StreamUtils.readStream(url);
    Assert.assertEquals(0, data.length);
    // check toString
    Assert.assertEquals("data:,", url.toString());
  }
}
