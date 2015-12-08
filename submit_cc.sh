#!/bin/env bash

#PBS -A plgmorgaroth2015b
module load plgrid/apps/spark
start-multinode-spark-cluster.sh
cd $PROJ_HOME
echo "loading project in home $PROJ_HOME"
echo "actual spark conf $SPARK_CONF_DIR"
echo "starting with graph from $GRAPH_URI"
echo "running on $NODES nodes"
D=`date`
echo "starting at $D"
$SPARK_HOME/bin/spark-submit --master spark://`hostname`:7077 --class ConnectedComponents $PROJ_HOME/target/scala-2.10/sparkpagerank_2.10-1.0.jar $GRAPH_URI 1000 >> 1_${NODES}.csv
D=`date`
echo "Done V1 at $D"
$SPARK_HOME/bin/spark-submit --master spark://`hostname`:7077 --class ConnectedComponentsV2 $PROJ_HOME/target/scala-2.10/sparkpagerank_2.10-1.0.jar $GRAPH_URI 1000 >> 2_${NODES}.csv
D=`date`
echo "done V2 at $D"
$SPARK_HOME/bin/spark-submit --master spark://`hostname`:7077 --class ConnectedComponentsV3 $PROJ_HOME/target/scala-2.10/sparkpagerank_2.10-1.0.jar $GRAPH_URI 1000 >> 3_${NODES}.csv
D=`date`
echo "done V3 at $D"
$SPARK_HOME/bin/spark-submit --master spark://`hostname`:7077 --class ConnectedComponentsV4 $PROJ_HOME/target/scala-2.10/sparkpagerank_2.10-1.0.jar $GRAPH_URI 1000 >> 4_${NODES}.csv
D=`date`
echo "end of all work at $D"
stop-multinode-spark-cluster.sh