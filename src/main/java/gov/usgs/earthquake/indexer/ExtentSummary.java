/**
 * ExtentSummary
 */

package gov.usgs.earthquake.indexer;

import java.util.Date;
import java.util.Map;

import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.util.XmlUtils;

/**
 * Stores ExtentSummary information for products.
 */
public class ExtentSummary {
  private Long id;
  private Date startTime;
  private Date endTime;
  private Double maxLatitude;
  private Double minLatitude;
  private Double maxLongitude;
  private Double minLongitude;

  public ExtentSummary() {
    //Do nothing; this is if member vars are to be set manually
  }

  /**
   * Builds an extentSummary from product properties. If the product has none of the properties, the ExtentSummary is still built.
   * 
   * @param product the productSummary to build from
   */
  public ExtentSummary(ProductSummary product) {
    Map<String,String> properties = product.getProperties();

    id = product.getIndexId();

    if (properties.get(ExtentIndex.EXTENT_START_TIME) != null) {
      startTime = XmlUtils.getDate(properties.get(ExtentIndex.EXTENT_START_TIME));
    }
    if (properties.get(ExtentIndex.EXTENT_END_TIME) != null) {
      endTime = XmlUtils.getDate(properties.get(ExtentIndex.EXTENT_END_TIME));
    }
    if (properties.get(ExtentIndex.EXTENT_MAX_LAT) != null) {
      maxLatitude = Double.parseDouble(properties.get(ExtentIndex.EXTENT_MAX_LAT));
    }
    if (properties.get(ExtentIndex.EXTENT_MAX_LONG) != null) {
      maxLongitude = Double.parseDouble(properties.get(ExtentIndex.EXTENT_MAX_LONG));
    }
    if (properties.get(ExtentIndex.EXTENT_MIN_LAT) != null) {
      minLatitude = Double.parseDouble(properties.get(ExtentIndex.EXTENT_MIN_LAT));
    }
    if (properties.get(ExtentIndex.EXTENT_MIN_LONG) != null) {
      minLongitude = Double.parseDouble(properties.get(ExtentIndex.EXTENT_MIN_LONG));
    }
  }

  /**
   * Returns TRUE if this extent should be put in the extentSummary table (at least one property is not null)
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

  public Long getId() {
    return this.id;
  }
  public void setId(Long id) {
    this.id = id;
  }

  public Date getStartTime() {
    return this.startTime;
  }
  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }
  
  public Date getEndTime() {
    return this.endTime;
  }
  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public Double getMaxLatitude() {
    return this.maxLatitude;
  }
  public void setMaxLatitude(Double maxLatitude) {
    this.maxLatitude = maxLatitude;
  }

  public Double getMinLatitude() {
    return this.minLatitude;
  }
  public void setMinLatitude(Double minLatitude) {
    this.minLatitude = minLatitude;
  }

  public Double getMaxLongitude() {
    return this.maxLongitude;
  }
  public void setMaxLongitude(Double maxLongitude) {
    this.maxLongitude = maxLongitude;
  }

  public Double getMinLongitude() {
    return this.minLongitude;
  }
  public void setMinLongitude(Double minLongitude) {
    this.minLongitude = minLongitude;
  }

} 