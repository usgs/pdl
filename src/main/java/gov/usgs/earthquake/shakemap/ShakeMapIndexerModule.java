package gov.usgs.earthquake.shakemap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import gov.usgs.earthquake.indexer.DefaultIndexerModule;
import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.util.StreamUtils;

/**
 * ShakeMap Indexer Module.
 *
 * Provides a higher and more specific level of support for ShakeMap products,
 * including reading additional product information out of the ShakeMap content
 * files provided with the Product and placing it into the ProductSummary for
 * the Product itself.
 */
public class ShakeMapIndexerModule extends DefaultIndexerModule {

	private static final Logger LOGGER = Logger
			.getLogger(ShakeMapIndexerModule.class.getName());

	public static final String OVERLAY_IMAGE_PATH = "download/ii_overlay.png";
	public static final String OVERLAY_WIDTH_PROPERTY = "overlayWidth";
	public static final String OVERLAY_HEIGHT_PROPERTY = "overlayHeight";

	public static final int CONTAINS_EPICENTER_WEIGHT = 50;
	public static final int CENTERED_ON_EPICENTER_WEIGHT = 25;
	// Number of degrees at which no additional weight will be
	// assigned based on the proximity of the map center to the
	// epicenter.
	public static final double MAX_DELTA_DEGREES = 2.0;

	// ShakeMap atlas is the most preferred ShakeMap contributor
	public static final String SHAKEMAP_ATLAS_SOURCE = "atlas";
	public static final int SHAKEMAP_ATLAS_WEIGHT = 200;

	@Override
	public int getSupportLevel(Product product) {
		int supportLevel = IndexerModule.LEVEL_UNSUPPORTED;
		String type = getBaseProductType(product.getId().getType());
		// Support only ShakeMap products that contain grid.xml
		if (type.equals("shakemap") && product.getContents().containsKey(
				ShakeMap.GRID_XML_ATTACHMENT))
			supportLevel = IndexerModule.LEVEL_SUPPORTED;
		return supportLevel;
	}

	@Override
	public ProductSummary getProductSummary(Product product) throws Exception {
		// Load additional properties into the ProductSummary by loading these
		// properties specifically through a ShakeMap product
		ProductSummary summary = super.getProductSummary(new ShakeMap(product));

		Content overlayImage = product.getContents().get(OVERLAY_IMAGE_PATH);
		if (overlayImage != null) {
			InputStream overlayInputStream = null;
			try {
				overlayInputStream = overlayImage.getInputStream();
				BufferedImage info = ImageIO.read(overlayInputStream);
				summary.getProperties().put(OVERLAY_WIDTH_PROPERTY,
						Integer.toString(info.getWidth()));
				summary.getProperties().put(OVERLAY_HEIGHT_PROPERTY,
						Integer.toString(info.getHeight()));
				LOGGER.finest("overlay width=" + info.getWidth() +
						", overlay height=" + info.getHeight());
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "exception reading "
						+ OVERLAY_IMAGE_PATH + " width/height", e);
			} finally {
				StreamUtils.closeStream(overlayInputStream);
			}
		}

		return summary;
	}

	@Override
	protected long getPreferredWeight(ProductSummary summary)
			throws Exception {
		// Get the default preferred weight value from the parent class
		long weight = super.getPreferredWeight(summary);

		if (SHAKEMAP_ATLAS_SOURCE.equals(summary.getSource())) {
			weight += SHAKEMAP_ATLAS_WEIGHT;
		}

		// check that shakemap has event properties and map extents
		Map<String, String> properties = summary.getProperties();
		if (summary.getEventLatitude() == null ||
				summary.getEventLongitude() == null ||
				properties.get(ShakeMap.MINIMUM_LATITUDE_PROPERTY) == null ||
				properties.get(ShakeMap.MAXIMUM_LATITUDE_PROPERTY) == null ||
				properties.get(ShakeMap.MINIMUM_LONGITUDE_PROPERTY) == null ||
				properties.get(ShakeMap.MAXIMUM_LONGITUDE_PROPERTY) == null) {
			return weight;
		}

		// Get properties for comparison to alter authoritative weight
		BigDecimal eventLat = summary.getEventLatitude();
		BigDecimal eventLon = summary.getEventLongitude();
		BigDecimal minLat = new BigDecimal(properties.get(
				ShakeMap.MINIMUM_LATITUDE_PROPERTY));
		BigDecimal maxLat = new BigDecimal(properties.get(
				ShakeMap.MAXIMUM_LATITUDE_PROPERTY));
		BigDecimal minLon = new BigDecimal(properties.get(
				ShakeMap.MINIMUM_LONGITUDE_PROPERTY));
		BigDecimal maxLon = new BigDecimal(properties.get(
				ShakeMap.MAXIMUM_LONGITUDE_PROPERTY));
		BigDecimal centerLat = minLat.add(maxLat).divide(new BigDecimal(2));
		BigDecimal centerLon = minLon.add(maxLon).divide(new BigDecimal(2));

		// Calculate delta in degrees between map center and event epicenter
		double latDelta = Math.abs(centerLat.doubleValue() - eventLat.doubleValue());
		double lonDelta = Math.abs(centerLon.doubleValue() - eventLon.doubleValue());
		double locationDelta = (double) Math.sqrt(Math.pow(latDelta, 2)
				+ Math.pow(lonDelta, 2));

		// Increase weight dynamically if the map center is within
		// MAX_DELTA_DEGREES of the event epicenter
		if (locationDelta <= MAX_DELTA_DEGREES) {
			// Add more weight based on the map center being closer to
			// the event epicenter
			weight += Math.round((1 - (locationDelta / MAX_DELTA_DEGREES))
					* CENTERED_ON_EPICENTER_WEIGHT);
		}

		// Increase weight further if the map contains the epicenter within
		// its boundaries.
		if (eventLat.longValue() < maxLat.longValue()
				&& eventLat.longValue() > minLat.longValue()
				&& eventLon.longValue() < maxLon.longValue()
				&& eventLon.longValue() > minLon.longValue()) {
			weight += CONTAINS_EPICENTER_WEIGHT;
		}

		return weight;
	}

}
