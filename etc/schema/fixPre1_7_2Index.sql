-- tested against sqlite and mysql, may require customization for others


-- events with all origins deleted, but other products not deleted and have location

CREATE TEMPORARY TABLE DELETED_ORIGIN AS
SELECT * FROM event e 
WHERE e.status='UPDATE' AND 
EXISTS (SELECT * FROM productSummary s1 WHERE s1.eventid=e.id AND s1.type='origin') AND 
NOT EXISTS (SELECT * FROM productSummary s2 WHERE s2.eventid=e.id AND s2.type='origin' AND UPPER(s2.status)<>'DELETE');

SELECT * FROM DELETED_ORIGIN;

UPDATE event 
SET status='DELETE' 
WHERE id IN (SELECT id from DELETED_ORIGIN);

DROP TABLE DELETED_ORIGIN;


-- events with bad magnitude, from product with largest preferred weight instead of origin (when there is an origin).

CREATE TEMPORARY TABLE WRONG_MAGNITUDE AS
SELECT * FROM event e
WHERE e.status='UPDATE' AND
EXISTS (SELECT * FROM productSummary s1 WHERE s1.eventid=e.id AND s1.type='origin' AND UPPER(s1.status)<>'DELETE') AND 
NOT EXISTS (SELECT * FROM productSummary s2 WHERE s2.eventid=e.id AND s2.type='origin' AND UPPER(s2.status)<>'DELETE' AND s2.eventMagnitude=e.magnitude);

CREATE TEMPORARY TABLE RIGHT_MAGNITUDE AS
SELECT e.id, s.eventMagnitude
FROM WRONG_MAGNITUDE e, productSummary s
WHERE s.eventid=e.id AND s.type='origin' AND UPPER(s.status)<>'DELETE' AND 
NOT EXISTS( 
  SELECT * FROM productSummary 
  WHERE eventid=s.eventid AND type='origin' AND UPPER(status)<>'DELETE' AND 
  id<>s.id AND updateTime>s.updateTime
);

SELECT w.*, r.eventMagnitude as rightMagnitude
FROM WRONG_MAGNITUDE w, RIGHT_MAGNITUDE r
WHERE w.id = r.id;

UPDATE event 
SET magnitude=(SELECT eventMagnitude FROM RIGHT_MAGNITUDE WHERE event.id=RIGHT_MAGNITUDE.id)
WHERE id IN (SELECT id FROM WRONG_MAGNITUDE);

DROP TABLE WRONG_MAGNITUDE;
DROP TABLE RIGHT_MAGNITUDE;

