#!/bin/sh


WAITING=`qstat | grep $USER | wc -l`

while [ $WAITING != 0 ]; do
	qstat | grep $USER
	echo "Waiting: $WAITING"
	sleep 3
	echo -e "\n\n"
	DATE=`date -u +"%Y-%m-%d_%H:%M:%SZ"`
	echo $DATE
	WAITING=`qstat | grep $USER | wc -l`
done

echo "No jobs in $USER queue. Yay!"