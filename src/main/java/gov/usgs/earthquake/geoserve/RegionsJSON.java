package gov.usgs.earthquake.geoserve;


import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import gov.usgs.earthquake.qdm.Point;
import gov.usgs.earthquake.qdm.Region;
import gov.usgs.earthquake.qdm.Regions;

/**
 * RegionsJSON reads GeoJSON formatted ANSS Authoritative Regions.
 */
public class RegionsJSON {

    /**
     * Parse {@link gov.usgs.earthquake.qdm.Regions} from a GeoJSON feature collection.
     *
     * @param json geojson feature collection.
     */
    public Regions parseRegions(final JsonObject json) {
        Regions regions = new Regions();
        // NEIC is always the default
        regions.defaultNetid = "us";

        JsonArray features = json.getJsonArray("features");
        for (JsonValue value : features) {
            JsonObject jsonRegion = value.asJsonObject();            
            Region region = parseRegion(jsonRegion);
            regions.netids.add(region.netid);
            regions.regions.add(region);
        }

        return regions;
    }

    /**
     * Parse {@link gov.usgs.earthquake.qdm.Region} from a GeoJSON feature.
     *
     * @param json geojson feature.
     */
    public Region parseRegion(final JsonObject json) {
        JsonObject properties = json.getJsonObject("properties");
        String networkId = properties.getString("network");
        String regionId = properties.getString("region");

        Region region = new Region(networkId, regionId);
        JsonArray coordinates = json.getJsonObject("geometry")
                .getJsonArray("coordinates");
        for (JsonValue coord : coordinates.getJsonArray(0)) {
            JsonArray point = coord.asJsonArray();
            region.points.add(new Point(
                point.getJsonNumber(0).doubleValue(),
                point.getJsonNumber(1).doubleValue()));
        }

        return region;
    }

}