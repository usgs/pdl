package gov.usgs.earthquake.eidsutil;

import com.isti.quakewatch.server.qw_feeder.QWFeeder;
import com.isti.quakewatch.server.qw_feeder.QWFeederHelper;

import org.omg.CORBA.ORB;
//import org.omg.CORBA.Object;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

/**
 * A <code>CorbaSender</code> is essentially a client-side wrapper for the
 * <code>QWFeeder</code> IDL file specified by ISTI. This class is designed to
 * provide simplified CORBA interaction with a QWServer (EIDS) machine. All the
 * varied methods of sending messages are provided, however they are wrapped
 * into a single method, namely, <code>sendMessage</code>. See the method
 * documentation for details.
 *
 * @since 0.0.1
 * @author <a href="mailto:emartinez@usgs.gov">Eric Martinez</a>
 */
public class CorbaSender {

	//---------------------------------------------------------------------------
	// Member Variables
	//---------------------------------------------------------------------------

	/** This is the URL used to obtain the underlying QWFeeder object. */
	private String url = null;

	/**
	 * This is the underlying <code>QWFeeder</code> used to send the messages to
	 * the server. The interface specification can be found in the
	 * <code>QWFeeder.idl</code> file from ISTI.
	 */
	private QWFeeder feeder = null;

	private ORB orb = null;
	private POA poa = null;
	private POAManager poaManager = null;
	private org.omg.CORBA.Object object = null;

	//---------------------------------------------------------------------------
	// Constructors
	//---------------------------------------------------------------------------

