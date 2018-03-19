package org.meerkat.graph.static_analysis

import org.meerkat.Syntax._
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._
import org.meerkat.graph._
import org.meerkat.graph.static_analysis._
import org.meerkat.input.Input

trait StaticAnalysisMixin {
  val grammar = new AnyRef {
    val M: Nonterminal[String] = syn("nd" ~ V ~ "d")
    val V: Nonterminal[String] =
      syn(syn(M.? ~ "na").* ~ M.? ~ syn("a" ~ M.?).*)
    val ALL = syn(M | V)
  }

  val jsons = List(
    "gzip.json",
    "bzip2.json",
    "ls.json",
    "pr.json",
    "wc.json"
  )

  private def getEdges(file: String) = {
    val Some((nodes, edges)) =
      parseJsonGraph(getClass.getResource(s"/static_analysis/$file").getFile)
    (edges, nodes.size)
  }

  def getResults(edgesToGraph: (List[(Int, String, Int)], Int) => Input[String]): List[(String, Int, Int)] =
    jsons.map { file =>
      val (res1, res2, _) = query(file, edgesToGraph)
      (file, res1, res2)
    }
  def benchmark(times: Int, edgesToGraph: (List[(Int, String, Int)], Int) => Input[String]): List[(String, Long)] =
    jsons.map { file =>
      val time =
        List
          .fill(times)(query(file, edgesToGraph))
          .foldLeft(0L) { case (acc, (_, _, t)) => acc + t }
      (file, time / times)
    }

  private def query(file: String, edgesToGraph: (List[(Int, String, Int)], Int) => Input[String]): (Int, Int, Long) = {
    val (edges, nodesCount) = getEdges(file)
    val graph               = edgesToGraph(edges, nodesCount)
    def parseAndGetRunningTime = {
      val start = System.currentTimeMillis
      val pairs =
        (for {
          root <- parseGraphFromAllPositions(grammar.ALL, graph, Some(List("M", "V")))
          nontermName      = root.name.toString
          Seq(left, right) = Seq(root.leftExtent, root.rightExtent).sorted
          if left != right
        } yield (nontermName, left, right)).distinct
      val mCount = pairs.count(_._1 == "M")
      val vCount = pairs.count(_._1 == "V")

      val end = System.currentTimeMillis
      (vCount, mCount, end - start)
    }
    parseAndGetRunningTime
  }

}
