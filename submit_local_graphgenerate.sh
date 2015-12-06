#!/bin/env bash

module load plgrid/apps/spark
module load plgrid/tools/java8
module load tools/sbt/0.13.9

PROJ_HOME="$HOME/AR/zad3/ar-pagerank"

cd ${PROJ_HOME} && sbt package

${SPARK_HOME}/bin/spark-submit \
    --class GenerateGraph \
    --master local[*] \
    ${PROJ_HOME}/target/scala-2.10/sparkpagerank_2.10-1.0.jar \
    $1 $2
