#! /bin/bash

#run from this directory
cd `dirname $0`

# run test
php region_test.php > output.html

echo "Open output.html to view test results"
