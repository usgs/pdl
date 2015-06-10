<?php

include "test_queries.inc.php";

$user = "root";
$pass = "";
$host = "127.0.0.1";
$port = "3306";
$dbname = "fe2";

mysql_connect($host . ':' . $port, $user, $pass);
mysql_select_db($dbname);

$num_queries = 0;
$query_time = 0;
$num_differences = 0;

foreach ($queries as $query) {
	$before = microtime(true);
	$rs = mysql_query($query);
	if (!$rs) {
		print mysql_error();
		continue;
	}
	$after = microtime(true);

	$num_queries += 1;
	$query_time += $after - $before;

	//only discrepencies return results
	while ($row = mysql_fetch_assoc($rs)) {
		$num_differences += 1;

		print '<h2>' . $row['expected'] . '</h2>';
		print 'Actual: ' . $row['actual'] . '<br/>';
		print 'Event ID: ' . $row['eventid'] . '<br/>';
		print 'Lat, Lon: ' . $row['lat'] . ', ' . $row['lon'] . '<br/>';

		// show raw data
		$all_query = 'SELECT s, m, l, e, h, area, feregion, priority, dataset, point_in_polygon(get_point(' . $row['lat'] . ',' . $row['lon'] . '), shape) as in_polygon, geometryType(shape) as type, numGeometries(shape) as poly_count, geometryType(geometryn(shape, 1)) as child_type
			FROM feplus
			WHERE MBRContains(shape, get_point(' . $row['lat'] . ',' . $row['lon'] . ')) = 1
			ORDER BY
				priority ASC,
				area ASC';
		$all_rs = mysql_query($all_query);
		print '<table>';
		print('<tr><th>s</th><th>m</th><th>l</th><th>e</th><th>h</th><th>area</th><th>feregion</th><th>priority</th><th>dataset</th><th>in_polygon</th><th>type</th></tr>');
		$selected = false;
		while ($all_row = mysql_fetch_assoc($all_rs)) {
			if (!$selected && $all_row['in_polygon'] == 1) {
				$class = ' style="font-weight:bold;"';
				$selected = true;
			} else {
				$class = '';
			}
			printf('<tr%s><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>',
				$class,
				$all_row['s'],
				$all_row['m'],
				$all_row['l'],
				$all_row['e'],
				$all_row['h'],
				$all_row['area'],
				$all_row['feregion'],
				$all_row['priority'],
				$all_row['dataset'],
				$all_row['in_polygon'],
				$all_row['type']);
		}
		print '</table>';
	}

}


print "<h2>Summary</h2>";
print "Executed " . $num_queries . " queries<br/>";
print "Query time " . $query_time . "s (avg=" . ($query_time / $num_queries) . "s)<br/>";
print "Found " . $num_differences . " differences<br/>";
