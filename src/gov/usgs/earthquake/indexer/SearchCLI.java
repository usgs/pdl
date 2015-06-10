package gov.usgs.earthquake.indexer;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.logging.Logger;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.XmlUtils;

/**
 * Command line interface for search socket.
 * 
 * This class reads arguments from the command line that represent a
 * ProductIndexQuery. Then it connects to a configured socket, executes the
 * search, and outputs the response xml.
 */
public class SearchCLI {

	private static final Logger LOGGER = Logger.getLogger(SearchCLI.class
			.getName());

	/**
	 * Command-line argument definitions
	 */
	public static String SEARCH_METHOD_ARGUMENT = "--searchMethod=";
	public static String RESULT_TYPE_ARGUMENT = "--resultType=";
	public static String EVENT_SOURCE_ARGUMENT = "--eventSource=";
	public static String EVENT_SOURCE_CODE_ARGUMENT = "--eventSourceCode=";
	public static String MIN_EVENT_TIME_ARGUMENT = "--minEventTime=";
	public static String MAX_EVENT_TIME_ARGUMENT = "--maxEventTime=";
	public static String MIN_EVENT_LATITUDE_ARGUMENT = "--minEventLatitude=";
	public static String MIN_EVENT_LONGITUDE_ARGUMENT = "--minEventLongitude=";
	public static String MAX_EVENT_LATITUDE_ARGUMENT = "--maxEventLatitude=";
	public static String MAX_EVENT_LONGITUDE_ARGUMENT = "--maxEventLongitude=";
	public static String MIN_EVENT_DEPTH_ARGUMENT = "--minEventDepth=";
	public static String MAX_EVENT_DEPTH_ARGUMENT = "--maxEventDepth=";
	public static String MIN_EVENT_MAGNITUDE_ARGUMENT = "--minEventMagnitude=";
	public static String MAX_EVENT_MAGNITUDE_ARGUMENT = "--maxEventMagnitude=";
	public static String PRODUCT_ID_ARGUMENT = "--productId=";
	public static String MIN_PRODUCT_UPDATE_TIME_ARGUMENT = "--minProductUpdateTime=";
	public static String MAX_PRODUCT_UPDATE_TIME_ARGUMENT = "--maxProductUpdateTime=";
	public static String PRODUCT_SOURCE_ARGUMENT = "--productSource=";
	public static String PRODUCT_TYPE_ARGUMENT = "--productType=";
	public static String PRODUCT_VERSION_ARGUMENT = "--productVersion=";
	public static String PRODUCT_STATUS_ARGUMENT = "--productStatus=";

	public static String SEARCH_HOST_ARGUMENT = "--searchHost=";
	public static String SEARCH_PORT_ARGUMENT = "--searchPort=";

	public static String FILE_OUTPUT_ARGUMENT = "--outputFile=";

	/**
	 * Default constructor, for configurable interface.
	 */
	public SearchCLI() {
	}

