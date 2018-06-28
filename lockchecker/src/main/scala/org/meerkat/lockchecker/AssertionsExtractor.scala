package org.meerkat.lockchecker

import org.meerkat.sppf._

import scala.collection.immutable.HashSet
import scala.collection.mutable

private class DFSAssertionsExtractorImpl {
  private val visited = mutable.HashSet[NonPackedNode]()
  private val found = mutable.HashSet[String]()

  private def extract(node: SPPFNode): Unit = {
    val result = node match {
      case edge: EdgeNode[_] => HashSet(edge.s.toString)
      case vertex: VertexNode[_] => HashSet("")
      case epsilon: EpsilonNode => HashSet.empty[String]
      case nonp: NonPackedNode => {
        val res =
          if (visited.contains(nonp)) {}
          else {
            visited.add(nonp)
            if (nonp.name.toString == "ba")
              found.add(nonp.first.children.head.name.toString)
            else
              nonp.children.foreach(extract)
          }

        res
      }
      case packed: PackedNode => {
        if (packed.leftChild != null)
          extract(packed.leftChild)

        if (packed.rightChild != null)
          extract(packed.rightChild)
      }
    }

    result
  }

  def find(root: NonPackedNode): Set[String] = {
    extract(root)
    found.toSet
  }
}

object DFSAssertionsExtractor {
  def apply[L](roots: Seq[NonPackedNode], num: Int): Set[String] = {
    val extractor = new DFSAssertionsExtractorImpl()
    roots.flatMap(root => extractor.find(root)).toSet
  }
}
