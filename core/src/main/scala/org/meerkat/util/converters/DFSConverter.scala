package org.meerkat.util.converters

import org.meerkat.sppf._

import scala.collection.mutable


private class DFSConverterInstance {
  type Choices = List[SPPFNode]
  private trait ConversionResult extends (() => List[Choices]) {
    val cyclic: Boolean
    val cyclicReferences: Seq[NonPackedNode] = null
  }

  private val visited = mutable.HashSet[SPPFNode]()
  private val storedResults = mutable.HashMap[NonPackedNode, List[Choices]]()

  private val terminalResult = new ConversionResult {
    override val cyclic = false
    override def apply(): List[Choices] = List(List.empty)
  }

  private val emptyResult = new ConversionResult {
    override val cyclic = false
    override def apply(): List[Choices] = List.empty
  }

  private class CyclicResult(ref: NonPackedNode) extends ConversionResult {
    override val cyclic = true
    override val cyclicReferences: Seq[NonPackedNode] = Seq(ref)
    override def apply(): List[Choices] =
      storedResults(ref)
  }

  private class NonPackedResult(children: Seq[(SPPFNode, ConversionResult)],
                                parentNode: NonPackedNode) extends ConversionResult {
    override val cyclic = children.exists{case (c, r) => r.cyclic}
    override val cyclicReferences: Seq[NonPackedNode] = if (!cyclic) null else
      children.flatMap{case (c, r) => if (r.cyclic) r.cyclicReferences else Seq()}

    override def apply(): List[Choices] = {
      val (lazyChildren, strictChildren) = children.partition{
        case (c, r) => r.cyclic && r.cyclicReferences.contains(parentNode)}

      val strictResult = strictChildren.flatMap{case (c, r) => r.apply().map(c +: _)}
      storedResults.put(parentNode, strictResult.toList)
      val lazyResult = lazyChildren.flatMap{case (c, r) => r.apply().map(c +: _)}
      val res = (lazyResult ++ strictResult).toList
      res
    }
  }

  private class PackedResult(leftChild: ConversionResult, rightChild: ConversionResult) extends ConversionResult {
    override val cyclic = leftChild.cyclic || rightChild.cyclic
    override val cyclicReferences: Seq[NonPackedNode] = {
      if (!rightChild.cyclic)
        leftChild.cyclicReferences
      else if (!leftChild.cyclic)
        rightChild.cyclicReferences
      else leftChild.cyclicReferences ++ rightChild.cyclicReferences
    }

    override def apply(): List[Choices] = {
      val leftResult = leftChild()
      val rightResult = rightChild()

      if (leftResult.isEmpty)
        return rightResult

      if (rightResult.isEmpty)
        return leftResult

      leftResult.flatMap(leftChoices => rightResult.map(rightChoices => leftChoices ++ rightChoices))
    }
  }

  private def convert(node: SPPFNode): ConversionResult = {
    if (node.isInstanceOf[NonPackedNode]) {
      if (visited.contains(node)) {
        return new CyclicResult(node.asInstanceOf[NonPackedNode])
      }

      visited.add(node)
    }

    val result = node match {
      case edge: EdgeNode[_] => terminalResult
      case vertex: VertexNode[_] => terminalResult
      case epsilon: EpsilonNode => terminalResult
      case nonp: NonPackedNode => {
        if (nonp.isAmbiguous) {
          val convertedChildren = nonp.children.map(c => (c, convert(c)))

          new NonPackedResult(convertedChildren, nonp)
        } else {
          convert(nonp.first)
        }
      }
      case packed: PackedNode => {
        val left = if (packed.leftChild != null) convert(packed.leftChild) else emptyResult
        val right = if (packed.rightChild != null) convert(packed.rightChild) else emptyResult

        new PackedResult(left, right)
      }
    }

    if (node.isInstanceOf[NonPackedNode])
      visited.remove(node)

    return result
  }

  def extract(root: NonPackedNode): List[NonPackedNode] =
    convert(root).apply().map(choices => {constructTreeFromDFSChoices(root, choices.toIterator)})
}

object DFSConverter extends Converter {
  override def apply(roots: Seq[NonPackedNode]): Stream[NonPackedNode] =
    roots.flatMap(root => new DFSConverterInstance().extract(root)).toStream
}