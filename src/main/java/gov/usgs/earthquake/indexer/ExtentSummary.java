/**
 * ExtentSummary
 */
package gov.usgs.earthquake.indexer;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import gov.usgs.util.XmlUtils;

/**
 * Stores ExtentSummary information for products.
 */
public class ExtentSummary {
  private Long id;
  private Date startTime;
  private Date endTime;
  private BigDecimal maxLatitude;
  private BigDecimal minLatitude;
  private BigDecimal maxLongitude;
  private BigDecimal minLongitude;

  /** Property for Extent Start time */
  public static final String EXTENT_START_TIME_PROPERTY = "starttime";
  /** Property for Extent End Time */
  public static final String EXTENT_END_TIME_PROPERTY = "endtime";
  /** Property for Extent Max Lat */
  public static final String EXTENT_MAX_LAT_PROPERTY = "maximum-latitude";
  /** Property for Extent Min lat */
  public static final String EXTENT_MIN_LAT_PROPERTY = "minimum-latitude";
  /** Property for Extent Max Long */
  public static final String EXTENT_MAX_LONG_PROPERTY = "maximum-longitude";
  /** Property for Extent Min Long */
  public static final String EXTENT_MIN_LONG_PROPERTY = "minimum-longitude";


  /** Empty constructor */
  public ExtentSummary() {
    //Do nothing; this is if member vars are to be set manually
  }

  /**
   * Builds an extentSummary from product properties. If the product has none of
   * the properties, the ExtentSummary is still built.
   *
   * @param product the productSummary to build from
   */
  public ExtentSummary(ProductSummary product) {
    Map<String,String> properties = product.getProperties();

    id = product.getIndexId();

    if (properties.get(EXTENT_START_TIME_PROPERTY) != null) {
      startTime = XmlUtils.getDate(properties.get(EXTENT_START_TIME_PROPERTY));
    }
    if (properties.get(EXTENT_END_TIME_PROPERTY) != null) {
      endTime = XmlUtils.getDate(properties.get(EXTENT_END_TIME_PROPERTY));
    }
    if (properties.get(EXTENT_MAX_LAT_PROPERTY) != null) {
      maxLatitude = new BigDecimal(properties.get(EXTENT_MAX_LAT_PROPERTY));
    }
    if (properties.get(EXTENT_MAX_LONG_PROPERTY) != null) {
      maxLongitude = new BigDecimal(properties.get(EXTENT_MAX_LONG_PROPERTY));
    }
    if (properties.get(EXTENT_MIN_LAT_PROPERTY) != null) {
      minLatitude = new BigDecimal(properties.get(EXTENT_MIN_LAT_PROPERTY));
    }
    if (properties.get(EXTENT_MIN_LONG_PROPERTY) != null) {
      minLongitude = new BigDecimal(properties.get(EXTENT_MIN_LONG_PROPERTY));
    }
  }

  /**
   * Returns TRUE if this extent should be put in the extentSummary table (at
   * least one property is not null)
   * @return boolean
   */
  public boolean isValid() {
    return
      startTime != null ||
      endTime != null ||
      maxLatitude != null ||
      maxLongitude != null ||
      minLatitude != null ||
      minLongitude != null;
  }

  /** @return index Id */
  public Long getIndexId() {
    return this.id;
  }

  /** @param id indexId to set */
  public void setIndexId(Long id) {
    this.id = id;
  }

  /** @return startTime */
  public Date getStartTime() {
    return this.startTime;
  }

  /** @param startTime date to set */
  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  /** @return endTime */
  public Date getEndTime() {
    return this.endTime;
  }

  /** @param endTime date to set */
  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  /** @return maxLatitude */
  public BigDecimal getMaxLatitude() {
    return this.maxLatitude;
  }

  /** @param maxLatitude BigDecimal to set */
  public void setMaxLatitude(BigDecimal maxLatitude) {
    this.maxLatitude = maxLatitude;
  }

  /** @return minLatitude */
  public BigDecimal getMinLatitude() {
    return this.minLatitude;
  }

  /** @param minLatitude BigDecimal to set */
  public void setMinLatitude(BigDecimal minLatitude) {
    this.minLatitude = minLatitude;
  }

  /** @return maxLongitude */
  public BigDecimal getMaxLongitude() {
    return this.maxLongitude;
  }

  /** @param maxLongitude BigDecimal to set */
  public void setMaxLongitude(BigDecimal maxLongitude) {
    this.maxLongitude = maxLongitude;
  }

  /** @return minLongitude */
  public BigDecimal getMinLongitude() {
    return this.minLongitude;
  }

  /** @param minLongitude to set */
  public void setMinLongitude(BigDecimal minLongitude) {
    this.minLongitude = minLongitude;
  }

}