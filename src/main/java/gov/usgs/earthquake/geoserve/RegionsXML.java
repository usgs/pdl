package gov.usgs.earthquake.geoserve;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeSet;

import gov.usgs.earthquake.qdm.Point;
import gov.usgs.earthquake.qdm.Region;
import gov.usgs.earthquake.qdm.Regions;
import gov.usgs.earthquake.qdm.RegionsHandler;
import gov.usgs.util.XmlUtils;

/**
 * Legacy Regions XML formatting for gov.usgs.earthquake.qdm.Regions.
 *
 */
public class RegionsXML {

    /**
     * Output ANSS Authoritative Regions in the legacy Regions XML format.
     */
    public String formatXML(final Regions regions) {
        StringBuffer xml = new StringBuffer(String.join("\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<regions>",
            "  <file name=\"regions.xml\">",
            "  <update date=\"" + XmlUtils.formatDate(new Date()) + "\"/>",
            "  <format version=\"0.2\"/>",
            "\n<!--",
            "# ANSS Authoritative Regions",
            "# DEFAULT indicates no polygon - matches any event.",
            "# A network may have more than 1 region list,",
            "# but each region may be associated with only ONE network.",
            "# There may only be 1 DEFAULT region.",
            "-->\n"
        ));

        // group regions by netid
        final HashMap<String, ArrayList<Region>> networks = new HashMap<>();
        for (final Region region : regions.regions) {
            String netid = region.netid;
            ArrayList<Region> netRegions = networks.get(netid);
            if (netRegions == null) {
                netRegions = new ArrayList<Region>();
                networks.put(netid, netRegions);
            }
            netRegions.add(region);
        }

        // output networks
        for (String netid : new TreeSet<>(networks.keySet())) {
            xml.append("\n<net")
                    .append(" code=\"").append(netid).append("\"")
                    // name is "DEFAULT" or not, use netid since it's unique
                    .append(" name=\"").append(netid).append("\"")
                    .append(">\n");
            // output network regions
            for (Region region : networks.get(netid)) {
                xml.append("  <region code=\"").append(region.regionid).append("\">\n");
                for (Point point : region.points) {
                    xml.append("    <coordinate")
                            .append(" latitude=\"").append(point.y).append("\"")
                            .append(" longitude=\"").append(point.x).append("\"")
                            .append("/>\n");
                }
                xml.append("  </region>\n");
            }
            xml.append("</net>\n");
        }

        // add NEIC as default region
        xml.append(String.join("\n",
                "",
                "<net code=\"US\" name=\"DEFAULT\">",
                "  <region code=\"US\"/>",
                "</net>",
                ""));

        xml.append("</regions>");

        return xml.toString();
    }

    /**
     * Parse regions from an XML input stream.
     *
     * @throws Exception
     */
    public static Regions getRegions(final InputStream in) throws Exception {
        RegionsHandler regionsHandler = new RegionsHandler();
        Exception error = regionsHandler.parse(in);
        if (error != null) {
            throw error;
        }
		return regionsHandler.regions;
    }

    /**
     * Download ANSS Authoritative regions from Geoserve,
     * and print to the screen in Regions XML format.
     */
    public static void main(final String[] args) throws Exception {
        Regions regions = ANSSRegionsFactory.getFactory().getRegions();
        String xml = new RegionsXML().formatXML(regions);
        System.out.println(xml);
    }

}