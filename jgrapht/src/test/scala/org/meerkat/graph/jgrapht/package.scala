package org.meerkat.graph

import org.jgrapht.graph.{DefaultDirectedGraph, DefaultEdge}

package object jgrapht {
  class LabeledEdge[V](from: V, to: V, label: String) extends DefaultEdge {
    override def toString: String = label

    override def getSource: AnyRef = from.asInstanceOf[AnyRef]

    override def getTarget: AnyRef = to.asInstanceOf[AnyRef]
  }

  def edgesToJGraphT(edges: List[(Int, String, Int)], nodesCount: Int): JGraphTInput[Int, LabeledEdge[Int]] = {
    val graph = new DefaultDirectedGraph[Int, LabeledEdge[Int]](classOf[LabeledEdge[Int]])

    List.range(0, nodesCount).foreach(graph.addVertex)

    edges.foreach {
      case (from, label, to) =>
        println(graph.addEdge(from, to, new LabeledEdge(from, to, label)))
    }

    val a = graph.edgeSet().size()

    new JGraphTInput[Int, LabeledEdge[Int]](graph)
  }
}
