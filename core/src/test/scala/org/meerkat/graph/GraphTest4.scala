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

class GraphTest4 extends FunSuite {
  val E: Nonterminal[String] =syn(
    "a" ~ "b" ~ E |
      "a" ~ "b"
  )
  val g = Graph(
    (0 ~+#> 1)("a"),
    (1 ~+#> 0)("b")
  )

  test("sppfStat") {
    parseGraphAndGetSppfStatistics(E, GraphxInput(g)).value shouldBe SPPFStatistics(1, 3, 2, 5, 1)
  }
}
