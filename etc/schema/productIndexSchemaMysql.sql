
CREATE TABLE IF NOT EXISTS event  (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	created BIGINT NOT NULL,
	updated BIGINT DEFAULT NULL,
	source VARCHAR(255) DEFAULT NULL,
	sourceCode VARCHAR(255) DEFAULT NULL,
	eventTime BIGINT DEFAULT NULL,
	latitude DOUBLE DEFAULT NULL,
	longitude DOUBLE DEFAULT NULL,
	depth DOUBLE DEFAULT NULL,
	magnitude DOUBLE DEFAULT NULL,
	status VARCHAR(255) DEFAULT NULL,

	UNIQUE KEY eventIdIdx (source, sourceCode),

	KEY eventLatLonIdx (latitude, longitude),
	KEY eventTimeLatLonIdx(eventTime, latitude, longitude)
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS productSummary(
	id BIGINT PRIMARY KEY AUTO_INCREMENT ,
	created BIGINT NOT NULL,
	productId VARCHAR(255) NOT NULL,
	eventId BIGINT DEFAULT NULL,
	`type` VARCHAR(255) NOT NULL,
	source VARCHAR(255) NOT NULL,
	code VARCHAR(255) NOT NULL,
	updateTime BIGINT NOT NULL,
	eventSource VARCHAR(255) DEFAULT NULL,
	eventSourceCode VARCHAR(255) DEFAULT NULL,
	eventTime BIGINT DEFAULT NULL,
	eventLatitude DOUBLE DEFAULT NULL,
	eventLongitude DOUBLE DEFAULT NULL,
	eventDepth DOUBLE DEFAULT NULL,
	eventMagnitude DOUBLE DEFAULT NULL,
	version VARCHAR(255) DEFAULT NULL,
	status VARCHAR(255) NOT NULL,
	trackerURL VARCHAR(255) NOT NULL,
	preferred BIGINT NOT NULL,

	UNIQUE KEY summaryIdIndex (source, `type`, code, updateTime),

	KEY summaryEventIdIndex (eventSource, eventSourceCode),
	KEY summaryTimeLatLonIdx (eventTime, eventLatitude, eventLongitude),
	KEY preferredEventProductIndex (eventId, type, preferred, updateTime),
	KEY productIdIndex (productId)
) ENGINE = INNODB;

CREATE TABLE IF NOT EXISTS productSummaryLink (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	productSummaryIndexId BIGINT,
	relation VARCHAR(255),
	url TEXT,
	FOREIGN KEY (productSummaryIndexId) REFERENCES productSummary(id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE IF NOT EXISTS productSummaryProperty (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	productSummaryIndexId BIGINT,
	name VARCHAR(255),
	value TEXT,
	FOREIGN KEY (productSummaryIndexId) REFERENCES productSummary(id) ON DELETE CASCADE,
	KEY productIdNameIndex (productSummaryIndexId, name)
) ENGINE=INNODB;
