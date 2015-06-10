

DROP TABLE IF EXISTS feplus;
CREATE TABLE feplus (
	s VARCHAR(255),
	m VARCHAR(255),
	l VARCHAR(255),
	e VARCHAR(255),
	h VARCHAR(255),
	area INT,
	shape POLYGON,
	feregion INT,
	priority INT,
	dataset VARCHAR(255)
);




delimiter //
DROP PROCEDURE IF EXISTS get_feregion//
CREATE PROCEDURE get_feregion(
	IN in_point POINT,
	OUT out_s VARCHAR(255),
	OUT out_m VARCHAR(255),
	OUT out_l VARCHAR(255),
	OUT out_e VARCHAR(255),
	OUT out_h VARCHAR(255),
	OUT out_area INT,
	OUT out_shape POLYGON,
	OUT out_feregion INT,
	OUT out_priority INT,
	OUT out_dataset VARCHAR(255)
)
	COMMENT 'Find the best FERegion polygon for in_point'
	READS SQL DATA
BEGIN
	DECLARE done INT DEFAULT 0;
	DECLARE cur_points CURSOR FOR
		SELECT s, m, l, e, h, area, shape, feregion, priority, dataset
		FROM feplus
		WHERE MBRContains(shape, in_point) = 1
		ORDER BY
			priority ASC,
			area ASC;
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

	OPEN cur_points;
	cur_points_loop: LOOP
		FETCH cur_points INTO out_s, out_m, out_l, out_e, out_h, out_area, out_shape, out_feregion, out_priority, out_dataset;
		IF done = 1 THEN
			CLOSE cur_points;
			LEAVE cur_points_loop;

			-- not found, set outputs to null
			SET out_s = NULL;
			SET out_m = NULL;
			SET out_l = NULL;
			SET out_e = NULL;
			SET out_h = NULL;
			SET out_area = NULL;
			SET out_shape = NULL;
			SET out_feregion = NULL;
			SET out_priority = NULL;
			SET out_dataset = NULL;
		END IF;
		
		IF point_in_polygon(in_point, out_shape) = 1 THEN
			-- exit loop at first "precise" match
			CLOSE cur_points;
			LEAVE cur_points_loop;
		END IF;
	END LOOP cur_points_loop;
END;
//
delimiter ;



delimiter //
DROP FUNCTION IF EXISTS get_region_name//
CREATE FUNCTION get_region_name(lat DECIMAL(9,6), lon DECIMAL(9,6), type CHAR(1)) RETURNS VARCHAR(255) 
COMMENT 'Get a name for a location. Type is one of M, S, L, E, H'
	-- M - "basic" 40-character name
	-- S - short 32-character name
	-- L - long 64-character name (mixed case)
	-- E - spanish name
	-- H - HDS (continent;country;region)
DETERMINISTIC
BEGIN
	DECLARE l_s VARCHAR(255);
	DECLARE l_m VARCHAR(255);
	DECLARE l_l VARCHAR(255);
	DECLARE l_e VARCHAR(255);
	DECLARE l_h VARCHAR(255);
	DECLARE l_area INT;
	DECLARE l_shape POLYGON;
	DECLARE l_feregion INT;
	DECLARE l_priority INT;
	DECLARE l_dataset VARCHAR(255);

	CALL get_feregion(get_point(lat, lon), l_s, l_m, l_l, l_e, l_h, l_area, l_shape, l_feregion, l_priority, l_dataset);

	IF type = 'S' THEN
		RETURN l_s;
	ELSEIF type = 'M' THEN
		RETURN l_m;
	ELSEIF type = 'L' THEN
		RETURN l_l;
	ELSEIF type = 'E' THEN
		RETURN l_e;
	ELSEIF type = 'H' THEN
		RETURN l_h;
	END IF;

	RETURN NULL;
END;
//
delimiter ;
