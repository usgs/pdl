#! /bin/bash
#
# cron script for the product distribution hub
# $Id: cron.sh 7903 2010-10-25 22:36:35Z jmfee $
# $URL: https://ghttrac.cr.usgs.gov/websvn/ProductDistribution/trunk/etc/examples/hub/bin/cron.sh $
#

LOG_FILE=log/initlog.log


# work from parent directory
pushd `dirname $0`/.. 2>&1 >/dev/null

# make sure the log directory exists
mkdir -p log



date >>$LOG_FILE

# make sure the hub is running
./hub_init.sh start >>$LOG_FILE

# make sure the eids server is running
EIDS/server_init.sh start >>$LOG_FILE

echo >>$LOG_FILE
