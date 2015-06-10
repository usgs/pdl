package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;

import java.io.File;
import java.security.MessageDigest;
import java.util.logging.Logger;

/**
 * A FileProductStorage that builds directory paths based on a SHA-1 hash of the
 * product id.
 *
 * This helps overcome a limitation of the ext3 filesystem which limits the
 * number of subdirectories any one directory may contain to 32000. This
 * implementation should generate no more than 4096 (16 ^ 3) subdirectories of
 * any one subdirectory.
 *
 * Note: no collision handling has been implemented, although hash collisions
 * are not expected.
 *
 * Examples: <br/>
 * Product ID: urn:usgs-product:us:shakemap:abcd1234:1304116272636 <br/>
 * SHA-1 hash: dde7b3986ee2fda8a793b599b6ae725ab35df58b <br/>
 * Directory: shakemap/dde/7b3/986/ee2/fda/8a7/93b/599/b6a/e72/5ab/35d/f58/b <br/>
 * <br/>
 * Product ID: urn:usgs-product:us:shakemap2:efgh5678:1304116272711 <br/>
 * SHA-1 hash: 8174d0f8d961d48c8a94a6bd0ab2a882e01173c6 <br/>
 * Directory: shakemap2/817/4d0/f8d/961/d48/c8a/94a/6bd/0ab/2a8/82e/011/73c/6
 *
 * @deprecated
 * @see FileProductStorage
 */
public class HashFileProductStorage extends FileProductStorage {

	private static Logger LOGGER = Logger
			.getLogger(HashFileProductStorage.class.getName());

	// create this digest once, and clone it later
	private static final MessageDigest SHA_DIGEST;
	static {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA");
		} catch (Exception e) {
			LOGGER.warning("Unable to create SHA Digest for HashFileProductStorage");
			digest = null;
		}
		SHA_DIGEST = digest;
	}

	/**
	 * This is chosen because 16^3 = 4096 &lt; 32000, which is the ext3
	 * subdirectory limit.
	 */
	public static final int DIRECTORY_NAME_LENGTH = 3;

	public HashFileProductStorage() {
		super();
	}

	public HashFileProductStorage(final File directory) {
		super(directory);
	}

	/**
	 * A method for subclasses to override the storage path.
	 *
	 * The returned path is appended to the base directory when storing and
	 * retrieving products.
	 *
	 * @param id
	 *            the product id to convert.
	 * @return the directory used to store id.
	 */
	@Override
	public String getProductPath(final ProductId id) {
		try {
			MessageDigest digest;
			synchronized (SHA_DIGEST) {
				digest = ((MessageDigest) SHA_DIGEST.clone());
			}

			String hexDigest = toHexString(digest.digest(id.toString()
					.getBytes()));

			StringBuffer buf = new StringBuffer();
			// start with product type, to give idea of available products and
			// disk usage when looking at filesystem
			buf.append(id.getType());

			// sub directories based on hash
			int length = hexDigest.length();
			for (int i = 0; i < length; i += DIRECTORY_NAME_LENGTH) {
				String part;
				if (i + DIRECTORY_NAME_LENGTH < length) {
					part = hexDigest.substring(i, i + DIRECTORY_NAME_LENGTH);
				} else {
					part = hexDigest.substring(i);
				}
				buf.append(File.separator);
				buf.append(part);
			}

			return buf.toString();
		} catch (CloneNotSupportedException e) {
			// fall back to parent class
			return super.getProductPath(id);
		}
	}

	/**
	 * Convert an array of bytes into a hex string. The string will always be
	 * twice as long as the input byte array, because bytes < 0x10 are zero
	 * padded.
	 *
	 * @param bytes
	 *            byte array to convert to hex.
	 * @return hex string equivalent of input byte array.
	 */
	private String toHexString(final byte[] bytes) {
		StringBuffer buf = new StringBuffer();
		int length = bytes.length;
		for (int i = 0; i < length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				buf.append('0');
			}
			buf.append(hex);
		}
		return buf.toString();
	}

}
