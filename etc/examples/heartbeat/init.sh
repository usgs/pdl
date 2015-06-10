#!/bin/bash

configfile="`dirname $0`/config.sh";
if [ ! -f "$configfile" ]; then
	echo 'No configuration file found.';
	echo 'Create a configuration file before starting heartbeat.';
	echo 'See: README file for more information.';
	exit -1;
fi

source "$configfile";

action=$1;
sudo='';

if [ `whoami` != "root" ]; then
	sudo='sudo -H';
	$sudo ls > /dev/null 2>&1;
	success=$?;

	if [ $success -ne 0 ]; then
		echo 'Authorization failed. Nothing done.';
		exit -1;
	fi
fi

installed=`$sudo crontab -u ${HEARTBEAT_USER} -l | grep "${HEARTBEAT_MARKER}"`;

case $action in
	start)
		if [ ! -z "$installed" ]; then
			echo 'Heartbeat already running. Try stopping first.';
		else
			echo -n 'Starting heartbeat sender...';
			(
				$sudo crontab -u ${HEARTBEAT_USER} -l
				echo -n "${HEARTBEAT_SCHEDULE} ${HEARTBEAT_SENDER} ";
				echo -n ">> ${HEARTBEAT_LOG} 2>&1 ";
				echo -n "${HEARTBEAT_MARKER} ";
				date +%Y-%m-%d_%H:%M:%S
			) | $sudo crontab -u ${HEARTBEAT_USER} /dev/stdin
			echo 'done.';
		fi
		;;
	stop)
		if [ -z "$installed" ]; then
			echo 'Heartbeat not running. Try starting first.';
		else
			echo -n 'Stopping heartbeat sender...';
			$sudo crontab -u ${HEARTBEAT_USER} -l 2>&1 | grep -v "${HEARTBEAT_MARKER}" | $sudo crontab -u ${HEARTBEAT_USER} /dev/stdin
			echo 'done.';
		fi
		;;
	status)
		if [ ! -z "$installed" ]; then
			since=`echo $installed | awk '{print $NF}'`;
			echo "Heartbeat sender is running. (${since})";
		else
			echo 'Heartbeat sender is stopped.';
		fi
		;;
	restart)
		echo -n 'Restarting sender...';
		$sudo `dirname $0`/init.sh stop > /dev/null 2>&1;
		$sudo `dirname $0`/init.sh start > /dev/null 2>&1;
		echo 'done.';
		;;
	log)
		echo "Log information stored in ${HEARTBEAT_LOG}";
		tail -f ${HEARTBEAT_LOG};
		;;
	test)
		echo -n 'Sending a heartbeat product...';
		sudo -H -u ${HEARTBEAT_USER} ${HEARTBEAT_SENDER} >> ${HEARTBEAT_LOG} 2>&1;
		$sudo chown ${HEARTBEAT_USER} ${HEARTBEAT_LOG};
		echo 'done.';
		;;
	*)
		echo "Usage $0 [start|stop|status|restart|log|test]";
		;;
esac
