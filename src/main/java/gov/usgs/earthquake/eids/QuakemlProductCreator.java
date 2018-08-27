package gov.usgs.earthquake.eids;

import gov.usgs.earthquake.event.Converter;
import gov.usgs.earthquake.product.ByteContent;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductId;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.earthquake.quakeml.FileToQuakemlConverter;
import gov.usgs.util.StreamUtils;
import gov.usgs.util.XmlUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.quakeml_1_2.Axis;
import org.quakeml_1_2.ConfidenceEllipsoid;
import org.quakeml_1_2.CreationInfo;
import org.quakeml_1_2.EvaluationMode;
import org.quakeml_1_2.Event;
import org.quakeml_1_2.EventDescription;
import org.quakeml_1_2.EventDescriptionType;
import org.quakeml_1_2.EventParameters;
import org.quakeml_1_2.EventType;
import org.quakeml_1_2.FocalMechanism;
import org.quakeml_1_2.InternalEvent;
import org.quakeml_1_2.Magnitude;
import org.quakeml_1_2.MomentTensor;
import org.quakeml_1_2.NodalPlane;
import org.quakeml_1_2.NodalPlanes;
import org.quakeml_1_2.Origin;
import org.quakeml_1_2.OriginQuality;
import org.quakeml_1_2.OriginUncertainty;
import org.quakeml_1_2.OriginUncertaintyDescription;
import org.quakeml_1_2.Quakeml;
import org.quakeml_1_2.RealQuantity;
import org.quakeml_1_2.ScenarioEvent;
import org.quakeml_1_2.SourceTimeFunction;
import org.quakeml_1_2.SourceTimeFunctionType;
import org.quakeml_1_2.Tensor;
import org.quakeml_1_2.TimeQuantity;
import org.quakeml_1_2.PrincipalAxes;
import org.quakeml_1_2.EvaluationStatus;

/**
 * Create Products from ANSS Quakeml files.
 */
public class QuakemlProductCreator implements ProductCreator {

	public static final Logger LOGGER = Logger
			.getLogger(QuakemlProductCreator.class.getName());

	public static final String XML_CONTENT_TYPE = "application/xml";
	public static final String QUAKEML_CONTENT_PATH = "quakeml.xml";
	public static final String CONTENTS_XML_PATH = "contents.xml";

	public static final BigDecimal METERS_PER_KILOMETER = new BigDecimal("1000");

	public static final String VERSION = "1.0";

	/**
	 * When phases exist it is a "phase" type product. When this flag is set to
	 * true, a lightweight, origin-only type product (without bulky phase data)
	 * is also sent.
	 */
	private boolean sendOriginWhenPhasesExist = false;
	private boolean sendMechanismWhenPhasesExist = false;

	// xml for the eqmessage currently being processed
	private String quakemlXML;
	private CreationInfo eventParametersCreationInfo;

	// attributes of current quakeml being processed
	private String productSource;
	private String productCode;
	private String eventSource;
	private String eventCode;
	private Date updateTime;

	private FileToQuakemlConverter converter = null;
	private Converter formatConverter = new Converter();
	private boolean validate = false;

	public List<Product> getQuakemlProducts(final Quakeml message)
			throws Exception {
		return getQuakemlProducts(message, null);
	}

	public List<Product> getQuakemlProducts(final String message)
			throws Exception {
		Quakeml quakeml = formatConverter.getQuakeml(message, validate);
		return getQuakemlProducts(quakeml, message);
	}

