CREATE TABLE IF NOT EXISTS extentSummary (
  productid BIGINT PRIMARY KEY AUTO_INCREMENT,
  starttime BIGINT,
  endtime BIGINT,
  minlatitude DOUBLE,
  maxlatitude DOUBLE,
  minlongitude DOUBLE,
  maxlongitude DOUBLE,
) ENGINE = INNODB;
