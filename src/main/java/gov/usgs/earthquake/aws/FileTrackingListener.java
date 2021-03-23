package gov.usgs.earthquake.aws;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.naming.ConfigurationException;

import gov.usgs.earthquake.distribution.NotificationEvent;
import gov.usgs.earthquake.distribution.NotificationListener;
import gov.usgs.util.Config;
import gov.usgs.util.DefaultConfigurable;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;

/**
 * This class is a stop-gap to store file-based tracking information in a
 * TrackingIndex.
 *
 * It implements the NotificationListener interface so it can hook into
 * startup/shutdown lifecycle calls used by ProductClient.  Listeners are
 * started before and shutdown after Receivers, and can put a tracking file
 * in place before the receiver starts and save its state after a receiver stops.
 */
public class FileTrackingListener extends DefaultConfigurable implements NotificationListener {

  /** Initialzation of logger. For us later in file. */
  private static final Logger LOGGER = Logger.getLogger(FileTrackingListener.class.getName());

  /** Tracking index property */
  public static final String TRACKING_INDEX_PROPERTY = "trackingIndex";
  /** Tracking index file property */
  public static final String TRACKING_INDEX_FILE_PROPERTY = "trackingIndexFile";

  /** Tracking file property */
  public static final String TRACKING_FILE_PROEPRTY = "trackingFile";

  /** File being tracked. */
  private File trackingFile;
  /** Tracking Index where contents are stored */
  private TrackingIndex trackingIndex;

  /** FileTrackingListener constructor */
  public FileTrackingListener() {
  }

  /** Initializable FileTrackingListener
   * @param trackingFile file to be traacked
   * @param trackingIndex Index where contents are stored
   */
  public FileTrackingListener(final File trackingFile, final TrackingIndex trackingIndex) {
    this.trackingFile = trackingFile;
    this.trackingIndex = trackingIndex;
  }

  /** Getter for trackingFile
   * @return trackingFile
   */
  public File getTrackingFile() { return this.trackingFile; }

  /** Setter for trackingFile
   * @param trackingFile File to be tracked
   */
  public void setTrackingFile(final File trackingFile) {
    this.trackingFile = trackingFile;
  }

  /** Getter for trackingIndex
   * @return trackingIndex
   */
  public TrackingIndex getTrackingIndex() { return this.trackingIndex; }

  /** Setter for trackingIndex
   * @param trackingIndex Index where contents are stored
   */
  public void setTrackingIndex(final TrackingIndex trackingIndex) {
    this.trackingIndex = trackingIndex;
  }

  @Override
  public void configure(Config config) throws Exception {
    super.configure(config);

    // configure tracking index
    final String trackingIndexName = config.getProperty(TRACKING_INDEX_PROPERTY);
    if (trackingIndexName != null) {
      LOGGER.config("[" + getName() + "] loading tracking index "
          + trackingIndexName);
      try {
        // read object from global config
        trackingIndex = (TrackingIndex) Config.getConfig().getObject(trackingIndexName);
      } catch (Exception e) {
        LOGGER.log(
            Level.WARNING,
            "[" + getName() + "] error loading tracking index "
                + trackingIndexName,
            e);
      }
    } else {
      final String trackingIndexFileName = config.getProperty(TRACKING_INDEX_FILE_PROPERTY);
      if (trackingIndexFileName != null) {
        LOGGER.config("[" + getName() + "] creating tracking index at"
            + trackingIndexFileName);
        trackingIndex = new TrackingIndex(
            TrackingIndex.DEFAULT_DRIVER,
            "jdbc:sqlite:" + trackingIndexFileName);
      }
    }

    // configure tracking file
    final String trackingFileName = config.getProperty(TRACKING_FILE_PROEPRTY);
    if (trackingFileName == null) {
      throw new ConfigurationException(TRACKING_FILE_PROEPRTY + " is required");
    }
    trackingFile = new File(trackingFileName);
  }

  /**
   * When starting, call loadTrackingFile to create/update file on disk.
   */
  @Override
  public void startup() throws Exception {
    super.startup();
    trackingIndex.startup();
    loadTrackingFile();
  }

  /**
   * When shutting down, call storeTrackingFile to read from file on disk.
   */
  @Override
  public void shutdown() throws Exception {
    storeTrackingFile();
    trackingIndex.shutdown();
    super.shutdown();
  }

  /**
   * Read trackingIndex and write trackingFile.
   *
   * @throws Exception Exception
   */
  public void loadTrackingFile() throws Exception {
    final String name = trackingFile.getAbsolutePath();
    final JsonObject trackingData = trackingIndex.getTrackingData(name);
    if (trackingData == null) {
      LOGGER.info("[" + getName() + "] tracking data not found in index, ignoring");
      return;
    }
    final byte[] data = Base64.getDecoder().decode(trackingData.getString("data"));
    FileUtils.writeFile(trackingFile, data);
  }

  /**
   * Read trackingFile and write into trackingIndex.
   *
   * @throws Exception Exception
   */
  public void storeTrackingFile() throws Exception {
    try {
      // use absolute path as name
      final String name = trackingFile.getAbsolutePath();
      final byte[] data = StreamUtils.readStream(trackingFile);
      final JsonObject trackingData = Json.createObjectBuilder()
          .add("name", name)
          .add("data", Base64.getEncoder().encodeToString(data))
          .build();
      trackingIndex.setTrackingData(trackingFile.getAbsolutePath(), trackingData);
    } catch (FileNotFoundException fnf) {
      LOGGER.info("[" + getName() + "] tracking file not found, ignoring");
    }
  }

  /**
   * Notification Listener method stubs.
   * These are used only to gain access to lifecycle hooks above.
   */

  @Override
  public void onNotification(NotificationEvent event) throws Exception {
    // ignore
  }

  @Override
  public int getMaxTries() {
    return 1;
  }

  @Override
  public long getTimeout() {
    return 1;
  }
}
