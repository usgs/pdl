#! /bin/bash

#
# Product Client init script
# Manages the product client process.
#

if [ $# -lt 1 ]; then
	echo "Usage: $0 {start|stop|restart|status}"
	exit 1
fi
GENERIC_INIT="bin/generic_init.sh"
ACTION=$1
EXIT_STATUS=0


#work from same directory as init script
pushd `dirname $0` 2>&1 >/dev/null

#make sure log directory exists
mkdir -p log 2>&1 >/dev/null


## repeat this section for each command
#$GENERIC_INIT $ACTION "Program Display Name" "Unique Command"
#let EXIT_STATUS=$EXIT_STATUS+$?
##


# start the product client
$GENERIC_INIT $ACTION "Product Listener" \
		"java -jar bin/ProductClient.jar --receive --configFile=conf/config.ini"
let EXIT_STATUS=$EXIT_STATUS+$?


# return to original directory
popd 2>&1 >/dev/null


exit $EXIT_STATUS

