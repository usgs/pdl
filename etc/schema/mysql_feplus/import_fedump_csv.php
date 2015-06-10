<?php

$user = "TODO";
$pass = "TODO";
$host = "127.0.0.1";
$port = "3306";
$dbname = "fe2";

mysql_connect($host . ':' . $port, $user, $pass);
mysql_select_db($dbname);


// make shape column hold text values
mysql_query("alter table feplus modify column shape TEXT");

$contents = file_get_contents('fedump.csv');
$lines = split("\n", $contents);
foreach ($lines as $line) {
	if ($line != '') {
		mysql_query("insert into feplus (s, m, e, h, l, area, shape, feregion, priority, dataset) values (" . $line . ")") or print mysql_error();
	}
}

// now convert text values to polygons
mysql_query("alter table feplus ADD COLUMN polyshape POLYGON AFTER shape") or print mysql_error();
mysql_query("update feplus SET polyshape = GeomFromText(shape)") or print mysql_error();
mysql_query("alter table feplus DROP COLUMN shape") or print mysql_error();
mysql_query("alter table feplus CHANGE polyshape shape POLYGON NOT NULL") or print mysql_error();
mysql_query("alter table feplus ADD SPATIAL INDEX(shape)") or print mysql_error();
