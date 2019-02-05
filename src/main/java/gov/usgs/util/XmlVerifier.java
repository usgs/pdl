package gov.usgs.util;

import java.io.File;
import java.security.PublicKey;
import java.security.cert.Certificate;

import com.google.publicalerts.cap.TrustStrategy;
import com.google.publicalerts.cap.XmlSignatureValidator;
import com.google.publicalerts.cap.XmlSignatureValidator.Result;
import com.google.publicalerts.cap.XmlSignatureValidator.Result.Detail;

/**
 * Class to verify xml signatures.
 */
public class XmlVerifier {

	/**
	 * Main method for verifying XML signatures.
	 */
	public static void main(final String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("Usage: XmlVerifier certificate xml [xml ...])");
			System.err.println("\tcertificate is a DER or PEM encoded certificate");
			System.err.println("\txml are files signed with certificate");
			System.exit(1);
		}

		XmlSignatureValidator validator = null;
		for (String arg : args) {
			if (validator == null) {
				// first argument is certificate.
				Certificate certificate = CryptoUtils
						.readCertificate(StreamUtils.readStream(StreamUtils
								.getInputStream(new File(arg))));
				validator = new XmlSignatureValidator(new TrustSpecificKey(
						certificate.getPublicKey()));
			} else {
				// other arguments are xml files.
				try {
					Result result = validator.validate(new String(FileUtils
							.readFile(new File(arg))));
					if (result.isSignatureValid()) {
						System.err.println("verified " + arg);
					} else {
						System.err.println("result for " + arg + "\n");
						for (Detail detail : result.details()) {
							System.err.println("\t" + detail);
						}
					}
				} catch (Exception e) {
					System.err.println("exception verifying " + arg);
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Implement the TrustStrategy interface to trust a specific certificate.
	 */
	public static class TrustSpecificKey implements TrustStrategy {
		private PublicKey key;

		/**
		 * Create a new TrustSpecificKey TrustStrategy.
		 *
		 * @param key the key to trust.
		 */
		public TrustSpecificKey(final PublicKey key) {
			this.key = key;
		}

		public boolean isKeyTrusted(final PublicKey k) {
			return this.key.equals(k);
		}

		@Override
		public boolean allowMissingSignatures() {
			return false;
		}

		@Override
		public boolean allowUntrustedCredentials() {
			return false;
		}
	}

}
