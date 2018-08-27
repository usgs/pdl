/*
 * EventAddonProductParser
 */
package gov.usgs.earthquake.eids;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import gov.usgs.ansseqmsg.Action;
import gov.usgs.ansseqmsg.Comment;
import gov.usgs.ansseqmsg.EQMessage;
import gov.usgs.ansseqmsg.Event;
import gov.usgs.ansseqmsg.ProductLink;
import gov.usgs.earthquake.eidsutil.EIDSMessageEvent;
import gov.usgs.util.SAXAdapter;
import gov.usgs.util.XmlUtils;

/**
 * Parser for event addon messages.
 * 
 * Maps these messages into an EQMessage with a
 * product link.
 */
public class EventAddonParser extends SAXAdapter {

	/** Date format used in event addon message. */
	public static final SimpleDateFormat ADDON_DATE_FORMAT = new SimpleDateFormat(
			"yyyy/MM/dd_HH:mm:ss");

	/** The parsed addon object. */
	private EventAddon addon;

	public EQMessage parseMessage(EIDSMessageEvent event) throws Exception {
		// create a parser object for this message
		EventAddonParser parser = new EventAddonParser();

		try {
			// parse the message using the parser
			XmlUtils.parse(event.getMessage(), parser);
			// get the parsed addon
			EventAddon addon = parser.getAddon();
			// convert the parsed addon to an EQMessage
			return addon.getEQMessage();
		} catch (SAXException e) {
			if (!(e.getCause() instanceof ParseException)) {
				throw e;
			}
		}

		return null;
	}

	/**
	 * Data structure for event addon message. Also performs mapping onto
	 * EQMessage,Event, and ProductLink or Comment elements.
	 */
	public static class EventAddon {

		public String fileName;
		public String submitter;
		public Date submitTime;
		public String email;
		public String eventid;
		public String type;
		public String version;
		public String action;
		public String description;
		public String link;
		public String text;

		public EventAddon() {
		}

		/**
		 * Parse eventaddon xml into a ProductLink.
		 * 
		 * @return null, if there is no link.
		 */
		public ProductLink getProductLink() {
			if (this.link == null) {
				return null;
			}

			Action plAction = Action.UPDATE;
			if (action.equalsIgnoreCase("DEL")) {
				plAction = Action.DELETE;
			}

			ProductLink link = new ProductLink();
			link.setSourceKey(submitter);
			link.setTypeKey("LinkURL");
			link.setAction(plAction);
			link.setCode(type);
			link.setLink(this.link);
			link.setNote(text);
			link.setVersion(version);

			return link;
		}

		/**
		 * Parse eventaddon xml into a Comment.
		 * 
		 * @return null, if there is a link.
		 */
		public Comment getComment() {
			if (this.link != null) {
				return null;
			}

			Action cAction = Action.UPDATE;
			if (action.equalsIgnoreCase("DEL")) {
				cAction = Action.DELETE;
			}

			Comment comment = new Comment();
			comment.setSourceKey(submitter);
			comment.setTypeKey(type);
			comment.setAction(cAction);
			comment.setText(text);
			comment.setVersion(version);

			return comment;
		}

		public Event getEvent() {
			Event event = new Event();
			event.setDataSource(eventid.substring(0, 2));
			event.setEventID(eventid.substring(2));

			ProductLink link = getProductLink();
			if (link != null) {
				event.getProductLink().add(getProductLink());
			} else {
				Comment comment = getComment();
				event.getComment().add(comment);
			}

			return event;
		}

		public EQMessage getEQMessage() {
			EQMessage message = new EQMessage();
			message.setSent(submitTime);
			message.setSource(submitter);
			message.getEvent().add(getEvent());
			return message;
		}
	}

	/**
	 * Get parsed addon.
	 * 
	 * @return parsed addon, or null if nothing parsed.
	 */
	public EventAddon getAddon() {
		return addon;
	}

	/**
	 * SAXAdapter start element handler.
	 * 
	 * @param uri
	 *            element uri.
	 * @param localName
	 *            element localName.
	 * @param qName
	 *            element qName.
	 * @param attributes
	 *            element attributes.
	 * @throws SAXException
	 *             if there is an error.
	 */
	public void onStartElement(final String uri, final String localName,
			final String qName, final Attributes attributes)
			throws SAXException {

		if (localName.equals("eventaddon")) {
			addon = new EventAddon();
		} else if (localName.equals("file")) {
			addon.fileName = attributes.getValue("name");
		}
	}

	/**
	 * SAXAdapter end element handler. Content only includes characters that
	 * were read from this element, NOT any characters from child elements.
	 * 
	 * @param uri
	 *            element uri.
	 * @param localName
	 *            element localName.
	 * @param qName
	 *            element qName.
	 * @param content
	 *            element content.
	 * @throws SAXException
	 *             if onEndElement throws a SAXException.
	 */
	public void onEndElement(final String uri, final String localName,
			final String qName, final String content) throws SAXException {

		if (localName.equals("submitter")) {
			addon.submitter = content;
		} else if (localName.equals("email")) {
			addon.email = content;
		} else if (localName.equals("eventid")) {
			addon.eventid = content;
		} else if (localName.equals("type")) {
			addon.type = content;
		} else if (localName.equals("version")) {
			addon.version = content;
		} else if (localName.equals("action")) {
			addon.action = content;
		} else if (localName.equals("description")) {
			addon.description = content;
		} else if (localName.equals("link")) {
			addon.link = content;
		} else if (localName.equals("text")) {
			addon.text = content;
		} else if (localName.equals("submit_time")) {
			try {
				addon.submitTime = ADDON_DATE_FORMAT.parse(content);
			} catch (ParseException e) {
				throw new SAXException(e);
			}
		}
	}

}
