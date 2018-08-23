package gov.usgs.earthquake.product.io;

import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.ProductHandler;

/**
 * Deliver content in a separate thread.
 */
public class ContentOutputThread extends Thread {

	private static final Logger LOGGER = Logger
			.getLogger(ContentOutputThread.class.getName());

	private final ProductHandler handler;
	private final ProductId id;
	private final String path;
	private final Content content;

	public ContentOutputThread(final ProductHandler handler,
			final ProductId id, final String path, final Content content) {
		this.handler = handler;
		this.id = id;
		this.path = path;
		this.content = content;
	}

	@Override
	public void run() {
		try {
			handler.onContent(id, path, content);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception delivering content '" + path
					+ "'", e);
		} finally {
			content.close();
		}
	}

}
