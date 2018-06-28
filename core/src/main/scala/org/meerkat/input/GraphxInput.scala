package org.meerkat.input

import scalax.collection.Graph
import scalax.collection.edge.LkDiEdge

class GraphxInput[L](graph: Graph[Int, LkDiEdge]) extends Input[L, Nothing] {

  override def edgesCount: Int = graph.order

  override def filterEdges(nodeId: Int, predicate: L => Boolean, outgoing: Boolean): collection.Seq[(L, Int)] = {
    val edges = {
      if (outgoing) graph.get(nodeId).outgoing
      else graph.get(nodeId).incoming
    }
    edges
      .collect {
        case e if predicate(e.label.asInstanceOf[L]) => (e.label.asInstanceOf[L], e.to.value)
      }
      .toSeq
  }

  override def checkNode(nodeId: Int, predicate: Nothing => Boolean): Option[Nothing] =
    None
}

object GraphxInput {
  def apply[L](graph: Graph[Int, LkDiEdge]): GraphxInput[L] = new GraphxInput(graph)
}