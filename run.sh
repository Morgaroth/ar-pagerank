#!/usr/bin/env bash
sbt package
rm -fr resul*.graph
rm -fr outpu*.graph

${SPARK_HOME}/bin/spark-submit \
    --class ConnectedComponents \
    --master local[*] \
    ./target/scala-2.10/sparkpagerank_2.10-1.0.jar \
    ./src/main/resources/test.graph 1000
rm .*.crc