CREATE TABLE extentSummary (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  productSummaryIndexId INTEGER NOT NULL,
  starttime INTEGER DEFAULT NULL,
  endtime INTEGER DEFAULT NULL,
  minlatitude REAL DEFAULT NULL,
  maxlatitude REAL DEFAULT NULL,
  minlongitude REAL DEFAULT NULL,
  maxlongitude REAL DEFAULT NULL
);