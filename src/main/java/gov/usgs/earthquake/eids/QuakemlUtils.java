package gov.usgs.earthquake.eids;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.quakeml_1_2.CreationInfo;
import org.quakeml_1_2.Event;
import org.quakeml_1_2.IntegerQuantity;
import org.quakeml_1_2.Magnitude;
import org.quakeml_1_2.MomentTensor;
import org.quakeml_1_2.Origin;
import org.quakeml_1_2.Quakeml;
import org.quakeml_1_2.RealQuantity;
import org.quakeml_1_2.TimeQuantity;
import org.quakeml_1_2.FocalMechanism;
import org.quakeml_1_2.EventParameters;

/**
 * Utility methods for ANSS Quakeml objects.
 */
public class QuakemlUtils {

	/**
	 * Find the preferred Origin in an Event.
	 * 
	 * @param event
	 * @return Origin with publicID equal to event.getPreferredOriginID(), or
	 *         null if not found.
	 */
	public static Origin getPreferredOrigin(final Event event) {
		return getOrigin(event, event.getPreferredOriginID());
	}

	/**
	 * Find a specific Origin in an Event.
	 * 
	 * @param event
	 *            event to search
	 * @param id
	 *            publicID to find
	 * @return Origin with publicID equal to id, or null if not found.
	 */
	public static Origin getOrigin(final Event event, final String id) {
		if (id != null) {
			Iterator<Origin> iter = event.getOrigins().iterator();
			while (iter.hasNext()) {
				Origin next = iter.next();
				String originId = next.getPublicID();
				if (originId != null && originId.equals(id)) {
					return next;
				}
			}
		}
		return null;
	}

	/**
	 * Find the preferred Magnitude in an Event.
	 * 
	 * @param event
	 *            event to search
	 * @return Magnitude with publicID equal to event.getPreferredMagnitudeID(),
	 *         or null if not found.
	 */
	public static Magnitude getPreferredMagnitude(final Event event) {
		return getMagnitude(event, event.getPreferredMagnitudeID());
	}

	/**
	 * Find a specific Magnitude in an event.
	 * 
	 * @param event
	 *            event to search
	 * @param id
	 *            publicID to find.
	 * @return Magnitude with publicID equal to id, or null if not found.
	 */
	public static Magnitude getMagnitude(final Event event, final String id) {
		if (id != null) {
			Iterator<Magnitude> iter = event.getMagnitudes().iterator();
			while (iter.hasNext()) {
				Magnitude next = iter.next();
				String magnitudeId = next.getPublicID();
				if (magnitudeId != null && magnitudeId.equals(id)) {
					return next;
				}
			}
		}
		return null;
	}

	/**
	 * Find a specific FocalMechanism in an event.
	 * 
	 * @param event
	 *            event to search
	 * @param id
	 *            publicID to find.
	 * @return FocalMechanism with publicID equal to id, or null if not found.
	 */
	public static FocalMechanism getFocalMechanism(final Event event,
			final String id) {
		if (id != null) {
			Iterator<FocalMechanism> iter = event.getFocalMechanisms()
					.iterator();
			while (iter.hasNext()) {
				FocalMechanism next = iter.next();
				String mechId = next.getPublicID();
				if (mechId != null && mechId.equals(id)) {
					return next;
				}
			}
		}
		return null;
	}

	/**
	 * Flatten multiple creation info objects, but using the most specific (at
	 * end of list) value that is not null.
	 * 
	 * @param infos
	 * @return a CreationInfo object with the most specific properties (later in
	 *         arguments list), which may be null.
	 */
	public static CreationInfo getCreationInfo(final CreationInfo... infos) {
		CreationInfo info = new CreationInfo();

		for (int i = 0, len = infos.length; i < len; i++) {
			CreationInfo next = infos[i];
			if (next == null) {
				continue;
			}

			if (next.getAgencyID() != null) {
				// set agencyid and agency uri at same time
				info.setAgencyID(next.getAgencyID());
				info.setAgencyURI(next.getAgencyURI());
			}

			if (next.getAuthor() != null) {
				// author and author uri at same time
				info.setAuthor(next.getAuthor());
				info.setAuthorURI(next.getAuthorURI());
			}

			if (next.getCreationTime() != null) {
				info.setCreationTime(next.getCreationTime());
			}

			if (next.getVersion() != null) {
				info.setVersion(next.getVersion());
			}
		}

		return info;
	}

