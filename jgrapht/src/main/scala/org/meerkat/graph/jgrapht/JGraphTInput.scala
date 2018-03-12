package org.meerkat.graph.jgrapht;

import org.meerkat.util.Input
import scala.collection.JavaConverters._
import org.jgrapht.graph.AbstractBaseGraph


class JGraphTInput(graph: AbstractBaseGraph[Int, String]) extends Input {
  override def length = graph.vertexSet().size()

  override def filterEdges(nodeId: Int, label: String): Seq[Int] =
    outEdges(nodeId)
      .filter(edge => edge._1 == label)
      .map(edge => edge._2)

  override def outEdges(nodeId: Int): Seq[Edge] =
    graph.outgoingEdgesOf(nodeId)
      .asScala
      .map(edge => (edge, graph.getEdgeTarget(edge)))
      .toSeq

  override def checkNode(id: Int, label: String): Boolean = true

  override def substring(start: Int, end: Int) =
    throw new RuntimeException("Can not be done for graphs")
}
