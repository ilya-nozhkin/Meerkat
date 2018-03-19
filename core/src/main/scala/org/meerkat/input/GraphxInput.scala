package org.meerkat.input

import scalax.collection.Graph
import scalax.collection.edge.LkDiEdge

class GraphxInput(graph: Graph[Int, LkDiEdge]) extends Input[String] {
  override type Edge = (String, Int)
  override def length: Int = graph.order


  override def filterEdges(nodeId: Int, label: String): collection.Seq[Edge] =
    graph.get(nodeId)
      .outgoing
      .collect {
        case e if e.label.toString == label => (e.label.toString, e.to.value)
      }
      .toSeq

  override def outEdges(nodeId: Int): collection.Seq[Edge] =
    graph
      .get(nodeId)
      .outgoing
      .map(e => (e.label.toString, e.to.value))
      .toSeq

  override def checkNode(nodeId: Int, label: String): Boolean =
    true

  override def substring(start: Int, end: Int): String =
    throw new RuntimeException("Can not be done for graphs")

  override def epsilonLabel: Any = "epsilon"

}

object GraphxInput {
  def apply(graph: Graph[Int, LkDiEdge]): GraphxInput = new GraphxInput(graph)
}