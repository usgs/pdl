CREATE TABLE IF NOT EXISTS extentSummary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  starttime BIGINT,
  endtime BIGINT,
  minlatitude DOUBLE,
  maxlatitude DOUBLE,
  minlongitude DOUBLE,
  maxlongitude DOUBLE,
) ENGINE = INNODB;
