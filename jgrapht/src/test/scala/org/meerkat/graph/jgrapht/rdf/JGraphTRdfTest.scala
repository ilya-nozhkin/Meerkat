package org.meerkat.graph.jgrapht.rdf

import org.meerkat.graph.jgrapht.edgesToJGraphT
import org.meerkat.graph.rdf.RdfMixin
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class JGraphTRdfTest extends FunSuite with RdfMixin {
  test("RDF JGraphT test") {
    getResults(edgesToJGraphT) should contain theSameElementsAs rdfs
  }
}
