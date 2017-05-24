#! /bin/bash
#
# cron script for the product distribution hub
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
