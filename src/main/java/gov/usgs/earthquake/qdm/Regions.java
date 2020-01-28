package gov.usgs.earthquake.qdm;

import java.util.ArrayList;

/**
 * Set of regions.
 *
 * @author - Alan Jones, 1999.09.10 <br>
 *         2002.01.16: Convert from "regions" directory with above files to
 *         regions.xml file. <br>
 *         2003.01.06: Make class public, add support for non-file input so that
 *         we can read from a JAR, add isDefaultNetID method to check for the
 *         default network (US), add isAuthor method to check if the event is
 *         authoritative
 *
 *         2018-08-21: JMF, reimplement parsing logic outside class.
 *         Update code to Java 1.8+.
 */
public class Regions {

    public String defaultNetid; // Default network
    public ArrayList<String> netids; // Array of network ids, e.g. nc, us, etc.
    public ArrayList<Region> regions; // Array of regions

    /**
     * Create a new set of regions.
     */
    public Regions() {
        this.defaultNetid = "";
        this.netids = new ArrayList<String>();
        this.regions = new ArrayList<Region>();
    }

    /**
     * Is this netid in the set of regions? The default net covers the whole world
     * so it is always valid since it has no finite boundaries.
     */
    public boolean isValidnetID(final String netid) {
        return this.netids.contains(netid);
    }

    //
    // [KF] - add methods to check for the default network (US)
    //

    /**
     * Checks if network ID is the default network.
     *
     * @param netid network ID
     * @return true if default network.
     */
    public boolean isDefaultNetID(final String netid) {
        return this.defaultNetid.equalsIgnoreCase(netid);
    }

    /**
     * Checks if an event's network ID is the default network.
     *
     * @param eq EQ event
     * @return true if default network.
     */
    public boolean isDefaultNetID(final EQEvent eq) {
        return this.isDefaultNetID(eq.getNetID());
    }

    //
    // [KF] - add methods to determine if the event is from the authoritative
    // network.
    //

    /**
     * Determines if the event is from the authoritative network.
     *
     * @param netid network ID
     * @param p     event point
     * @return true if event is authoritative
     */
    public boolean isAuthor(final String netid, final Point p) {
        if (this.isDefaultNetID(netid)) {
            // if any non-default regions match, default is not authoritative
            for (Region region : this.regions) {
                if (netid.equalsIgnoreCase(region.netid)) {
                    continue;
                }
                if (region.inpoly(p)) {
                    // another region is authoritative
                    return false;
                }
            }
            // no other regions authoritative
            return true;
        } else {
            // if any network regions match, network is authoritative
            for (Region region : regions) {
                if (netid.equalsIgnoreCase(region.netid) && region.inpoly(p)) {
                    // network is authoritative
                    return true;
                }
            }
            // network is not authoritative
            return false;
        }
    }

    /**
     * Determines if the event is from the authoritative network.
     *
     * @param eq EQ event
     * @return true if event is authoritative
     */
    public boolean isAuthor(final EQEvent eq) {
        return this.isAuthor(eq.getNetID(), eq.getPoint());
    }

}
