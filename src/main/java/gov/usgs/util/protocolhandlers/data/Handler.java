package gov.usgs.util.protocolhandlers.data;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Data URLs handler.
 *
 * "data:[&lt;mediatype&gt;][;base64],&lt;data&gt;"
 */
public class Handler extends URLStreamHandler {

  /** property for protocol handlers */
  public static final String PROTOCOL_HANDLERS_PROPERTY = "java.protocol.handler.pkgs";

  /**
   * Register data protocol handler
   */
  public static void register() {
    final String packageName = Handler.class.getPackage().getName().replace(".data", "");
    final String protocolHandlers = System.getProperty(PROTOCOL_HANDLERS_PROPERTY);
    if (protocolHandlers == null || protocolHandlers.indexOf("gov.usgs.util.protocolhandlers") == -1) {
      System.setProperty(
          PROTOCOL_HANDLERS_PROPERTY,
          protocolHandlers == null ? packageName : protocolHandlers + "|" + packageName);
    }
  }


  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    try {
      return new DataURLConnection(url);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  protected void parseURL(final URL url, final String spec, final int start, final int end) throws SecurityException {
    int colon = spec.indexOf(":");

    final String protocol = "data";
    final String host = null;
    final int port = 80;
    final String authority = null;
    final String userInfo = null;
    final String path = spec.substring(colon + 1, end);
    final String query = null;
    final String ref = null;

    setURL(url, protocol, host, port, authority, userInfo, path, query, ref);
  }

  @Override
  protected String toExternalForm(final URL url) {
    return url.getProtocol() + ":" + url.getPath();
  }
}
