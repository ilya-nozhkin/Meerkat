package org.meerkat.graph

import org.meerkat.Syntax._
import org.meerkat.input.GraphxInput
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.OptionValues._

import scalax.collection.Graph
import scalax.collection.edge.Implicits._

class GraphTest5 extends FunSuite {
  val A = syn("a" ~ "b")
  val B: Nonterminal[String] =syn(
    "d" |
      B ~ "d"
  )
  val E: Nonterminal[String] =syn(
    A ~ E ~ B |
      "d"
  )
  val g = Graph(
    (0 ~+#> 1)("a"),
    (1 ~+#> 2)("b"),
    (0 ~+#> 3)("d"),
    (3 ~+#> 2)("a"),
    (2 ~+#> 4)("d"),
    (4 ~+#> 2)("d")
  )
  test("sppfStat") {
    parseGraphAndGetSppfStatistics(E, GraphxInput(g)).value shouldBe SPPFStatistics(7, 6, 5, 14, 1)
  }
}
