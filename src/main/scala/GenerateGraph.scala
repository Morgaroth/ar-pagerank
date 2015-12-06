import java.net.URI

import org.apache.hadoop.fs.{FileSystem, FileUtil, Path}
import org.apache.spark.graphx._
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.rdd.RDD
import org.apache.spark.{HashPartitioner, Logging, SparkConf, SparkContext}

object GenerateGraph {
  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println("Usage: GenerateGraph <verts> <file_name>")
      System.exit(1)
    }
    val verts = args(0).toInt
    val output = args(1)

    val sparkConf = new SparkConf().setAppName("GraphGenerator")
    val ctx: SparkContext = new SparkContext(sparkConf)

    val graph = GraphGenerators.logNormalGraph(ctx, verts)
    val tmp = s"output_$output"
    graph.edges.saveAsTextFile(tmp)
    mergeOutputFiles(ctx, tmp, output)

    ctx.stop()
  }

  def mergeOutputFiles(ctx: SparkContext, filesDirectory: String, resultingFolder: String): Boolean = {
    val fs = FileSystem.get(new URI(filesDirectory), ctx.hadoopConfiguration)
    FileUtil.copyMerge(
      fs, new Path(filesDirectory),
      fs, new Path(resultingFolder),
      true, fs.getConf, null)
  }
}
