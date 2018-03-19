package org.meerkat

import org.meerkat.parsers.Parsers.Nonterminal
import org.scalatest.{FunSuite, Matchers}
import org.meerkat.Syntax._
import org.meerkat.input.LinearInput._
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._

class LinearInputTest1 extends FunSuite with Matchers {
  val S: Nonterminal[String] =
    syn("a" ~ "b" ~ "a" | "a" ~ S ~ "a")

  test("test") {
    val inputList = Vector("a", "a", "b", "a", "a")
    implicit val eps: String = ""
    getSPPF(S, inputList) shouldBe 'Right
  }
}
