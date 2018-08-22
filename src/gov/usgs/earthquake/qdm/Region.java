package gov.usgs.earthquake.qdm;

import java.util.ArrayList;

/**
 * Has a list of coordinates as Points.
 * 
 * Points are assumed to use x=longitude, y=latitude.
 * A "default" region has no boundary points, and contains all points.
 */
public class Region {

  public String netid;
  public String regionid;

  public ArrayList<Point> points;

  public Region(String netid, String regionid) {
    this.netid = netid;
    this.regionid = regionid;
    this.points = new ArrayList<Point>();
  }

  /**
   * Method to determine if this lat-lon in this region? In or out algorithm taken
   * from an algorithm by Edwards and Coleman of Oak Ridge Lab, version for BNL by
   * Benkovitz translated to C by Andy Michael and into Java by Alan Jones.
   */
  public boolean inpoly(Point xy) {
    int in;
    double sine;
    boolean bool = false;
    int inside = 0;
    int nvert = this.points.size();
    // If there are no points in the region, assume default region
    // and declare the point inside
    if (nvert == 0)
      return true;
    Point p[] = (Point[]) this.points.toArray(new Point[0]);
    double x = xy.x;
    double y = xy.y;
    for (int i = 0; i < nvert; ++i) {
      in = i + 1;
      if (in >= nvert)
        in = 0;
      if (p[in].y == p[i].y && p[in].x == p[i].x)
        continue;
      sine = (x - p[i].x) * (p[in].y - p[i].y) - (y - p[i].y) * (p[in].x - p[i].x);
      if (sine == 0) {
        if (((x - p[i].x) * (p[in].x - p[i].x) + (y - p[i].y) * (p[in].y - p[i].y))
            * ((x - p[in].x) * (p[in].x - p[i].x) + (y - p[in].y) * (p[in].y - p[i].y)) > 0)
          continue;
        return true;
      }
      if (y > p[in].y && y <= p[i].y && sine < 0 || y <= p[in].y && y > p[i].y && sine > 0) {
        bool = !bool;
      }
    }
    if (bool)
      inside = 1;
    if (inside != 0)
      return true;
    else
      return false;
  }

}
