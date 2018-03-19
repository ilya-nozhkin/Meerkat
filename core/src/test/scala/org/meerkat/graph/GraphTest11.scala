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

class GraphTest11 extends FunSuite {
  val E = syn(
    "a" ~ "c" ~ "d" |
      "a" ~ "l" ~ "d"
  )

  val g = Graph(
    (0 ~+#> 1)("a"),
    (1 ~+#> 2)("c"),
    (2 ~+#> 3)("d"),
    (0 ~+#> 4)("a"),
    (4 ~+#> 2)("l")
  )

  test("sppfStat") {
    parseGraphAndGetSppfStatistics(E, GraphxInput(g)).value shouldBe SPPFStatistics(1, 4, 5, 6, 1)
  }
}
