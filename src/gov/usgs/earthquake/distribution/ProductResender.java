package gov.usgs.earthquake.distribution;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.IOUtil;
import gov.usgs.earthquake.product.io.ObjectProductHandler;
import gov.usgs.earthquake.product.io.ProductSource;

/**
 * A utility class to (re)send an existing product to pdl hubs.
 * 
 * Mainly used when one server has not received a product, in order to
 * redistribute the product.
 */
public class ProductResender {

	private static final Logger LOGGER = Logger.getLogger(ProductResender.class
			.getName());

	public static final String SERVERS_ARGUMENT = "--servers=";
	public static final String BATCH_ARGUMENT = "--batch";

	public static void main(final String[] args) throws Exception {
		// disable tracker
		ProductTracker.setTrackerEnabled(false);

		File inFile = null;
		String inFormat = null;
		String servers = null;
		boolean binaryFormat = false;
		boolean enableDeflate = true;
		boolean batchMode = false;

		for (String arg : args) {
			if (arg.startsWith(IOUtil.INFILE_ARGUMENT)) {
				inFile = new File(arg.replace(IOUtil.INFILE_ARGUMENT, ""));
			} else if (arg.startsWith(IOUtil.INFORMAT_ARGUMENT)) {
				inFormat = arg.replace(IOUtil.INFORMAT_ARGUMENT, "");
			} else if (arg.startsWith(SERVERS_ARGUMENT)) {
				servers = arg.replace(SERVERS_ARGUMENT, "");
			} else if (arg.equals(CLIProductBuilder.BINARY_FORMAT_ARGUMENT)) {
				binaryFormat = true;
			} else if (arg.equals(CLIProductBuilder.DISABLE_DEFLATE)) {
				enableDeflate = false;
			} else if (arg.equals(BATCH_ARGUMENT)) {
				batchMode = true;
			}
		}

		// read product
		Product product = null;
		if (!batchMode) {
			ProductSource source = IOUtil.getProductSource(inFormat, inFile);
			if (source != null) {
				product = ObjectProductHandler.getProduct(source);
			}
		}

		ProductBuilder builder = new ProductBuilder();
		builder.getProductSenders().addAll(
				CLIProductBuilder.parseServers(servers, 15000, binaryFormat,
						enableDeflate));

		if ((!batchMode && product == null) || builder.getProductSenders().size() == 0) {
			System.err.println("Usage: ProductResender --servers=SERVERLIST"
					+ " --informat=(zip|directory|xml) --infile=FILE"
					+ " [--binaryFormat] [--disableDeflate] [--batch]");
			System.err.println("When using batch mode (--batch), the --infile argument is ignored.");
			System.err.println("Files to send are read one per line from stdin.");
			System.exit(CLIProductBuilder.EXIT_INVALID_ARGUMENTS);
		}

		builder.startup();

		if (!batchMode) {
			sendProduct(builder, product, batchMode);
		} else {
			// send batch
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			while ((line = br.readLine()) != null) {
				inFile = new File(line);
				ProductSource source = IOUtil.getProductSource(inFormat, inFile);
				if (source != null) {
					product = ObjectProductHandler.getProduct(source);
				} else {
					System.err.println("ERROR: unable to load product from '" +
							inFile.getCanonicalPath() + "'");
					continue;
				}
				sendProduct(builder, product, batchMode);
			}
		}

		// normal exit
		builder.shutdown();
		System.exit(0);
	}

	protected static void sendProduct(final ProductBuilder builder,
			final Product product, final boolean batchMode) throws Exception {
		// extracted from CLIProductBuilder

		// send the product
		Map<ProductSender, Exception> sendExceptions = builder
				.sendProduct(product);

		// handle any send exceptions
		if (sendExceptions.size() != 0) {
			Iterator<ProductSender> senders = sendExceptions.keySet()
					.iterator();
			// log the exceptions
			while (senders.hasNext()) {
				ProductSender sender = senders.next();
				if (sender instanceof SocketProductSender) {
					// put more specific information about socket senders
					SocketProductSender socketSender = (SocketProductSender) sender;
					LOGGER.log(
							Level.WARNING,
							"Exception sending product to "
									+ socketSender.getHost() + ":"
									+ socketSender.getPort(),
							sendExceptions.get(sender));
				} else {
					LOGGER.log(Level.WARNING, "Exception sending product "
							+ sendExceptions.get(sender));
				}
			}

			if (sendExceptions.size() < builder.getProductSenders().size()) {
				LOGGER.warning("Partial failure sending product,"
						+ " at least one sender accepted product."
						+ " Check the tracker for more information.");
				// still output built product id
				System.out.println(product.getId().toString());
				if (batchMode) {
					// don't interrupt the batch
					return;
				}
				// but exit with partial failure
				System.exit(CLIProductBuilder.EXIT_PARTIALLY_SENT);
			} else {
				LOGGER.severe("Total failure sending product");
				// still output built product id
				System.err.println("ERROR: " + product.getId().toString());
				if (batchMode) {
					// don't interrupt the batch
					return;
				}
				System.exit(CLIProductBuilder.EXIT_UNABLE_TO_SEND);
			}

		}

		// otherwise output built product id
		System.out.println(product.getId().toString());
	}

}
