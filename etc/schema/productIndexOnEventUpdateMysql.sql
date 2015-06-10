-- drop this trigger before any updates so it won't interfere with indexer
DROP TRIGGER IF EXISTS on_event_update_trigger;



delimiter //

DROP FUNCTION IF EXISTS getEventIdByFulleventid//
CREATE FUNCTION `getEventIdByFulleventid` (fulleventid VARCHAR(255)) RETURNS int(11) DETERMINISTIC
BEGIN
	DECLARE l_event_id INT;
	DECLARE l_source VARCHAR(255);
	DECLARE l_code VARCHAR(255);
	DECLARE l_done INT DEFAULT 0;

	DECLARE cur_sources CURSOR FOR
		SELECT DISTINCT eventSource
		FROM productSummary
		WHERE eventSource IS NOT NULL
		AND eventSource <> '';
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET l_done = 1;

	OPEN cur_sources;
	cur_sources_loop: LOOP
		FETCH cur_sources INTO l_source;

		IF l_done = 1 THEN
			CLOSE cur_sources;
			LEAVE cur_sources_loop;
		END IF;

		IF LOCATE(l_source, fulleventid) = 1 THEN

			SET l_code = SUBSTRING(fulleventid, LENGTH(l_source) + 1);
			SET l_event_id = getEventIdBySourceAndCode(l_source, l_code);
			IF l_event_id IS NOT NULL THEN

				CLOSE cur_sources;
				LEAVE cur_sources_loop;
			END IF;
		END IF;
	END LOOP cur_sources_loop;

	RETURN l_event_id;
END;

//

delimiter ;


delimiter //
DROP PROCEDURE IF EXISTS getProductProperty//

CREATE PROCEDURE getProductProperty(IN in_productid INT, IN in_name VARCHAR(255), OUT out_value TEXT)
	READS SQL DATA
BEGIN
	DECLARE done INT DEFAULT 0;
	DECLARE cur_property CURSOR FOR
		SELECT value 
		FROM productSummaryProperty
		WHERE productSummaryIndexId=in_productid
		AND name=in_name;
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;
	
	OPEN cur_property;
	FETCH cur_property INTO out_value;

	IF done = 1 THEN
		SET out_value = NULL;
	END IF;

	CLOSE cur_property;
END;

//

delimiter ;



delimiter //
DROP PROCEDURE IF EXISTS getTsunamiLinkProduct//
CREATE PROCEDURE getTsunamiLinkProduct(
	IN in_eventid INT,
	OUT out_summaryid INT
)
	READS SQL DATA
BEGIN
	DECLARE done INT DEFAULT 0;
	DECLARE cur_tsunamilink CURSOR FOR
		SELECT s.id
		FROM productSummary s
		WHERE s.eventid = in_eventid AND
			s.`type`='impact-link' AND
			EXISTS (
				SELECT * from productSummaryProperty
				WHERE productSummaryIndexId=s.id AND
					name='addon-code' AND
					UPPER(value) LIKE 'TSUNAMILINK%'
			);
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

	SET out_summaryid = NULL;

	OPEN cur_tsunamilink;
	FETCH cur_tsunamilink INTO out_summaryid;
	CLOSE cur_tsunamilink;

	IF done = 1 THEN
		SET out_summaryid = NULL;
	END IF;

END;
//
delimiter ;



delimiter //
DROP PROCEDURE IF EXISTS getEventLastModified//
CREATE PROCEDURE getEventLastModified(
	IN in_eventid INT,
	OUT out_lastmodified BIGINT
)
	READS SQL DATA
BEGIN
	DECLARE l_lastmodified BIGINT;

	DECLARE done INT DEFAULT 0;
	DECLARE cur_lastmodified CURSOR FOR
		SELECT MAX(updateTime) as updateTime
		FROM productSummary
		WHERE eventid = in_eventid;
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

	SET out_lastmodified = NULL;

	OPEN cur_lastmodified;
	FETCH cur_lastmodified INTO out_lastmodified;
	CLOSE cur_lastmodified;

	IF done = 1 THEN
		SET out_lastmodified = NULL;
	END IF;
