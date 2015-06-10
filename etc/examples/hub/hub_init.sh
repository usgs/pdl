#! /bin/bash

#
# Product Hub init script
# Manages the product client and eids server
#
# $Id: hub_init.sh 7903 2010-10-25 22:36:35Z jmfee $
# $URL: https://ghttrac.cr.usgs.gov/websvn/ProductDistribution/trunk/etc/examples/hub/hub_init.sh $
#

if [ $# -lt 1 ]; then
	echo "Usage: $0 {start|stop|restart|status}"
	exit 1
fi
GENERIC_INIT="bin/generic_init.sh"
ACTION=$1
EXIT_STATUS=0


# work from same directory as this script
pushd `dirname $0` 2>&1 >/dev/null

#make sure log directory exists
mkdir -p log 2>&1 >/dev/null


## repeat this section for each command
#$GENERIC_INIT $ACTION "Program Display Name" "Unique Command"
#let EXIT_STATUS=$EXIT_STATUS+$?
##


# start the product client
$GENERIC_INIT $ACTION "Product Hub" \
		"java -jar bin/ProductClient.jar --receive --configFile=conf/config.ini"
let EXIT_STATUS=$EXIT_STATUS+$?


# restore original directory
popd 2>&1 >/dev/null


exit $EXIT_STATUS

