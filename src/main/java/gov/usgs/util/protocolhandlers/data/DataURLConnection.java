package gov.usgs.util.protocolhandlers.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

/**
 * URLConnection for data protocol URLs.
 */
public class DataURLConnection extends URLConnection {

  private byte[] data;
  private String type;

  public DataURLConnection(final URL url) throws Exception {
    super(url);

    // "data:[<mediatype>][;base64],<data>"
    // path is everything after "data:"
    final String path = url.getPath();
    final int base64 = path.indexOf(";base64");
    final int comma = path.indexOf(",");

    type = path.substring(0, base64 >= 0 ? base64 : comma);
    if ("".equals(type)) {
      type = null;
    }
    data = path.substring(comma + 1).getBytes("UTF8");
    if (base64 >= 0) {
      data = Base64.getDecoder().decode(data);
    }
  }

  @Override
  public int getContentLength() {
    return data.length;
  }

  @Override
  public String getContentType() {
    return type;
  }

	@Override
	public void connect() throws IOException {
    // no connection needed
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(data);
  }

}
