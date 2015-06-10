DROP TABLE IF EXISTS event ;
CREATE TABLE event  (
	id SERIAL PRIMARY KEY,
	created BIGINT NOT NULL,
	updated BIGINT,
	source VARCHAR(255),
	sourceCode VARCHAR(255),
	eventTime BIGINT,
	latitude DOUBLE PRECISION,
	longitude DOUBLE PRECISION,
	depth DOUBLE PRECISION,
	magnitude DOUBLE PRECISION,
	status VARCHAR(255)
);
DROP TABLE IF EXISTS productSummary;
CREATE TABLE productSummary(
	id SERIAL PRIMARY KEY,
	created DOUBLE PRECISION NOT NULL,
	productId VARCHAR(255) NOT NULL,
	eventId BIGINT,
	type VARCHAR(255) NOT NULL,
	source VARCHAR(255) NOT NULL,
	code VARCHAR(255) NOT NULL,
	updateTime DOUBLE PRECISION NOT NULL,
	eventSource VARCHAR(255),
	eventSourceCode VARCHAR(255),
	eventTime DOUBLE PRECISION,
	eventLatitude DOUBLE PRECISION,
	eventLongitude DOUBLE PRECISION,
	eventDepth DOUBLE PRECISION,
	eventMagnitude DOUBLE PRECISION,
	version VARCHAR(255),
	status VARCHAR(255) NOT NULL,
	trackerURL VARCHAR(255) NOT NULL,
	preferred BIGINT NOT NULL
);

DROP TABLE IF EXISTS productSummaryLink;
CREATE TABLE productSummaryLink (
	id SERIAL PRIMARY KEY,
	productSummaryIndexId BIGINT,
	relation VARCHAR(255),
	url TEXT,
	FOREIGN KEY (productSummaryIndexId) REFERENCES productSummary(id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS productSummaryProperty;
CREATE TABLE productSummaryProperty (
	id SERIAL PRIMARY KEY,
	productSummaryIndexId BIGINT,
	name VARCHAR(255),
	value TEXT,
	FOREIGN KEY (productSummaryIndexId) REFERENCES productSummary(id) ON DELETE CASCADE
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
