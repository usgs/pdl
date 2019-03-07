package gov.usgs.earthquake.geoserve;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
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
 * Regions are not fetched until {@link #startup()}
 * (or {@link #fetchRegions()}) is called.
 */
public class ANSSRegionsFactory {

    // logging object
    public static final Logger LOGGER = Logger.getLogger(ANSSRegionsFactory.class.getName());

    // milliseconds per day
    public static final long MILLISECONDS_PER_DAY = 86400000L;

    // path to write regions.json
    public static final String DEFAULT_REGIONS_JSON = "regions.json";

    // global factory object
    private static ANSSRegionsFactory SINGLETON;


    // service used to load regions
    private GeoserveLayersService geoserveLayersService;

    // path to local regions file
    private File localRegions = new File(DEFAULT_REGIONS_JSON);

    // the current regions object
    private Regions regions;

    // shutdown hook registered by startup
    private Thread shutdownHook;

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
    public static synchronized ANSSRegionsFactory getFactory() {
        return getFactory(true);
    }

    public static synchronized ANSSRegionsFactory getFactory(final boolean startup) {
        if (SINGLETON == null) {
            SINGLETON = new ANSSRegionsFactory();
            if (startup) {
                SINGLETON.startup();
            }
        }
        return SINGLETON;
    }

    /**
     * Set the global ANSSRegionsFactory,
     * shutting down any existing factory if needed.
     */
    public static synchronized void setFactory(final ANSSRegionsFactory factory) {
        if (SINGLETON != null) {
            SINGLETON.shutdown();
        }
        SINGLETON = factory;
    }

    /**
     * Download regions from geoserve.
     *
     * Writes out to "regions.json" in current working directory and,
     * if unable to update, reads in local copy.
     */
    public void fetchRegions () {
        try {
            // try loading from geoserve
            this.regions = loadFromGeoserve();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Error fetching ANSS Regions from geoserve",
                    e);
            try {
                if (this.regions == null) {
                    // fall back to local cache
                    this.regions = loadFromFile();
                }
            } catch (Exception e2) {
                LOGGER.log(Level.WARNING,
                        "Error fetching ANSS Regions from local file",
                        e);
            }
        }
    }

    /**
     * Read regions from local regions file.
     */
    protected Regions loadFromFile() throws IOException {
        try (InputStream in = StreamUtils.getInputStream(this.localRegions)) {
            JsonObject json = Json.createReader(in).readObject();
            Regions regions = new RegionsJSON().parseRegions(json);
            // regions loaded
            LOGGER.fine("Loaded ANSS Authoritative Regions from "
                    + this.localRegions
                    + ", last modified=" + XmlUtils.formatDate(
                            new Date(this.localRegions.lastModified())));
            return regions;
        }
    }

    /**
     * Read regions from geoserve service.
     */
    protected Regions loadFromGeoserve() throws IOException {
        LOGGER.fine("Fetching ANSS Authoritative Regions from Geoserve");
        JsonObject json = this.geoserveLayersService.getLayer("anss");
        Regions regions = new RegionsJSON().parseRegions(json);
        LOGGER.finer("Loaded ANSS Authoritative Regions from Geoserve");
        try {
            saveToFile(this.localRegions, json);
        } catch (IOException e) {
            // log for now, since saving is value added
            LOGGER.log(Level.INFO, "Error saving local regions", e);
        }
        return regions;
    }

    /**
     * Store json to local regions file.
     *
     * @param json json response to store locally.
     */
    protected void saveToFile(final File regionsFile, final JsonObject json) throws IOException {
        LOGGER.fine("Storing ANSS Authoritative Regions to " + regionsFile);
        // save regions if needed later
        FileUtils.writeFileThenMove(
                new File(regionsFile.toString() + ".temp"),
                regionsFile,
                json.toString().getBytes());
        LOGGER.finer("Stored ANSS Regions to " + regionsFile);
    }

    /**
     * Start updating regions.
     */
    public void startup() {
        if (this.shutdownHook != null) {
            // already started
            return;
        }

        // do initial fetch
        fetchRegions();

        // schedule periodic fetch
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

        // register shutdown hook
        this.shutdownHook = new Thread(() -> {
            // stop periodic fetch
            this.updateTimer.cancel();
        });
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }

    /**
     * Stop updating regions.
     */
    public void shutdown() {
        if (this.shutdownHook == null) {
            // not started or already stopped
            return;
        }

        // stop periodic fetch
        this.updateTimer.cancel();

        // remove shutdown hook
        Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        this.shutdownHook = null;
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
     * Get the local regions file.
     */
    public File getLocalRegions() {
        return this.localRegions;
    }

    /**
     * Set the local regions file.
     */
    public void setLocalRegions(final File localRegions) {
        this.localRegions = localRegions;
    }

    /**
     * Get the most recently fetched Regions.
     */
    public Regions getRegions () {
        return this.regions;
    }

}
