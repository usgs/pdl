#! /bin/bash


echo "Compiling SqlToCsv.java"
javac -cp usgs_utils-1.0.jar SqlToCsv.java

echo "Dumping FEPlus data from oracle"
java -cp .:usgs_utils-1.0.jar:ojdbc5.jar SqlToCsv > fedump.csv

echo "Done"
