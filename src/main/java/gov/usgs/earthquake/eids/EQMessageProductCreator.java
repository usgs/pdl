package gov.usgs.earthquake.eids;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//Does it make sense to import objects from quakeml when we're parsing eqxml?
//import org.quakeml.FocalMechanism;
//import org.quakeml.NodalPlane;
//import org.quakeml.NodalPlanes;

import gov.usgs.ansseqmsg.Action;
import gov.usgs.ansseqmsg.Comment;
import gov.usgs.ansseqmsg.EQMessage;
import gov.usgs.ansseqmsg.EventAction;
import gov.usgs.ansseqmsg.EventScope;
import gov.usgs.ansseqmsg.EventType;
import gov.usgs.ansseqmsg.EventUsage;
import gov.usgs.ansseqmsg.Fault;
import gov.usgs.ansseqmsg.Magnitude;
import gov.usgs.ansseqmsg.Method;
import gov.usgs.ansseqmsg.MomentTensor;
import gov.usgs.ansseqmsg.NodalPlanes;
import gov.usgs.ansseqmsg.Origin;
import gov.usgs.ansseqmsg.Event;
import gov.usgs.ansseqmsg.ProductLink;
import gov.usgs.ansseqmsg.Tensor;
import gov.usgs.earthquake.cube.CubeAddon;
import gov.usgs.earthquake.cube.CubeDelete;
import gov.usgs.earthquake.cube.CubeEvent;
import gov.usgs.earthquake.cube.CubeMessage;
import gov.usgs.earthquake.eqxml.EQMessageParser;
import gov.usgs.earthquake.event.Converter;
import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

/**
 * Convert EQXML messages to Products.
 * 
 * <p>
 * Product source is EQMessage/Source.
 * </p>
 * <p>
 * Product type is "origin", "magnitude", or "addon". Types may be prefixed by
 * non-Public Event/Scope, and suffixed by non-Actual Event/Usage
 * (internal-magnitude-scenario).
 * </p>
 * <p>
 * Product code is Event/DataSource + Event/EventId. When an addon product,
 * either ProductLink/Code or Comment/TypeKey is appended to code.
 * </p>
 * <p>
 * Product updateTime is EQMessage/Sent.
 * </p>
 * 
 * <p>
 * Origin properties appear only on origin type products. Magnitude properties
 * appear on both magnitude and origin products.
 * </p>
 */
public class EQMessageProductCreator implements ProductCreator {

	private static final Logger LOGGER = Logger
			.getLogger(EQMessageProductCreator.class.getName());

	public static final String XML_CONTENT_TYPE = "application/xml";

	/** Path to content where source message is stored in created product. */
	public static final String EQMESSAGE_CONTENT_PATH = "eqxml.xml";
	public static final String CONTENTS_XML_PATH = "contents.xml";

	/**
	 * When phases exist is is a "phase" type product. When this flag is set to
	 * true, a lightweight, origin-only type product is also sent.
	 */
	private boolean sendOriginWhenPhasesExist = false;

	/**
	 * Whether to validate when parsing and serializing. When validating, only
	 * native EQXML is supported via the ProductCreator interface.
	 */
	private boolean validate = false;

	// the eqmessage currently being processed.
	private EQMessage eqmessage;

	// xml for the eqmessage currently being processed
	private String eqmessageXML;
	private String eqmessageSource;
	private Date eqmessageSent;

	private String eventDataSource;
	private String eventEventId;
	private String eventVersion;
	private EventAction eventAction;
	private EventUsage eventUsage;
	private EventScope eventScope;

	private BigDecimal originLatitude;
	private BigDecimal originLongitude;
	private BigDecimal originDepth;
	private Date originEventTime;

	private BigDecimal magnitude;

	/**
	 * Default, empty constructor.
	 */
	public EQMessageProductCreator() {
	}

