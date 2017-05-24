/**
 * ProductKey
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.CryptoUtils;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.StringUtils;

import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This represents a public key used to verify product signatures.
 * 
 * A key should have at least one source and/or one type.
 */
public class ProductKey extends DefaultConfigurable {

	public final Logger LOGGER = Logger.getLogger(ProductKey.class.getName());

	/** Property name for sources. */
	public final String SOURCES_PROPERTY_NAME = "sources";

	/** Property name for types. */
	public final String TYPES_PROPERTY_NAME = "types";

	/** Property name for key. */
	public final String KEY_PROPERTY_NAME = "key";

	/** The source(s) used for this key. */
	private List<String> sources = new LinkedList<String>();

	/** The type(s) used for this key. */
	private List<String> types = new LinkedList<String>();

	/** The key itself. */
	private PublicKey key;

	/**
	 * Empty constructor for the Configurable interface.
	 */
	public ProductKey() {
	}

	/**
	 * Construct a new ProductPublicKey.
	 * 
	 * Sources
	 * 
	 * @param key
	 *            the public key.
	 * @param sources
	 *            the sources to use with this key.
	 * @param types
	 *            the types to use with this key.
	 */
	public ProductKey(final PublicKey key, final List<String> sources,
			final List<String> types) {
		setKey(key);
		if (sources != null) {
			getSources().addAll(sources);
		}
		if (types != null) {
			getTypes().addAll(types);
		}
	}

	/**
	 * Check whether this key is a candidate for verifying a signature.
	 * 
	 * If any sources, product source must be in list. If any types, product
	 * type must be in list.
	 * 
	 * @param id
	 *            which product to check.
	 * @return true if this key might verify the signature for given product.
	 */
	public boolean isForProduct(final ProductId id) {
		if (sources.size() == 0 || sources.contains(id.getSource())) {
			if (types.size() == 0 || types.contains(id.getType())) {
				return true;
			}
		}
		return false;
	}

	public void configure(final Config config) throws Exception {
		String key = config.getProperty(KEY_PROPERTY_NAME);
		if (key == null) {
			throw new ConfigurationException(
					"'key' is a required configuration property");
		}
		LOGGER.config("key is '" + key + "'");
		setKey(CryptoUtils.readOpenSSHPublicKey(key.getBytes()));

		String sources = config.getProperty(SOURCES_PROPERTY_NAME);
		if (sources != null) {
			LOGGER.config("key sources '" + sources + "'");
			getSources().addAll(StringUtils.split(sources, ","));
		}

		String types = config.getProperty(TYPES_PROPERTY_NAME);
		if (types != null) {
			LOGGER.config("key types '" + types + "'");
			getTypes().addAll(StringUtils.split(types, ","));
		}
	}

	public void shutdown() throws Exception {
		// Nothing to do
	}

	public void startup() throws Exception {
		// Nothing to do
	}

	/**
	 * @return the key
	 */
	public PublicKey getKey() {
		return key;
	}

	/**
	 * @param key
	 *            the key to set
	 */
	public void setKey(PublicKey key) {
		this.key = key;
	}

	/**
	 * @return the sources
	 */
	public List<String> getSources() {
		return sources;
	}

	/**
	 * @return the types
	 */
	public List<String> getTypes() {
		return types;
	}

}
