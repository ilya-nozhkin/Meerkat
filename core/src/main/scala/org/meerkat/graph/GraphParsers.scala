package org.meerkat.graph
import org.meerkat.input.Input
import org.meerkat.parsers.CPSResult
import org.meerkat.parsers.Parsers._
import org.meerkat.sppf.{NonPackedNode, SPPFLookup, TerminalNode}
import org.meerkat.tree.{TerminalSymbol, VertexSymbol}

object GraphParsers {
  def E[L](label: L): Terminal[L] =
    terminal(label)

  // TODO: fix naming if critical
  def anyE[L]: Terminal[L] = new Terminal[L] {
    override def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]): CPSResult[TerminalNode[L]] =
      input.outEdges(i) match {
        case edges if edges.isEmpty => CPSResult.failure
        case edges: Seq[(L, Int)] =>
          val terminals = edges.map {
            case (edgeName, to) =>
              CPSResult.success(sppfLookup.getTerminalNode(edgeName, i, to))
          }
          terminals.reduceLeft(_.orElse(_))
      }
    override def name: String     = "anyE"
    override def symbol           = TerminalSymbol(name)
    override def toString: String = name
  }

  def V[L](label: L): Vertex[L] = new Vertex[L]  {
    override def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]): CPSResult[NonPackedNode] =
      if (input.checkNode(i, label))
        CPSResult.success(sppfLookup.getEpsilonNode(i)) // TODO: replace with vertex node
      else CPSResult.failure

    override def symbol: VertexSymbol = VertexSymbol("label")
    override def name: String         = "label"
  }

}