	/**
	 * Get all the products contained in an EQMessage.
	 * 
	 * Same as getEQMessageProducts(message, null).
	 * 
	 * @param message
	 *            the EQMessage containing products.
	 * @return a list of created products.
	 * @throws Exception
	 */
	public synchronized List<Product> getEQMessageProducts(
			final EQMessage message) throws Exception {
		return getEQMessageProducts(message, null);
	}

	/**
	 * Get all the products contained in an EQMessage.
	 * 
	 * Parses rawEqxml string into an EQMessage, but preserves raw eqxml in
	 * created products.
	 * 
	 * Same as getEQMessageProducts(EQMessageParser.parse(rawEqxml), rawEqxml);
	 * 
	 * @param rawEqxml
	 *            the raw EQXML message.
	 * @return a list of created products.
	 * @throws Exception
	 */
	public synchronized List<Product> getEQMessageProducts(final String rawEqxml)
			throws Exception {
		EQMessage message = EQMessageParser.parse(rawEqxml, validate);
		return getEQMessageProducts(message, rawEqxml);
	}

	/**
	 * Get all the products contained in an EQMessage.
	 * 
	 * @param message
	 *            the EQMessage containing products.
	 * @param rawEqxml
	 *            the raw EQXML message. When null, an EQXML message is
	 *            serialized from the object.
	 * @return a list of created products.
	 * @throws Exception
	 */
	public synchronized List<Product> getEQMessageProducts(
			final EQMessage message, final String rawEqxml) throws Exception {
		List<Product> products = new LinkedList<Product>();

		if (message == null) {
			return products;
		}

		this.eqmessage = message;
		this.eqmessageSource = message.getSource();
		this.eqmessageSent = message.getSent();

		if (this.eqmessageSent == null) {
			this.eqmessageSent = new Date();
		}

		// convert to xml
		if (rawEqxml != null) {
			this.eqmessageXML = rawEqxml;
		} else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			EQMessageParser.serialize(message, baos, validate);
			this.eqmessageXML = baos.toString();
		}

		// process each event
		List<Event> events = message.getEvent();
		if (events != null) {
			Iterator<Event> iter = events.iterator();
			while (iter.hasNext()) {
				products.addAll(getEventProducts(iter.next()));
			}
		}

		this.eqmessageSource = null;
		this.eqmessageSent = null;
		this.eqmessageXML = null;

