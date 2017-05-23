/*
 * EIDSMsgProcessor
 */
package gov.usgs.earthquake.eidsutil;

import com.isti.quakewatch.message.MsgProcessorInterface;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

/**
 * Adapts the ISTI MsgProcessorInterface for EIDSClient. When the EIDSClient
 * receives a message, the processDataMessage method is invoked.
 * 
 * @see EIDSClient
 */
class QWEmbeddedMsgProcessor implements MsgProcessorInterface {

	/** The client to notify when messages are available. */
	private QWEmbeddedClient client;

	/**
	 * Construct a new EIDSMsgProcessor.
	 * 
	 * @param client
	 *            the client that uses this processor.
	 */
	public QWEmbeddedMsgProcessor(final QWEmbeddedClient client) {
		this.client = client;
	}

	/**
	 * Convert QWmessages to QWMsgRecords. Override the notifyMessage method to
	 * further process messages before sending to listeners.
	 * 
	 * @param qwMsgElement
	 *            The "QWmessage" element.
	 * @param dataMsgElement
	 *            The "DataMessage" element.
	 * @param xmlMsgStr
	 *            the XML text message string.
	 * @param requestedFlag
	 *            true to indicate the the message was "requested" (and that it
	 *            should not be processed as a "real-time" message).
	 * @param msgNumObj
	 *            a 'Long' object holding the message number for the message, or
	 *            null if a message number is not available.
	 * @param timeGenObj
	 *            a 'Date' object holding the time-generated value for the
	 *            message, or null if a time-generated value is not available.
	 */
	public void processDataMessage(Element qwMsgElement,
			Element dataMsgElement, String xmlMsgStr, boolean requestedFlag,
			Long msgNumObj, Date timeGenObj) {
		// extract unique message id
		String fdrSourceHost = getAttribute(qwMsgElement, "FdrSourceHost");
		Long fdrSourceMsgNum = Long.valueOf(getAttribute(qwMsgElement,
				"FdrSourceMsgNum"));

		Element root = (Element) dataMsgElement.getChildren().get(0);
		String rootElement = root.getName();
		String rootNamespace = root.getNamespaceURI();

		client.onEIDSMessage(new EIDSMessageEvent(client, msgNumObj,
				timeGenObj, fdrSourceHost, fdrSourceMsgNum, rootNamespace,
				rootElement, getXML(root)));
	}

	/**
	 * Extract a JDOM attribute value.
	 * 
	 * @param element
	 *            a JDOM element
	 * @param name
	 *            the attribute name
	 * @return the attribute value, or null if not present.
	 */
	public String getAttribute(final Element element, final String name) {
		Attribute attribute = element.getAttribute(name);
		String value = null;
		if (attribute != null) {
			value = attribute.getValue();
		}
		return value;
	}

	/**
	 * Serialize an element to an outputstream.
	 * 
	 * @param element
	 *            element to serialize.
	 * @param out
	 *            outputstream where serialized element is written.s
	 */
	public static void writeXML(final Element element, final OutputStream out) {
		try {
			Document doc = new Document((Element) element.clone());
			XMLOutputter outputter = new XMLOutputter();
			outputter.output(doc, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Serialize an element into a String.
	 * 
	 * @param element
	 *            element to serialize.
	 */
	public static String getXML(final Element element) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			writeXML(element, baos);
			return baos.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
}