END;
//
delimiter ;


-- build a distinct list of product sources associated with an event
delimiter //
-- remove the old name for this procedure too
DROP PROCEDURE IF EXISTS getEventProducts//
DROP PROCEDURE IF EXISTS getEventProductSources//
CREATE PROCEDURE getEventProductSources(
	IN in_eventid INT,
	OUT out_productsources TEXT
)
	READS SQL DATA
BEGIN
	DECLARE l_source VARCHAR(255);

	DECLARE done INT DEFAULT 0;
	DECLARE cur_products CURSOR FOR
		SELECT DISTINCT source
		FROM productSummary
		WHERE eventid = in_eventid;
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

	SET out_productsources = NULL;
	OPEN cur_products;
	cur_products_loop: LOOP
		FETCH cur_products INTO l_source;
		IF done = 1 THEN
			CLOSE cur_products;
			LEAVE cur_products_loop;
		END IF;

		SET out_productsources = CONCAT(COALESCE(out_productsources, ''), ',', l_source);
	END LOOP cur_products_loop;

	IF out_productsources <> '' THEN
		SET out_productsources = CONCAT(out_productsources, ',');
	END IF;
END;
//
delimiter ;


-- build a distinct list of product types associated with an event
delimiter //
DROP PROCEDURE IF EXISTS getEventProductTypes//
CREATE PROCEDURE getEventProductTypes(
	IN in_eventid INT,
	OUT out_producttypes TEXT
)
	READS SQL DATA
BEGIN
	DECLARE l_type VARCHAR(255);

	DECLARE done INT DEFAULT 0;
	DECLARE cur_products CURSOR FOR
		SELECT DISTINCT type
		FROM productSummary
		WHERE eventid = in_eventid;
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

	SET out_producttypes = NULL;
	OPEN cur_products;
	cur_products_loop: LOOP
		FETCH cur_products INTO l_type;
		IF done = 1 THEN
			CLOSE cur_products;
			LEAVE cur_products_loop;
		END IF;

		SET out_producttypes = CONCAT(COALESCE(out_producttypes, ''), ',', l_type);
	END LOOP cur_products_loop;

	IF out_producttypes <> '' THEN
		SET out_producttypes = CONCAT(out_producttypes, ',');
	END IF;
END;
//
delimiter ;


delimiter //
DROP PROCEDURE IF EXISTS getEventIds//
CREATE PROCEDURE getEventIds(
	IN in_eventid INT,
	OUT out_eventids TEXT,
	OUT out_eventsources TEXT
)
	READS SQL DATA
BEGIN
	DECLARE l_source VARCHAR(255);
	DECLARE l_code VARCHAR(255);

	DECLARE done INT DEFAULT 0;
	DECLARE cur_ids CURSOR FOR
		SELECT DISTINCT eventSource, eventSourceCode 
		FROM productSummary 
		WHERE eventid = in_eventid 
			AND eventSource IS NOT NULL 
			AND eventSourceCode IS NOT NULL;
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

	SET out_eventids = NULL;
	SET out_eventsources = NULL;
	OPEN cur_ids;
	cur_ids_loop: LOOP
		FETCH cur_ids INTO l_source, l_code;
		IF done = 1 THEN
			CLOSE cur_ids;
			LEAVE cur_ids_loop;
		END IF;

		-- build list of product types
		SET out_eventids = CONCAT(COALESCE(out_eventids, ''), ',', l_source, l_code);
		SET out_eventsources = CONCAT(COALESCE(out_eventsources, ''), ',', l_source);
	END LOOP cur_ids_loop;

	IF out_eventids <> '' THEN
		SET out_eventids = CONCAT(out_eventids, ',');
		SET out_eventsources = CONCAT(out_eventsources, ',');
	END IF;
END;
//
delimiter ;