		return products;
	}

	/**
	 * Get products from an event.
	 * 
	 * @param event
	 *            the event containing products.
	 * @return a list of created products.
	 * @throws Exception
	 */
	protected synchronized List<Product> getEventProducts(final Event event)
			throws Exception {
		List<Product> products = new LinkedList<Product>();
		if (event == null) {
			return products;
		}

		eventDataSource = event.getDataSource();
		eventEventId = event.getEventID();
		eventVersion = event.getVersion();
		eventAction = event.getAction();
		eventUsage = event.getUsage();
		eventScope = event.getScope();

		// default values
		if (eventAction == null) {
			eventAction = EventAction.UPDATE;
		}
		if (eventUsage == null) {
			eventUsage = EventUsage.ACTUAL;
		}
		if (eventScope == null) {
			eventScope = EventScope.PUBLIC;
		}

		if (eventAction == EventAction.DELETE) {
			// delete origin product (only product with location)
			Product deleteProduct = getProduct("origin", eventAction.toString());
			products.add(deleteProduct);
		} else {
			// update origin product
			products.addAll(getOriginProducts(event.getOrigin(), event));
		}

		for (ProductLink eventLink : event.getProductLink()) {
			products.addAll(getProductLinkProducts(eventLink));
		}
		for (Comment eventComment : event.getComment()) {
			products.addAll(getCommentProducts(eventComment));
		}

		eventDataSource = null;
		eventEventId = null;
		eventVersion = null;
		eventAction = null;
		eventUsage = null;
		eventScope = null;

		return products;
	}

	/**
	 * Get origin product(s).
	 * 
	 * This implementation only creates one origin (the first one) regardless of
	 * how many origins are provided.
	 * 
	 * @param origins
	 *            the list of origins.
	 * @return a list of created products.
	 * @throws Exception
	 */
	protected synchronized List<Product> getOriginProducts(
			final List<Origin> origins, final Event event) throws Exception {
		List<Product> products = new LinkedList<Product>();
		if (origins == null || origins.size() == 0) {
			return products;
		}

		// only process first origin
		Origin origin = origins.get(0);

		// get sub-products
		products.addAll(getFocalMechanismProducts(origin.getMomentTensor()));

		this.originLatitude = origin.getLatitude();
		this.originLongitude = origin.getLongitude();
		this.originDepth = origin.getDepth();
		this.originEventTime = origin.getTime();

		boolean preferred = (origin.getPreferredFlag() == null || origin
				.getPreferredFlag());

		// only process "preferred" origins
		// this is how hydra differentiates between origins as input parameters
		// to focal mechanisms, and origins as origins
		if (preferred && this.originLatitude != null
				&& this.originLongitude != null && this.originEventTime != null) {
			// create an origin/magnitude product only if origin has
			// lat+lon+time

			List<Product> magnitudeProducts = getMagnitudeProducts(origin
					.getMagnitude());

			// now build origin product
			Action originAction = origin.getAction();
			Product originProduct = getProduct("origin",
					originAction == null ? null : originAction.toString());
			// origin specific properties
			Map<String, String> properties = originProduct.getProperties();

			// set event type
			properties.put(
					"event-type",
					(event.getType() == null ? EventType.EARTHQUAKE : event
							.getType()).value().toLowerCase());

			if (magnitudeProducts.size() > 0) {
				// transfer magnitude product properties to origin
				properties.putAll(magnitudeProducts.get(0).getProperties());
			}

			if (origin.getSourceKey() != null) {
				properties.put("origin-source", origin.getSourceKey());
			}
			if (origin.getAzimGap() != null) {
				properties.put("azimuthal-gap", origin.getAzimGap().toString());
			}
			if (origin.getDepthError() != null) {
				properties
						.put("depth-error", origin.getDepthError().toString());
			}
			if (origin.getDepthMethod() != null) {
				properties.put("depth-method", origin.getDepthMethod());
			}
			if (origin.getErrh() != null) {
				properties.put("horizontal-error", origin.getErrh().toString());
			}
			if (origin.getErrz() != null) {
				properties.put("vertical-error", origin.getErrz().toString());
			}
			if (origin.getLatError() != null) {
				properties.put("latitude-error", origin.getLatError()
						.toString());
			}
			if (origin.getLonError() != null) {
				properties.put("longitude-error", origin.getLonError()
						.toString());
			}
			if (origin.getMinDist() != null) {
				properties.put("minimum-distance", origin.getMinDist()
						.toString());
			}
			if (origin.getNumPhaUsed() != null) {
				properties.put("num-phases-used", origin.getNumPhaUsed()
						.toString());
			}
			if (origin.getNumStaUsed() != null) {
				properties.put("num-stations-used", origin.getNumStaUsed()
						.toString());
			}
			if (origin.getRegion() != null) {
				properties.put("region", origin.getRegion());
			}
			if (origin.getStatus() != null) {
				properties.put("review-status", origin.getStatus().toString());
			}
			if (origin.getStdError() != null) {
				properties.put("standard-error", origin.getStdError()
						.toString());
			}

			// origin method
			Iterator<Method> methods = origin.getMethod().iterator();
			if (methods.hasNext()) {
				Method method = methods.next();
				if (method.getClazz() != null) {
					properties.put("location-method-class", method.getClazz());
				}
				if (method.getAlgorithm() != null) {
					properties.put("location-method-algorithm",
							method.getAlgorithm());
				}
				if (method.getModel() != null) {
					properties.put("location-method-model", method.getModel());
				}

				String cubeLocationMethod = getCubeCode(method.getComment());
				if (cubeLocationMethod != null) {
					properties.put("cube-location-method", cubeLocationMethod);
				}
			}

			if (origin.getPhase() != null && origin.getPhase().size() > 0) {
				originProduct.getId().setType("phase-data");
				products.add(originProduct);

				if (sendOriginWhenPhasesExist) {
					// create lightweight origin product
					Product lightweightOrigin = new Product(originProduct);
					lightweightOrigin.getId().setType("origin");
					lightweightOrigin.getContents().remove(
							EQMESSAGE_CONTENT_PATH);

					// seek and destroy phases
					Iterator<Origin> iter = origins.iterator();
					while (iter.hasNext()) {
						Origin next = iter.next();
						if (next.getPhase() != null) {
							next.getPhase().clear();
						}
					}

					// serialize xml without phase data
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					EQMessageParser.serialize(this.eqmessage, baos, validate);
					lightweightOrigin.getContents().put(EQMESSAGE_CONTENT_PATH,
							new ByteContent(baos.toByteArray()));

					products.add(0, lightweightOrigin);
				}
			} else {
				// insert origin at start of list
				products.add(0, originProduct);
			}
		}

		this.originDepth = null;
		this.originEventTime = null;
		this.originLatitude = null;
		this.originLongitude = null;
		this.magnitude = null;

		for (Comment originComment : origin.getComment()) {
			products.addAll(getCommentProducts(originComment));
		}

		return products;
	}

	/**
	 * Build magnitude products.
	 * 
	 * This implementation builds at most one magnitude product (the first).
	 * 
	 * @param magnitudes
	 *            a list of candidate magsnitude objects.
	 * @return a list of built magnitude products, which may be empty.
	 */
	protected synchronized List<Product> getMagnitudeProducts(
			final List<Magnitude> magnitudes) {
		List<Product> products = new LinkedList<Product>();
		if (magnitudes == null || magnitudes.size() == 0) {
			return products;
		}

		// build product based on the first magnitude
		Magnitude magnitude = magnitudes.get(0);
		// set "globalish" property before getProduct()
		this.magnitude = magnitude.getValue();

		Action magnitudeAction = magnitude.getAction();
		// build magnitude product
		Product product = getProduct("magnitude",
				magnitudeAction == null ? null : magnitudeAction.toString());

		Map<String, String> properties = product.getProperties();
		if (magnitude.getSourceKey() != null) {
			properties.put("magnitude-source", magnitude.getSourceKey());
		}
		if (magnitude.getTypeKey() != null) {
			properties.put("magnitude-type", magnitude.getTypeKey());
		}
		if (magnitude.getAzimGap() != null) {
			properties.put("magnitude-azimuthal-gap", magnitude.getAzimGap()
					.toString());
		}
		if (magnitude.getError() != null) {
			properties.put("magnitude-error", magnitude.getError().toString());
		}
		if (magnitude.getNumStations() != null) {
			properties.put("magnitude-num-stations-used", magnitude
					.getNumStations().toString());
		}

		String cubeMagnitudeType = getCubeCode(magnitude.getComment());
		if (cubeMagnitudeType != null) {
			properties.put("cube-magnitude-type", cubeMagnitudeType);
		}

		// don't clear property here, so origin can borrow magnitude property
		// this.magnitude = null;

		products.add(product);
		return products;
	}

	protected synchronized List<Product> getFocalMechanismProducts(
			final List<MomentTensor> momentTensors) {
		List<Product> products = new LinkedList<Product>();
		if (momentTensors == null || momentTensors.size() == 0) {
			return products;
		}

		// build moment tensors
		Iterator<MomentTensor> iter = momentTensors.iterator();
		while (iter.hasNext()) {
			MomentTensor mt = iter.next();

			Action mtAction = mt.getAction();
			// may be set to "moment-tensor" below
			Product product = getProduct("focal-mechanism",
					mtAction == null ? null : mtAction.toString());

			Map<String, String> properties = product.getProperties();

			// fill in product properties
			if (mt.getSourceKey() != null) {
				properties.put("beachball-source", mt.getSourceKey());
			}
			if (mt.getTypeKey() != null) {
				properties.put("beachball-type", mt.getTypeKey());
				// append source+type to code
				ProductId productId = product.getId();
				productId.setCode((productId.getCode() + "-"
						+ mt.getSourceKey() + "-" + mt.getTypeKey())
						.toLowerCase());
			}
			if (mt.getMagMw() != null) {
				product.setMagnitude(mt.getMagMw());
			}
			if (mt.getM0() != null) {
				properties.put("scalar-moment", mt.getM0().toString());
			}

			if (mt.getTensor() != null) {
				// if the tensor is included, it is a "moment-tensor" instead of
				// a "focal-mechanism"
				product.getId().setType("moment-tensor");

				Tensor t = mt.getTensor();
				if (t.getMtt() != null) {
					properties.put("tensor-mtt", t.getMtt().toString());
				}
				if (t.getMpp() != null) {
					properties.put("tensor-mpp", t.getMpp().toString());
				}
				if (t.getMrr() != null) {
					properties.put("tensor-mrr", t.getMrr().toString());
				}
				if (t.getMtp() != null) {
					properties.put("tensor-mtp", t.getMtp().toString());
				}
				if (t.getMrp() != null) {
					properties.put("tensor-mrp", t.getMrp().toString());
				}
				if (t.getMrt() != null) {
					properties.put("tensor-mrt", t.getMrt().toString());
				}
			}

			if (mt.getNodalPlanes() != null) {
				NodalPlanes np = mt.getNodalPlanes();
				List<Fault> faults = np.getFault();
				if (faults.size() == 2) {
					Fault fault1 = faults.get(0);
					if (fault1.getDip() != null) {
						properties.put("nodal-plane-1-dip", fault1.getDip()
								.toString());
					}
					if (fault1.getSlip() != null) {
						properties.put("nodal-plane-1-slip", fault1.getSlip()
								.toString());
					}
					if (fault1.getStrike() != null) {
						properties.put("nodal-plane-1-strike", fault1
								.getStrike().toString());
					}
					Fault fault2 = faults.get(1);
					if (fault2.getDip() != null) {
						properties.put("nodal-plane-2-dip", fault2.getDip()
								.toString());
					}
					if (fault2.getSlip() != null) {
						properties.put("nodal-plane-2-slip", fault2.getSlip()
								.toString());
					}
					if (fault2.getStrike() != null) {
						properties.put("nodal-plane-2-strike", fault2
								.getStrike().toString());
					}
				}
			}

			if (mt.getDerivedOriginTime() != null) {
				properties.put("derived-eventtime",
						XmlUtils.formatDate(mt.getDerivedOriginTime()));
			}
			if (mt.getDerivedLatitude() != null) {
				properties.put("derived-latitude", mt.getDerivedLatitude()
						.toString());
			}
			if (mt.getDerivedLongitude() != null) {
				properties.put("derived-longitude", mt.getDerivedLongitude()
						.toString());
			}
			if (mt.getDerivedDepth() != null) {
				properties
						.put("derived-depth", mt.getDerivedDepth().toString());
			}

			if (mt.getPerDblCpl() != null) {
				properties.put("percent-double-couple", mt.getPerDblCpl()
						.toString());
			}
			if (mt.getNumObs() != null) {
				properties.put("num-stations-used", mt.getNumObs().toString());
			}

			// attach original message as product content
			ByteContent xml = new ByteContent(eqmessageXML.getBytes());
			xml.setLastModified(eqmessageSent);
			xml.setContentType("application/xml");
			product.getContents().put(EQMESSAGE_CONTENT_PATH, xml);

			// add to list of built products
			products.add(product);
		}

		return products;
	}

	/**
	 * Get product(s) from a ProductLink object.
	 * 
	 * 
	 * @param link
	 *            the link object.
	 * @return a list of found products.
	 * @throws Exception
	 */
	protected synchronized List<Product> getProductLinkProducts(
			final ProductLink link) throws Exception {
		List<Product> products = new LinkedList<Product>();

		String linkType = getLinkAddonProductType(link.getCode());
		if (linkType == null) {
			LOGGER.finer("No product type found for productlink with code '"
					+ link.getCode() + "', skipping");
			return products;
		}

		Action linkAction = link.getAction();
		Product linkProduct = getProduct(linkType, linkAction == null ? null
				: linkAction.toString());
		// remove the EQXML, only send product link attributes with link
		// products
		linkProduct.getContents().clear();

		// add addon code to product code
		ProductId id = linkProduct.getId();
		id.setCode(id.getCode() + "-" + link.getCode().toLowerCase());

		if (link.getVersion() != null) {
			linkProduct.setVersion(link.getVersion());
		}

		Map<String, String> properties = linkProduct.getProperties();
		if (link.getLink() != null) {
			properties.put("url", link.getLink());
		}
		if (link.getNote() != null) {
			properties.put("text", link.getNote());
		}
		if (link.getCode() != null) {
			properties.put("addon-code", link.getCode());
		}
		properties.put("addon-type", link.getTypeKey());

		products.add(linkProduct);
		return products;
	}

	/**
	 * Get product(s) from a Comment object.
	 * 
	 * @param comment
	 *            the comment object.
	 * @return a list of found products.
	 * @throws Exception
	 */
	protected synchronized List<Product> getCommentProducts(
			final Comment comment) throws Exception {
		List<Product> products = new LinkedList<Product>();

		// CUBE_Codes are attributes of the containing product.
		String typeKey = comment.getTypeKey();
		if (typeKey != null && !typeKey.equals("CUBE_Code")) {
			String commentType = getTextAddonProductType(typeKey);
			if (commentType == null) {
				LOGGER.finer("No product type found for comment with type '"
						+ comment.getTypeKey() + "'");
				return products;
			}

			Action commentAction = comment.getAction();
			Product commentProduct = getProduct(commentType,
					commentAction == null ? null : commentAction.toString());
			// remove the EQXML, only send comment text with comment products
			commentProduct.getContents().clear();

			// one product per comment type
			ProductId id = commentProduct.getId();
			id.setCode(id.getCode() + "_" + comment.getTypeKey().toLowerCase());

			Map<String, String> properties = commentProduct.getProperties();
			properties.put("addon-type", "comment");
			if (comment.getTypeKey() != null) {
				properties.put("code", comment.getTypeKey());
			}

			// store the comment text as content instead of a property, it may
			// contain newlines
			commentProduct.getContents().put("",
					new ByteContent(comment.getText().getBytes()));
			products.add(commentProduct);
		}

		return products;
	}

	/**
	 * Build a product skeleton based on the current state.
	 * 
	 * Product type is : [internal-](origin,magnitude,addon)[-(scenario|test)]
	 * where the optional scope is not "Public", and the optional usage is not
	 * "Actual".
	 * 
	 * @param type
	 *            short product type, like "origin", "magnitude".
	 * @param action
	 *            override the global message action.
	 * @return a Product so that properties and content can be added.
	 */
	protected synchronized Product getProduct(final String type,
			final String action) {

		String productType = type;
		// prepend type with non Public scopes (Internal)
		if (eventScope != EventScope.PUBLIC) {
			productType = eventScope.toString() + "-" + productType;
		}
		// append to type with non Actual usages
		if (eventUsage != EventUsage.ACTUAL) {
			productType = productType + "-" + eventUsage.toString();
		}
		// make it all lower case
		productType = productType.toLowerCase();

		// use event id
		String productCode = eventDataSource + eventEventId;
		productCode = productCode.toLowerCase();

		ProductId id = new ProductId(eqmessageSource.toLowerCase(),
				productType, productCode, eqmessageSent);
		Product product = new Product(id);

		// figure out whether this is a delete
		String productAction = action;
		if (productAction == null) {
			productAction = eventAction.toString();
			if (productAction == null) {
				productAction = "Update";
			}
		}
		String productStatus;
		if (productAction.equalsIgnoreCase("Delete")) {
			productStatus = Product.STATUS_DELETE;
		} else {
			productStatus = Product.STATUS_UPDATE;
		}
		product.setStatus(productStatus);

		if (eventDataSource != null && eventEventId != null) {
			product.setEventId(eventDataSource, eventEventId);
		}
		if (originEventTime != null) {
			product.setEventTime(originEventTime);
		}
		if (originLongitude != null) {
			product.setLongitude(originLongitude);
		}
		if (originLatitude != null) {
			product.setLatitude(originLatitude);
		}
		if (originDepth != null) {
			product.setDepth(originDepth);
		}
		if (magnitude != null) {
			product.setMagnitude(magnitude);
		}
		if (eventVersion != null) {
			product.setVersion(eventVersion);
		}

		/*
		 * Map<String, String> properties = product.getProperties(); if
		 * (eventUsage != null) { properties.put("eqxml-usage",
		 * eventUsage.toString()); } if (eventScope != null) {
		 * properties.put("eqxml-scope", eventScope.toString()); } if
		 * (eventAction != null) { properties.put("eqxml-action",
		 * eventAction.toString()); }
		 */

		ByteContent xml = new ByteContent(eqmessageXML.getBytes());
		xml.setLastModified(eqmessageSent);
		product.getContents().put(EQMESSAGE_CONTENT_PATH, xml);

		// add contents.xml to product to describe above content
		product.getContents().put(CONTENTS_XML_PATH, getContentsXML());

		return product;
	}

	protected Content getContentsXML() {
		StringBuffer buf = new StringBuffer();
		buf.append("<?xml version=\"1.0\"?>\n");
		buf.append("<contents xmlns=\"http://earthquake.usgs.gov/earthquakes/event/contents\">\n");
		buf.append("<file title=\"Earthquake XML (EQXML)\" id=\"eqxml\">\n");
		buf.append("<format type=\"xml\" href=\"")
				.append(EQMESSAGE_CONTENT_PATH).append("\"/>\n");
		buf.append("</file>\n");
		buf.append("<page title=\"Location\" slug=\"location\">\n");
		buf.append("<file refid=\"eqxml\"/>\n");
		buf.append("</page>\n");
		buf.append("</contents>\n");

		ByteContent content = new ByteContent(buf.toString().getBytes());
		content.setLastModified(eqmessageSent);
		// this breaks things
		// content.setContentType("application/xml");
		return content;
	}

	/**
	 * Extract a CUBE_Code from a Comment.
	 * 
	 * This is the ISTI convention for preserving CUBE information in EQXML
	 * messages. Checks a list of Comment objects for one with
	 * TypeKey="CUBE_Code" and Text="CUBE_Code X", where X is the returned cube
	 * code.
	 * 
	 * @param comments
	 *            the list of comments.
	 * @return the cube code, or null if not found.
	 */
	protected synchronized String getCubeCode(final List<Comment> comments) {
		String cubeCode = null;

		if (comments != null) {
			Iterator<Comment> iter = comments.iterator();
			while (iter.hasNext()) {
				Comment comment = iter.next();
				if (comment.getTypeKey().equals("CUBE_Code")) {
					cubeCode = comment.getText().replace("CUBE_Code ", "");
					break;
				}
			}
		}

		return cubeCode;
	}

	public boolean isSendOriginWhenPhasesExist() {
		return sendOriginWhenPhasesExist;
	}

	public void setSendOriginWhenPhasesExist(boolean sendOriginWhenPhasesExist) {
		this.sendOriginWhenPhasesExist = sendOriginWhenPhasesExist;
	}

	@Override
	public boolean isValidate() {
		return validate;
	}

	@Override
	public void setValidate(boolean validate) {
		this.validate = validate;
	}

	@Override
	public List<Product> getProducts(File file) throws Exception {
		EQMessage eqxml = null;
		String content = new String(FileUtils.readFile(file));
		String rawEqxml = null;

		// try to read eqxml
		try {
			eqxml = EQMessageParser.parse(
					StreamUtils.getInputStream(content.getBytes()), validate);
			rawEqxml = content;
		} catch (Exception e) {
			if (validate) {
				throw e;
			}

			// try to read cube
			try {
				Converter converter = new Converter();
				CubeMessage cube = converter.getCubeMessage(content);
				eqxml = converter.getEQMessage(cube);
			} catch (Exception e2) {
				if (content.startsWith(CubeEvent.TYPE) ||
						content.startsWith(CubeDelete.TYPE) ||
						content.startsWith(CubeAddon.TYPE)) {
					// throw cube parsing exception
					throw e2;
				} else {
					// log cube parsing exception
					LOGGER.log(Level.FINE, "Unable to parse cube message", e2);
				}

				// try to read eventaddon xml
				try {
					EventAddonParser parser = new EventAddonParser();
					parser.parse(content);
					eqxml = parser.getAddon().getEQMessage();
				} catch (Exception e3) {
					// log eventaddon parsing exception
					LOGGER.log(Level.FINE, "Unable to parse eventaddon", e3);
					// throw original exception
					throw e;
				}
			}
		}

		return this.getEQMessageProducts(eqxml, rawEqxml);
	}

	public static final String GENERAL_TEXT_TYPE = "general-text";
	public static final String[] GENERAL_TEXT_ADDONS = new String[] {};

	public static final String SCITECH_TEXT_TYPE = "scitech-text";
	public static final String[] SCITECH_TEXT_ADDONS = new String[] {};

	public static final String IMPACT_TEXT_TYPE = "impact-text";
	public static final String[] IMPACT_TEXT_ADDONS = new String[] { "feltreports" };

	/** Selected link type products have a mapping. */
	public static final String GENERAL_LINK_TYPE = "general-link";
	public static final String[] GENERAL_LINK_ADDONS = new String[] {
			"aftershock", "afterwarn", "asw", "generalmisc" };

	public static final String SCITECH_LINK_TYPE = "scitech-link";
	public static final String[] SCITECH_LINK_ADDONS = new String[] { "energy",
			"focalmech", "ncfm", "histmomenttensor", "finitefault",
			"momenttensor", "mtensor", "phase", "seiscrosssec", "seisrecsec",
			"traveltimes", "waveform", "seismograms", "scitechmisc" };

	public static final String IMPACT_LINK_TYPE = "impact-link";
	public static final String[] IMPACT_LINK_ADDONS = new String[] {
			"tsunamilinks", "impactmisc" };

	/**
	 * Map from cube style link addon to product type.
	 * 
	 * @param addonType
	 * @return null if link should not be converted to a product.
	 */
	public String getLinkAddonProductType(final String addonType) {
		String c = addonType.toLowerCase();

		for (String general : GENERAL_LINK_ADDONS) {
			if (c.startsWith(general)) {
				return GENERAL_LINK_TYPE;
			}
		}

		for (String scitech : SCITECH_LINK_ADDONS) {
			if (c.startsWith(scitech)) {
				return SCITECH_LINK_TYPE;
			}
		}

		for (String impact : IMPACT_LINK_ADDONS) {
			if (c.startsWith(impact)) {
				return IMPACT_LINK_TYPE;
			}
		}

		return null;
	}

	/**
	 * Map from cube style text addon to product type.
	 * 
	 * @param addonType
	 * @return null if comment should not be converted to a product.
	 */
	public String getTextAddonProductType(final String addonType) {
		String c = addonType.toLowerCase();

		for (String general : GENERAL_TEXT_ADDONS) {
			if (c.startsWith(general)) {
				return GENERAL_TEXT_TYPE;
			}
		}

		for (String impact : IMPACT_TEXT_ADDONS) {
			if (c.startsWith(impact)) {
				return IMPACT_TEXT_TYPE;
			}
		}

		for (String scitech : SCITECH_TEXT_ADDONS) {
			if (c.startsWith(scitech)) {
				return SCITECH_TEXT_TYPE;
			}
		}

		return null;
	}

}
