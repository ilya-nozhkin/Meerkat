package org.meerkat

import org.meerkat.input.{GraphxInput, Input}
import org.meerkat.parsers.{AbstractCPSParsers, Trampoline}
import org.meerkat.sppf.{DefaultSPPFLookup, NonPackedNode, NonterminalNode}
import org.meerkat.util._

import scala.language.reflectiveCalls
import scalax.collection.Graph
import scalax.collection.edge.Implicits._

import scala.collection.mutable

package object graph {
  def parseGraphFromAllPositions[L, N, T](parser: AbstractCPSParsers.AbstractSymbol[L, N, T, _],
                                 graph: Input[L, N],
                                 nontermsOpt: Option[List[String]] = None): collection.Seq[T] = {
    val sppfLookup = new DefaultSPPFLookup[L, N](graph)
    val nodesCount = graph.edgesCount
    parser.reset()

    val roots = mutable.MutableList[T]()
    for (i <- 0 until nodesCount) {
      parser(graph, i, sppfLookup)(t => roots += t)
      Trampoline.run
    }
    roots.toList
    //nontermsOpt
    //  .getOrElse(List(parser.name))
    //  .flatMap(sppfLookup.findNonterminalsByName)
  }

  def parseGraphFromSpecifiedPositions[L, N, T](parser: AbstractCPSParsers.AbstractSymbol[L, N, T, _],
                                 graph: Input[L, N],
                                 positions: List[Int],
                                 nontermsOpt: Option[List[String]] = None): collection.Seq[T] = {
    val sppfLookup = new DefaultSPPFLookup[L, N](graph)
    val nodesCount = graph.edgesCount
    parser.reset()

    val roots = mutable.MutableList[T]()
    for (i <- positions) {
      parser(graph, i, sppfLookup)(t => roots += t)
      Trampoline.run
      println(i)
      roots.foreach(root => print(root + " "))
      println()
    }
    roots.toList
    //nontermsOpt
    //  .getOrElse(List(parser.name))
    //  .flatMap(sppfLookup.findNonterminalsByName)
  }

  def edgesToInMemoryGraph(edges: List[(Int, String, Int)], nodesCount: Int): GraphxInput[String] = {
    val scalaxEdges = edges.map {
      case (f, l, t) =>
        (f ~+#> t)(l)
    }
    GraphxInput(Graph(scalaxEdges: _*))
  }
}

