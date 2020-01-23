package gov.usgs.earthquake.geoserve;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;

public interface GeoservePlaces {
  /**
   * Uses the location information provided in order to generate an event title
   * from data obtained from the GeoservePlaces endpoint.
   *
   * @param latitude  Decimal degrees latitude for the location of interest
   * @param longitude Decimal degrees longitude for the location of interest
   *
   * @throws IOException           If the GeoesrvePlaces endpoint can not be
   *                               contacted
   * @throws MalformedURLException If the configured endpoint URL is invalid
   */
  public String getEventTitle(BigDecimal latitude, BigDecimal longitude) throws IOException, MalformedURLException;

  // TODO :: Add other methods to this interface
}