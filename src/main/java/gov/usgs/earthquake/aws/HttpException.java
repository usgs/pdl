package gov.usgs.earthquake.aws;

/**
 * Utility exception class if there are errors while processing an HttpResponse.
 *
 * Formats as string with response code, response message, and response body.
 */
class HttpException extends Exception {
  private static final long serialVersionUID = 1L;

  public final HttpResponse response;

  public HttpException(final HttpResponse response, final String message) {
    super(message);
    this.response = response;
  }

  public String toString() {
    int code;
    String message;
    try {
      code = this.response.connection.getResponseCode();
      message = this.response.connection.getResponseMessage();
    } catch (Exception e) {
      code = -1;
      message = null;
    }
    return this.getMessage()
        + ", response " + code + " " + message
        + " : " + new String(this.response.response);
  }
}
