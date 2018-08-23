//EQEvent.java:  Methods to get EQ information.
//
//  12/23/2002 -- [KF]
//

package gov.usgs.earthquake.qdm;

import java.util.Date;

/**
 * Interface EQEvent methods to get EQ information.
 */
public interface EQEvent {
  /**
   * @return true if the event is authoritative
   */
  public boolean isAuthor();

  /**
   * @return the event network ID in lower case
   */
  public String getNetID();

  /**
   * Gets the event direction cosines.
   * @return 3 direction cosines or null if not calculated
   */
  public double[] getDirCos();

  /**
  * @return event ID
  */
  public String getEventID();

  /**
  * @return event ID Key
  * which is a combination of the network ID and the event ID.
  */
  public String getEventIDKey();

  /**
   * @return the event latitude
   */
  public double getLatitude();

  /**
   * @return the event longitude/latitude as a point
   */
  public Point getPoint();

  /**
   * @return the event longitude
   */
  public double getLongitude();

  /**
   * @return the event magnitude
   */
  public double getMagnitude();

  /**
   * The CUBE Magnitude type code.
   * @return the event magnitude type
   */
  public char getCUBEMagnitudeType();

  /**
   * @return the event time
   */
  public Date getTime();

  /**
   * @return true if the event should trump other events (if for example the
   * event is a verified event)
   */
  public boolean getTrump();
}
