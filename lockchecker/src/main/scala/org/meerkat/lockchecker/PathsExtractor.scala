package org.meerkat.lockchecker

import org.meerkat.sppf._

import scala.collection.immutable.HashSet
import scala.collection.mutable

private class DFSPathsExtractorImpl {
  private val visited = mutable.HashSet[NonPackedNode]()
  private val storedResults = mutable.HashMap[NonPackedNode, Set[String]]()

  private def convert(node: SPPFNode): Set[String] = {
    val result = node match {
      case edge: EdgeNode[_] => HashSet(edge.s.toString)
      case vertex: VertexNode[_] => HashSet("")
      case epsilon: EpsilonNode => HashSet.empty[String]
      case nonp: NonPackedNode => {
        val res =
          if (visited.contains(nonp)) {
            storedResults.getOrElse(nonp, HashSet.empty[String])
          }
          else {
            if (storedResults.contains(nonp))
              return storedResults(nonp)

            visited.add(nonp)

            val converted = if (nonp.isAmbiguous) {
              nonp.children.flatMap(convert)
            } else {
              convert(nonp.first)
            }

            visited.remove(nonp)
            converted.toSet[String]
          }

        /*
        val (cyclic, strict) = res.partition(_.exists(pp => pp.placeholder &&
                                                      pp.asInstanceOf[CyclicReference[L]].reference == nonp))

        val replaced =
          cyclic.flatMap(path => {
            if (strict.nonEmpty) {
              strict.map(part =>
                path.flatMap(pp => {
                  if (pp.placeholder && pp.asInstanceOf[CyclicReference[L]].reference == nonp)
                    part
                  else
                    Vector(pp)
                }))
            } else Vector(path)
          })

        strict ++ replaced
        */
        storedResults.put(nonp, res)
        res
      }
      case packed: PackedNode => {
        if (packed.leftChild == null)
          return convert(packed.rightChild)

        if (packed.rightChild == null)
          return convert(packed.leftChild)

        // comment it if you want to get full paths
        val leftName = packed.leftChild.name.toString
        if (leftName == "S0" || leftName == "S1")
          return convert(packed.rightChild)

        val rightName = packed.rightChild.name.toString
        if (rightName == "S0" || rightName == "S1")
          return convert(packed.leftChild)

        val left = convert(packed.leftChild)
        val right = convert(packed.rightChild)

        left.flatMap(lpath => right.map(rpath => lpath  ++ " " ++ rpath))
      }
    }

    result
  }

  def extract(root: NonPackedNode, num: Int): Set[String] = {
    Stream.range(0, num - 1).foreach(i => convert(root))
    convert(root)
  }
}

object DFSPathsExtractor {
  def apply[L](roots: Seq[NonPackedNode], num: Int): Set[String] = {
    val extractor = new DFSPathsExtractorImpl()
    roots.flatMap(root => extractor.extract(root, num)).toSet[String]
  }
}

/*
private class DFSPathsExtractorImpl[L] {
  private type Path = Vector[PathPart[L]]

  private trait PathPart[+L] {
    val placeholder: Boolean = false
  }

  private case class SingleNode[L](value: L) extends PathPart[L] {}

  private case class CyclicReference[L](reference: NonPackedNode) extends PathPart[L] {
    override val placeholder: Boolean = true
  }

  private val visited = mutable.HashSet[SPPFNode]()
  private val storedResults = mutable.HashMap[NonPackedNode, Set[Path]]()

  private def convert(node: SPPFNode): Vector[Path] = {
    val result = node match {
      case edge: EdgeNode[L] => Vector(Vector(SingleNode(edge.s)))
      case vertex: VertexNode[_] => Vector(Vector.empty)
      case epsilon: EpsilonNode => Vector(Vector.empty)
      case nonp: NonPackedNode => {

        // comment it if you want to get full paths
        val name = nonp.name.toString
        if (name == "S0" || name == "S1")
          return Vector(Vector.empty)

        val res =
          if (visited.contains(node)) {
            Vector(Vector(CyclicReference(nonp)))
          }
          else {
            visited.add(node)

            val converted = if (nonp.isAmbiguous) {
              nonp.children.flatMap(convert)
            } else {
              convert(nonp.first)
            }

            visited.remove(node)
            converted
          }.toVector.distinct

        val (cyclic, strict) = res.partition(_.exists(pp => pp.placeholder &&
                                                      pp.asInstanceOf[CyclicReference[L]].reference == nonp))

        val replaced =
          cyclic.flatMap(path => {
            if (strict.nonEmpty) {
              strict.map(part =>
                path.flatMap(pp => {
                  if (pp.placeholder && pp.asInstanceOf[CyclicReference[L]].reference == nonp)
                    part
                  else
                    Vector(pp)
                }))
            } else Vector(path)
          })

        strict ++ replaced
      }
      case packed: PackedNode => {
        if (packed.leftChild == null)
          return convert(packed.rightChild)

        if (packed.rightChild == null)
          return convert(packed.leftChild)

        val left = convert(packed.leftChild)
        val right = convert(packed.rightChild)

        left.flatMap(lpath => right.map(rpath => lpath ++ rpath))
      }
    }

    result
  }

  def extract(root: NonPackedNode): Vector[Vector[L]] = {
    convert(root).map(path => path.map(place => place.asInstanceOf[SingleNode[L]].value))
  }
}

object DFSPathsExtractor {
  def apply[L](roots: Seq[NonPackedNode]): Vector[Vector[L]] =
    roots.flatMap(root => new DFSPathsExtractorImpl[L]().extract(root)).toVector
}*/
