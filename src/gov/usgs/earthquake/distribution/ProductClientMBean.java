package gov.usgs.earthquake.distribution;

/**
 * Used to define JMX monitoring interface.
 * 
 * The init script command should be modified to include the following
 * arguments:
 * 
 * <ul>
 * <li>-Dcom.sun.management.jmxremote=true
 * <li>-Dcom.sun.management.jmxremote.port=11237
 * <li>-Dcom.sun.management.jmxremote.ssl=false
 * <li>-Dcom.sun.management.jmxremote.authenticate=false
 * </ul>
 * 
 * Then this can be accessed on the command line using the Syabru nagios jmx
 * plugin: http://snippets.syabru.ch/nagios-jmx-plugin/
 * 
 * <pre>
 * java -jar check_jmx.jar \
 * 	-U service:jmx:rmi:///jndi/rmi://localhost:11237/jmxrmi \
 * 	-O ProductClient:name=jmx \
 * 	-A ListenerQueueStatus
 * </pre>
 * 
 * Where the service url is updated for the correct host and port, and the
 * Attribute is one of the "get" methods without the word "get".
 */
public interface ProductClientMBean {

	/**
	 * @return A string describing the current listener queues.
	 */
	public String getListenerQueueStatus();

	/**
	 * @return client version
	 */
	public String getVersion();

	/**
	 * @return maximum amount of memory
	 */
	public long getMaxMemory();

	/**
	 * @return amount of free memory
	 */
	public long getFreeMemory();

}
