package gov.usgs.earthquake.geoserve;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;

import gov.usgs.earthquake.geoserve.GeoserveLayersService;
import gov.usgs.earthquake.qdm.Regions;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

/**
 * Class to manage ANSS Authoritative Region updates.
 *
 * Simplest usage:
 *     ANSSRegionsFactory.getFactory().getRegions()
 * 
 * Regions are not fetched until is called.
 */
public class ANSSRegionsFactory {

    // logging object
    public static final Logger LOGGER = Logger.getLogger(ANSSRegionsFactory.class.getName());

    // milliseconds per day
    public static final long MILLISECONDS_PER_DAY = 86400000L;

    // path to write regions.json
    public static final String REGIONS_JSON = "regions.json";

    // global factory object
    private static ANSSRegionsFactory SINGLETON;

    // service used to load regions
    private GeoserveLayersService geoserveLayersService;

    // the current regions object.
    private Regions regions;

    // timer used to auto fetch region updates
    private Timer updateTimer = new Timer();


    /**
     * Use default GeoserveLayersService.
     */
    public ANSSRegionsFactory () {
        this(new GeoserveLayersService());
    }

    /**
     * Use custom GeoserveLayersService.
     */
    public ANSSRegionsFactory (final GeoserveLayersService geoserveLayersService) {
        this.geoserveLayersService = geoserveLayersService;
    }

    /**
     * Get the global ANSSRegionsFactory, 
     * creating and starting if needed.
     */
    public static ANSSRegionsFactory getFactory() {
        if (SINGLETON == null) {
            SINGLETON = new ANSSRegionsFactory();
            SINGLETON.startup();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                SINGLETON.shutdown();
            }));
        }
        return SINGLETON;
    }

    /**
     * Set the global ANSSRegionsFactory,
     * shutting down any existing factory if needed.
     */
    public static void setFactory(final ANSSRegionsFactory factory) {
        if (SINGLETON != null) {
            SINGLETON.shutdown();
        }
        SINGLETON = factory;
    }

    /**
     * Get the service.
     */
    public GeoserveLayersService getGeoserveLayersService() {
        return this.geoserveLayersService;
    }

    /**
     * Set the service.
     */
    public void setGeoserveLayersService(final GeoserveLayersService service) {
        this.geoserveLayersService = service;
    }

    /**
     * Get the most recently fetched Regions.
     */
    public Regions getRegions () {
        return regions;
    }

    /**
     * Download regions from geoserve.
     *
     * Writes out to "regions.json" in current working directory and,
     * if unable to update, reads in local copy.
     */
    public void fetchRegions () {
        LOGGER.info("Fetching ANSS Authoritative Regions from Geoserve");
        File regionsJson = new File(REGIONS_JSON);

        // try loading from geoserve
        try {
            JsonObject json = this.geoserveLayersService.getLayer("anss");
            this.regions = new RegionsJSON().parseRegions(json);
            LOGGER.info("Loaded ANSS Authoritative Regions from Geoserve");
            // save regions if needed later
            FileUtils.writeFileThenMove(
                    new File(REGIONS_JSON + ".temp"),
                    regionsJson,
                    json.toString().getBytes());
            LOGGER.finer("Storing ANSS Authoritative Regions to " + REGIONS_JSON);
            // everything worked
            return;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating ANSS Authoritative Regions", e);
        }

        // maybe network error, does local json file exist?
        if (regionsJson.exists()) {
            LOGGER.finer("Loading ANSS Authoritative Regions from " + REGIONS_JSON);
            try (InputStream in = StreamUtils.getInputStream(regionsJson)) {
                JsonObject json = Json.createParser(in).getObject();
                this.regions = new RegionsJSON().parseRegions(json);
                // regions loaded
                LOGGER.fine("Loaded ANSS Authoritative Regions from " + REGIONS_JSON +
                        ", last modified=" + XmlUtils.formatDate(new Date(regionsJson.lastModified())));
                return;
            } catch (Exception e) {
                LOGGER.warning("Error loading ANSS Authoritative Regions from " + REGIONS_JSON);
            }
        }
    }

    /**
     * Start updating regions.
     */
    public void startup() {
        // do initial fetch
        fetchRegions();

        long now = new Date().getTime();
        long nextMidnight = MILLISECONDS_PER_DAY - (now % MILLISECONDS_PER_DAY);
        updateTimer.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        fetchRegions();
                    }
                },
                // firstt time at midnight
                nextMidnight,
                // once per day
                MILLISECONDS_PER_DAY);
    }

    /**
     * Stop updating regions.
     */
    public void shutdown() {
        updateTimer.cancel();
    }

}