	/**
	 * @param value
	 * @return value.getValue(), or null if value == null.
	 */
	public static BigDecimal getValue(final RealQuantity value) {
		if (value == null) {
			return null;
		} else {
			return value.getValue();
		}
	}

	/**
	 * @param value
	 * @return value.getValue(), or null if value == null.
	 */
	public static BigInteger getValue(final IntegerQuantity value) {
		if (value == null) {
			return null;
		} else {
			return value.getValue();
		}
	}

	/**
	 * @param value
	 * @return value.getValue(), or null if value == null.
	 */
	public static Date getValue(final TimeQuantity value) {
		if (value == null) {
			return null;
		} else {
			return value.getValue();
		}
	}

	public static String getMagnitudeType(final String magnitudeType) {
		if (magnitudeType == null) {
			return null;
		}

		return magnitudeType.toLowerCase();
	}

	/**
	 * @return true if list is not null and not empty.
	 */
	private static boolean listHasData(final List<?> list) {
		if (list != null && list.size() > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Clear a list if it is not null and not empty.
	 */
	private static void listRemoveData(final List<?> list) {
		if (list != null && list.size() > 0) {
			list.clear();
		}
	}

	/**
	 * Check if an event has phase data.
	 * 
	 * <ul>
	 * <li>event.getPicks()</li>
	 * <li>event.getAmplitudes()</li>
	 * <li>event.getStationMagnitudes()</li>
	 * <li>event.getOrigins()
	 * <ul>
	 * <li>origin.getArrivals()</li>
	 * </ul>
	 * </li>
	 * <li>event.getMagnitudes()
	 * <ul>
	 * <li>magnitude.getStationMagnitudeContributions()</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param event
	 *            event to search.
	 * @return true if phase data found, false otherwise.
	 */
	public static boolean hasPhaseData(final Event event) {
		// event level phase data
		if (listHasData(event.getPicks()) || listHasData(event.getAmplitudes())
				|| listHasData(event.getStationMagnitudes())) {
			return true;
		}

		// origin level phase data
		if (event.getOrigins() != null) {
			Iterator<Origin> iter = event.getOrigins().iterator();
			while (iter.hasNext()) {
				Origin next = iter.next();
				if (listHasData(next.getArrivals())) {
					return true;
				}
			}
		}

		// magnitude level phase data
		if (event.getMagnitudes() != null) {
			Iterator<Magnitude> iter = event.getMagnitudes().iterator();
			while (iter.hasNext()) {
				Magnitude next = iter.next();
				if (listHasData(next.getStationMagnitudeContributions())) {
					return true;
				}
			}
		}

		// haven't found phase data
		return false;
	}

	/**
	 * Similar to {@link #hasPhaseData(Event)}, but empties any lists that have
	 * phase data. Also removes &lt;waveformID&gt; elements from focalMechanism.
	 * 
	 * @param event
	 *            event to clear.
	 */
	public static void removePhaseData(final Event event) {
		// event level phase data
		listRemoveData(event.getPicks());
		listRemoveData(event.getAmplitudes());
		listRemoveData(event.getStationMagnitudes());

		// origin level phase data
		if (event.getOrigins() != null) {
			Iterator<Origin> iter = event.getOrigins().iterator();
			while (iter.hasNext()) {
				Origin next = iter.next();
				listRemoveData(next.getArrivals());
			}
		}

		// magnitude level phase data
		if (event.getMagnitudes() != null) {
			Iterator<Magnitude> iter = event.getMagnitudes().iterator();
			while (iter.hasNext()) {
				Magnitude next = iter.next();
				listRemoveData(next.getStationMagnitudeContributions());
			}
		}

		// additionally, remove waveformIDs from focal mechanisms
		if (event.getFocalMechanisms() != null) {
			Iterator<FocalMechanism> iter = event.getFocalMechanisms()
					.iterator();
			while (iter.hasNext()) {
				FocalMechanism next = iter.next();
				listRemoveData(next.getWaveformIDs());
			}
		}
	}

	/**
	 * Extract the preferred origin and magnitude from the first event in a
	 * quakeml message.
	 * 
	 * @param q
	 *            the quakeml message with a preferred origin.
	 * @return a new Quakeml object.
	 */
	public static Quakeml getLightweightOrigin(final Quakeml q) {
		Quakeml quakeml = new Quakeml();

		EventParameters oldEventParameters = q.getEventParameters();
		EventParameters eventParameters = shallowClone(oldEventParameters);
		quakeml.setEventParameters(eventParameters);

		Event oldEvent = q.getEventParameters().getEvents().get(0);
		Event event = shallowClone(oldEvent);
		eventParameters.getEvents().add(event);

		Origin oldOrigin = getPreferredOrigin(oldEvent);
		if (oldOrigin == null) {
			return null;
		}
		Origin origin = shallowClone(oldOrigin);
		event.getOrigins().add(origin);
		event.setPreferredOriginID(origin.getPublicID());

		Magnitude oldMagnitude = getPreferredMagnitude(oldEvent);
		if (oldMagnitude != null) {
			// magnitude is not required for origin
			Magnitude magnitude = shallowClone(oldMagnitude);
			event.getMagnitudes().add(magnitude);
			event.setPreferredMagnitudeID(magnitude.getPublicID());
		}

		return quakeml;
	}

	/**
	 * Extract a focalMechanism, triggering origin, derived origin, and derived
	 * magnitude from the first event in a quakeml message.
	 * 
	 * @param q
	 *            the quakeml message with a focalMechanism
	 * @param focalMechanismId
	 *            the focalMechanism to extract.
	 * @return a new Quakeml object.
	 */
	public static Quakeml getLightweightFocalMechanism(final Quakeml q,
			final String focalMechanismId) {
		Quakeml quakeml = new Quakeml();

		EventParameters oldEventParameters = q.getEventParameters();
		EventParameters eventParameters = shallowClone(oldEventParameters);
		quakeml.setEventParameters(eventParameters);

		Event oldEvent = q.getEventParameters().getEvents().get(0);
		Event event = shallowClone(oldEvent);
		eventParameters.getEvents().add(event);

		FocalMechanism oldMech = getFocalMechanism(oldEvent, focalMechanismId);
		event.getFocalMechanisms().add(shallowClone(oldMech));

		// add triggering origin
		Origin triggeringOrigin = getOrigin(oldEvent,
				oldMech.getTriggeringOriginID());
		if (triggeringOrigin != null) {
			event.getOrigins().add(shallowClone(triggeringOrigin));
		}

		// pull in derived origin and magnitude if present
		MomentTensor tensor = oldMech.getMomentTensor();
		if (tensor != null) {
			Origin derivedOrigin = getOrigin(oldEvent,
					tensor.getDerivedOriginID());
			if (derivedOrigin != null) {
				event.getOrigins().add(shallowClone(derivedOrigin));
			}
			Magnitude momentMagnitude = getMagnitude(oldEvent,
					tensor.getMomentMagnitudeID());
			if (momentMagnitude != null) {
				event.getMagnitudes().add(shallowClone(momentMagnitude));
			}
		}

		return quakeml;
	}

	/**
	 * Create a copy of an event parameters object.
	 * 
	 * omits anies, events, and other attributes.
	 * 
	 * @param oldEventParameters
	 * @return a new EventParameters object.
	 */
	public static EventParameters shallowClone(
			final EventParameters oldEventParameters) {
		EventParameters eventParameters = new EventParameters();
		eventParameters.getComments().addAll(oldEventParameters.getComments());
		eventParameters.setCreationInfo(oldEventParameters.getCreationInfo());
		eventParameters.setDescription(oldEventParameters.getDescription());
		eventParameters.setPublicID(oldEventParameters.getPublicID());
		return eventParameters;
	}

	/**
	 * Create a copy of an event object.
	 * 
	 * omits amplitudes, anies, event descriptions, mechanisms, magnitudes,
	 * origins, other attributes, picks, preferred*ID, and station magnitudes.
	 * 
	 * @param oldEvent
	 * @return a new Event object.
	 */
	public static Event shallowClone(final Event oldEvent) {
		Event event = new Event();
		event.getComments().addAll(oldEvent.getComments());
		event.setCreationInfo(oldEvent.getCreationInfo());
		event.setDataid(oldEvent.getDataid());
		event.setDatasource(oldEvent.getDatasource());
		event.setEventid(oldEvent.getEventid());
		event.setEventsource(oldEvent.getEventsource());
		event.setPublicID(oldEvent.getPublicID());
		event.setType(oldEvent.getType());
		event.setTypeCertainty(oldEvent.getTypeCertainty());
		return event;
	}

	/**
	 * Create a copy of an origin object.
	 * 
	 * omits anies, arrivals, and other attributes.
	 * 
	 * @param oldOrigin
	 * @return a new Origin object.
	 */
	public static Origin shallowClone(final Origin oldOrigin) {
		Origin origin = new Origin();
		origin.getComments().addAll(oldOrigin.getComments());
		origin.getCompositeTimes().addAll(oldOrigin.getCompositeTimes());
		origin.setCreationInfo(oldOrigin.getCreationInfo());
		origin.setDataid(oldOrigin.getDataid());
		origin.setDatasource(oldOrigin.getDatasource());
		origin.setDepth(oldOrigin.getDepth());
		origin.setDepthType(oldOrigin.getDepthType());
		origin.setEarthModelID(oldOrigin.getEarthModelID());
		origin.setEpicenterFixed(oldOrigin.getEpicenterFixed());
		origin.setEvaluationMode(oldOrigin.getEvaluationMode());
		origin.setEvaluationStatus(oldOrigin.getEvaluationStatus());
		origin.setEventid(oldOrigin.getEventid());
		origin.setEventsource(oldOrigin.getEventsource());
		origin.setLatitude(oldOrigin.getLatitude());
		origin.setLongitude(oldOrigin.getLongitude());
		origin.setMethodID(oldOrigin.getMethodID());
		origin.setOriginUncertainty(oldOrigin.getOriginUncertainty());
		origin.setPublicID(oldOrigin.getPublicID());
		origin.setQuality(oldOrigin.getQuality());
		origin.setReferenceSystemID(oldOrigin.getReferenceSystemID());
		origin.setRegion(oldOrigin.getRegion());
		origin.setTime(oldOrigin.getTime());
		origin.setTimeFixed(oldOrigin.getTimeFixed());
		origin.setType(oldOrigin.getType());
		return origin;
	}

	/**
	 * Create a copy of a magnitude object.
	 * 
	 * omits anies, other attributes, and station magnitude contributions.
	 * 
	 * @param oldMagnitude
	 * @return a new Magnitude object.
	 */
	public static Magnitude shallowClone(final Magnitude oldMagnitude) {
		Magnitude magnitude = new Magnitude();
		magnitude.setAzimuthalGap(oldMagnitude.getAzimuthalGap());
		magnitude.getComments().addAll(oldMagnitude.getComments());
		magnitude.setCreationInfo(getCreationInfo(oldMagnitude
				.getCreationInfo()));
		magnitude.setDataid(oldMagnitude.getDataid());
		magnitude.setDatasource(oldMagnitude.getDatasource());
		magnitude.setEvaluationMode(oldMagnitude.getEvaluationMode());
		magnitude.setEvaluationStatus(oldMagnitude.getEvaluationStatus());
		magnitude.setEventid(oldMagnitude.getEventid());
		magnitude.setEventsource(oldMagnitude.getEventsource());
		magnitude.setMag(oldMagnitude.getMag());
		magnitude.setMethodID(oldMagnitude.getMethodID());
		magnitude.setOriginID(oldMagnitude.getOriginID());
		magnitude.setPublicID(oldMagnitude.getPublicID());
		magnitude.setStationCount(oldMagnitude.getStationCount());
		magnitude.setType(oldMagnitude.getType());
		return magnitude;
	}

	/**
	 * Create a copy of a focal mechanism object.
	 * 
	 * omits anies, other attributes.
	 * 
	 * @param oldMech
	 * @return a new FocalMechanism object.
	 */
	public static FocalMechanism shallowClone(final FocalMechanism oldMech) {
		FocalMechanism mech = new FocalMechanism();
		mech.setAzimuthalGap(oldMech.getAzimuthalGap());
		mech.getComments().addAll(oldMech.getComments());
		mech.setCreationInfo(oldMech.getCreationInfo());
		mech.setDataid(oldMech.getDataid());
		mech.setDatasource(oldMech.getDatasource());
		mech.setEvaluationMode(oldMech.getEvaluationMode());
		mech.setEvaluationStatus(oldMech.getEvaluationStatus());
		mech.setEventid(oldMech.getEventid());
		mech.setEventsource(oldMech.getEventsource());
		mech.setMethodID(oldMech.getMethodID());
		mech.setMisfit(oldMech.getMisfit());
		mech.setMomentTensor(oldMech.getMomentTensor());
		mech.setNodalPlanes(oldMech.getNodalPlanes());
		mech.setPrincipalAxes(oldMech.getPrincipalAxes());
		mech.setPublicID(oldMech.getPublicID());
		mech.setStationDistributionRatio(oldMech.getStationDistributionRatio());
		mech.setStationPolarityCount(oldMech.getStationPolarityCount());
		mech.setTriggeringOriginID(oldMech.getTriggeringOriginID());
		mech.getWaveformIDs().addAll(oldMech.getWaveformIDs());
		return mech;
	}

}
