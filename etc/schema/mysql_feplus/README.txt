1) Create a mysql database to hold the feplus data.

2) Update db config in import_feplus.php and (optionally) setup.sh

3) Run setup.sh, or to skip pulling data from oracle,
	a) load feplus.sql into db
	b) php import_feplus.php
	c) load get_region.sql into db


4) To test
	a) update db config in test/region_test.php
	b) Optionally, update events json feed in test/make_region_test.html, view in browser, copy to test_queries.inc.php
	c) run test/test.sh
	d) open test/output.html
	