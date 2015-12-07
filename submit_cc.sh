#!/bin/env bash
#PBS -A plgmorgaroth2015b
module load plgrid/apps/spark
start-multinode-spark-cluster.sh
$SPARK_HOME/bin/spark-submit --master spark://`hostname`:7077 --class ConnectedComponents $PROJ_HOME/target/scala-2.10/sparkpagerank_2.10-1.0.jar $GRAPH_URI 1000 > results_${NODES}.csv
stop-multinode-spark-cluster.sh