	/**
	 * Entry point into search. Called by Main when the --search argument is
	 * used.
	 * 
	 * @param args
	 *            command line arguments.
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		String outputFilePath = null;
		SearchMethod type = null;
		InetAddress host = InetAddress.getByName("localhost");
		int port = Integer.parseInt(SearchServerSocket.DEFAULT_SEARCH_PORT);
		ProductIndexQuery query = new ProductIndexQuery();
		// Alternate result types are currently not supported.
		query.setResultType(ProductIndexQuery.RESULT_TYPE_CURRENT);
		for (String arg : args) {
			if (arg.startsWith(SEARCH_METHOD_ARGUMENT)) {
				type = SearchMethod.fromXmlMethodName(arg.replace(
						SEARCH_METHOD_ARGUMENT, ""));
				if (type == null) {
					LOGGER.severe("Undefined search method received: '"
							+ arg.replace(SEARCH_METHOD_ARGUMENT, "")
							+ "'. Search could not be processed.");
					throw new Exception();
				}
				/*
				 * Alternate result types are currently not supported. } else if
				 * (arg.startsWith(RESULT_TYPE_ARGUMENT)) { String resultType =
				 * arg.replace(RESULT_TYPE_ARGUMENT, ""); if
				 * (resultType.toLowerCase() == "current")
				 * query.setResultType(ProductIndexQuery.RESULT_TYPE_CURRENT);
				 * else if (resultType.toLowerCase() == "superseded")
				 * query.setResultType
				 * (ProductIndexQuery.RESULT_TYPE_SUPERSEDED); else if
				 * (resultType.toLowerCase() == "all")
				 * query.setResultType(ProductIndexQuery.RESULT_TYPE_ALL);
				 */
			} else if (arg.startsWith(EVENT_SOURCE_ARGUMENT)) {
				query.setEventSource(arg.replace(EVENT_SOURCE_ARGUMENT, ""));
			} else if (arg.startsWith(EVENT_SOURCE_CODE_ARGUMENT)) {
				query.setEventSourceCode(arg.replace(
						EVENT_SOURCE_CODE_ARGUMENT, ""));
			} else if (arg.startsWith(MIN_EVENT_TIME_ARGUMENT)) {
				query.setMinEventTime(XmlUtils.getDate(arg.replace(
						MIN_EVENT_TIME_ARGUMENT, "")));
			} else if (arg.startsWith(MAX_EVENT_TIME_ARGUMENT)) {
				query.setMaxEventTime(XmlUtils.getDate(arg.replace(
						MAX_EVENT_TIME_ARGUMENT, "")));
			} else if (arg.startsWith(MIN_EVENT_LATITUDE_ARGUMENT)) {
				query.setMinEventLatitude(new BigDecimal(arg.replace(
						MIN_EVENT_LATITUDE_ARGUMENT, "")));
			} else if (arg.startsWith(MIN_EVENT_LONGITUDE_ARGUMENT)) {
				query.setMinEventLongitude(new BigDecimal(arg.replace(
						MIN_EVENT_LONGITUDE_ARGUMENT, "")));
			} else if (arg.startsWith(MIN_EVENT_DEPTH_ARGUMENT)) {
				query.setMinEventDepth(new BigDecimal(arg.replace(
						MIN_EVENT_DEPTH_ARGUMENT, "")));
			} else if (arg.startsWith(MAX_EVENT_LATITUDE_ARGUMENT)) {
				query.setMaxEventLatitude(new BigDecimal(arg.replace(
						MAX_EVENT_LATITUDE_ARGUMENT, "")));
			} else if (arg.startsWith(MAX_EVENT_LONGITUDE_ARGUMENT)) {
				query.setMaxEventLongitude(new BigDecimal(arg.replace(
						MAX_EVENT_LONGITUDE_ARGUMENT, "")));
			} else if (arg.startsWith(MAX_EVENT_DEPTH_ARGUMENT)) {
				query.setMaxEventDepth(new BigDecimal(arg.replace(
						MAX_EVENT_DEPTH_ARGUMENT, "")));
			} else if (arg.startsWith(MIN_EVENT_MAGNITUDE_ARGUMENT)) {
				query.setMinEventMagnitude(new BigDecimal(arg.replace(
						MIN_EVENT_MAGNITUDE_ARGUMENT, "")));
			} else if (arg.startsWith(MAX_EVENT_MAGNITUDE_ARGUMENT)) {
				query.setMaxEventMagnitude(new BigDecimal(arg.replace(
						MAX_EVENT_MAGNITUDE_ARGUMENT, "")));
			} else if (arg.startsWith(PRODUCT_ID_ARGUMENT)) {
				query.getProductIds().add(
						ProductId.parse(arg.replace(PRODUCT_ID_ARGUMENT, "")));
			} else if (arg.startsWith(PRODUCT_SOURCE_ARGUMENT)) {
				query.setProductSource(arg.replace(PRODUCT_SOURCE_ARGUMENT, ""));
			} else if (arg.startsWith(PRODUCT_STATUS_ARGUMENT)) {
				query.setProductStatus(arg.replace(PRODUCT_STATUS_ARGUMENT, ""));
			} else if (arg.startsWith(PRODUCT_TYPE_ARGUMENT)) {
				query.setProductType(arg.replace(PRODUCT_TYPE_ARGUMENT, ""));
			} else if (arg.startsWith(PRODUCT_VERSION_ARGUMENT)) {
				query.setProductVersion(arg.replace(PRODUCT_VERSION_ARGUMENT,
						""));
			} else if (arg.startsWith(SEARCH_HOST_ARGUMENT)) {
				host = InetAddress.getByName(arg.replace(SEARCH_HOST_ARGUMENT,
						""));
			} else if (arg.startsWith(SEARCH_PORT_ARGUMENT)) {
				port = Integer.parseInt(arg.replace(SEARCH_PORT_ARGUMENT, ""));
			} else if (arg.startsWith(FILE_OUTPUT_ARGUMENT)) {
				outputFilePath = arg.replace(FILE_OUTPUT_ARGUMENT, "");
			}
		}
		SearchRequest request = new SearchRequest();
		if (type == null) {
			LOGGER.severe("No search type was provided.");
		}
		OutputStream stream;
		if (outputFilePath != null) {
			stream = new FileOutputStream(FILE_OUTPUT_ARGUMENT);
		} else {
			stream = System.out;
		}
		request.addQuery(SearchQuery.getSearchQuery(type, query));

		SearchSocket socket = new SearchSocket(host, port);
		socket.search(request, stream);
	}

	public static String getUsage() {
		StringBuffer buf = new StringBuffer();

		buf.append("Product Index Search Interface:\n");
		buf.append("--search : Command is using the search interface\n");
		buf.append(SEARCH_METHOD_ARGUMENT + "METHOD : Any one of ");
		SearchMethod[] methods = SearchMethod.values();
		for (int i = 0; i < methods.length; i++) {
			buf.append("'" + methods[i].getXmlMethodName() + "'");
			if (i < methods.length - 1) {
				buf.append(", ");
			} else {
				buf.append("\n");
			}
		}
		buf.append("[" + EVENT_SOURCE_ARGUMENT
				+ "SOURCE] : Filter results by event source network.\n");
		buf.append("[" + EVENT_SOURCE_CODE_ARGUMENT
				+ "CODE] : Filter results by event source network code.\n");
		buf.append("["
				+ MIN_EVENT_TIME_ARGUMENT
				+ "XML_FORMATTED_TIME] : Filter results by start of time window.\n");
		buf.append("["
				+ MAX_EVENT_TIME_ARGUMENT
				+ "XML_FORMATTED_TIME] : Filter results by end of time window.\n");
		buf.append("[" + MIN_EVENT_LATITUDE_ARGUMENT
				+ "LATITUDE] : Filter results by Southern boundary.\n");
		buf.append("[" + MAX_EVENT_LATITUDE_ARGUMENT
				+ "LATITUDE] : Filter results by Northern boundary.\n");
		buf.append("[" + MIN_EVENT_LONGITUDE_ARGUMENT
				+ "LONGITUDE] : Filter results by Western boundary.\n");
		buf.append("[" + MAX_EVENT_LONGITUDE_ARGUMENT
				+ "LONGITUDE] : Filter results by Eastern boundary.\n");
		buf.append("[" + MIN_EVENT_DEPTH_ARGUMENT
				+ "DEPTH] : Filter results by minimum depth in km.\n");
		buf.append("[" + MAX_EVENT_DEPTH_ARGUMENT
				+ "DEPTH] : Filter results by maximum depth in km.\n");
		buf.append("[" + MIN_EVENT_MAGNITUDE_ARGUMENT
				+ "MAGNITUDE] : Filter results by minimum magnitude.\n");
		buf.append("[" + MAX_EVENT_MAGNITUDE_ARGUMENT
				+ "MAGNITUDE] : Filter results by maximum magnitude.\n");
		buf.append("["
				+ PRODUCT_ID_ARGUMENT
				+ "PRODUCT_ID] : Filter by product ID. Each time this appears the new ID is added.\n");
		buf.append("[" + PRODUCT_SOURCE_ARGUMENT
				+ "SOURCE] : Filter results by product source.\n");
		buf.append("[" + PRODUCT_STATUS_ARGUMENT
				+ "STATUS] : Filter results by product status.\n");
		buf.append("[" + PRODUCT_TYPE_ARGUMENT
				+ "TYPE] : Filter results by product type.\n");
		buf.append("[" + PRODUCT_VERSION_ARGUMENT
				+ "VERSION] : Filter results by product version.\n");
		buf.append("["
				+ SEARCH_HOST_ARGUMENT
				+ "HOST] : The Product Index host to receive this query. Default is localhost.");
		buf.append("["
				+ SEARCH_PORT_ARGUMENT
				+ "PORT] : The port on which to execute this query. Default is "
				+ SearchServerSocket.DEFAULT_SEARCH_PORT + ".\n");
		buf.append("["
				+ FILE_OUTPUT_ARGUMENT
				+ "FILE] : The file to write output to. If this is not included, output will be directed over stdout.");

		return buf.toString();
	}

}
