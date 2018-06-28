package org.meerkat.util.converters

import org.meerkat.Syntax._
import org.meerkat.input.GraphxInput
import org.meerkat.parsers.Parsers.{Nonterminal, _}
import org.meerkat.parsers._
import org.meerkat.sppf.NonPackedNode
import org.meerkat.tree._
import org.meerkat.util.converters.TestUtils._
import org.scalatest.{FunSuite, Matchers}

import scalax.collection.Graph
import scalax.collection.edge.Implicits._
import scalax.collection.edge.LkDiEdge

object GraphInputTestCases extends Matchers {
  def nonAmbiguousGrammarTwoPathsTestCorrectness(converter: Converter): Unit = {
    var S: Nonterminal[String, Nothing] = null
    S = syn("x" ~ "+" ~ "x" | "x" ~ "-" ~ "x")

    val graph = Graph(
      (0 ~+#> 1)("x"),
      (1 ~+#> 2)("+"),
      (1 ~+#> 3)("-"),
      (2 ~+#> 4)("x"),
      (3 ~+#> 4)("x"))

    val trees = getTrees(graph, S, converter).toList

    trees.size shouldBe 2

    val rule1 = Rule(S.symbol, Sequence(EdgeSymbol("x"), EdgeSymbol("+"), EdgeSymbol("x")))
    val rule2 = Rule(S.symbol, Sequence(EdgeSymbol("x"), EdgeSymbol("-"), EdgeSymbol("x")))

    compareTreesIgnoringExtents(trees(0),
      RuleNode(rule1, Seq(terminalNode("x"), terminalNode("+"), terminalNode("x")))
    ) shouldBe true

    compareTreesIgnoringExtents(trees(1),
      RuleNode(rule2, Seq(terminalNode("x"), terminalNode("-"), terminalNode("x")))
    ) shouldBe true
  }

  /*
  test("asdfasdfasdf") {
    var S: Nonterminal[String] = null
    S = syn(S ~ S | "a")

    val graph = Graph(
      (0 ~+#> 0)("a")
    )

    getTrees(graph, S).take(10).foreach(println)
  }*/

  def nonAmbiguousGrammarInfiniteNumberOfPathsTestCorrectness(converter: Converter): Unit = {
    var S: Nonterminal[String, Nothing] = null
    S = syn("a" ~ S ~ "b" | "a" ~ "b")

    val graph = Graph(
      (0 ~+#> 1)("a"),
      (1 ~+#> 2)("a"),
      (2 ~+#> 0)("a"),
      (0 ~+#> 3)("b"),
      (3 ~+#> 0)("b"))

    val trees = getTrees(graph, S, converter)

    val rule1 = Rule(S.symbol, Sequence(EdgeSymbol("a"), S.symbol, EdgeSymbol("b")))
    val rule2 = Rule(S.symbol, Sequence(EdgeSymbol("a"), EdgeSymbol("b")))

    val minTree = RuleNode(rule2, Seq(terminalNode("a"), terminalNode("b")))
    val expectedTrees = Stream.iterate(minTree)(tree => RuleNode(rule1, Seq(terminalNode("a"), tree, terminalNode("b"))))
      .zipWithIndex.filter({case (_, index) => index % 3 == 2}).map({case (tree, _) => tree})

    expectedTrees.zip(trees).take(10).foreach {
      case (expected, actual) => compareTreesIgnoringExtents(expected, actual) shouldBe true}
  }

  def ambiguousGrammarTwoPathsTestQuantity(converter: Converter): Unit = {
    var S: Nonterminal[String, Nothing] = null
    S = syn("x" ~ S | S ~ "x" | "x" ~ "x")

    val graph = Graph(
      (0 ~+#> 1)("x"),
      (1 ~+#> 2)("x"),
      (2 ~+#> 3)("x"),
      (0 ~+#> 4)("x"),
      (4 ~+#> 5)("x"),
      (5 ~+#> 6)("x"))

    getTrees(graph, S, converter).size shouldBe 6
  }

  def nonAmbiguousGrammarTestPaths(converter: Converter): Unit = {
    /*
    var S: Nonterminal[String, Nothing] = null
    S = syn(S ~ "b" ~ "b" | S ~ "c" | "a")

    val graph = Graph(
      (0 ~+#> 1)("a"),
      (1 ~+#> 2)("b"),
      (2 ~+#> 3)("b"),
      (1 ~+#> 4)("c"),
      (0 ~+#> 6)("b"),
      (6 ~+#> 7)("c"),
      (7 ~+#> 8)("c"))

    val paths = getTrees(graph, S, converter).map(extractPath).toSet
    val expected = Set(Seq(0, 1), Seq(0, 1, 4), Seq(0, 1, 2, 3))

    paths shouldBe expected
    */
  }

  def getTrees[V](graph: Graph[Int, LkDiEdge],
                  S: AbstractCPSParsers.AbstractSymbol[String, Nothing, NonPackedNode, V],
                  converter: Converter): Stream[Tree] = {
    val input = new GraphxInput(graph)
    extractTreesFromSPPF(getSPPFs(S, input).right.getOrElse(null)._1, converter)(input)
  }
}
