package org.meerkat.graph.neo4j

import org.meerkat.input.Input
import org.neo4j.graphdb.{Direction, GraphDatabaseService, Node, Relationship}

import scala.collection.JavaConverters._

class Neo4jInput(db: GraphDatabaseService) extends Input[String] {
  override type Edge = (String, Int)
  private val internalIdToDbId =
    db.getAllNodes.asScala
      .map(_.getId)
      .zipWithIndex
      .map(_.swap)
      .toMap

  private val dbIdToInternalId =
    internalIdToDbId.map(_.swap)

  override def filterEdges(nodeId: Int, label: String): Seq[Edge] =
    db.getNodeById(internalIdToDbId(nodeId))
      .getRelationships(Direction.OUTGOING)
      .asScala
      .collect {
        case r if r.getType.name == label =>
          (r.getType.name, dbIdToInternalId(r.getEndNodeId))
      }
      .toSeq

  override def length: Int =
    internalIdToDbId.size

  override def outEdges(nodeId: Int): Seq[(String, Int)] =
    db.getNodeById(internalIdToDbId(nodeId))
      .getRelationships(Direction.OUTGOING)
      .asScala
      .map(r => (r.getType.name, dbIdToInternalId(r.getEndNodeId)))
      .toSeq

  override def checkNode(nodeId: Int, label: String): Boolean =
    db.getNodeById(internalIdToDbId(nodeId))
      .getLabels
      .asScala
      .exists(_.name == label)

  override def substring(start: Int, end: Int): String =
    throw new RuntimeException("Can not be done for graphs")

  override def epsilonLabel: String = "epsilon"
}
