package org.meerkat.graph.jgrapht

import org.jgrapht.graph.DefaultDirectedGraph
import org.meerkat.Syntax._
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.OptionValues._

class JGraphTTest extends FunSuite {
  test("JGraphTTest_1") {
    val graph = new DefaultDirectedGraph[Int, String](classOf[String])
    val S = syn ("a" ~ "b")
    graph.addVertex(0);
    graph.addVertex(1);
    graph.addVertex(2);
    graph.addEdge(0, 1, "a");
    graph.addEdge(1, 2, "b");
    parseGraphAndGetSppfStatistics(S, new JGraphTInput(graph)).value shouldBe SPPFStatistics(1, 1, 2, 2, 0)
  }
}
