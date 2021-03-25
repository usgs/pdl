package gov.usgs.earthquake.qdm;

/**
 * A 2-D point class.
 *
 * <br>2003.01.06: Make class public, use double instead of float.
 */
public class Point {

  /** A double for x position */
  public double x;
  /** A double for y position */
  public double y;

  /**
   * Constructor
   * @param x double x pos
   * @param y double y pos
   */
  public Point(double x, double y) {
    this.x = x;
    this.y = y;
  }

}
