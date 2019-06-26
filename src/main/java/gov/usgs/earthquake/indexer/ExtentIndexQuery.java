/**
 * Extent Index Query
 */

package gov.usgs.earthquake.indexer;

public class ExtentIndexQuery {
  private Long id;
  private Long startTime;
  private Long endTime;
  private Double maxLatitude;
  private Double minLatitude;
  private Double maxLongitude;
  private Double minLongitude;

  public Long getId() {
    return this.id;
  }
  public void setId(Long id) {
    this.id = id;
  }

  public Long getStartTime() {
    return this.startTime;
  }
  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }
  
  public Long getEndTime() {
    return this.endTime;
  }
  public void setEndTime(Long endTime) {
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