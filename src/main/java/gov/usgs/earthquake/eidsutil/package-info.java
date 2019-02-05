/**
 * Wrappers for the Earthquake Information Distribution System (EIDS).
 *
 * {@link CorbaSender} sends messages to an EIDS server that is configured with a 
 * CorbaFeeder.
 * 
 * {@link EIDSClient} receives messages from an EIDS server, and allows them
 * to be processed in memory instead of using a filesystem.
 * 
 * EIDS distributes any valid XML message,
 * but was particularly integrated with EQXML messages.
 * 
 * PDL uses EIDS components as a reliable notification channel,
 * but is designed in a way for other reliable notification channels to be integrated.
 * See these classes for more information on how these systems are integrated:
 * <ul>
 * <li>{@link gov.usgs.earthquake.distribution.EIDSNotificationReceiver}</li>
 * <li>{@link gov.usgs.earthquake.distribution.EIDSNotificationSender}</li>
 * <li>{@link gov.usgs.earthquake.distribution.URLNotification}</li>
 * </ul>
 */
package gov.usgs.earthquake.eidsutil;