delimiter //
DROP PROCEDURE IF EXISTS getEventSummary//
CREATE PROCEDURE getEventSummary(
	IN in_eventid INT,
	OUT out_maxmmi DOUBLE,
	OUT out_alertlevel TEXT,
	OUT out_review_status TEXT,
	OUT out_event_type TEXT,
	OUT out_azimuthal_gap DOUBLE,
	OUT out_magnitude_type TEXT,
	OUT out_region TEXT,
	OUT out_producttypes TEXT,
	OUT out_eventids TEXT,
	OUT out_eventsources TEXT,
	OUT out_productsources TEXT,
	OUT out_tsunami INT,
	OUT out_offset INT,
	OUT out_num_responses INT,
	OUT out_maxcdi DOUBLE,
	OUT out_magnitude DOUBLE,
	OUT out_lastmodified BIGINT,
	OUT out_significance INT,
	OUT out_num_stations_used INT,
	OUT out_minimum_distance DOUBLE,
	OUT out_standard_error DOUBLE
)
	READS SQL DATA
BEGIN
	DECLARE pager_id INT DEFAULT -1;
	DECLARE origin_id INT DEFAULT -1;
	DECLARE dyfi_id INT DEFAULT -1;
	DECLARE shakemap_id INT DEFAULT -1;
	DECLARE geoserve_id INT DEFAULT -1;
	DECLARE significance_id INT DEFAULT -1;
	DECLARE l_latitude DOUBLE;
	DECLARE l_longitude DOUBLE;
	DECLARE summary_type VARCHAR(255);
	DECLARE summary_id INT;

	DECLARE mag_sig DOUBLE;
	DECLARE pager_sig DOUBLE;
	DECLARE dyfi_sig DOUBLE;

	DECLARE done INT DEFAULT 0;
	DECLARE cur_summary_products CURSOR FOR
		SELECT ps.id, ps.type
		FROM productSummary ps
		WHERE ps.eventId=in_eventid
			AND ps.type IN ('losspager', 'origin', 'shakemap', 'dyfi', 'geoserve', 'significance')
			AND NOT EXISTS ( 
				SELECT * 
				FROM productSummary 
				WHERE source = ps.source 
					AND type = ps.type 
					AND code = ps.code 
					AND updateTime > ps.updateTime 
				) 
			AND NOT EXISTS ( 
				SELECT * 
				FROM productSummary 
				WHERE eventId = ps.eventId 
				AND type = ps.type 
				AND ( 
					preferred > ps.preferred 
					OR (
						preferred = ps.preferred 
						AND updateTime > ps.updateTime
					)
				)
			);
	-- used to look up event location for region name
	DECLARE cur_location CURSOR FOR
		SELECT latitude, longitude
		FROM event
		WHERE id=in_eventid;
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

	-- find product ids for products used to generate summary
	OPEN cur_summary_products;
	cur_summary_products_loop: LOOP
		FETCH cur_summary_products INTO summary_id, summary_type;
		IF done = 1 THEN
			CLOSE cur_summary_products;
			LEAVE cur_summary_products_loop;
		END IF;

		-- save relevant product ids for fetching properties
		IF summary_type = 'losspager' THEN SET pager_id = summary_id;
		ELSEIF summary_type = 'origin' THEN SET origin_id = summary_id;
		ELSEIF summary_type = 'shakemap' THEN SET shakemap_id = summary_id;
		ELSEIF summary_type = 'dyfi' THEN SET dyfi_id = summary_id;
		ELSEIF summary_type = 'geoserve' THEN SET geoserve_id = summary_id;
		ELSEIF summary_type = 'significance' THEN SET significance_id = summary_id;
		END IF;
	END LOOP cur_summary_products_loop;

	-- load shakemap properties
	IF shakemap_id <> -1 THEN
		CALL getProductProperty(shakemap_id, 'maxmmi', out_maxmmi);
	END IF;

	-- load pager properties
	IF pager_id <> -1 THEN
		IF out_maxmmi IS NULL THEN
			-- shakemap didn't specify, check losspager
			CALL getProductProperty(pager_id, 'maxmmi', out_maxmmi);
		END IF;
		CALL getProductProperty(pager_id, 'alertlevel', out_alertlevel);
	END IF;
	
	
	-- load dyfi properties
	IF dyfi_id <> -1 THEN
		CALL getProductProperty(dyfi_id, 'maxmmi', out_maxcdi);
		CALL getProductProperty(dyfi_id, 'numResp', out_num_responses);
	END IF;

	-- load origin properties
	IF origin_id <> -1 THEN
		CALL getProductProperty(origin_id, 'magnitude', out_magnitude);
		CALL getProductProperty(origin_id, 'magnitude-type', out_magnitude_type);
		CALL getProductProperty(origin_id, 'region', out_region);
		CALL getProductProperty(origin_id, 'azimuthal-gap', out_azimuthal_gap);
		CALL getProductProperty(origin_id, 'review-status', out_review_status);
		CALL getProductProperty(origin_id, 'event-type', out_event_type);
		CALL getProductProperty(origin_id, 'num-stations-used', out_num_stations_used);
		CALL getProductProperty(origin_id, 'minimum-distance', out_minimum_distance);
		CALL getProductProperty(origin_id, 'standard-error', out_standard_error);
		IF out_event_type IS NULL THEN
			SET out_event_type = 'earthquake';
		END IF;
	END IF;

	-- improved location information from geoserve
	IF geoserve_id <> -1 THEN
		CALL getProductProperty(geoserve_id, 'location', out_region);
		CALL getProductProperty(geoserve_id, 'tsunamiFlag', out_tsunami);
		CALL getProductProperty(geoserve_id, 'utcOffset', out_offset);
	END IF;

	IF out_region IS NULL THEN
		-- only use feregion if not already set by origin or geoserve
		SET done = 0;
		OPEN cur_location;
		FETCH cur_location INTO l_latitude, l_longitude;
		CLOSE cur_location;
		IF done <> 1 THEN
			-- found lat/lon, uses feplus get_region_name function
			SET out_region = get_region_name(l_latitude, l_longitude, 'L');
		END IF;
	END IF;

	IF out_tsunami IS NULL OR out_tsunami = 0 THEN
		CALL getTsunamiLinkProduct(in_eventid, out_tsunami);
		IF out_tsunami IS NOT NULL AND out_tsunami <> 0 THEN
			SET out_tsunami = 1;
		END IF;
	END IF;
	
	IF significance_id <> -1 THEN
		CALL getProductProperty(significance_id, 'significance', out_significance);
	ELSE
		-- calculate significance
		IF out_magnitude IS NOT NULL THEN
			SET mag_sig = out_magnitude * 100 * (out_magnitude / 6.5);
		ELSE
			SET mag_sig = 0;
		END IF;

		SET pager_sig = 0;
		IF out_alertlevel IS NOT NULL THEN
			IF out_alertlevel = 'red' THEN
				SET pager_sig = 2000;
			ELSEIF out_alertlevel = 'orange' THEN
				SET pager_sig = 1000;
			ELSEIF out_alertlevel = 'yellow' THEN
				SET pager_sig = 650;
			END IF;
		END IF;

		IF out_num_responses IS NOT NULL THEN
			SET dyfi_sig = (LEAST(out_num_responses, 1000) * out_maxcdi / 10);
		ELSE
			SET dyfi_sig = 0;
		END IF;

		SET out_significance = GREATEST(mag_sig, pager_sig) + dyfi_sig;
	END IF;

	-- get event ids and sources
	CALL getEventIds(in_eventid, out_eventids, out_eventsources);
	-- get product sources
	CALL getEventProductSources(in_eventid, out_productsources);
	-- get product types
	CALL getEventProductTypes(in_eventid, out_producttypes);
	-- event last modified is most recent product update time
	CALL getEventLastModified(in_eventid, out_lastmodified);
