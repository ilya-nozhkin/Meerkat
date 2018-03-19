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

class GraphTest12 extends FunSuite {
  val E = syn(
    "a" ~ "c" ~ "d" |
      "b" ~ "c"
  )
  val g = Graph(
    (0 ~+#> 1)("a"),
    (0 ~+#> 3)("a"),
    (0 ~+#> 5)("a"),
    (5 ~+#> 6)("c"),
    (6 ~+#> 7)("d"),
    (1 ~+#> 2)("c"),
    (3 ~+#> 2)("c"),
    (2 ~+#> 4)("d")
  )

  test("sppfStat") {
    parseGraphAndGetSppfStatistics(E, GraphxInput(g)).value shouldBe SPPFStatistics(2, 4, 8, 7, 1)
  }
}
