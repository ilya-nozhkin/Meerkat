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

class GraphTest7 extends FunSuite {
  val S: Nonterminal[String] =
    syn(
      "a" ~ S ~ "b"
        | "a" ~ S
        | "c"
    )
  val g = Graph(
    (0 ~+#> 1)("a"),
    (1 ~+#> 2)("a"),
    (1 ~+#> 2)("c"),
    (2 ~+#> 3)("b")
  )

  test("sppfStat") {
    parseGraphAndGetSppfStatistics(S, GraphxInput(g)).value shouldBe SPPFStatistics(3, 3, 4, 6, 0)
  }
}
