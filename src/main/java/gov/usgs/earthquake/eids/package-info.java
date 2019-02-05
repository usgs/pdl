/**
 * Classes to bridge between PDL and other file formats.
 *
 * The Earthquake Information Distribution System (EIDS)
 * operated using a poll model, where XML messages were read
 * from a polling directory and delivered to an output directory.
 * Users periodically checked (polled) the output directory for
 * new files to be processed.
 *
 * {@link EIDSInputWedge} and {@link EIDSOutputWedge} allow PDL
 * to be integrated into legacy systems that operate on a poll model.
 * Note that EIDSInputWedge can also be run in a push mode.
 *
 * Several classes implement translation from external formats into
 * PDL products:
 * 
 * <ul>
 * <li>
 *   {@link QuakemlProductCreator} translates ANSS Quakeml 1.2 messages
 *   into <code>origin</code>, <code>phase-data</code>,
 *   <code>focal-mechanism</code>, <code>moment-tensor</code>,
 *   and <code>broadband-depth</code> type products.
 * </li>
 * 
 * <li>
 *   {@link EQMessageProductCreator} translates ANSS EQXML messages.
 *   This format is deprecated, and has been replaced by ANSS Quakeml 1.2
 * </li>
 *
 * <li>
 *   The interfaces {@link ProductCreator} and
 *   {@link gov.usgs.earthquake.quakeml.FileToQuakemlConverter}
 *   provide mechanisms for other file formats to be used with the
 *   EIDSInputWedge.
 * </li>
 * </ul>
 */
package gov.usgs.earthquake.eids;
