/*
 * EIDSMessageEvent
 */
package gov.usgs.earthquake.eidsutil;

import java.util.EventObject;
import java.util.Date;

/**
 * EIDSMessageEvent objects are sent from EIDSClients to EIDSListeners.
 */
public class EIDSMessageEvent extends EventObject {

	/** Serialization id. */
	private static final long serialVersionUID = 1L;

	/** The sequence number the server assigned. */
	private Long serverSequenceNumber;

	/** The time the server received this message. */
	private Date serverTimeGenerated;

	/** The unique source name. */
	private String feederSourceHost;

	/** The unique source number. */
	private Long feederSequenceNumber;

	/** Namespace for root element. */
	private String rootNamespace;

	/** Root element of message. */
	private String rootElement;

	/** Message that was received via EIDS. */
	private String message;

	/** The server that delivered this message. */
	private String serverHost;

	/**
	 * Create a new EIDSMessageEvent.
	 * 
	 * @param source
	 *            the EIDSClient that received the message.
	 * @param feederSourceHost
	 *            the origin of the message.
	 * @param feederSequenceNumber
	 *            a unique identifier from feederSourceHost.
	 * @param rootNamespace
	 *            the rootElement namespace of the message
	 * @param rootElement
	 *            the message rootElement local name.
	 * @param message
	 *            the message that was received.
	 * @param serverSequenceNumber
	 *            the sequence number assigned by the server that delivered this
	 *            message.
	 * @param serverTimeGenerated
	 *            the date the server received this message.
	 */
	public EIDSMessageEvent(QWEmbeddedClient source, final Long serverSequenceNumber,
			final Date serverTimeGenerated, final String feederSourceHost,
			final Long feederSequenceNumber, final String rootNamespace,
			final String rootElement, final String message) {
		super(source);
		this.serverHost = source.getServerHost();
		this.serverSequenceNumber = serverSequenceNumber;
		this.serverTimeGenerated = serverTimeGenerated;
		this.feederSourceHost = feederSourceHost;
		this.feederSequenceNumber = feederSequenceNumber;
		this.rootNamespace = rootNamespace;
		this.rootElement = rootElement;
		this.message = message;
	}

	/**
	 * @return the message source as an EIDSClient.
	 */
	public QWEmbeddedClient getQWEmbeddedClient() {
		return (QWEmbeddedClient) getSource();
	}

	/**
	 * @return the serverHost
	 */
	public String getServerHost() {
		return serverHost;
	}

	/**
	 * @return the serverSequenceNumber
	 */
	public Long getServerSequence() {
		return serverSequenceNumber;
	}

	/**
	 * @return the serverTimeGenerated
	 */
	public Date getServerTimeGenerated() {
		return serverTimeGenerated;
	}

	/**
	 * @return combined with message sequence, uniquely identifies this message.
	 */
	public String getMessageSource() {
		return feederSourceHost;
	}

	/**
	 * @return combined with message source, uniquely identifies this message.
	 */
	public Long getMessageSequence() {
		return feederSequenceNumber;
	}

	/**
	 * @return the namespace of the xml root element.
	 */
	public String getRootNamespace() {
		return rootNamespace;
	}

	/**
	 * @return the xml root element of the message.
	 */
	public String getRootElement() {
		return rootElement;
	}

	/**
	 * @return the message.
	 */
	public String getMessage() {
		return message;
	}

}
