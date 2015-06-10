#! /bin/bash


## db configuration, for now must also be set in import_feplus.php
host=127.0.0.1
username=TODO
password=TODO
database=fe2


echo "Creating point_in_polygon functions"
mysql5 -h $host -u $username -p$password $database < point_in_polygon.sql

echo "Creating FEPlus schema and functions"
mysql5 -h $host -u $username -p$password $database < feplus.sql

echo "Importing FEPlus data from fedump.csv"
php import_fedump_csv.php
