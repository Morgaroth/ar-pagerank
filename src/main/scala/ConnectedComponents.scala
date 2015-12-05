
import java.net.URI

import org.apache.hadoop.fs.{FileSystem, FileUtil, Path}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{Logging, SparkConf, SparkContext}

object ConnectedComponents extends Logging {

  def main(args: Array[String]) {
    if (args.length != 2) {
      System.err.println("Usage: SparkPageRank <graphfile> <max iterations>")
      System.exit(1)
    }

    val maxIterations = args(1).toInt

    val sparkConf = new SparkConf().setAppName("ConnectedComponents")
    val ctx: SparkContext = new SparkContext(sparkConf)

    // load graph and copy vertex id to vertex data
    var graph: Graph[Long, PartitionID] = GraphLoader
      .edgeListFile(ctx, args(0))
      .mapVertices[Long]((x, y) => x)

    // calculate ConnectedComponents

    // 1. using pregel function
    val pregelled = graph.pregel[Long](Long.MaxValue)(
      getNewVertexData,
      sendMessagesFunc,
      mergeMessages
    )

    // 2. using connectedComponents directly
    val connectedComponents = graph.connectedComponents()

    // 3. implemented by hand
    var iterationsDone = 0

    // duplicate edges to make undirected graph from directed
    val edges: RDD[(VertexId, List[VertexId])] = graph.edges
      .flatMap(e => List((e.dstId, e.srcId), (e.srcId, e.dstId)))
      .distinct()
      .groupByKey().mapValues(_.toList)
      .cache()

    var actual: RDD[(VertexId, Long)] = graph.vertices.map(e => (e._1, e._2))

    while (iterationsDone < maxIterations) {
      val vertsWithActualIndexAndNeighbours: RDD[(VertexId, (Long, List[VertexId]))] = actual.join(edges)

      val messages = vertsWithActualIndexAndNeighbours.flatMap {
        case (src, (actualSrcMin, dsts)) => // here is Pregel paradigm about sending messages to neighbours
          dsts.map(dst => dst -> actualSrcMin)
      }.groupByKey()

      val newActual = messages.mapValues(_.toList.min)

      val changesCount = actual.join(newActual).filter {
        case (_, (old, nev)) => nev < old
      }.count()

      if (changesCount <= 0) {
        iterationsDone = maxIterations
        log.info("calculating components end")
      } else {
        actual = newActual
        log.info(s"end iteration $iterationsDone")
        iterationsDone += 1
      }
    }
    val outputDirectory = "result.graph"
    val outputFile = "output.graph"
    val outputPregelledDirectory = "result-p.graph"
    val outputPregelledFile = "output-p.graph"
    val outputCCDirectory = "result-cc.graph"
    val outputCCFile = "output-cc.graph"

    actual.saveAsTextFile(outputDirectory)
    mergeOutputFiles(ctx, outputDirectory, outputFile)
    pregelled.vertices.saveAsTextFile(outputPregelledDirectory)
    mergeOutputFiles(ctx, outputPregelledDirectory, outputPregelledFile)
    connectedComponents.vertices.saveAsTextFile(outputCCDirectory)
    mergeOutputFiles(ctx, outputCCDirectory, outputCCFile)

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


//
//// :( nie umiem englisz na tyle
//// oblicz wszystkie wiadomości, robi się z nich tabela (VertexId, Wartość)
//// wiadomości to najmniejsze liczby
//val messages = graph.aggregateMessages(sendMessage, mergeMessages, new TripletFields(true, true, false))
//
//// to składamy z aktualnym grafem joinem, przy czym wyliczamy mniejszą
//// wartość z aktualnego wierzchołka i przychodzącej wartości
//graph = graph.joinVertices(messages)(getNewVertexData)
////    val output = ranks.collect()
////    utput.foreach(tup => println(tup._1 + " has rank: " + tup._2 + "."))
//if (messages.count() <= 0) {
//  iterationsDone = maxIterations
//  log.info("calculating components end")
//} else {
//  log.info(s"end iteration $iterationsDone")
//  iterationsDone += 1
//}