END;
//
delimiter ;


delimiter //
DROP PROCEDURE IF EXISTS updateEventSummary //
CREATE PROCEDURE updateEventSummary(IN in_eventid INT)
	MODIFIES SQL DATA
BEGIN
	DECLARE maxmmi DOUBLE;
	DECLARE alertlevel TEXT;
	DECLARE review_status TEXT;
	DECLARE event_type TEXT;
	DECLARE azimuthal_gap DOUBLE;
	DECLARE magnitude_type TEXT;
	DECLARE region TEXT;
	DECLARE types TEXT;
	DECLARE eventids TEXT;
	DECLARE eventsources TEXT;
	DECLARE productsources TEXT;
	DECLARE tsunami INT;
	DECLARE offset INT;
	DECLARE num_responses INT;
	DECLARE maxcdi DOUBLE;
	DECLARE magnitude DOUBLE;
	DECLARE lastmodified BIGINT;
	DECLARE significance INT;
	DECLARE num_stations_used INT;
	DECLARE minimum_distance DOUBLE;
	DECLARE standard_error DOUBLE;

	CALL getEventSummary(in_eventid, maxmmi, alertlevel, review_status, event_type, azimuthal_gap, magnitude_type, region, types, eventids, eventsources, productsources, tsunami, offset, num_responses, maxcdi, magnitude, lastmodified, significance, num_stations_used, minimum_distance, standard_error);

	-- update table
	DELETE FROM eventSummary WHERE eventid=in_eventid;
	INSERT INTO eventSummary VALUES (in_eventid, lastmodified, maxmmi, alertlevel, review_status, event_type, azimuthal_gap, magnitude_type, region, types, eventids, eventsources, productsources, tsunami, offset, num_responses, maxcdi, significance, num_stations_used, minimum_distance, standard_error);
