CREATE TABLE extentSummary (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  productSummaryIndexId INTEGER NOT NULL,
  starttime INTEGER DEFAULT NULL,
  endtime INTEGER DEFAULT NULL,
  minimum_latitude REAL DEFAULT NULL,
  maximum_latitude REAL DEFAULT NULL,
  minimum_longitude REAL DEFAULT NULL,
  maximum_longitude REAL DEFAULT NULL
);