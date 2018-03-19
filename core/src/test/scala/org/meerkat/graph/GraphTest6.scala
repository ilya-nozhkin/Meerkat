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

class GraphTest6 extends FunSuite {
  val E: Nonterminal[String] =syn(
    "(" ~ E ~ ")" |
      "N"
  )
  val S = syn(E)
  val g = Graph(
    (0 ~+#> 1)("("),
    (1 ~+#> 2)("N"),
    (2 ~+#> 3)(")")
  )

  test("sppfStat") {
    parseGraphAndGetSppfStatistics(S, GraphxInput(g)).value shouldBe SPPFStatistics(3, 2, 3, 5, 0)
  }
}
