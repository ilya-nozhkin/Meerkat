package org.meerkat.graph.jgrapht;

import org.meerkat.input.Input
import scala.collection.JavaConverters._
import org.jgrapht.graph.AbstractBaseGraph

class JGraphTInput[V, E](graph: AbstractBaseGraph[V, E]) extends Input[String] {
  private val vertexToId =
    graph.vertexSet().asScala.zipWithIndex.toMap

  private val idToVertex =
    vertexToId.map(_.swap)

  override def length = graph.vertexSet().size()

  override type Edge = (String, Int)

  override def filterEdges(nodeId: Int, label: String): Seq[Edge] =
    outEdges(nodeId)
      .filter {case (data, _) => data.equals(label)}

  override def outEdges(nodeId: Int): Seq[Edge] =
    graph.outgoingEdgesOf(idToVertex(nodeId))
      .asScala
      .map(edge => (edge.toString, vertexToId(graph.getEdgeTarget(edge))))
      .toSeq

  override def checkNode(nodeId: Int, label: String): Boolean =
    idToVertex(nodeId).toString.equals(label.toString)

  override def epsilonLabel = "epsilon"
}
