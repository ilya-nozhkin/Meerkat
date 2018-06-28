package org.meerkat.util

import org.meerkat.input.Input
import org.meerkat.sppf._
import org.meerkat.tree

import scala.collection.mutable
import scala.util.Try

package object converters {
  private def constructNodeFromDFSSequence(current: SPPFNode, parent: SPPFNode, sequence: Iterator[SPPFNode]): SPPFNode = {
    val clone = current.copy()

    clone match {
      case packed: PackedNode =>
        if (packed.leftChild != null) {
          packed.leftChild = constructNodeFromDFSSequence(packed.leftChild, packed, sequence)
                                .asInstanceOf[NonPackedNode]
        }
        if (packed.rightChild != null) {
          packed.rightChild = constructNodeFromDFSSequence(packed.rightChild, packed, sequence)
                                .asInstanceOf[NonPackedNode]
        }
      case nonpacked: NonPackedNode =>
        if (nonpacked.first != null) {
          val next = if (nonpacked.isAmbiguous) sequence.next else nonpacked.first
          nonpacked.first = constructNodeFromDFSSequence(next, nonpacked, sequence).asInstanceOf[PackedNode]
          nonpacked.rest = null
        }
    }

    clone
  }

  def constructTreeFromDFSChoices(root: NonPackedNode, sequence: Iterator[SPPFNode]): NonPackedNode = {
    if (sequence.isEmpty)
      return root

    val start = root.copy()

    constructNodeFromDFSSequence(start, null, sequence).asInstanceOf[NonPackedNode]
  }

  private def disambiguateNode(node: SPPFNode, sequence: Iterator[SPPFNode]): Seq[SPPFNode] = {
    node match {
      case nonpacked: NonPackedNode =>
        if (nonpacked.first != null) {
          val next = if (nonpacked.isAmbiguous) sequence.next() else nonpacked.first
          nonpacked.first = next.copy().asInstanceOf[PackedNode]
          nonpacked.rest = null

          Seq(nonpacked.first)
        } else Seq()
      case _ =>
        val packed = node.asInstanceOf[PackedNode]

        if (packed.leftChild != null)
          packed.leftChild = packed.leftChild.copy().asInstanceOf[NonPackedNode]
        if (packed.rightChild != null)
          packed.rightChild = packed.rightChild.copy().asInstanceOf[NonPackedNode]

        packed.children
    }
  }

  def constructTreeFromBFSChoices(root: NonPackedNode, sequence: Iterator[SPPFNode]): NonPackedNode = {
    if (sequence.isEmpty)
      return root;

    val start = root.copy()

    var currentLevel = Seq[SPPFNode](start)
    while (currentLevel.nonEmpty) {
      currentLevel = currentLevel.flatMap(node => disambiguateNode(node, sequence))
    }

    start.asInstanceOf[NonPackedNode]
  }

  private class CyclicSPPFException extends Exception;

  private def tryToCountTrees(node: SPPFNode, visited: mutable.Set[SPPFNode]): Int = {
    if (visited.contains(node))
      throw new CyclicSPPFException()

    visited.add(node)

    val count = node match {
      case packed: PackedNode => packed.children.map(tryToCountTrees(_, visited)).sum - (packed.children.size - 1)
      case vertex: VertexNode[_] => 1
      case terminal: EdgeNode[_] => 1
      case epsilon: EpsilonNode => 1
      case nonpacked: NonPackedNode => nonpacked.children.map(tryToCountTrees(_, visited)).sum
    }

    visited.remove(node)

    count
  }

  def tryToCountTrees(root: SPPFNode): Try[Int] = {
    val visited = mutable.HashSet[SPPFNode]()
    Try(tryToCountTrees(root, visited))
  }

  def findAllCycles(roots: Seq[SPPFNode]): Set[(SPPFNode, SPPFNode)] = {
    val visited = mutable.HashSet[SPPFNode]()
    val cycles = mutable.HashSet[(SPPFNode, SPPFNode)]()

    var findCycles: SPPFNode => Unit = null
    findCycles = (node: SPPFNode) => {
      visited.add(node)

      node.children.foreach(child => {
        if (visited.contains(child))
          cycles.add((node, child))
        else
          findCycles(child)
      })

      visited.remove(node)
    }

    roots.foreach(findCycles)
    cycles.toSet
  }

  def extractNonAmbiguousSPPFs(roots: Seq[NonPackedNode],
                               converter: Converter = BFSConverter) = converter(roots)

  def extractNonAmbiguousSPPFs(root: NonPackedNode, converter: Converter): Stream[NonPackedNode] =
    extractNonAmbiguousSPPFs(Seq(root), converter)
  def extractNonAmbiguousSPPFs(root: NonPackedNode): Stream[NonPackedNode] =
    extractNonAmbiguousSPPFs(Seq(root))

  def extractTreesFromSPPF(roots: Seq[NonPackedNode],
                           converter: Converter = BFSConverter)
                          (implicit input: Input[_, _]): Stream[tree.Tree] =
    converter(roots).map(sppf => TreeBuilder.build(sppf, false))

  def extractTreesFromSPPF(root: NonPackedNode, converter: Converter)
                          (implicit input: Input[_, _]): Stream[tree.Tree] =
    extractTreesFromSPPF(Seq(root), converter)(input)

  def extractTreesFromSPPF(root: NonPackedNode)
                          (implicit input: Input[_, _]): Stream[tree.Tree] =
    extractTreesFromSPPF(Seq(root))(input)

  /*private def extractPath(root: tree.Tree, isMostLeft: Boolean): Seq[Int] = root match {
    case node @ tree.RuleNode(_, children) => extractPath(children.head, isMostLeft) ++
      children.tail.flatMap(child => extractPath(child, false))

    case node @ tree.TerminalNode(_, leftExtent, rightExtent) =>
      if (isMostLeft) Seq(leftExtent, rightExtent) else Seq(rightExtent)
  }

  def extractPath(root: tree.Tree): Seq[Int] = root match {
    case node @ tree.RuleNode(_, children) => extractPath(children.head, true) ++
      children.tail.flatMap(child => extractPath(child, false))

    case node @ tree.TerminalNode(_, leftExtent, rightExtent) =>
      Seq(leftExtent, rightExtent)
  }*/
}
