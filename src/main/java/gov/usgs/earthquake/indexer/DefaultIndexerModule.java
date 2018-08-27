/*
 * DefaultIndexerModule
 */
package gov.usgs.earthquake.indexer;

import gov.usgs.earthquake.distribution.SignatureVerifier;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.qdm.RegionsHandler;
import gov.usgs.earthquake.qdm.Point;
import gov.usgs.earthquake.qdm.Regions;
import gov.usgs.util.StreamUtils;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.logging.Logger;


/**
 * Default implementation of the IndexerModule interface,
 * implements ANSS Authoritative Region logic.
 * 
 * Provides a basic level of support for any type of product. Creates a
 * ProductSummary using the ProductSummary(product) constructor, which copies
 * all properties, and links from the product.
 */
public class DefaultIndexerModule implements IndexerModule {

	private static final Logger LOGGER = Logger
			.getLogger(DefaultIndexerModule.class.getName());

	public static final String REGIONS_XML = "etc/config/regions.xml";

	/** Initial preferred weight. */
	public static final long DEFAULT_PREFERRED_WEIGHT = 1;

	/** Weight added when product source is same as event source. */
	public static final long SAME_SOURCE_WEIGHT = 5;

	/** Weight added when product author is in its authoritative region. */
	public static final long AUTHORITATIVE_WEIGHT = 100;

	/** Weight added when product refers to an authoritative event. */
	public static final long AUTHORITATIVE_EVENT_WEIGHT = 50;

	/** ANSS Authoritative Regions. */
	public static Regions REGIONS = null;
	static {

		InputStream in;
		try {
			// first try loading out of the jar (etc/regions.xml)
			URL regionsXmlURL = DefaultIndexerModule.class.getClassLoader()
					.getResource(REGIONS_XML);
			in = StreamUtils.getInputStream(regionsXmlURL);
			try {
				RegionsHandler regionsHandler = new RegionsHandler();
				regionsHandler.parse(in);
				REGIONS = regionsHandler.regions;
			} finally {
				StreamUtils.closeStream(in);
			}
		} catch (Exception e) {
			try {
				// now try the file system (etc/regions.xml)
				in = StreamUtils.getInputStream(new File(REGIONS_XML));
				try {
					RegionsHandler regionsHandler = new RegionsHandler();
					regionsHandler.parse(in);
					REGIONS = regionsHandler.regions;
				} finally {
					StreamUtils.closeStream(in);
				}
			} catch (Exception e2) {
				// now try regions.xml
				REGIONS = new Regions();
			}
		}
	}

	/** Signature verifier, configured by indexer. */
	private SignatureVerifier signatureVerifier = new SignatureVerifier();

	/**
	 * Create a ProductSummary from a Product.
	 * 
	 * Uses the ProductSummary(Product) constructor, which copies product
	 * information. Checks whether product is within its authoritative region,
	 * and if so boosts preferredWeight by AUTHORITATIVE_WEIGHT.
	 * 
	 * @param product
	 *            the product to summarize.
	 * @return ProductSummary for Product object.
	 */
	public ProductSummary getProductSummary(final Product product)
			throws Exception {
		ProductSummary summary = new ProductSummary(product);

		// allow sender to assign preferredWeight if we add them to the keychain
		String preferredWeight = product.getProperties().get("preferredWeight");
		if (preferredWeight != null
				&& signatureVerifier.verifySignature(product)) {
			LOGGER.fine("Signature verified, using sender assigned preferredWeight "
					+ preferredWeight);
			summary.setPreferredWeight(Long.valueOf(preferredWeight));
		} else {
			summary.setPreferredWeight(getPreferredWeight(summary));
		}
		return summary;
	}

	/**
	 * Calculate the preferred weight for a product summary.
	 * 
	 * This method is called after creating a product summary, but before
	 * returning the created summary. It's return value is used to assign the
	 * product summary preferred weight.
	 * 
	 * Within each type of product, the summary with the largest preferred
	 * weight is considered preferred.
	 * 
	 * @param summary
	 *            the summary to calculate a preferred weight.
	 * @return the absolute preferred weight.
	 */
	protected long getPreferredWeight(final ProductSummary summary) {
		long preferredWeight = DEFAULT_PREFERRED_WEIGHT;

		String source = summary.getId().getSource();
		String eventSource = summary.getEventSource();
		BigDecimal latitude = summary.getEventLatitude();
		BigDecimal longitude = summary.getEventLongitude();
		Point location = null;
		if (latitude != null && longitude != null) {
			location = new Point(longitude.doubleValue(),
					latitude.doubleValue());
		}

		// authoritative check
		if (location != null) {
			if (REGIONS.isAuthor(source, location)) {
				// based on product source, who authored this product.
				preferredWeight += AUTHORITATIVE_WEIGHT;
			}
			if (eventSource != null) {
				if (REGIONS.isAuthor(eventSource, location)) {
					// based on event source, which event this product is about
					preferredWeight += AUTHORITATIVE_EVENT_WEIGHT;
				}
			}
		}

		if (eventSource != null) {
			// same source check
			if (eventSource.equalsIgnoreCase(source)) {
				preferredWeight += SAME_SOURCE_WEIGHT;
			}
		}

		return preferredWeight;
	}

	/**
	 * Remove "internal-" prefix and "-scenario" suffix from product type".
	 *
	 * @param type
	 *        product type.
	 * @return base product type (without any known prefix or suffix).
	 */
	public String getBaseProductType(String type) {
		if (type.startsWith("internal-")) {
			type = type.replace("internal-", "");
		}

		if (type.endsWith("-scenario")) {
			type = type.replace("-scenario", "");
		}

		return type;
	}

	/**
	 * This module provides a default level of support for any type of product.
	 * 
	 * @param product
	 *            the product to test.
	 * @return IndexerModule.LEVEL_DEFAULT.
	 */
	public int getSupportLevel(final Product product) {
		return IndexerModule.LEVEL_DEFAULT;
	}

	public SignatureVerifier getSignatureVerifier() {
		return signatureVerifier;
	}

	public void setSignatureVerifier(SignatureVerifier signatureVerifier) {
		this.signatureVerifier = signatureVerifier;
	}

}
