#!/bin/bash

module load plgrid/apps/spark
start-multinode-spark-cluster.sh

export SPARK_CONF_DIR="$PROJ_HOME/src/main/resources/log4j.properties"

netstat -at | grep 7077

${SPARK_HOME}/bin/spark-submit \
    --master spark://`hostname`:7077 \
    --class ConnectedComponents \
    $PROJ_HOME/target/scala-2.10/sparkpagerank_2.10-1.0.jar \
    $GRAPH_URI 1000

stop-multinode-spark-cluster.sh
