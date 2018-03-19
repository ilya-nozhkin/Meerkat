package org.meerkat.graph

import org.meerkat.Syntax._
import org.meerkat.input.GraphxInput
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._
import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import org.scalatest.{FunSuite, _}

import scalax.collection.Graph
import scalax.collection.edge.Implicits._

/**
 * Created by sofysmo on 27.11.16.
 */
class GraphTest1 extends FunSuite {
  val A  = syn("a")
  val B  = syn("b")
  val D  = syn("d")
  val AB = A ~ B
  val BA = B ~ A
  val S  = syn(A ~ B ~ D | D ~ B ~ A)

  val g = Graph(
    (0 ~+#> 1)('a'),
    (0 ~+#> 3)('d'),
    (1 ~+#> 2)('b'),
    (2 ~+#> 8)('d'),
    (3 ~+#> 4)('a'),
    (4 ~+#> 7)('b'),
    (0 ~+#> 5)('b'),
    (5 ~+#> 6)('d')
  )

  test("sppfStat") {
    parseGraphAndGetSppfStatistics(S, GraphxInput(g)).value shouldBe SPPFStatistics(5, 2, 4, 7, 0)
  }
}
