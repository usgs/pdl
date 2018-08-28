/**
 * <strong>Distribution</strong> distributes Products.
 *
 * <dl>
 * <dt>{@link EmbeddedPDLClient}</dt>
 * <dd>
 * Provides an example configuration for
 * PDL to be embedded in another java application.
 * </dd>
 *
 * <dt>{@link ProductClient}</dt>
 * <dd>
 * The primary command-line entry point, via the {@link Bootstrap} main-class.
 * </dd>
 * </dl>
 * 
 * 
 * The primary distribution components are:
 *
 * <dl>
 * <dt>{@link ProductSender}</dt>
 * <dd>
 * Used to send products to NotificationReceivers.
 * <br><br>
 * Usually directly using {@link SocketProductSender},
 * or when running a central hub using {@link EIDSNotificationSender}.
 * </dd>
 *
 * <dt>{@link NotificationReceiver}</dt>
 * <dd>
 * Used to receive products.
 * <br><br>
 * Usually by connecting to a hub using {@link EIDSNotificationReceiver},
 * or when running a central hub using {@link SocketProductReceiver}.
 * <br><br>
 * A receiver notifies all its listeners when new products are received.
 * </dd>
 *
 * <dt>{@link NotificationListener}</dt>
 * <dd>
 * Used to process received products.
 * <br><br>
 * Listeners receive {@link Notification}s when new products are
 * available, and can request a product be processed based on its
 * <code>source</code> and/or <code>type</code>.
 * <br><br>
 * The {@link DefaultNotificationListener} processes in a different thread,
 * the {@link ExternalNotificationListener} executes an external process,
 * and {@link RelayNotificationListener} forwards products over a socket.
 * </dd>
 * </dl>
 *
 * These components are also regularly used:
 * <dl>
 * <dt>{@link ProductStorage}</dt>
 * <dd>
 * Store product contents in the file system.
 * {@link FileProductStorage} supports a number of different storage formats,
 * including "directory", "xml", "binary", and "zip".
 * </dd>
 * 
 * <dt>{@link NotificationIndex}</dt>
 * <dd>
 * Used to keep track of which products are in storage,
 * have been processed, and when those products should be automatically
 * cleaned up (if ever).
 * </dd>
 * </dl>
 */
package gov.usgs.earthquake.distribution;