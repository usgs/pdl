
CREATE TABLE event  (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	created INTEGER,
	updated INTEGER,
	source TEXT,
	sourceCode TEXT,
	eventTime INTEGER,
	latitude REAL,
	longitude REAL,
	depth REAL,
	magnitude REAL,
	status TEXT
);

CREATE TABLE productSummary (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	created INTEGER,
	productId TEXT,
	eventId INTEGER,
	type TEXT,
	source TEXT,
	code TEXT,
	updateTime INTEGER,
	eventSource TEXT,
	eventSourceCode TEXT,
	eventTime INTEGER,
	eventLatitude REAL,
	eventLongitude REAL,
	eventDepth REAL,
	eventMagnitude REAL,
	version TEXT,
	status TEXT,
	trackerURL TEXT,
	preferred INTEGER
);

CREATE TABLE productSummaryLink (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	productSummaryIndexId INTEGER,
	relation TEXT,
	url TEXT
);

CREATE TABLE productSummaryProperty (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	productSummaryIndexId INTEGER,
	name TEXT,
	value TEXT
);




CREATE UNIQUE INDEX summaryIdIndex
	ON productSummary (source, type, code, updateTime);

CREATE INDEX summaryEventIdIndex
	ON productSummary (eventSource, eventSourceCode);

CREATE INDEX summaryTimeLatLonIdx
	ON productSummary (eventTime, eventLatitude, eventLongitude);

CREATE INDEX preferredEventProductIndex
	ON productSummary (eventId, type, preferred, updateTime);

CREATE INDEX productIdIndex
	ON productSummary (productId);


CREATE UNIQUE INDEX eventIdIdx
	ON event (source, sourceCode);

CREATE INDEX eventLatLonIdx
	ON event (latitude, longitude);

CREATE INDEX eventTimeLatLonIdx
	ON event (eventTime, latitude, longitude);


CREATE UNIQUE INDEX propertyIdNameIndex
	ON productSummaryProperty (productSummaryIndexId, name);

-- Indexes commonly used by user queries have been omitted (affects processing speed)
-- Please see create-index steps found in productIndexSchemaMysql.sql
