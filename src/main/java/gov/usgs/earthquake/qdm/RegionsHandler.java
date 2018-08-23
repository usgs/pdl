package gov.usgs.earthquake.qdm;

import java.util.Date;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import gov.usgs.util.SAXAdapter;
import gov.usgs.util.XmlUtils;


/**
 * XML SAX Handler for ANSS "regions.xml".
 *
 * Example:
 * <pre>{@code
 * InputStream in = ...
 * RegionsHanlder handler = new RegionsHandler();
 * try {
 *     handler.parse(in)
 * } finally {
 *     in.close();
 * }
 * return handler.regions;
 * }</pre>
 */
public class RegionsHandler extends SAXAdapter {

    public static final Logger LOGGER = Logger.getLogger(RegionsHandler.class.getName());

    // the regions that have been parsed
    public Regions regions = new Regions();
    // update timestamp
    public Date updated = null;
    // reported format version (no version-specific logic implemented)
    public String formatVersion = null;
    
    // variables for tracking state
    private boolean inRegions = false;
    private String netid = null;
    private Region region = null;
    
    @Override
    public void onStartElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
            throws SAXException {
        if ("regions".equals(localName)) {
            this.inRegions = true;
            LOGGER.finer("Parsing regions xml");
        } else if (!inRegions) {
            throw new SAXException("Expected 'regions' root element");
        } else if ("update".equals(localName)) {
            this.updated = XmlUtils.getDate(attributes.getValue("date"));
            LOGGER.finer("\tupdated " + XmlUtils.formatDate(updated));
        } else if ("format".equals(localName)) {
            this.formatVersion = attributes.getValue("version");
            LOGGER.finer("\tversion " + formatVersion);
        } else if ("net".equals(localName)) {
            if (this.region != null) {
                throw new SAXException("Unexpected 'net' element inside 'region'");
            }
            this.netid = attributes.getValue("code");
            LOGGER.finer("\tnetid=" + netid);
        } else if ("region".equals(localName)) {
            if (this.region != null) {
                throw new SAXException("Unexpected 'region' element inside 'region'");
            }
            String regionid = attributes.getValue("code");
            this.region = new Region(netid, regionid);
            LOGGER.finer("\t\tregionid=" + regionid);
        } else if ("coordinate".equals(localName)) {
            if (region == null) {
                throw new SAXException("Unexpected 'coordinate' element outside 'region'");
            }
            Double latitude = Double.valueOf(attributes.getValue("latitude"));
            Double longitude = Double.valueOf(attributes.getValue("longitude"));
            this.region.points.add(new Point(longitude, latitude));
            LOGGER.finer("\t\t\tcoordinate = " + latitude + ", " + longitude);
        }
    }

    @Override
    public void onEndElement(final String uri, final String localName,
            final String qName, final String content) throws SAXException {
        if ("region".equals(localName)) {
            if (this.region == null) {
                throw new SAXException("Unexpected closing 'region' element");
            }
            regions.regions.add(this.region);
            if (this.region.points.size() == 0) {
                regions.defaultNetid = this.region.netid;
            }
            this.region = null;
        } else if ("regions".equals(localName)) {
            this.inRegions = false;
        }
    }

}