#!/bin/sh


IDS=`qstat | grep $USER | awk '{print $1}'`
CNT=`qstat | grep $USER | wc -l`

for ID in ${IDS}; do
    qdel ${sID}
done

echo "Removed $CNT tasks. Yay!"