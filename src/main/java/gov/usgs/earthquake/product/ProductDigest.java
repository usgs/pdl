/*
 * ProductDigest
 */
package gov.usgs.earthquake.product;

import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.ProductHandler;
import gov.usgs.earthquake.util.NullOutputStream;
import gov.usgs.util.CryptoUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;
import gov.usgs.util.CryptoUtils.Version;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.DigestOutputStream;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;


/**
 * Used to generate product digests.
 *
 * All product attributes and content are used when generating a digest, except
 * any existing signature, since the digest is used to generate or verify
 * signatures.
 *
 * Calls to ProductOutput methods on this class must occur in identical order to
 * generate consistent signatures. Therefore it is almost required to use the
 * ObjectProductInput, which fulfills this requirement.
 */
public class ProductDigest implements ProductHandler {

	/** Logging object. */
	private static final Logger LOGGER = Logger.getLogger(ProductDigest.class
			.getName());

	/** Character set used when computing digests. */
	public static final String CHARSET = "UTF-8";

	/** Algorithm used when generating product digest. */
	public static final String MESSAGE_DIGEST_ALGORITHM = "SHA1";

	/** v2 digest algorithm */
	public static final String MESSAGE_DIGEST_V2_ALGORITHM = "SHA-256";

	/** The stream used to compute the product digest. */
	private DigestOutputStream digestStream;

	/** The computed digest. */
	private byte[] digest = null;

	/** The signature version. */
	private Version version = null;

	/**
	* Construct a new ProductDigest.
	 */
	protected ProductDigest(final Version version) throws NoSuchAlgorithmException {
		final String algorithm = version == Version.SIGNATURE_V2
				? MESSAGE_DIGEST_V2_ALGORITHM
				: MESSAGE_DIGEST_ALGORITHM;
		LOGGER.fine("Using digest version " + version.toString()
				+ ", algorithm=" + algorithm);
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		this.digestStream = new DigestOutputStream(new NullOutputStream(), digest);
		this.version = version;
	}

	/**
	 * A convenience method that generates a product digest.
	 *
	 * @param product
	 *            the product to digest
	 * @return the computed digest.
	 * @throws Exception
	 *             if errors occur while digesting product.
	 */
	public static byte[] digestProduct(final Product product) throws Exception {
		return digestProduct(product, Version.SIGNATURE_V1);
	}

	public static byte[] digestProduct(final Product product, final Version version)
			throws Exception {
		Date start = new Date();
		ProductDigest productDigest = new ProductDigest(version);
		// ObjectProductInput generates ProductOutput calls in a reliable order.
		new ObjectProductSource(product).streamTo(productDigest);
		Date end = new Date();

		byte[] digest = productDigest.getDigest();
		LOGGER.fine("Digest='" + Base64.getEncoder().encodeToString(digest)
				+ "' , " + (end.getTime() - start.getTime()) + "ms");
		return digest;
	}

	/**
	 * @return the computed digest, or null if not finished yet.
	 */
	public byte[] getDigest() {
		return digest;
	}

	/**
	 * Digest the id, update time, status, and URL.
	 */
	public void onBeginProduct(ProductId id, String status, URL trackerURL)
			throws Exception {
		digestStream.write(id.toString().getBytes(CHARSET));
		digestStream.write(XmlUtils.formatDate(id.getUpdateTime()).getBytes(
				CHARSET));
		digestStream.write(status.getBytes(CHARSET));
		if (this.version != Version.SIGNATURE_V2 && trackerURL != null) {
			digestStream.write(trackerURL.toString().getBytes(CHARSET));
		}
	}

	/**
	 * Digest the path, content attributes, and content bytes.
	 */
	public void onContent(ProductId id, String path, Content content)
			throws Exception {
		digestStream.write(path.getBytes(CHARSET));
		digestStream.write(content.getContentType().getBytes(CHARSET));
		digestStream.write(XmlUtils.formatDate(content.getLastModified())
				.getBytes(CHARSET));
		digestStream.write(content.getLength().toString().getBytes(CHARSET));
		if (this.version == Version.SIGNATURE_V2) {
			digestStream.write(content.getSha256().getBytes(CHARSET));
		} else {
			StreamUtils.transferStream(content.getInputStream(),
					new StreamUtils.UnclosableOutputStream(digestStream));
		}
	}

	/**
	 * Finish computing digest.
	 */
	public void onEndProduct(ProductId id) throws Exception {
		// finish computing message digest.
		digestStream.flush();
		digest = digestStream.getMessageDigest().digest();
	}

	/**
	 * Digest the link relation and href.
	 */
	public void onLink(ProductId id, String relation, URI href)
			throws Exception {
		digestStream.write(relation.getBytes(CHARSET));
		digestStream.write(href.toString().getBytes(CHARSET));
	}

	/**
	 * Digest the property name and value.
	 */
	public void onProperty(ProductId id, String name, String value)
			throws Exception {
		digestStream.write(name.getBytes(CHARSET));
		digestStream.write(value.getBytes(CHARSET));
	}

	/**
	 * Don't digest signature version.
	 */
	@Override
	public void onSignatureVersion(ProductId id, Version version) throws Exception {
		// generating signature, ignore
	}

	/**
	 * Don't digest the signature.
	 */
	@Override
	public void onSignature(ProductId id, String signature) throws Exception {
		// generating signature, ignore
	}

	/**
	 * Free any resources associated with this handler.
	 */
	@Override
	public void close() {
		StreamUtils.closeStream(digestStream);
	}


	public static void main(final String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("Usage: ProductDigest FILE [FILE ...]");
			System.err
					.println("where FILE is a file or directory to include in digest");
			System.exit(1);
		}

		Product product = new Product(new ProductId("test", "test", "test"));
		try {
			product.setTrackerURL(new URL("http://localhost/tracker"));
		} catch (Exception e) {
			// ignore
		}

		// treat all arguments as files or directories to be added as content
		for (String arg : args) {
			File file = new File(arg);
			if (!file.exists()) {
				System.err.println(file.getCanonicalPath() + " does not exist");
				System.exit(1);
			}

			if (file.isDirectory()) {
				product.getContents().putAll(
						FileContent.getDirectoryContents(file));
			} else {
				product.getContents()
						.put(file.getName(), new FileContent(file));
			}
		}

		long totalBytes = 0L;
		Iterator<String> iter = product.getContents().keySet().iterator();
		while (iter.hasNext()) {
			totalBytes += product.getContents().get(iter.next()).getLength();
		}

		KeyPair keyPair = CryptoUtils.generateDSAKeyPair(CryptoUtils.DSA_1024);
		Date start = new Date();
		product.sign(keyPair.getPrivate());
		Date end = new Date();
		long elapsed = (end.getTime() - start.getTime());

		System.err.println("Digested " + totalBytes + " bytes of content in "
				+ elapsed + "ms");
		System.err.println("Average rate = " + (totalBytes / (elapsed / 1000.0))
				+ " bytes/second");
	}

}
