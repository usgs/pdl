<?php

// Jeremy Fee <jmfee@usgs.gov>
// 2014-06-04
//
// this script helps generate testing data from a live PDL product index.
// see EQH-2777, and gov.usgs.earthquake.indexer.NullMagnitudeTest for examples.


// database event id.
$eventid = 466121;

// run this on a pdl instance with observed issue (or update connection info)
mysql_connect('localhost', 'web', 'readonly');
mysql_select_db('product_index');

function safe_null($val, $prefix='"', $suffix='"') {
	if ($val == null) {
		return "null";
	} else {
		return $prefix . $val . $suffix;
	}
}

?>
	Event event = new Event();
	ProductSummary summary;
	Map<String, String> properties;
<?php

$products = mysql_query('select * from productSummary where eventId=' . intval($eventid));
while ( ($product = mysql_fetch_assoc($products)) != null ) {
	$id = safe_null($product['productId'], 'ProductId.parse("', '")');
	$indexId = intval($product['id']);
	$eventSource = safe_null($product['eventSource']);
	$eventSourceCode = safe_null($product['eventSourceCode']);
	$eventTime = safe_null($product['eventTime'], 'new Date(', 'L)');
	$latitude = safe_null($product['eventLatitude'], 'new BigDecimal("', '")');
	$longitude = safe_null($product['eventLongitude'], 'new BigDecimal("', '")');
	$depth = safe_null($product['eventDepth'], 'new BigDecimal("', '")');
	$magnitude = safe_null($product['eventMagnitude'], 'new BigDecimal("', '")');
	$version = safe_null($product['version']);
	$preferred = safe_null($product['preferred'], '', '');
	$status = safe_null($product['status']);
	$trackerURL = safe_null($product['trackerURL'], 'new URL("', '")');;

?>
	summary = new ProductSummary();
	summary.setIndexId(<?php echo $indexId; ?>L);
	summary.setId(<?php echo $id; ?>); 
	summary.setEventSource(<?php echo $eventSource; ?>);
	summary.setEventSourceCode(<?php echo $eventSourceCode; ?>);
	summary.setEventTime(<?php echo $eventTime; ?>);
	summary.setEventLatitude(<?php echo $latitude; ?>);
	summary.setEventLongitude(<?php echo $longitude; ?>);
	summary.setEventDepth(<?php echo $depth; ?>);
	summary.setEventMagnitude(<?php echo $magnitude; ?>);
	summary.setVersion(<?php echo $version; ?>);
	summary.setPreferredWeight(<?php echo $preferred; ?>L);
	properties = summary.getProperties();
<?php
	$props = mysql_query('select * from productSummaryProperty where productSummaryIndexId=' . intval($product['id']));
	while ( ($prop = mysql_fetch_assoc($props)) != null ) {
?>
	properties.put("<?php echo $prop['name']; ?>", "<?php echo $prop['value']; ?>");
<?php
	}
?>
	event.addProduct(summary);
<?php

}

