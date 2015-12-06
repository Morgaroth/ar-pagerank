name := "SparkPageRank"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.5.1" % "provided",
  "org.apache.spark" %% "spark-graphx" % "1.5.1" % "provided"
)
