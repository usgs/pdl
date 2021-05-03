/*
 * ProductPublicKey
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.Config;
import gov.usgs.util.StringUtils;

import java.security.PublicKey;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * A group of keys that can be used to verify product signatures.
 */
public class ProductKeyChain {

	/** Logging object. */
	private static final Logger LOGGER = Logger.getLogger(ProductKeyChain.class
			.getName());

	/** List of candidate keys. */
	private List<ProductKey> keychain = new LinkedList<ProductKey>();

	/** Empty constructor */
	public ProductKeyChain() {
	}

	/**
	 * Constructor for a string of keys
	 * @param keys String of keys, separated by commas
	 * @param config Config file
	 * @throws Exception if error occurs
	 */
	public ProductKeyChain(final String keys, final Config config)
			throws Exception {
		this(StringUtils.split(keys, ","), config);
	}

	/**
	 * Constructor for list of keys
	 * @param keys String list of keys
	 * @param config Config file
	 * @throws Exception if error occurs
	 */
	public ProductKeyChain(final List<String> keys, final Config config)
			throws Exception {
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String keyName = iter.next();
			LOGGER.config("Loading key '" + keyName + "'");
			ProductKey key = (ProductKey) Config.getConfig().getObject(keyName);
			if (key != null) {
				keychain.add(key);
			}
		}
	}

	/**
	 * @return the keys
	 */
	public List<ProductKey> getKeychain() {
		return keychain;
	}

	/**
	 * Find public keys based on configured Keys.
	 *
	 * @param id ID of product
	 * @return an array of candidate keys used to verify a signature.
	 */
	public PublicKey[] getProductKeys(final ProductId id) {
		LinkedList<PublicKey> publicKeys = new LinkedList<PublicKey>();
		Iterator<ProductKey> iter = keychain.iterator();
		while (iter.hasNext()) {
			ProductKey key = iter.next();
			if (key.isForProduct(id)) {
				publicKeys.add(key.getKey());
			}
		}
		return publicKeys.toArray(new PublicKey[0]);
	}

}
