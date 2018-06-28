package org.meerkat.lockchecker

import java.io.{FileWriter, PrintWriter}

import scala.io.Source
import org.meerkat.Syntax._
import org.meerkat.input.GraphxInput
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._
import org.meerkat.graph._
import org.meerkat.util.converters._
import org.meerkat.util.visualization._
import scalax.collection.mutable.Graph
import scalax.collection.edge.Implicits._
import scalax.collection.edge.LkDiEdge

object Main extends App {
  def loadGraph(name: String): (List[Int], GraphxInput[String], StringNonterminal) = {
    val lines = Source.fromFile(name).getLines().toVector
    val count = lines.size

    val starts = lines(count - 1)
      .split(" ")
      .map(_.toInt)

    val info = lines(count - 2).split(" ").map(_.toInt)
    var grammar: StringNonterminal = generateGrammar(info(2), info(1), info(3))

    val edges = lines.take(count - 2).filter(_.length > 0)
      .map(_.split(" "))
      .map({case Array(s, l, e) => (s.toInt ~+#> e.toInt)(l)})

    val graph = Graph[Int, LkDiEdge](edges.toList: _*)

    (starts.filter(node => graph.find(node).isDefined).toList, GraphxInput[String](graph), grammar)
  }

  type StringNonterminal = Nonterminal[String, Nothing]

  private def brackets(count: Int, left: String, body: StringNonterminal, right: String,
                       next: StringNonterminal) = {
    val list = Stream.range(0, count).map(i => (left + i) ~ body ~ (right + i) ~ next).toList

    if (count == 1)
      list.head | list.head
    else {
      val z = list(0) | list(1)
      list.drop(2).foldLeft(z){case (r, a) => r | a}
    }
  }

  def generateAsserts(count: Int): AlternationBuilder[String, Nothing, NoValue] = {
    val list = Stream.range(0, count).map(i => "A" + i)
    val start = list(0) | list(1)
    list.drop(2).foldLeft(start){case (r, a) => r | a}
  }

  def generateGrammar(locks: Int, calls: Int, asserts: Int): StringNonterminal = {
    val ba = if (asserts == 1) syn("A0") else syn(generateAsserts(asserts))
    val ca = if (asserts == 1) syn("A0") else syn(generateAsserts(asserts))

    lazy val S0: Nonterminal[String, Nothing] = syn(
      (ca ~ S0) | ca | epsilon |
      brackets(calls, "C", S0, "RT", S0) |
      brackets(locks, "G", S0, "RL", S0))

    lazy val S1: Nonterminal[String, Nothing] = syn(epsilon |
      brackets(calls, "C", S1, "RT", S1) |
      brackets(locks, "G", S0, "RL", S1))

    lazy val S: Nonterminal[String, Nothing] = syn(
      (ba ~ S) | (S ~ ba) | (S ~ S1) | (S1 ~ S) | ba |
      brackets(calls, "C", S, "RT", S1) |
      brackets(calls, "C", S, "RT", S))

    S
  }

  def test(): Unit = {
    lazy val S: Nonterminal[String, Nothing] = syn("a" ~ S ~ "b" | epsilon)

    val graph = Graph(
      (0 ~+#> 1)("b"),
      (0 ~+#> 0)("a")
    )

    val input = GraphxInput(graph)

    val roots = parseGraphFromSpecifiedPositions(S, input, List(0))
    visualize(roots(0), input)(toDot)

    val paths = DFSPathsExtractor[String](roots, 1)

    paths.foreach(println)
  }

  def execute(graphFile: String, outputFile: String): Unit = {
    val checkTime = args.exists(_ == "--showtime")
    val assertionsOnly = args.exists(_ == "--assertions-only")
    val full = args.exists(_ == "--full")

    var tstart: Long = 0
    var tend: Long = 0

    if (checkTime)
      tstart = System.currentTimeMillis()

    val (startNodes, graphInput, grammar) = loadGraph(graphFile)

    if (checkTime) {
      tend = System.currentTimeMillis()
      println("loaded " + (tend - tstart))
    }

    if (checkTime)
      tstart = System.currentTimeMillis()

    print("Parsing from: ")
    startNodes.foreach(n => print(n + " "))
    println()

    val roots = parseGraphFromSpecifiedPositions(grammar, graphInput, startNodes)

    print("Start nodes: ")
    roots.foreach(root => print(root.toString + " "))
    println()

    if (checkTime) {
      tend = System.currentTimeMillis()
      println("parsed " + (tend - tstart))
    }

    if (checkTime)
      tstart = System.currentTimeMillis()

    if (assertionsOnly || full) {
      val assertions = DFSAssertionsExtractor(roots, 1)

      if (checkTime) {
        tend = System.currentTimeMillis()
        println("Assertions extracted " + (tend - tstart))
      }

      if (outputFile == "") {
        assertions.foreach(assertion => print(assertion + " "))
        println()
        println(assertions.size)
      } else {
        val pw = new PrintWriter(new FileWriter((if (full) "assert_" else "") + outputFile))

        assertions.foreach(assertion => pw.print(assertion + " "))

        pw.close()
      }
    }
    if (!assertionsOnly || full) {
      val paths = DFSPathsExtractor(roots, 1)

      if (checkTime) {
        tend = System.currentTimeMillis()
        println("Paths extracted " + (tend - tstart))
      }

      if (outputFile == "") {
        paths.foreach(println)
        println(paths.size)
      } else {
        val pw = new PrintWriter(new FileWriter(outputFile))

        paths.foreach(pw.println)

        pw.close()
      }
    }
  }

  //test()
  val options = List("--showtime", "--assertions-only", "--full")
  val inarg = args.find(p => !options.contains(p))
  var outarg: Option[String] = Option.empty

  if (inarg.isDefined)
    outarg = args.find(p => !options.contains(p) && p != inarg.get)

  execute(inarg.getOrElse("graph"),
          outarg.getOrElse(""))
}