	/**
	 * Get products in a quakeml message.
	 *
	 * @param message
	 *            the parsed quakeml message.
	 * @param rawQuakeml
	 *            bytes of quakeml message. If null, the quakeml object will be
	 *            serialized into xml. This parameter is used to preserve the
	 *            original input, instead of always serializing from the quakeml
	 *            object.
	 * @return list of products generated from quakeml message.
	 * @throws Exception
	 */
	public List<Product> getQuakemlProducts(final Quakeml message,
			final String rawQuakeml) throws Exception {
		List<Product> products = new ArrayList<Product>();

		// serialize for embedding in product
		quakemlXML = rawQuakeml;
		if (quakemlXML == null) {
			quakemlXML = new Converter().getString(message, validate);
		}

		EventParameters eventParameters = message.getEventParameters();
		eventParametersCreationInfo = eventParameters.getCreationInfo();

		// only process first event
		if (eventParameters.getEvents().size() > 0) {
			// found actual event
			products.addAll(getEventProducts(message, eventParameters
					.getEvents().get(0)));
		} else {
			// check for internal/scenario events
			List<Object> anies = eventParameters.getAnies();
			Iterator<Object> anyIter = anies.iterator();
			while (anyIter.hasNext()) {
				Object next = anyIter.next();
				if (next instanceof InternalEvent) {
					// found internal event
					products.addAll(getInternalEventProducts(message,
							(InternalEvent) next));
					break;
				} else if (next instanceof ScenarioEvent) {
					// found scenario event
					products.addAll(getScenarioEventProducts(message,
							(ScenarioEvent) next));
					break;
				}
			}
		}

		// add property to all products to indicate they all come from the same
		// eventParameters "envelope"
		String eventParametersPublicID = eventParameters.getPublicID();
		Iterator<Product> productIter = products.iterator();
		while (productIter.hasNext()) {
			Product next = productIter.next();
			setProperty(next.getProperties(), "eventParametersPublicID",
					eventParametersPublicID);
		}

		eventParametersCreationInfo = null;
		quakemlXML = null;

		return products;
	}

