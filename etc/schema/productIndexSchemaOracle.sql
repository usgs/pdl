  CREATE TABLE event (
    id NUMBER(19,0) NOT NULL,
    created NUMBER(19,0),
    updated NUMBER(19,0),
    source VARCHAR2(255),
    sourceCode VARCHAR2(255),
    eventTime NUMBER(19,0),
    latitude FLOAT,
    longitude FLOAT,
    depth FLOAT,
    magnitude FLOAT,
    status VARCHAR2(255),
    CONSTRAINT event_pk PRIMARY KEY (id)
    )
  ;

  CREATE TABLE productsummary (
    id NUMBER(19,0) NOT NULL,
    created NUMBER(19,0) NOT NULL,
    productid VARCHAR2(255) NOT NULL,
    eventid NUMBER(19,0),
    type VARCHAR2(255) NOT NULL,
    source VARCHAR2(255) NOT NULL,
    code VARCHAR2(255) NOT NULL,
    updatetime NUMBER(19,0) NOT NULL,
    eventsource VARCHAR2(255),
    eventsourcecode VARCHAR2(255),
    eventtime NUMBER(19,0),
    eventlatitude FLOAT,
    eventlongitude FLOAT,
    eventdepth FLOAT,
    eventmagnitude FLOAT,
    version VARCHAR2(255),
    status VARCHAR2(255) NOT NULL,
    trackerurl VARCHAR2(255) NOT NULL,
    preferred NUMBER(19,0) NOT NULL,
    CONSTRAINT productsummary_pk PRIMARY KEY (id)
    )
  ;

  CREATE TABLE productsummarylink (
    id NUMBER(19,0) NOT NULL,
    productsummaryindexid NUMBER(19,0),
    relation VARCHAR2(255),
    url VARCHAR2(4000),
    CONSTRAINT productsummarylink_pk PRIMARY KEY (id),
    CONSTRAINT fk_produtsummaryindexid FOREIGN KEY (productsummaryindexid) REFERENCES productsummary(id) ON DELETE CASCADE
    )
  ;

  CREATE TABLE productsummaryproperty (
    id NUMBER(19,0) NOT NULL,
    productsummaryindexid NUMBER(19,0),
    name VARCHAR2(255),
    value VARCHAR2(4000),
    CONSTRAINT productsummaryproperty_pk PRIMARY KEY (id),
    CONSTRAINT fk_produtsummaryindexid2 FOREIGN KEY (productsummaryindexid) REFERENCES productsummary(id) ON DELETE CASCADE
    )
  ;


CREATE UNIQUE INDEX summaryIdIndex ON productSummary (source, type, code, updateTime);
CREATE INDEX summaryEventIdIndex ON productSummary (eventSource, eventSourceCode);
CREATE INDEX summaryTimeLatLonIdx ON productSummary (eventTime, eventLatitude, eventLongitude);
CREATE INDEX preferredEventProductIndex ON productSummary (eventId, type, preferred, updateTime);
CREATE INDEX productIdIndex ON productSummary (productId);

CREATE UNIQUE INDEX eventIdIdx ON event (source, sourceCode);
CREATE INDEX eventLatLonIdx ON event (latitude, longitude);
CREATE INDEX eventTimeLatLonIdx ON event (eventTime, latitude, longitude);

CREATE UNIQUE KEY productIdNameIndex (productSummaryIndexId, name);


CREATE SEQUENCE  event_seq MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE ;
CREATE SEQUENCE  productsummary_seq  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE ;
CREATE SEQUENCE  productsummarylink_seq  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE ;
CREATE SEQUENCE  productsummaryproperty_seq  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE ;

create trigger event_trig
before insert on event
for each row
begin
  select event_seq.nextval into :new.id from dual;
end;
/

create trigger productsummary_trig
before insert on productsummary
for each row
begin
  select productsummary_seq.nextval into :new.id from dual;
end;
/

create trigger productsummarylink_trig
before insert on productsummarylink
for each row
begin
  select productsummarylink_seq.nextval into :new.id from dual;
end;
/

create trigger productsummaryproperty_trig
before insert on productsummaryproperty
for each row
begin
  select productsummaryproperty_seq.nextval into :new.id from dual;
end;
/

commit;

-- Indexes commonly used by user queries have been omitted (affects processing speed)
-- Please see create-index steps found in productIndexSchemaMysql.sql