END;
//
delimiter ;


delimiter //
DROP PROCEDURE IF EXISTS recreateEventSummary //
CREATE PROCEDURE recreateEventSummary()
	MODIFIES SQL DATA
BEGIN
	DECLARE eventid INT;

	DECLARE done INT DEFAULT 0;
	DECLARE cur_events CURSOR FOR SELECT id FROM event;
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

	START TRANSACTION;

	-- recreate eventSummary table
	DROP TABLE IF EXISTS eventSummary;
	CREATE TABLE eventSummary (
			eventid BIGINT(20) REFERENCES event(id),
			lastmodified BIGINT,
			maxmmi DOUBLE,
			alertlevel VARCHAR(255),
			review_status VARCHAR(255),
			event_type VARCHAR(255),
			azimuthal_gap DOUBLE,
			magnitude_type VARCHAR(255),
			region VARCHAR(255),
			types TEXT,
			eventids TEXT,
			eventsources TEXT,
			productsources TEXT,
			tsunami INT,
			offset INT,
			num_responses INT,
			maxcdi DOUBLE,
			significance INT,
			num_stations_used INT,
			minimum_distance DOUBLE,
			standard_error DOUBLE,

			UNIQUE INDEX(eventid),
			INDEX(lastmodified),
			INDEX(maxmmi),
			INDEX(alertlevel),
			INDEX(review_status),
			INDEX(event_type),
			INDEX(azimuthal_gap),
			INDEX(magnitude_type),
			INDEX(region),
			INDEX(types(255)),
			INDEX(eventids(255)),
			INDEX(eventsources(255)),
			INDEX(productsources(255)),
			INDEX(tsunami),
			INDEX(num_responses),
			INDEX(maxcdi),
			INDEX(significance),
			INDEX(num_stations_used),
			INDEX(minimum_distance),
			INDEX(standard_error)
		) ENGINE = InnoDB;

	-- loop over all events, updating eventSummary table
	OPEN cur_events;
	cur_events_loop: LOOP
		FETCH cur_events INTO eventid;
		IF done = 1 THEN
			CLOSE cur_events;
			LEAVE cur_events_loop;
		END IF;

		CALL updateEventSummary(eventid);
	END LOOP cur_events_loop;

	COMMIT;

END;
//
delimiter ;

CALL recreateEventSummary();

delimiter //
CREATE TRIGGER on_event_update_trigger AFTER UPDATE ON event FOR EACH ROW
BEGIN
	CALL updateEventSummary(NEW.id);
END;
//
delimiter ;
