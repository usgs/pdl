#! /usr/bin/env bash

# run from same directory as script
cd `dirname $0`

# run client
java -jar ProductClient.jar --receive --configFile=config.ini
