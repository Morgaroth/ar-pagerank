#!/usr/bin/env bash
sbt package
rm -fr resul*.graph
rm -fr outpu*.graph

mkdir logs
DATE=`date +%H_%M_%S_%d%m%Y`
LOG_NAME="logs/log_$DATE"

time ${SPARK_HOME}/bin/spark-submit \
    --class ConnectedComponents \
    --master local[*] \
    --driver-memory 6g \
    ./target/scala-2.10/sparkpagerank_2.10-1.0.jar \
    ~/Pobrane/web-Google.txt 1000 | tee ${LOG_NAME}

rm .*.crc
