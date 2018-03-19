package org.meerkat.graph.rdf

import java.io.FileInputStream
import java.net.URI

import org.apache.jena.rdf.model.ModelFactory
import org.meerkat.Syntax._
import org.meerkat.graph._
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._
import org.meerkat.graph._
import org.meerkat.input.Input
import org.meerkat.tree.NonterminalSymbol

import scala.collection.JavaConverters._

trait RdfMixin {
  type L = String
  val rdfs =
    List(
      ("atom-primitive.owl", 15454, 122),
      ("biomedical-mesure-primitive.owl", 15156, 2871),
      ("foaf.rdf", 4118, 10),
      ("funding.rdf", 17634, 1158),
      ("generations.owl", 2164, 0),
      ("people_pets.rdf", 9472, 37),
      ("pizza.owl", 56195, 1262),
      ("skos.rdf", 810, 1),
      ("travel.owl", 2499, 63),
      ("univ-bench.owl", 2540, 81),
      ("wine.rdf", 66572, 133)
    )

  private val grammar = new AnyRef {
    private def sameGen(bs: List[(Symbol[L, _], Symbol[L, _])]): Symbol[L, _] =
      bs.map { case (ls, rs) => ls ~ syn(sameGen(bs) | epsilon) ~ rs } match {
        case x :: Nil     => syn(epsilon | x)
        case x :: y :: xs => syn(xs.foldLeft(x | y)(_ | _))
      }

    val G1 =
      syn(sameGen(List(("subclassof-1", "subclassof"), ("type-1", "type"))))

    val G2 =
      syn(sameGen(List(("subclassof-1", "subclassof"))) ~ "subclassof")
  }

  def getResults(edgesToGraph: (List[(Int, String, Int)], Int) => Input[L]): List[(String, Int, Int)] =
    rdfs.map {
      case (file, _, _) =>
        val ((res1, _), (res2, _)) = queryRdf(file, edgesToGraph)
        (file, res1, res2)
    }
  def benchmark(times: Int, edgesToGraph: (List[(Int, String, Int)], Int) => Input[L]): List[(String, Long, Long)] =
    rdfs.map {
      case (file, _, _) =>
        val (time1, time2) =
          List
            .fill(times)(queryRdf(file, edgesToGraph))
            .foldLeft((0L, 0L)) { case (acc, ((_, t1), (_, t2))) => (acc._1 + t1, acc._2 + t2) }
        (file, time1 / times, time2 / times)
    }

  def queryRdf(file: String, edgesToGraph: (List[(Int, String, Int)], Int) => Input[L]): ((Int, Long), (Int, Long)) = {
    val triples             = getTriples(file)
    val (edges, nodesCount) = triplesToEdges(triples)
    val graph               = edgesToGraph(edges, nodesCount)

    def parseAndGetRunningTime(grammar: AbstractCPSParsers.AbstractSymbol[L,_, _]) = {
      val start = System.currentTimeMillis
      val res   = parseGraphFromAllPositions(grammar, graph).length
      val end   = System.currentTimeMillis
      (res, end - start)
    }

    val r1 = parseAndGetRunningTime(grammar.G1)
    val r2 = parseAndGetRunningTime(grammar.G2)
    (r1, r2)
  }

  def triplesToEdges(triples: List[(String, String, String)]): (List[(Int, String, Int)], Int) = {
    val nodes =
      triples.flatMap { case (f, _, t) => List(f, t) }.toSet.zipWithIndex.toMap
    val edges = triples.flatMap {
      case (f, l, t) =>
        val from  = nodes(f)
        val to    = nodes(t)
        val label = Option(new URI(l).getFragment).map(_.toLowerCase)
        label match {
          case Some("type") =>
            List((from, "type", to), (to, "type-1", from))
          case Some("subclassof") =>
            List((from, "subclassof", to), (to, "subclassof-1", from))
          case Some(lbl) => List((from, lbl, to))
          case None      => List((from, "noLabel", to))
        }
    }
    (edges, nodes.size)
  }

  def getTriples(file: String): List[(String, String, String)] = {
    val inputStream = new FileInputStream(getClass.getResource(s"/rdf/$file").getFile)

    val model = ModelFactory.createDefaultModel
    model.read(inputStream, null)
    model
      .listStatements()
      .asScala
      .map { stmt =>
        (stmt.getObject.toString, stmt.getPredicate.toString, stmt.getSubject.toString)
      }
      .toList
  }
}
