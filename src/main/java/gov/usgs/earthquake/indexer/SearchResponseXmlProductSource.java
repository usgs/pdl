/*
 * SearchResponseXmlProductSource
 */
package gov.usgs.earthquake.indexer;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import gov.usgs.earthquake.distribution.FileProductStorage;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.ProductHandler;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.earthquake.product.io.XmlProductSource;

/**
 * Used by SearchResponseParser to store xml products in a FileProductStorage as
 * they are parsed.
 * 
 * Creates a "background" storage thread for storing, while this classes
 * startElement, characters, and endElement methods are called by the
 * "foreground" xml parsing thread.
 */
public class SearchResponseXmlProductSource extends XmlProductSource {

	/** Logging object. */
	private static final Logger LOGGER = Logger
			.getLogger(SearchResponseXmlProductSource.class.getName());

	/** The storage where the product is streamed. */
	private FileProductStorage storage;

	/** The stored id. */
	private Product storedProduct = null;

	/** The thread where storage does its thing (current thread is xml parsing). */
	private Thread storageThread;

	/** */
	private final Object waitForSetHandlerSync = new Object();

	/** */
	private final Object waitForStreamToSync = new Object();

	/** start product attributes, for acquiring writelock in background thread */
	private String uri;
	private String localName;
	private String qName;
	private Attributes attributes;
	private SAXException exception;

	/**
	 * Construct a SearchResponseXmlProductSource.
	 * 
	 * @param storage
	 *            the storage where the parsed product is stored.
	 */
	public SearchResponseXmlProductSource(final FileProductStorage storage) {
		super((ProductHandler) null);
		this.setStorage(storage);
	}

	/**
	 * Called by the underlying product storage as part os storeProductSource.
	 * 
	 * This method notifies the XML parsing thread that parsing may continue,
	 * since the handler is now setup.
	 */
	@Override
	public void streamTo(final ProductHandler handler) {
		this.setHandler(handler);

		try {
			// start the product in this thread, so the write lock is acquired
			// and released in the same thread
			super.startElement(uri, localName, qName, attributes);
		} catch (SAXException e) {
			exception = e;
		}
		// clear references that are no longer needed
		this.uri = null;
		this.localName = null;
		this.qName = null;
		this.attributes = null;

		synchronized (waitForSetHandlerSync) {
			// notify xml parsing thread that handler is all set
			waitForSetHandlerSync.notify();
		}

		synchronized (waitForStreamToSync) {
			try {
				// wait for xml parsing thread to notify streamTo is complete
				waitForStreamToSync.wait();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	@Override
	public void startElement(final String uri, final String localName,
			final String qName, final Attributes attributes)
			throws SAXException {
		boolean startElementAlreadySent = false;

		if (uri.equals(XmlProductHandler.PRODUCT_XML_NAMESPACE)) {
			if (localName.equals(XmlProductHandler.PRODUCT_ELEMENT)) {
				// save these to write lock can be acquired by correct thread.
				this.uri = uri;
				this.localName = localName;
				this.qName = qName;
				this.attributes = attributes;

				// starting a product, set up the storage handler/thread
				// reference used by storage thread to set product
				final SearchResponseXmlProductSource thisSource = this;
				storageThread = new Thread() {
					public void run() {
						try {
							ProductId id = storage
									.storeProductSource(thisSource);
							thisSource.setProduct(storage.getProduct(id));
						} catch (Exception e) {
							LOGGER.log(Level.WARNING,
									"Exception while storing product", e);
							thisSource.setProduct(null);
						}
					}
				};

				synchronized (waitForSetHandlerSync) {
					storageThread.start();
					try {
						// wait for storage thread to call streamTo with
						// handler
						waitForSetHandlerSync.wait();
					} catch (InterruptedException e) {
						// ignore
					}
					// handler set, ready to continue parsing

					// signal that we've already sent the startElement
					startElementAlreadySent = true;

					// if an exception was generated in background thread, throw
					// it here
					if (exception != null) {
						throw exception;
					}
				}
			}
		}

		if (!startElementAlreadySent) {
			// forward call to parser
			super.startElement(uri, localName, qName, attributes);
		}
	}

	public void endElement(final String uri, final String localName,
			final String qName) throws SAXException {

		// forward call to parser
		super.endElement(uri, localName, qName);

		if (uri.equals(XmlProductHandler.PRODUCT_XML_NAMESPACE)) {
			if (localName.equals(XmlProductHandler.PRODUCT_ELEMENT)) {
				// done parsing the product

				synchronized (waitForStreamToSync) {
					// notify storageThread streamTo is complete
					waitForStreamToSync.notify();
				}

				try {
					// wait for storageThread to complete so storage will have
					// called setProduct before returning
					storageThread.join();
				} catch (InterruptedException e) {
					// ignore
				} finally {
					storageThread = null;
				}
			}
		}
	}

	public void setStorage(FileProductStorage storage) {
		this.storage = storage;
	}

	public FileProductStorage getStorage() {
		return storage;
	}

	/**
	 * @return the parsed, stored product.
	 */
	public Product getProduct() {
		return this.storedProduct;
	}

	/**
	 * Method used by storage to provide the parsed product.
	 * 
	 * @param product
	 */
	protected void setProduct(final Product product) {
		this.storedProduct = product;
	}

}
