#!/bin/env bash
#PBS -A plgmorgaroth2015b
module load plgrid/apps/spark
start-multinode-spark-cluster.sh
cd $PROJ_HOME
$SPARK_HOME/bin/spark-submit --master spark://`hostname`:7077 --class ConnectedComponents $PROJ_HOME/target/scala-2.10/sparkpagerank_2.10-1.0.jar $GRAPH_URI 1000 >> 1_${NODES}.csv
$SPARK_HOME/bin/spark-submit --master spark://`hostname`:7077 --class ConnectedComponentsV2 $PROJ_HOME/target/scala-2.10/sparkpagerank_2.10-1.0.jar $GRAPH_URI 1000 >> 2_${NODES}.csv
$SPARK_HOME/bin/spark-submit --master spark://`hostname`:7077 --class ConnectedComponentsV3 $PROJ_HOME/target/scala-2.10/sparkpagerank_2.10-1.0.jar $GRAPH_URI 1000 >> 3_${NODES}.csv
$SPARK_HOME/bin/spark-submit --master spark://`hostname`:7077 --class ConnectedComponentsV4 $PROJ_HOME/target/scala-2.10/sparkpagerank_2.10-1.0.jar $GRAPH_URI 1000 >> 4_${NODES}.csv
stop-multinode-spark-cluster.sh