	/**
	 * Get internal products in quakeml event element.
	 *
	 * Calls {@link #getEventProducts(Quakeml, Event)}, and adds "internal-"
	 * prefix to each type in the returned list of products.
	 *
	 * @param message
	 *            the quakeml message.
	 * @param event
	 *            the internal event element.
	 * @return list of internal products found in event element, may be empty.
	 * @throws Exception
	 */
	public List<Product> getInternalEventProducts(final Quakeml message,
			final InternalEvent event) throws Exception {
		List<Product> products = getEventProducts(message, event);
		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			ProductId nextId = iter.next().getId();
			nextId.setType("internal-" + nextId.getType());
		}
		return products;
	}

	/**
	 * Get scenario products in quakeml event element.
	 *
	 * Calls {@link #getEventProducts(Quakeml, Event)}, and adds "-scenario"
	 * suffix to each type in the returned list of products.
	 *
	 * @param message
	 *            the quakeml message.
	 * @param event
	 *            the scenario event element.
	 * @return list of scenario products found in event element, may be empty.
	 * @throws Exception
	 */
	public List<Product> getScenarioEventProducts(final Quakeml message,
			final ScenarioEvent event) throws Exception {
		List<Product> products = getEventProducts(message, event);
		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			ProductId nextId = iter.next().getId();
			nextId.setType(nextId.getType() + "-scenario");
		}
		return products;
	}

	/**
	 * Get products in quakeml event element.
	 *
	 * @param message
	 *            the quakeml message.
	 * @param event
	 *            the event element in the quakeml message.
	 * @return list of products found in event element, may be empty.
	 * @throws Exception
	 */
	public List<Product> getEventProducts(final Quakeml message, Event event)
			throws Exception {
		List<Product> products = new ArrayList<Product>();

		// read catalog attributes for product source and code, and event source
		// and code
		productSource = event.getDatasource();
		productCode = event.getDataid();
		eventSource = event.getEventsource();
		eventCode = event.getEventid();

		if (productSource == null || eventSource == null || eventCode == null) {
			LOGGER.warning("Missing catalog attributes from event element, skipping");
			// not anss information, don't convert to products
			return products;
		} else if (productCode == null) {
			productCode = eventSource + eventCode;
		}
		productSource = productSource.toLowerCase();

		// product update time
		updateTime = null;
		if (eventParametersCreationInfo != null) {
			updateTime = eventParametersCreationInfo.getCreationTime();
		} else {
			LOGGER.warning("Missing eventParameters creationTime, using now for update time");
			updateTime = new Date();
		}

		ByteContent quakemlContent = new ByteContent(quakemlXML.getBytes());
		quakemlContent.setContentType(XML_CONTENT_TYPE);
		quakemlContent.setLastModified(updateTime);

		boolean hasPhaseData = QuakemlUtils.hasPhaseData(event);

		// create this object, may go unused
		Product originProduct = new Product(new ProductId(productSource,
				(hasPhaseData ? "phase-data" : "origin"), productCode,
				updateTime));
		originProduct.setEventId(eventSource, eventCode);
		if (event.getCreationInfo() != null) {
			originProduct.setVersion(event.getCreationInfo().getVersion());
		}
		originProduct.getContents().put(QUAKEML_CONTENT_PATH, quakemlContent);
		originProduct.getContents().put(CONTENTS_XML_PATH, getContentsXML());

		// track which event this product is from
		setProperty(originProduct.getProperties(), "quakeml-publicid",
				event.getPublicID());

		// delete origin product
		if (event.getType() == EventType.NOT_EXISTING) {
			originProduct.setStatus(Product.STATUS_DELETE);
			products.add(originProduct);

			Product phaseDelete = new Product(originProduct);
			phaseDelete.getId().setType("phase-data");
			products.add(phaseDelete);

			// don't need to delete other stuff
			// when origins for event are deleted, event is deleted
			return products;
		}

		Origin origin = QuakemlUtils.getPreferredOrigin(event);
		if (origin != null) {
			// track which origin this product is from
			setProperty(originProduct.getProperties(), "quakeml-origin-publicid",
					origin.getPublicID());

			// update origin product
			Map<String, String> properties = originProduct.getProperties();

			TimeQuantity eventTime = origin.getTime();
			if (eventTime != null) {
				originProduct.setEventTime(eventTime.getValue());
				setProperty(properties, "eventtime-error",
						eventTime.getUncertainty());
			}
			RealQuantity eventLatitude = origin.getLatitude();
			if (eventLatitude != null) {
				originProduct.setLatitude(eventLatitude.getValue());
				setProperty(properties, "latitude-error",
						eventLatitude.getUncertainty());
			}
			RealQuantity eventLongitude = origin.getLongitude();
			if (eventLongitude != null) {
				originProduct.setLongitude(eventLongitude.getValue());
				setProperty(properties, "longitude-error",
						eventLongitude.getUncertainty());
			}
			RealQuantity depth = origin.getDepth();
			if (depth != null) {
				originProduct.setDepth(depth.getValue().divide(
						METERS_PER_KILOMETER));
				if (depth.getUncertainty() != null) {
					setProperty(properties, "vertical-error", depth
							.getUncertainty().divide(METERS_PER_KILOMETER));
				}
				if (origin.getDepthType() != null) {
					setProperty(properties, "depth-type", origin.getDepthType()
							.value());
				}
			}

			// read horizontal error
			OriginUncertainty originUncertainty = origin.getOriginUncertainty();
			if (originUncertainty != null) {
				if (originUncertainty.getHorizontalUncertainty() != null) {
					setProperty(properties, "horizontal-error",
							originUncertainty.getHorizontalUncertainty()
									.divide(METERS_PER_KILOMETER));
				} else if (originUncertainty.getPreferredDescription() == OriginUncertaintyDescription.HORIZONTAL_UNCERTAINTY) {
					throw new IllegalArgumentException(
							"Missing horizontal uncertainty value");
				}

				ConfidenceEllipsoid ellipse = originUncertainty.getConfidenceEllipsoid();
				if (ellipse != null) {
					setProperty(properties, "error-ellipse-azimuth",
							ellipse.getMajorAxisAzimuth());
					setProperty(properties, "error-ellipse-plunge",
							ellipse.getMajorAxisPlunge());
					setProperty(properties, "error-ellipse-rotation",
							ellipse.getMajorAxisRotation());
					setProperty(properties, "error-ellipse-major",
							ellipse.getSemiMajorAxisLength());
					setProperty(properties, "error-ellipse-minor",
							ellipse.getSemiMinorAxisLength());
					setProperty(properties, "error-ellipse-intermediate",
							ellipse.getSemiIntermediateAxisLength());
				}
			}

			EventType eventType = event.getType();
			properties.put("event-type",
					((eventType == null) ? EventType.EARTHQUAKE : eventType)
							.value());

			CreationInfo originCreationInfo = origin.getCreationInfo();
			if (originCreationInfo != null) {
				setProperty(properties, "origin-source",
						originCreationInfo.getAgencyID());
			}

			OriginQuality originQuality = origin.getQuality();
			if (originQuality != null) {
				setProperty(properties, "azimuthal-gap",
						originQuality.getAzimuthalGap());
				setProperty(properties, "num-phases-used",
						originQuality.getUsedPhaseCount());
				setProperty(properties, "num-stations-used",
						originQuality.getUsedStationCount());
				setProperty(properties, "minimum-distance",
						originQuality.getMinimumDistance());
				setProperty(properties, "standard-error",
						originQuality.getStandardError());
			}

			if (origin.getEvaluationMode() == EvaluationMode.MANUAL) {
				properties.put("review-status", "reviewed");
			} else {
				properties.put("review-status", "automatic");
			}

			if (origin.getEvaluationStatus() != null) {
				properties.put("evaluation-status", origin
						.getEvaluationStatus().value());
			} else {
				properties.put("evaluation-status",
						EvaluationStatus.PRELIMINARY.value());
			}

			// add magnitude properties
			Magnitude magnitude = QuakemlUtils.getPreferredMagnitude(event);
			if (magnitude != null) {
				// track which origin this product is from
				setProperty(originProduct.getProperties(), "quakeml-magnitude-publicid",
						magnitude.getPublicID());

				CreationInfo magnitudeCreationInfo = magnitude
						.getCreationInfo();
				if (magnitudeCreationInfo != null) {
					setProperty(properties, "magnitude-source",
							magnitudeCreationInfo.getAgencyID());
				}

				originProduct.setMagnitude(QuakemlUtils.getValue(magnitude
						.getMag()));

				setProperty(properties, "magnitude-type",
						QuakemlUtils.getMagnitudeType(magnitude.getType()));
				setProperty(properties, "magnitude-azimuthal-gap",
						magnitude.getAzimuthalGap());
				try {
					setProperty(properties, "magnitude-error", magnitude
							.getMag().getUncertainty());
				} catch (Exception e) {
					// no magnitude uncertainty
				}
				setProperty(properties, "magnitude-num-stations-used",
						magnitude.getStationCount());
			} else {
				// a location without a magnitude
			}

			products.add(originProduct);
		} else {
			// no preferred origin found
			// signal origin product hasn't been created
			originProduct = null;
		}

		// look for event description products
		Iterator<EventDescription> iter = event.getDescriptions().iterator();
		while (iter.hasNext()) {
			EventDescription next = iter.next();
			EventDescriptionType type = next.getType();

			if (type == EventDescriptionType.EARTHQUAKE_NAME) {
				if (originProduct != null) {
					// Region name overrides should be sent
					// from the admin pages, or attached to an origin. That way
					// only a preferred origin, or a manually sent region,
					// can alter the displayed region name
					setProperty(originProduct.getProperties(), "title", next
							.getText().trim());
				}
			} else if (type == EventDescriptionType.TECTONIC_SUMMARY) {
				Product product = new Product(new ProductId(productSource,
						"tectonic-summary", productCode, updateTime));
				product.setEventId(eventSource, eventCode);
				ByteContent content = new ByteContent(next.getText().getBytes());
				content.setLastModified(updateTime);
				product.getContents().put("tectonic-summary.inc.html", content);

				products.add(product);
			} else if (type == EventDescriptionType.FELT_REPORT) {
				Product product = new Product(new ProductId(productSource,
						"impact-text", productCode, updateTime));
				product.setEventId(eventSource, eventCode);
				ByteContent content = new ByteContent(next.getText().getBytes());
				content.setLastModified(updateTime);
				product.getContents().put("", content);

				products.add(product);
			}
		}

		if (sendMechanismWhenPhasesExist || originProduct == null
				|| !hasPhaseData) {
			Iterator<FocalMechanism> focalMechanisms = event
					.getFocalMechanisms().iterator();
			while (focalMechanisms.hasNext()) {
				FocalMechanism mech = focalMechanisms.next();
				Product mechProduct = null;

				if (hasPhaseData && originProduct != null) {
					// when a phase data product was created, send lightweight
					// focal mechanism
					Quakeml lightweightQuakeml = QuakemlUtils
							.getLightweightFocalMechanism(message,
									mech.getPublicID());
					Event lightweightEvent = lightweightQuakeml
							.getEventParameters().getEvents().get(0);
					FocalMechanism lightweightMech = QuakemlUtils
							.getFocalMechanism(lightweightEvent,
									mech.getPublicID());

					mechProduct = getFocalMechanismProduct(lightweightQuakeml,
							lightweightEvent, lightweightMech,
							formatConverter.getString(lightweightQuakeml,
									validate));
				} else {
					// otherwise, fall back to original
					mechProduct = getFocalMechanismProduct(message, event,
							mech, quakemlXML);
				}

				if (mechProduct != null) {
					products.add(mechProduct);
				}
			}
		}

		// send lightweight origin product
		if (sendOriginWhenPhasesExist && hasPhaseData && originProduct != null) {
			// create lightweight origin product
			Product lightweightOrigin = new Product(originProduct);
			lightweightOrigin.getId().setType("origin");

			Quakeml lightweightQuakeml = QuakemlUtils
					.getLightweightOrigin(message);

			// serialize xml without phase data
			ByteContent lightweightContent = new ByteContent(formatConverter
					.getString(lightweightQuakeml, validate).getBytes());
			lightweightContent.setContentType(XML_CONTENT_TYPE);
			lightweightContent.setLastModified(updateTime);
			lightweightOrigin.getContents().put(QUAKEML_CONTENT_PATH,
					lightweightContent);

			// insert at front of list
			products.add(0, lightweightOrigin);
		}

		return products;
	}

	protected Product getFocalMechanismProduct(final Quakeml quakeml,
			final Event event, final FocalMechanism mech,
			final String quakemlContent) {
		MomentTensor momentTensor = mech.getMomentTensor();

		// determine product id
		String mechSource = mech.getDatasource();
		String mechCode = mech.getDataid();
		String mechType = mech.getDatatype();
		if (mechType == null) {
			// automatically determine mechanism type based on available data
			mechType = "focal-mechanism";
			if (momentTensor != null && momentTensor.getTensor() != null) {
				mechType = "moment-tensor";
				if (mechCode == null && momentTensor.getMethodID() != null) {
					mechCode = productCode + "_" + momentTensor.getMethodID();
				}
			}
		}
		if (mechSource == null) {
			mechSource = productSource;
		}
		if (mechCode == null) {
			mechCode = productCode;
		}
		Product product = new Product(new ProductId(mechSource, mechType,
				mechCode, updateTime));
		if (mech.getEvaluationStatus() == EvaluationStatus.REJECTED) {
			// this is a delete
			product.setStatus(Product.STATUS_DELETE);
		}

		// product is being contributed to containing event
		product.setEventId(eventSource, eventCode);

		ByteContent tensorContent = new ByteContent(quakemlContent.getBytes());
		tensorContent.setContentType(XML_CONTENT_TYPE);
		tensorContent.setLastModified(updateTime);
		product.getContents().put(QUAKEML_CONTENT_PATH, tensorContent);
		product.getContents().put(CONTENTS_XML_PATH, getContentsXML());

		Map<String, String> properties = product.getProperties();

		// track which mechanism is used in this product, for later
		// extraction
		setProperty(properties, "quakeml-publicid", mech.getPublicID());
		// also track original event if set, in case this is a recontributed
		// product
		setProperty(properties, "original-eventsource", mech.getEventsource());
		setProperty(properties, "original-eventsourcecode", mech.getEventid());

		if (mech.getEvaluationMode() == EvaluationMode.MANUAL) {
			properties.put("review-status", "reviewed");
		} else {
			properties.put("review-status", "automatic");
		}

		if (mech.getEvaluationStatus() != null) {
			properties.put("evaluation-status", mech.getEvaluationStatus()
					.value());
		} else {
			properties.put("evaluation-status", EvaluationStatus.PRELIMINARY
					.value());
		}

		CreationInfo focalMechanismCreationInfo = mech.getCreationInfo();
		if (focalMechanismCreationInfo != null) {
			product.setVersion(focalMechanismCreationInfo.getVersion());
			setProperty(properties, "beachball-source",
					focalMechanismCreationInfo.getAgencyID());
		}

		NodalPlanes planes = mech.getNodalPlanes();
		if (planes != null) {
			NodalPlane plane1 = planes.getNodalPlane1();
			setProperty(properties, "nodal-plane-1-strike", plane1.getStrike());
			setProperty(properties, "nodal-plane-1-dip", plane1.getDip());
			setProperty(properties, "nodal-plane-1-rake", plane1.getRake());

			NodalPlane plane2 = planes.getNodalPlane2();
			setProperty(properties, "nodal-plane-2-strike", plane2.getStrike());
			setProperty(properties, "nodal-plane-2-dip", plane2.getDip());
			setProperty(properties, "nodal-plane-2-rake", plane2.getRake());
		}

		// add properties for principal axes
		PrincipalAxes axes = mech.getPrincipalAxes();
		if (axes != null) {
			Axis tAxis = axes.getTAxis();
			if (tAxis != null) {
				setProperty(properties, "t-axis-azimuth", tAxis.getAzimuth());
				setProperty(properties, "t-axis-plunge", tAxis.getPlunge());
				setProperty(properties, "t-axis-length", tAxis.getLength(), true);
			}
			Axis nAxis = axes.getNAxis();
			if (nAxis != null) {
				setProperty(properties, "n-axis-azimuth", nAxis.getAzimuth());
				setProperty(properties, "n-axis-plunge", nAxis.getPlunge());
				setProperty(properties, "n-axis-length", nAxis.getLength(), true);
			}
			Axis pAxis = axes.getPAxis();
			if (pAxis != null) {
				setProperty(properties, "p-axis-azimuth", pAxis.getAzimuth());
				setProperty(properties, "p-axis-plunge", pAxis.getPlunge());
				setProperty(properties, "p-axis-length", pAxis.getLength(), true);
			}
		}

		setProperty(properties, "num-stations-used",
				mech.getStationPolarityCount());

		// try to read triggering origin attributes for association
		Origin triggeringOrigin = QuakemlUtils.getOrigin(event,
				mech.getTriggeringOriginID());
		if (triggeringOrigin != null) {
			// set properties for association purposes.
			product.setLatitude(triggeringOrigin.getLatitude().getValue());
			product.setLongitude(triggeringOrigin.getLongitude().getValue());
			product.setEventTime(triggeringOrigin.getTime().getValue());
			// add triggering depth if present
			if (triggeringOrigin.getDepth() != null) {
				product.setDepth(triggeringOrigin.getDepth().getValue()
						.divide(METERS_PER_KILOMETER));
			}
		}

		Tensor tensor = null;
		if (momentTensor != null) {
			setProperty(properties, "percent-double-couple",
					momentTensor.getDoubleCouple());
			setProperty(properties, "scalar-moment",
					momentTensor.getScalarMoment(), true);
			setProperty(properties, "beachball-type",
					momentTensor.getMethodID());
			if (momentTensor.getInversionType() != null) {
				setProperty(properties, "inversion-type",
						momentTensor.getInversionType().value());
			}

			tensor = momentTensor.getTensor();
			if (tensor != null) {
				setProperty(properties, "tensor-mpp", tensor.getMpp(), true);
				setProperty(properties, "tensor-mrp", tensor.getMrp(), true);
				setProperty(properties, "tensor-mrr", tensor.getMrr(), true);
				setProperty(properties, "tensor-mrt", tensor.getMrt(), true);
				setProperty(properties, "tensor-mtp", tensor.getMtp(), true);
				setProperty(properties, "tensor-mtt", tensor.getMtt(), true);
			}

			SourceTimeFunction sourceTimeFunction = momentTensor
					.getSourceTimeFunction();
			if (sourceTimeFunction != null) {
				SourceTimeFunctionType sourceTimeFunctionType = sourceTimeFunction
						.getType();
				if (sourceTimeFunctionType != null) {
					setProperty(properties, "sourcetime-type",
							sourceTimeFunctionType.value());
				}
				setProperty(properties, "sourcetime-duration",
						sourceTimeFunction.getDuration());
				setProperty(properties, "sourcetime-risetime",
						sourceTimeFunction.getRiseTime());
				setProperty(properties, "sourcetime-decaytime",
						sourceTimeFunction.getDecayTime());
			}

			Origin derivedOrigin = QuakemlUtils.getOrigin(event,
					momentTensor.getDerivedOriginID());

			if (derivedOrigin != null) {
				setProperty(properties, "derived-latitude",
						derivedOrigin.getLatitude());
				setProperty(properties, "derived-longitude",
						derivedOrigin.getLongitude());
				RealQuantity depth = derivedOrigin.getDepth();
				if (depth != null) {
					setProperty(properties, "derived-depth", depth.getValue()
							.divide(METERS_PER_KILOMETER));
				}
				setProperty(properties, "derived-eventtime",
						derivedOrigin.getTime());
			}

			Magnitude derivedMagnitude = QuakemlUtils.getMagnitude(event,
					momentTensor.getMomentMagnitudeID());
			if (derivedMagnitude != null) {
				String derivedMagnitudeType = derivedMagnitude.getType();
				setProperty(properties, "derived-magnitude-type",
						derivedMagnitudeType);
				setProperty(properties, "derived-magnitude",
						derivedMagnitude.getMag());

				if (derivedMagnitudeType.equalsIgnoreCase("Mwd")) {
					product.getId().setType("broadband-depth");
				}
			}
		}

		if (!Product.STATUS_DELETE.equals(product.getStatus())) {
			// if not deleting, do some validation
			String type = product.getId().getType();
			if (type.equals("focal-mechanism")) {
				if (planes == null) {
					LOGGER.warning("Focal mechanism missing nodal planes");
					return null;
				}
			} else if (type.equals("moment-tensor")) {
				if (tensor == null) {
					LOGGER.warning("Moment tensor missing tensor parameters");
					return null;
				}
			}
		}

		return product;
	}

	public void setProperty(final Map<String, String> properties,
			final String name, final RealQuantity value) {
		setProperty(properties, name, value, false);
	}

	public void setProperty(final Map<String, String> properties,
			final String name, final RealQuantity value,
			final boolean allowExponential) {
		if (value == null) {
			return;
		}

		setProperty(properties, name, value.getValue(), allowExponential);
	}

	public void setProperty(final Map<String, String> properties,
			final String name, final String value) {
		if (value == null) {
			return;
		}

		properties.put(name, value);
	}

	public void setProperty(final Map<String, String> properties,
			final String name, final TimeQuantity value) {
		if (value == null) {
			return;
		}

		properties.put(name, XmlUtils.formatDate(value.getValue()));
	}

	public void setProperty(final Map<String, String> properties,
			final String name, final BigDecimal value) {
		setProperty(properties, name, value, false);
	}

	public void setProperty(final Map<String, String> properties,
			final String name, final BigDecimal value,
			final boolean allowExponential) {
		if (value == null) {
			return;
		}

		properties.put(name, allowExponential ? value.toString()
				: value.toPlainString());
	}

	public void setProperty(final Map<String, String> properties,
			final String name, final BigInteger value) {
		if (value == null) {
			return;
		}

		properties.put(name, value.toString());
	}

	public void setProperty(final Map<String, String> properties,
			final String name, final Integer value) {
		if (value == null) {
			return;
		}

		properties.put(name, value.toString());
	}

	public void setConverter(FileToQuakemlConverter converter) {
		this.converter = converter;
	}

	public FileToQuakemlConverter getConverter() {
		return converter;
	}

	@Override
	public boolean isValidate() {
		return validate;
	}

	@Override
	public void setValidate(boolean validate) {
		this.validate = validate;
	}

	/**
	 * Implement the ProductCreator interface.
	 */
	@Override
	public List<Product> getProducts(File file) throws Exception {
		if (this.converter == null) {
			// preserve quakeml input
			String contents = new String(StreamUtils.readStream(file));
			return this.getQuakemlProducts(contents);
		} else {
			Quakeml quakeml = this.converter.parseFile(file);
			return this.getQuakemlProducts(quakeml);
		}
	}

	protected Content getContentsXML() {
		StringBuffer buf = new StringBuffer();
		buf.append("<?xml version=\"1.0\"?>\n");
		buf.append("<contents xmlns=\"http://earthquake.usgs.gov/earthquakes/event/contents\">\n");
		buf.append("<file title=\"Earthquake XML (Quakeml)\">\n");
		buf.append("<format type=\"xml\" href=\"").append(QUAKEML_CONTENT_PATH)
				.append("\"/>\n");
		buf.append("</file>\n");
		buf.append("</contents>\n");

		ByteContent content = new ByteContent(buf.toString().getBytes());
		content.setContentType("application/xml");

		return content;
	}

	public boolean isSendOriginWhenPhasesExist() {
		return sendOriginWhenPhasesExist;
	}

	public void setSendOriginWhenPhasesExist(boolean sendOriginWhenPhasesExist) {
		this.sendOriginWhenPhasesExist = sendOriginWhenPhasesExist;
	}

	public void setSendMechanismWhenPhasesExist(
			boolean sendMechanismWhenPhasesExist) {
		this.sendMechanismWhenPhasesExist = sendMechanismWhenPhasesExist;
	}

	public boolean isSendMechanismWhenPhasesExist() {
		return sendMechanismWhenPhasesExist;
	}

	/**
	 * Convert quakeml files to products.
	 *
	 * @param args
	 *            a list of files to convert from quakeml to products.
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		QuakemlProductCreator creator = new QuakemlProductCreator();
		creator.setSendOriginWhenPhasesExist(true);
		creator.setSendMechanismWhenPhasesExist(true);

		if (args.length == 0) {
			System.err
					.println("Quakeml to product converter utility.  "
							+ "For visually inspecting products that would be created from quakeml files.");
			System.err.println();
			System.err.println("Usage: QuakemlProductCreator FILE [ FILE ]");
			System.err
					.println("\tFILE - a quakeml file to convert to products, repeat as needed");
			System.exit(1);
		}

		for (String arg : args) {
			File quakeml = new File(arg);
			System.err.println("reading quakeml from "
					+ quakeml.getCanonicalPath());

			Iterator<Product> iter = creator.getProducts(quakeml).iterator();
			while (iter.hasNext()) {
				Product next = iter.next();
				new ObjectProductSource(next).streamTo(new XmlProductHandler(
						new StreamUtils.UnclosableOutputStream(System.err)));
			}

			System.err.println();
		}
	}

}
