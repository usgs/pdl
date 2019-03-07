package gov.usgs.earthquake.geoserve;

import java.io.File;
import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.usgs.earthquake.qdm.Regions;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;

public class ANSSRegionsFactoryTest {

    private File localRegions;
    private JsonObject serviceData;

    @Before
    public void before() {
        localRegions = new File(ANSSRegionsFactory.DEFAULT_REGIONS_JSON);
        try (InputStream in = StreamUtils.getInputStream(new File("etc/config/example_regions.json"))) {
            serviceData = Json.createReader(in).readObject();
        } catch (Exception e) {
            serviceData = null;
            Assert.fail("unable to load test data");
        }
    }

    @After
    public void after() {
        localRegions = null;
        serviceData = null;
    }

    /**
     * Test that factory reads from local cache when unable to access from service.
     */
    @Test
    public void testFetchCache() throws Exception {
        ANSSRegionsFactory factory;
        Regions regions;

        // make sure local regions exist
        FileUtils.writeFile(localRegions, serviceData.toString().getBytes());
        // factory that is unable to access data from service
        factory = new ANSSRegionsFactory(new TestLayersService(null));
        regions = factory.getRegions();
        Assert.assertEquals("regions are null before calling fetchRegions", null, regions);
        // fetch "data" from service
        factory.fetchRegions();
        regions = factory.getRegions();
        Assert.assertNotNull("Got regions from local cache", regions);
        Assert.assertEquals("Default NetID is 'us'", "us", regions.defaultNetid);
    }

    /**
     * Test factory when it cannot read from service or cache.
     */
    @Test
    public void testFetchNone() throws Exception {
        ANSSRegionsFactory factory;
        Regions regions;

        // make sure local regions do not exist
        localRegions.delete();
        // factory that is unable to access data from service
        factory = new ANSSRegionsFactory(new TestLayersService(null));
        factory.fetchRegions();
        regions = factory.getRegions();
        Assert.assertNull("regions are still null after fetch", regions);
    }

    /**
     * Test that factory reads from service, and generates local cache.
     */
    @Test
    public void testFetchService() throws Exception {
        ANSSRegionsFactory factory;
        Regions regions;

        // factory that is able to access data from service
        factory = new ANSSRegionsFactory(new TestLayersService(serviceData));
        regions = factory.getRegions();
        Assert.assertEquals("regions are null before calling fetchRegions", null, regions);
        // remove any local regions
        localRegions.delete();
        Assert.assertFalse("local regions should not exist", localRegions.exists());
        // fetch "data" from service
        factory.fetchRegions();
        regions = factory.getRegions();
        Assert.assertNotNull("Got regions from service", regions);
        Assert.assertEquals("Default NetID is 'us'", "us", regions.defaultNetid);
        // wrote data to local file
        Assert.assertTrue("local regions should exist after successful fetch", localRegions.exists());
    }

    /**
     * Test startup/shutdown method.
     */
    @Test
    public void testStartupShutdown() throws Exception {
        ANSSRegionsFactory factory;
        Regions regions;

        factory = new ANSSRegionsFactory(new TestLayersService(serviceData));
        regions = factory.getRegions();
        Assert.assertNull("regions are null before calling fetchRegions", regions);
        factory.startup();
        regions = factory.getRegions();
        Assert.assertNotNull("regions are not null after calling fetchRegions", regions);
        factory.shutdown();
    }

    /**
     * Testing service that returns a specific value when getLayer is called.
     */
    private static class TestLayersService extends GeoserveLayersService {

        public JsonObject getLayersValue;

        public TestLayersService(final JsonObject getLayersValue) {
            this.getLayersValue = getLayersValue;
        }

        @Override
        public JsonObject getLayer(final String type) {
            return getLayersValue;
        }
    }

}