	/**
	 * Initializes the <code>CorbaSender</code> such that it is ready to send a
	 * message to the specified <code>host</code> over the specified
	 * <code>port</code>. This uses the <code>QWFeeder</code> idl specified by
	 * ISTI as the underlying feeder. After instantiating a
	 * <code>CorbaSender</code> through this constructor, the instance is 100%
	 * ready to use. One downside is one cannot reuse that instance to send
	 * messages to another host; for such a feature one must instantiate a new
	 * object.
	 *
	 * @param host The host machine (ip or cname) to which you want to send the
	 * messages using this <code>CorbaSender</code>.
	 * @param port The port number to send messages to on the host.
	 *
	 * @throws InvalidName If the RootPOA is not aware of the type of object we 
	 * request from it.
	 * @throws AdapterInactive If the poaManager is not active.
	 */
	public CorbaSender(String host, String port) 
			throws InvalidName, AdapterInactive {

		// Create the corbaloc url
		url = "corbaloc:iiop:1.2@" + host + ":" + port + "/QWFeeder";

		// Initialize the ORB
		orb = ORB.init((new String[0]), null);

		// Get the Root POA
		poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));

		// Activate the POAManager
		poaManager = poa.the_POAManager();
		poaManager.activate();

		// Get the generic object reference from the orb
		object = orb.string_to_object(url);

		// Narrow the generic object down to our a QWFeeder
		feeder = QWFeederHelper.narrow(object);

	} // END: constructor CorbaSender

	public void destroy() {
		try { feeder._release(); } catch (Exception ex) { /* ignore */ }
		try { object._release(); } catch (Exception ex) { /* ignore */ }
		try { orb.shutdown(true); } catch (Exception ex) { /* ignore */ }
		try { orb.destroy(); } catch (Exception ex) { /* ignore */ }
		
		feeder = null;
		object = null;
		poaManager = null;
		poa = null;
		orb = null;
	}

	/**
	 * Retrieve a QWFeeder object associated with this CorbaSender.
	 * 
	 * First checks if the object is "non_existent", and if so re-narrows the object.
	 * @return QWFeeder object, or null if unable to narrow.
	 */
	protected QWFeeder getFeeder() {
		if (feeder != null) {
			try {
				if (feeder._non_existent()) {
					feeder = null;
					object = null;
				}
			} catch (org.omg.CORBA.OBJECT_NOT_EXIST e) {
				// feeder._non_existent throws the OBJECT_NOT_EXIST exception 
				// instead of returning true...
				feeder = null;
				object = null;
			}
		}

		if (feeder == null) {
			//recreate the object, must string_to_object AND narrow
			object = orb.string_to_object(url);
			feeder = QWFeederHelper.narrow(object);
		}

		return feeder;
	}

	//---------------------------------------------------------------------------
	// Static Methods
	//---------------------------------------------------------------------------

	/* None at this time. */

	//---------------------------------------------------------------------------
	// Public Methods
	//---------------------------------------------------------------------------

	/**
	 * Sends a data event message. If the event data does not begin with a
	 * &ldquo;DataMessage&rdquo; XML element then the data will be surrounded 
	 * with one. The &ldquo;sendSourced...&rdquo; methods are preferred because 
	 * the feeder-source host name and message number are used for improved 
	 * message tracking.
	 *
	 * @param message The data event message string.
	 * @return <code>true</code> after the message has been successfully stored
	 * and processed; <code>false</code> if an error occurred.
	 */
	public boolean sendMessage(String message) {
		return getFeeder().sendMessage(message);
	}
	
	/**
	 * Sends a data event message. If the event data does not begin with a
	 * &ldquo;DataMessage&rdquo; XML element then the data will be surrounded 
	 * with one. The &ldquo;sendSourced...&rdquo; methods are preferred because 
	 * the feeder-source host name and message number are used for improved 
	 * message tracking.
	 *
	 * @param domain The domain name to use.
	 * @param type The type name to use.
	 * @param message The data event message string.
	 *
	 * @return <code>true</code> after the message has been successfully stored
	 * and processed; <code>false</code> if an error occurred.
	 */
	public boolean sendMessage(String domain, String type, String message) {
		return getFeeder().sendDomainTypeMessage(domain, type, message);
	}


	/**
	 * Sends a data event message. If the event data does not begin with a
	 * &ldquo;DataMessage&rdquo; XML element then the data will be surrounded 
	 * with one. The &ldquo;sendSourced...&rdquo; methods are preferred because 
	 * the feeder-source host name and message number are used for improved 
	 * message tracking.
	 *
	 * @param domain The domain name to use.
	 * @param type The type name to use.
	 * @param name The event name to use.
	 * @param message The data event message string.
	 *
	 * @return <code>true</code> after the message has been successfully stored
	 * and processed; <code>false</code> if an error occurred.
	 */
	public boolean sendMessage(String domain, String type, String name,
			String message) {
		return getFeeder().sendDomainTypeNameMessage(domain, type, name, message);
	}
	
	/**
	 * Sends a data event message. If the event data does not begin with a
	 * &ldquo;DataMessage&rdquo; XML element then the data will be surrounded
	 * with one. The &ldquo;sendSourced...&rdquo; methods are preferred because
	 * the feeder-source host name and message number are used for improved
	 * message tracking.
	 *
	 * @param message The data event message string.
	 * @param feederSourceHost The data-source host string for the message.
	 * @param feederSrcHostMsgId the message-ID number from the data source
	 * (positive value incremented after each message).
	 *
	 * @return <code>true</code> after the message has been successfully stored
	 * and processed; <code>false</code> if an error occurred.
	 */
	public boolean sendMessage(String message, String feederSourceHost, 
			long feederSrcHostMsgId) {
		return getFeeder().sendSourcedMsg(message, feederSourceHost, 
				feederSrcHostMsgId);
	}

	/**
	 * Sends a data event message. If the event data does not begin with a
	 * &ldquo;DataMessage&rdquo; XML element then the data will be surrounded 
	 * with one. The &ldquo;sendSourced...&rdquo; methods are preferred because 
	 * the feeder-source host name and message number are used for improved 
	 * message tracking.
	 *
	 * @param domain The domain name to use.
	 * @param type The type name to use.
	 * @param message The data event message string.
	 * @param feederSourceHost The data-source host string for the message.
	 * @param feederSrcHostMsgId The message-ID number from the data source
	 * (positive value incremented after each message).
	 *
	 * @return <code>true</code> after the message has been successfully stored
	 * and processed; <code>false</code> if an error occurred.
	 */
	public boolean sendMessage(String domain, String type, String message, 
			String feederSourceHost, long feederSrcHostMsgId) {
		return getFeeder().sendSourcedDomainTypeMsg(domain, type, message,
				feederSourceHost, feederSrcHostMsgId);
	}

	/**
	 * Sends a data event message. If the event data does not begin with a
	 * &ldquo;DataMessage&rdquo; XML element then the data will be surrounded 
	 * with one. The &ldquo;sendSourced...&rdquo; methods are preferred because 
	 * the feeder-source host name and message number are used for improved 
	 * message tracking.
	 *
	 * @param domain The domain name to use.
	 * @param type The type name to use.
	 * @param name The event name to use.
	 * @param message The data event message string.
	 * @param feederSourceHost The data-source host string for the message.
	 * @param feederSrcHostMsgId The message-ID number from the data source
	 * (positive value incremented after each message).
	 *
	 * @return <code>true</code> after the message has been successfully stored
	 * and processed; <code>false</code> if an error occurred.
	 */
	public boolean sendMessage(String domain, String type, String name, 
			String message, String feederSourceHost, long feederSrcHostMsgId) {
		return getFeeder().sendSourcedDomainTypeNameMsg(domain, type, name, message,
				feederSourceHost, feederSrcHostMsgId);
	}

	//---------------------------------------------------------------------------
	// Private Methods
	//---------------------------------------------------------------------------

	/* None at this time. */

} // END: class CorbaSender
