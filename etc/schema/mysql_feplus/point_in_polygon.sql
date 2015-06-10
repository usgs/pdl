


delimiter //
DROP FUNCTION IF EXISTS get_point//
CREATE FUNCTION get_point(lat DECIMAL(9,6), lon DECIMAL(9,6)) RETURNS POINT 
COMMENT 'Convenience method to convert lat and lon to POINT.'
DETERMINISTIC
BEGIN
	DECLARE result POINT;
	SET result = GeomFromText(CONCAT('POINT(', lon, ' ', lat, ')'));
	RETURN result;
END;
//
delimiter ;



delimiter //
DROP FUNCTION IF EXISTS point_in_one_polygon//
CREATE FUNCTION point_in_one_polygon(p POINT, poly POLYGON) RETURNS INT(1) 
COMMENT 'Should be combined with MBRContains as a prefilter'
-- This function is based on myWithin which can be found at:
--     http://forums.mysql.com/read.php?23,366732,366732
-- It has been modified so if point is on a vertex or edge it returns 1 immediately.
DETERMINISTIC
BEGIN
	DECLARE n INT DEFAULT 0;
	DECLARE pX DECIMAL(9,6);
	DECLARE pY DECIMAL(9,6);
	DECLARE ls LINESTRING;
	DECLARE poly1 POINT;
	DECLARE poly1X DECIMAL(9,6);
	DECLARE poly1Y DECIMAL(9,6);
	DECLARE poly2 POINT;
	DECLARE poly2X DECIMAL(9,6);
	DECLARE poly2Y DECIMAL(9,6);
	DECLARE i INT DEFAULT 0;
	DECLARE result INT(1) DEFAULT 0;
	DECLARE y_intercept DECIMAL(9,6);

	SET pX = X(p);
	SET pY = Y(p);
	SET ls = ExteriorRing(poly);
	SET poly2 = EndPoint(ls);
	SET poly2X = X(poly2);
	SET poly2Y = Y(poly2);
	SET n = NumPoints(ls);

	-- this is the infinite ray test, drawn straight down instead of to the right
	WHILE i < n DO
		SET poly1 = PointN(ls, (i+1));
		SET poly1X = X(poly1);
		SET poly1Y = Y(poly1);

		IF (pX = poly1X && pY = poly1Y) THEN
			-- on end point, return true
			RETURN 1;
		ELSEIF (( (poly1X <= pX) && (pX < poly2X) ) ||
				( (poly2X <= pX) && (pX < poly1X) )) THEN
			-- between x values, test y
			SET y_intercept = poly1Y + (poly2Y - poly1Y) * (pX - poly1X) / (poly2X - poly1X);
			IF y_intercept = pY THEN
				-- on segment, return true
				RETURN 1;
			ELSEIF pY > y_intercept THEN
				-- above segment, toggle result
				SET result = !result;
			END IF;
		END IF;

		SET poly2X = poly1X;
		SET poly2Y = poly1Y;
		SET i = i + 1;
	END WHILE;

	RETURN result;
END;
//
delimiter ;


delimiter //
DROP FUNCTION IF EXISTS point_in_polygon//
CREATE FUNCTION point_in_polygon(p POINT, poly GEOMETRY) RETURNS INT(1)
COMMENT 'Should be combined with MBRContains as a prefilter.'
-- poly can be POLYGON or MULTIPOLYGON
DETERMINISTIC
BEGIN
	DECLARE n INT DEFAULT 0;
	DECLARE i INT DEFAULT 1; -- 1 based
	DECLARE result INT(1) DEFAULT 0;
	DECLARE type VARCHAR(255);

	SET type = GeometryType(poly);

	IF type = 'POLYGON' THEN
		SET result = point_in_one_polygon(p, poly);
	ELSEIF type = 'MULTIPOLYGON' THEN
		-- test each sub polygon
		SET n = NumGeometries(poly);
		WHILE i <= n DO
			SET result = point_in_one_polygon(p, GeometryN(poly, i));
			IF result = 1 THEN
				RETURN result;
			END IF;

			SET i = i + 1;
		END WHILE;
	END IF;

	RETURN result;
END;
//
delimiter ;


