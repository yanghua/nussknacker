package pl.touk.esp.engine.uibackend

import cats.data.Validated
import cats.data.Validated._
import pl.touk.esp.engine.flatgraph.flatnode
import pl.touk.esp.engine.flatgraph.flatnode.FlatNode
import pl.touk.esp.engine.graph.expression.Expression
import pl.touk.esp.engine.uibackend.GraphDisplay.Edge

object GraphConverter {

  def toGraph(nodes: List[flatnode.FlatNode]): GraphDisplay.Graph = {
    val ne = toGraphInner(nodes)
    val (n, e) = ne
    GraphDisplay.Graph(n, e)
  }

  private def toGraphInner(nodes: List[flatnode.FlatNode]): (List[GraphDisplay.FlatNode], List[GraphDisplay.Edge]) = {
    def createNextEdge(id: String, tail: List[FlatNode], label: Option[Expression]): List[Edge] = {
      tail.headOption.map(n => GraphDisplay.Edge(id, n.id, label)).toList
    }
    nodes match {
      case flatnode.Source(id, ref) :: tail =>
        val (tailNodes, tailEdges) = toGraphInner(tail)
        (GraphDisplay.Source(id, ref) :: tailNodes, createNextEdge(id, tail, None) ::: tailEdges)
      case flatnode.Sink(id, ref, endResult) :: Nil =>
        (List(GraphDisplay.Sink(id, ref, endResult)), List())
      case flatnode.VariableBuilder(id, varName, fields) :: tail =>
        val (tailNodes, tailEdges) = toGraphInner(tail)
        (GraphDisplay.VariableBuilder(id, varName, fields) :: tailNodes, createNextEdge(id, tail, None) ::: tailEdges)
      case flatnode.Processor(id, service) :: tail =>
        val (tailNodes, tailEdges) = toGraphInner(tail)
        (GraphDisplay.Processor(id, service) :: tailNodes, createNextEdge(id, tail, None) ::: tailEdges)
      case flatnode.Enricher(id, service, output) :: tail =>
        val (tailNodes, tailEdges) = toGraphInner(tail)
        (GraphDisplay.Enricher(id, service, output) :: tailNodes, createNextEdge(id, tail, None) ::: tailEdges)
      case flatnode.Filter(id, expr, nextFalse) :: tail =>
        val (nextFalseNodes, nextFalseEdges) = toGraphInner(nextFalse)
        val nextFalseEdgesConnectedToFilter = nextFalseNodes match {
          case Nil => nextFalseEdges
          case h :: _ => GraphDisplay.Edge(id, h.id, None) :: nextFalseEdges
        }
        val (tailNodes, tailEdges) = toGraphInner(tail)
        (GraphDisplay.Filter(id, expr) :: nextFalseNodes ::: tailNodes, createNextEdge(id, tail, Some(expr)) ::: nextFalseEdgesConnectedToFilter ::: tailEdges)
      case flatnode.Switch(id, expr, exprVal, nexts, defaultNext) :: tail =>
        val (defaultNextNodes, defaultNextEdges) = toGraphInner(defaultNext)
        val defaultNextEdgesConnectedToSwitch = defaultNextNodes match {
          case Nil => defaultNextEdges
          case h :: _ => GraphDisplay.Edge(id, h.id, None) :: defaultNextEdges
        }
        val (tailNodes, tailEdges) = toGraphInner(tail)
        val (nextNodes, nextEdges) = unzipListTuple(nexts.map { c =>
          val (nextNodeNodes, nextNodeEdges) = toGraphInner(c.nodes)
          (nextNodeNodes, nextNodeNodes.headOption.map(n => GraphDisplay.Edge(id, n.id, Some(c.expression))).toList ::: nextNodeEdges)
        })
        (GraphDisplay.Switch(id, expr, exprVal) :: defaultNextNodes ::: nextNodes ::: tailNodes, createNextEdge(id, tail, None) ::: defaultNextEdgesConnectedToSwitch ::: nextEdges ::: tailEdges)
      case Nil =>
        (List(),List())
      case _ =>
        throw new RuntimeException(s"cannot parse nodes: $nodes")
    }
  }

  def fromGraph(graph: GraphDisplay.Graph): Validated[Any, List[flatnode.FlatNode]] = {
    val nodesMap = graph.nodes.groupBy(_.id).mapValues(_.head)
    val edgesFromMapStart = graph.edges.groupBy(_.from)

    def unFlattenNode(n: GraphDisplay.FlatNode, edgesFromMap: Map[String, List[GraphDisplay.Edge]]): List[flatnode.FlatNode] = {
      def unflattenEdgeEnd(id: String, e: GraphDisplay.Edge): List[flatnode.FlatNode] = {
        unFlattenNode(nodesMap(e.to), edgesFromMap.updated(id, edgesFromMap(id).filterNot(_ == e)))
      }
      n match {
        case GraphDisplay.Source(id, ref) =>
          val firstEdge = edgesFromMap(id).head
          flatnode.Source(id, ref) :: unflattenEdgeEnd(id, firstEdge)
        case GraphDisplay.VariableBuilder(id, varName, fields) =>
          val firstEdge = edgesFromMap(id).head
          flatnode.VariableBuilder(id, varName, fields) :: unflattenEdgeEnd(id, firstEdge)
        case GraphDisplay.Processor(id, service) =>
          val firstEdge = edgesFromMap(id).head
          flatnode.Processor(id, service) :: unflattenEdgeEnd(id, firstEdge)
        case GraphDisplay.Enricher(id, service, output) =>
          val firstEdge = edgesFromMap(id).head
          flatnode.Enricher(id, service, output) :: unflattenEdgeEnd(id, firstEdge)
        case GraphDisplay.Filter(id, expr) =>
          val filterEdges = edgesFromMap(id)
          val next = unflattenEdgeEnd(id, filterEdges.head)
          val nextFalse = filterEdges.tail.lastOption.map(nf => unflattenEdgeEnd(id, nf)).toList.flatten
          flatnode.Filter(id, expr, nextFalse) :: next
        case GraphDisplay.Switch(id, expr, exprVal) =>
          val nexts = edgesFromMap(id).collect { case e@GraphDisplay.Edge(_, _, Some(edgeExpr)) =>
            flatnode.Case(edgeExpr, unflattenEdgeEnd(id, e))
          }
          val default = edgesFromMap(id).find(_.label.isEmpty).map { e =>
            unflattenEdgeEnd(id, e)
          }.toList.flatten
          flatnode.Switch(id, expr, exprVal, nexts, default) :: Nil
        case GraphDisplay.Sink(id, ref, endResult) =>
          flatnode.Sink(id, ref, endResult) :: Nil
      }
    }
    graph.nodes.head match {
      case _: GraphDisplay.Source => valid(unFlattenNode(graph.nodes.head, edgesFromMapStart))
      case unexpected => invalid(graph.nodes)
    }
  }

  private def unzipListTuple[A, B](a: List[(List[A], List[B])]): (List[A], List[B]) = {
    val (aList, bList) = a.unzip
    (aList.flatten, bList.flatten)
  }


}