
import java.net.URI

import org.apache.hadoop.fs.{FileSystem, FileUtil, Path}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{HashPartitioner, Logging, SparkConf, SparkContext}

object ConnectedComponentsV3 extends Logging {

  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println("Usage: SparkPageRank <graphfile> <max iterations>")
      System.exit(1)
    }

    val startTime = System.currentTimeMillis()

    val maxIterations = args(1).toInt

    val sparkConf = new SparkConf().setAppName("ConnectedComponentsV2")
    val ctx: SparkContext = new SparkContext(sparkConf)

    // load graph and copy vertex id to vertex data
    val graph: Graph[Long, PartitionID] = GraphLoader
      .edgeListFile(ctx, args(0))
      .mapVertices[Long]((x, y) => x)

    val iterationsStart = System.currentTimeMillis()

    var iterationsDone = 0
    var done = false

    // duplicate edges to make undirected graph from directed
    val edges: RDD[(VertexId, List[VertexId])] = graph.edges
      .flatMap(e => List((e.dstId, e.srcId), (e.srcId, e.dstId)))
      .distinct()
      .groupByKey().mapValues(_.toList).map {
      case (k, nbrs) => k -> nbrs.filter(_ > k)
    }.filter(_._2.nonEmpty)
      .partitionBy(new HashPartitioner(16))
      .cache()

    var indexes: RDD[(VertexId, Long)] = graph.vertices.map(e => (e._1, e._2))

    while (iterationsDone < maxIterations && !done) {
      val vertsWithActualIndexAndNeighbours: RDD[(VertexId, (List[VertexId], VertexId))] = edges.join(indexes)

      val messages = vertsWithActualIndexAndNeighbours.flatMap {
        case (src, (nbrs, actualSrcMin)) => // here is Pregel paradigm about sending messages to neighbours
          nbrs.filter(_ > actualSrcMin).map(dst => dst -> actualSrcMin)
      }.groupByKey()

      val newIdexes = messages.mapValues(_.min)

      val changesCount = indexes.join(newIdexes).filter {
        case (_, (old, nev)) => nev < old
      }.count()

      if (changesCount <= 0) {
        done = true
        log.info(s"calculating components end after $iterationsDone")
      } else {
        indexes = newIdexes
        log.info(s"end iteration $iterationsDone with $changesCount changes")
        iterationsDone += 1
      }
    }

    val iterationsEnd = System.currentTimeMillis()
    log.info(s"przygotowanie= ${iterationsStart - startTime}")
    log.info(s"petla= ${iterationsEnd - iterationsStart}, iteracji= $iterationsDone")
    log.info(s"iteracji= $iterationsDone")
    println(s"${iterationsStart - startTime},${iterationsEnd - iterationsStart},$iterationsDone")

    ctx.stop()
  }

  /**
    * merge output files, prevents annoying part-XXXXX files.
    */
  def mergeOutputFiles(ctx: SparkContext, filesDirectory: String, resultingFolder: String): Boolean = {
    val fs = FileSystem.get(new URI(filesDirectory), ctx.hadoopConfiguration)
    FileUtil.copyMerge(
      fs, new Path(filesDirectory),
      fs, new Path(resultingFolder),
      true, fs.getConf, null)
  }

  def mergeMessages(a: Long, b: Long) = Math.min(a, b)

  def sendMessage(ctx: EdgeContext[Long, PartitionID, Long]) = {
    if (ctx.dstAttr > ctx.srcAttr) ctx.sendToDst(ctx.srcAttr)
    else if (ctx.dstAttr < ctx.srcAttr) ctx.sendToSrc(ctx.dstAttr)
  }

  def getNewVertexData(v: VertexId, vData: Long, incoming: Long): Long = Math.min(vData, incoming)

  def sendMessagesFunc(edge: EdgeTriplet[VertexId, PartitionID]): Iterator[(VertexId, Long)] =
    if (edge.dstAttr != edge.srcAttr)
      Iterator((Math.max(edge.srcId, edge.dstId), Math.min(edge.srcAttr, edge.dstAttr)))
    else Iterator.empty
}