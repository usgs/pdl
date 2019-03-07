package gov.usgs.earthquake.geoserve;

import java.util.Date;

import gov.usgs.earthquake.qdm.Point;
import gov.usgs.earthquake.qdm.Region;
import gov.usgs.earthquake.qdm.Regions;
import gov.usgs.util.XmlUtils;

/**
 * Custom formatting for {@link gov.usgs.earthquake.qdm.Regions()}.
 */
public class RegionsKML {

    /**
     * Output ANSS Authoritative Regions in KML format.
     */
    public String formatKML(final Regions regions) {
        StringBuffer kml = new StringBuffer(String.join("\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">",
            "<Document id=\"regions_xml\">",
            "  <name>ANSS Authoritative Regions</name>",
            "  <description>"
                    + XmlUtils.formatDate(new Date())
                    + "</description>",
            "  <open>1</open>",
            "  <Style id=\"RegionStyle\">",
            "    <LabelStyle>",
            "      <color>00000000</color>",
            "      <scale>0.000000</scale>",
            "    </LabelStyle>",
            "    <LineStyle>",
            "      <color>ff0000ff</color>",
            "      <width>2.000000</width>",
            "    </LineStyle>",
            "    <PolyStyle>",
            "      <color>00ffffff</color>",
            "      <outline>1</outline>",
            "    </PolyStyle>",
            "  </Style>",
            ""
        ));

        for (final Region region : regions.regions) {
            kml.append("\n<Placemark id=\"region_").append(region.regionid).append("\">\n");
            kml.append("<name>").append(region.regionid).append("</name>\n");
            kml.append("<styleUrl>#RegionStyle</styleUrl>\n");
            kml.append("<MultiGeometry><Polygon><outerBoundaryIs><LinearRing><coordinates>\n");
            for (final Point point : region.points) {
                kml.append("  ").append(point.x).append(",").append(point.y).append("\n");
            }
            kml.append("</coordinates></LinearRing></outerBoundaryIs></Polygon></MultiGeometry>\n");
            kml.append("</Placemark>\n");
        }

        kml.append(String.join("\n",
            "",
            "</Document>",
            "</kml>",
            ""
        ));

        return kml.toString();
    }

    /**
     * Download ANSS Authoritative Regions,
     * and print to console in Regions KML format.
     */
    public static void main(final String[] args) throws Exception {
        Regions regions = ANSSRegionsFactory.getFactory().getRegions();
        String kml = new RegionsKML().formatKML(regions);
        System.out.println(kml);
    }

}