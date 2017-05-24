/*
 * ProductTracker
 */
package gov.usgs.earthquake.distribution;

import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.distribution.HeartbeatListener;
import gov.usgs.util.Config;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Send updates and search sent updates about distribution status.
 * 
 * ProductDistribution clients to send status updates about received
 * notifications, and processed products.
 * 
 * <strong>Search Example</strong>
 * 
 * <pre>
 * ProductTracker tracker = new ProductTracker(new URL(
 * 		&quot;http://ehppdl1.cr.usgs.gov/tracker/&quot;));
 * String source = &quot;us&quot;;
 * String type = &quot;losspager&quot;;
 * String code = &quot;us2010abcd&quot;;
 * Date updateTime = null;
 * String className = null;
 * List&lt;ProductTrackerUpdate&gt; updates = tracker.getUpdates(source, type, code,
 * 		updateTime, className);
 * </pre>
 * 
 * <strong>Update Example</strong>
 * 
 * <pre>
 * Product product = ...;
 * ProductTracker tracker = new ProductTracker(product.getTrackerURL()).;
 * ProductTrackerUpdate update = new ProductTrackerUpdate(product.getTrackerURL(), 
 * 		product.getId(), 
 * 		"my component name", 
 * 		"my component message");
 * tracker.sendUpdate(update);
 * </pre>
 * 
 */
public class ProductTracker {

	/** Logging object. */
	private static final Logger LOGGER = Logger.getLogger(ProductTracker.class
			.getName());

	/** Whether tracker updates are enabled in this vm. */
	private static boolean TRACKER_ENABLED = false;

	/**
	 * Set whether sending tracker updates is enabled from this host.
	 * 
	 * @param enabled
	 *            true to send tracker updates, false to disable.
	 */
	public static void setTrackerEnabled(final boolean enabled) {
		TRACKER_ENABLED = enabled;
	}

	/** Location of tracker. */
	private URL trackerURL;

	/**
	 * Create a new ProductTracker object.
	 */
	public ProductTracker(final URL trackerURL) {
		this.trackerURL = trackerURL;
	}

	/**
	 * @return the trackerURL
	 */
	public URL getTrackerURL() {
		return trackerURL;
	}

	/**
	 * @param trackerURL
	 *            the trackerURL to set
	 */
	public void setTrackerURL(URL trackerURL) {
		this.trackerURL = trackerURL;
	}

	/**
	 * Send an update to this ProductTracker.
	 * 
	 * @param update
	 *            the update to send to the tracker.
	 * @return the update object processed by the tracker, including sequence
	 *         number, or null if unable to send.
	 * @throws Exception
	 */
	public ProductTrackerUpdate sendUpdate(final ProductTrackerUpdate update)
			throws Exception {

		HeartbeatListener.sendHeartbeatMessage(update.getClassName(), // component
				update.getMessage(), // key
				update.getId().toString() // value
				);
		String response = sendUpdateXML(update);
		try {
			List<ProductTrackerUpdate> updates = parseTrackerResponse(
					update.getTrackerURL(),
					StreamUtils.getInputStream(response));
			// should receive one update (the one that was sent)
			if (updates.size() == 1) {
				return updates.get(0);
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	/**
	 * Send an update to this ProductTracker.
	 * 
	 * @param update
	 *            the update to send to the tracker.
	 * @return the raw XML returned by the tracker, or null if unable to send.
	 * @throws Exception
	 */
	public String sendUpdateXML(final ProductTrackerUpdate update)
			throws Exception {

		// make sure this update hasn't already been sent
		Long sequenceNumber = update.getSequenceNumber();
		if (sequenceNumber != null && sequenceNumber > 0) {
			throw new IllegalArgumentException(
					"ProductTrackerUpdate already has a sequence number.");
		}

		ProductId id = update.getId();

		// log the update no matter what
		LOGGER.fine(new StringBuffer().append(update.getMessage())
				.append(" source=").append(id.getSource()).append(", type=")
				.append(id.getType()).append(", code=").append(id.getCode())
				.append(", updateTime=").append(id.getUpdateTime().toString())
				.toString());

		if (!TRACKER_ENABLED) {
			LOGGER.finest("Tracker updates disabled, not sent");
			// didn't send update
			return null;
		}

		// build update request
		Map<String, String> request = new HashMap<String, String>();
		request.put("action", "update");
		request.put("source", id.getSource());
		request.put("type", id.getType());
		request.put("code", id.getCode());
		request.put("updateTime", Long.toString(id.getUpdateTime().getTime()));
		request.put("className", update.getClassName());
		request.put("message", update.getMessage());

		try {
			String response = post(update.getTrackerURL(), request);
			return response;
		} catch (Exception e) {
			LOGGER.log(Level.INFO, "Unable to post to tracker", e);
		}
		return null;
	}

	/** Same as getUpdates with 0 for startid. */
	public List<ProductTrackerUpdate> getUpdates(final String source,
			final String type, final String code, final Date updateTime,
			final String className) throws Exception {
		return getUpdates(source, type, code, updateTime, className, 0L);
	}

	/**
	 * Search for updates on this tracker.
	 * 
	 * At least one field must be not null, or this method will return no
	 * updates.
	 * 
	 * @param source
	 *            product source.
	 * @param type
	 *            product type.
	 * @param code
	 *            product code.
	 * @param updateTime
	 *            product update time.
	 * @param className
	 *            module name.
	 * @return updates matching the provided fields.
	 * @throws Exception
	 */
	public List<ProductTrackerUpdate> getUpdates(final String source,
			final String type, final String code, final Date updateTime,
			final String className, final Long startid) throws Exception {
		String response = getUpdateXML(source, type, code, updateTime,
				className, startid);
		List<ProductTrackerUpdate> updates = parseTrackerResponse(trackerURL,
				StreamUtils.getInputStream(response));
		return updates;
	}

	/**
	 * Search for updates on this tracker, returning raw xml.
	 * 
	 * @param source
	 * @param type
	 * @param code
	 * @param updateTime
	 * @param className
	 * @param startid
	 * @return the raw xml response from the tracker.
	 * @throws Exception
	 */
	public String getUpdateXML(final String source, final String type,
			final String code, final Date updateTime, final String className,
			final Long startid) throws Exception {

		Map<String, String> request = new HashMap<String, String>();
		request.put("action", "search");

		if (source != null) {
			request.put("source", source);
		}
		if (type != null) {
			request.put("type", type);
		}
		if (code != null) {
			request.put("code", code);
		}
		if (updateTime != null) {
			request.put("updateTime", Long.toString(updateTime.getTime()));
		}

		if (className != null) {
			request.put("className", className);
		}

		if (startid != null) {
			request.put("startid", Long.toString(startid));
		}

		String response = post(trackerURL, request);
		return response;
	}

	/**
	 * Send a custom tracker update message.
	 * 
	 * @param className
	 *            the module that is sending the message.
	 * @param id
	 *            the product the message is about.
	 * @param message
	 *            the message about the product.
	 * @return the sent update.
	 * @throws Exception
	 */
	public ProductTrackerUpdate sendUpdate(final String className,
			final ProductId id, final String message) throws Exception {
		ProductTrackerUpdate update = new ProductTrackerUpdate(trackerURL, id,
				className, message);
		return sendUpdate(update);
	}

	/**
	 * Send a productCreated update.
	 * 
	 * @param className
	 *            the module that created the product.
	 * @param id
	 *            the product that was created.
	 * @return the sent update.
	 * @throws Exception
	 */
	public ProductTrackerUpdate productCreated(final String className,
			final ProductId id) throws Exception {
		ProductTrackerUpdate createdUpdate = new ProductTrackerUpdate(
				trackerURL, id, className, ProductTrackerUpdate.PRODUCT_CREATED);
		return sendUpdate(createdUpdate);
	}

	/**
	 * Send a productIndexed update.
	 * 
	 * @param className
	 *            the module that indexed the product.
	 * @param id
	 *            the product that was indexed.
	 * @return the sent update.
	 * @throws Exception
	 */
	public ProductTrackerUpdate productIndexed(final String className,
			final ProductId id) throws Exception {
		ProductTrackerUpdate indexedUpdate = new ProductTrackerUpdate(
				trackerURL, id, className, ProductTrackerUpdate.PRODUCT_INDEXED);
		return sendUpdate(indexedUpdate);
	}

	/**
	 * Send a notificationSent update.
	 * 
	 * @param className
	 *            the module that sent the notification.
	 * @param notification
	 *            the notification that was sent.
	 * @return the sent update.
	 * @throws Exception
	 */
	public ProductTrackerUpdate notificationSent(final String className,
			final Notification notification) throws Exception {
		ProductTrackerUpdate notifiedUpdate = new ProductTrackerUpdate(
				trackerURL, notification.getProductId(), className,
				ProductTrackerUpdate.NOTIFICATION_SENT);
		return sendUpdate(notifiedUpdate);
	}

	/**
	 * Send a notificationReceived update.
	 * 
	 * @param className
	 *            the module that received the notification.
	 * @param notification
	 *            the notification that was received.
	 * @return the sent update.
	 * @throws Exception
	 */
	public ProductTrackerUpdate notificationReceived(final String className,
			final Notification notification) throws Exception {
		ProductTrackerUpdate notifiedUpdate = new ProductTrackerUpdate(
				trackerURL, notification.getProductId(), className,
				ProductTrackerUpdate.NOTIFICATION_RECEIVED);
		return sendUpdate(notifiedUpdate);
	}

	/**
	 * Send a productDownloaded update.
	 * 
	 * @param className
	 *            the module that downloaded the product.
	 * @param id
	 *            the product that was downloaded.
	 * @return the sent update.
	 * @throws Exception
	 */
	public ProductTrackerUpdate productDownloaded(final String className,
			final ProductId id) throws Exception {
		ProductTrackerUpdate downloadedUpdate = new ProductTrackerUpdate(
				trackerURL, id, className,
				ProductTrackerUpdate.PRODUCT_DOWNLOADED);
		return sendUpdate(downloadedUpdate);
	}

	/**
	 * Send a productReceived update.
	 * 
	 * @param className
	 *            the module that received the product.
	 * @param id
	 *            the product that was received.
	 * @return the sent update.
	 * @throws Exception
	 */
	public ProductTrackerUpdate productReceived(final String className,
			final ProductId id) throws Exception {
		ProductTrackerUpdate receivedUpdate = new ProductTrackerUpdate(
				trackerURL, id, className,
				ProductTrackerUpdate.PRODUCT_RECEIVED);
		return sendUpdate(receivedUpdate);
	}

	/**
	 * Send an exception update.
	 * 
	 * @param className
	 *            the module that encountered an exception.
	 * @param id
	 *            the product that was being processed.
	 * @param e
	 *            the exception that was caught.
	 * @return the sent update.
	 * @throws Exception
	 */
	public ProductTrackerUpdate exception(final String className,
			final ProductId id, final Exception e) throws Exception {
		ProductTrackerUpdate exceptionUpdate = new ProductTrackerUpdate(
				trackerURL, id, className,
				ProductTrackerUpdate.PRODUCT_EXCEPTION + ": " + e.getMessage());
		return sendUpdate(exceptionUpdate);
	}

	/**
	 * Encode data for a HTTP Post.
	 * 
	 * @param data
	 *            a map containing name value pairs for encoding.
	 * @return a string of encoded data.
	 * @throws Exception
	 */
	public static String encodeURLData(final Map<String, String> data)
			throws Exception {
		StringBuffer buf = new StringBuffer();
		Iterator<String> iter = data.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			buf.append(URLEncoder.encode(key, "UTF-8")).append("=")
					.append(URLEncoder.encode(data.get(key), "UTF-8"));
			if (iter.hasNext()) {
				buf.append("&");
			}
		}
		return buf.toString();
	}

	/**
	 * Execute a HTTP Post.
	 * 
	 * @param url
	 *            the target url.
	 * @param data
	 *            the data to send.
	 * @return the response text.
	 * @throws Exception
	 */
	public static String post(final URL url, final Map<String, String> data)
			throws Exception {
		InputStream in = null;
		OutputStream out = null;
		String response = null;

		try {
			URLConnection connection = url.openConnection();
			if (data != null) {
				String encodedData = encodeURLData(data);
				connection.setDoOutput(true);
				out = connection.getOutputStream();
				StreamUtils.transferStream(encodedData, out);
			}

			in = connection.getInputStream();
			response = new String(StreamUtils.readStream(in));
		} finally {
			StreamUtils.closeStream(out);
			StreamUtils.closeStream(in);
		}

		return response;
	}

	/**
	 * Parse xml received from a ProductTracker using a ProductTrackerParser.
	 * 
	 * @param trackerURL
	 *            the trackerURL being parsed (so updates are flagged as from
	 *            this tracker).
	 * @param updateStream
	 *            the XML response stream from a product tracker.
	 * @return a list of parsed updates.
	 */
	public List<ProductTrackerUpdate> parseTrackerResponse(
			final URL trackerURL, final InputStream updateStream) {
		ProductTrackerParser parser = new ProductTrackerParser(trackerURL);
		parser.parse(updateStream);
		return parser.getUpdates();
	}

	/** Search a product tracker. */
	public static final String ACTION_SEARCH = "--search";
	/** Send an update to a product tracker. */
	public static final String ACTION_UPDATE = "--update";
	/** Used when searching or sending an update to a product tracker. */
	public static final String ARGUMENT_CLASSNAME = "--class=";
	/** Used when searching or sending an update to a product tracker. */
	public static final String ARGUMENT_PRODUCT_ID = "--productid=";
	/** Used when searching. */
	public static final String ARGUMENT_START_ID = "--startid=";

	/**
	 * Command Line Interface to ProductTracker.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		// whether we are sending an update (true)
		boolean isUpdate = false;
		// whether we are searching (true)
		boolean isSearch = false;
		// The update message being sent to the tracker (updates only)
		String updateMessage = null;
		// The tracker to communicate with
		URL trackerURL = null;
		// The module that sent an update
		String className = null;
		// The product id, an alternate to source+type+code+updateTime
		ProductId id = null;
		// The product source
		String source = null;
		// The product type
		String type = null;
		// The product code
		String code = null;
		// The product update time
		Date updateTime = null;
		// The starting sequence number (searches only)
		Long startid = 0L;

		// use the default tracker url unless overridden by arguments
		String defaultTrackerURL = Config.getConfig().getProperty(
				CLIProductBuilder.TRACKER_URL_CONFIG_PROPERTY);
		if (defaultTrackerURL != null) {
			trackerURL = new URL(defaultTrackerURL);
		}

		// parse the arguments
		for (String arg : args) {
			if (arg.equals(ACTION_UPDATE)) {
				isUpdate = true;
			} else if (arg.equals(ACTION_SEARCH)) {
				isSearch = true;
			} else if (arg.startsWith(ARGUMENT_CLASSNAME)) {
				className = arg.replace(ARGUMENT_CLASSNAME, "");
			} else if (arg.startsWith(ARGUMENT_PRODUCT_ID)) {
				id = ProductId.parse(arg.replace(ARGUMENT_PRODUCT_ID, ""));
				source = id.getSource();
				type = id.getType();
				code = id.getCode();
				updateTime = id.getUpdateTime();
			} else if (arg.startsWith(CLIProductBuilder.TRACKER_URL_ARGUMENT)) {
				trackerURL = new URL(arg.replace(
						CLIProductBuilder.TRACKER_URL_ARGUMENT, ""));
			} else if (arg.startsWith(CLIProductBuilder.SOURCE_ARGUMENT)) {
				source = arg.replace(CLIProductBuilder.SOURCE_ARGUMENT, "");
			} else if (arg.startsWith(CLIProductBuilder.TYPE_ARGUMENT)) {
				type = arg.replace(CLIProductBuilder.TYPE_ARGUMENT, "");
			} else if (arg.startsWith(CLIProductBuilder.CODE_ARGUMENT)) {
				code = arg.replace(CLIProductBuilder.CODE_ARGUMENT, "");
			} else if (arg.startsWith(CLIProductBuilder.UPDATE_TIME_ARGUMENT)) {
				updateTime = XmlUtils.getDate(arg.replace(
						CLIProductBuilder.UPDATE_TIME_ARGUMENT, ""));
			} else if (arg.startsWith(ARGUMENT_START_ID)) {
				startid = Long.valueOf(arg.replace(ARGUMENT_START_ID, ""));
			}
		}

		// run the search or update
		ProductTracker tracker = new ProductTracker(trackerURL);
		if (isUpdate && isSearch) {
			LOGGER.severe("Both " + ACTION_UPDATE + " and " + ACTION_SEARCH
					+ " present, only one allowed");
			System.exit(1);
		} else if (!isUpdate && !isSearch) {
			LOGGER.severe("Neither " + ACTION_UPDATE + " nor " + ACTION_SEARCH
					+ " present, one is required");
			System.exit(1);
		} else if (isUpdate) {
			LOGGER.info("Reading update message from STDIN...");
			updateMessage = new String(StreamUtils.readStream(System.in));
			if (id == null) {
				id = new ProductId(source, type, code, updateTime);
			}
			System.out.println(tracker.sendUpdateXML(new ProductTrackerUpdate(
					trackerURL, id, className, updateMessage)));
			// LOGGER.info("Update id=" + update.getSequenceNumber());
		} else if (isSearch) {
			// default to search action

			/*
			 * LOGGER.info("Searching for source=" + source + ", type=" + type +
			 * ", code=" + code + ", updateTime=" + updateTime + ", className="
			 * + className + ", startid=" + startid);
			 */
			System.out.println(tracker.getUpdateXML(source, type, code,
					updateTime, className, startid));
		}
	}

	public static String getUsage() {
		StringBuffer buf = new StringBuffer();

		buf.append("[--search]               search a tracker (default)\n");
		buf.append("                         when searching, include at least one parameter\n");
		buf.append("[--update]               send an update to a tracker\n");
		buf.append("                         when updating, productid and class are required\n");
		buf.append("                         the update message is read from STDIN\n");
		buf.append("\n");

		buf.append("--trackerURL=URL         which tracker to search or update\n");
		buf.append("[--productid=URN]        the product to search or update\n");
		buf.append("[--source=SOURCE]        a product source to search\n");
		buf.append("[--type=TYPE]            a product type to search\n");
		buf.append("[--code=CODE]            a product code to search\n");
		buf.append("[--updateTime=TIME]      a product update time to search\n");
		buf.append("[--class=MODULE]         a module to search or send an update\n");
		buf.append("[--startid=SEQ]          only return updates with sequence number > SEQ\n");
		buf.append("\n");

		return buf.toString();
	}

}
