package gov.usgs.earthquake.geoserve;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;

import gov.usgs.util.StreamUtils;

/**
 * Access layers from the Geoserve Layers service.
 */
public class GeoserveLayersService {

    /** Default URL for GeoServe Layers service. */
    public static final String DEFAULT_GEOSERVE_LAYERS_URL = "https://earthquake.usgs.gov/ws/geoserve/layers.json?type={type}";

    /** Configured URL for GeoServe Layers service. */
    private String endpointUrl;


    /**
     * Create a service using the default URL.
     */
    public GeoserveLayersService() {
        this(DEFAULT_GEOSERVE_LAYERS_URL);
    }

    /**
     * Create a service using a custom URL.
     * 
     * @param endpointUrl layers service URL.
     *       Should contain the string <code>{type}</code>,
     *       which is replaced during the #{@link #getLayer(String)}.
     */
    public GeoserveLayersService(final String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    /**
     * Get the endpoint URL.
     */
    public String getEndpointURL() {
        return this.endpointUrl;
    }

    /**
     * Set the endpoint URL.
     */
    public void setEndpointURL(final String url) {
        this.endpointUrl = url;
    }

    /**
     * Fetch and parse a JSON response from the Geoserve layers service.
     */
    public JsonObject getLayer(final String type) throws IOException, MalformedURLException {
        final URL url = new URL(endpointUrl.replace("{type}", type));
        try (InputStream in = StreamUtils.getInputStream(url)) {
            JsonObject json = Json.createReader(in).readObject();
            return json.getJsonObject(type);
        }
    }

}
