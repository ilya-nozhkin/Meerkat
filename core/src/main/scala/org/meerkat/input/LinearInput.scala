package org.meerkat.input

class LinearInput[L](list: Vector[L], eps: L) extends Input[L] {
  override type Edge = (L, Int)
  override def length: Int = list.length

  override def outEdges(nodeId: Int): Seq[(L, Int)] =
    if (nodeId >= list.length) Seq.empty
    else Seq((list(nodeId), nodeId + 1))

  override def filterEdges(nodeId: Int, label: L): scala.Seq[Edge] =
    if (nodeId >= list.length) Seq.empty
    else if (list(nodeId) == label) Seq((list(nodeId), nodeId + 1))
    else Seq.empty

  override def checkNode(id: Int, label: L): Boolean = true

  // TODO: get rid of
  override def substring(start: Int, end: Int): String =
    throw new RuntimeException("Not supported")

  // TODO: get rid of
  override def epsilonLabel: Any = eps
}

object LinearInput {
  implicit def toInput[L](list: Vector[L])(implicit eps: L): LinearInput[L] =
    new LinearInput(list, eps)
}
