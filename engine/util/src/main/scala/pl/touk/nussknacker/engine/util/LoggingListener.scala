package pl.touk.nussknacker.engine.util

import pl.touk.nussknacker.engine.api.exception.EspExceptionInfo
import pl.touk.nussknacker.engine.api.{Context, InterpreterMode, MetaData, ProcessListener}

import scala.util.Try

object LoggingListener extends ProcessListener with Serializable {
  import org.slf4j.LoggerFactory

  val className = getClass.getName.init

  //don't need to cache loggers, because logback already does it
  private def debug(keys: List[String], message: => String) = {
    val loggerKey = keys.mkString(".")
    val logger = LoggerFactory.getLogger(s"$className.$loggerKey")
    if (logger.isDebugEnabled()) {
      logger.debug(message)
    }
  }

  override def nodeEntered(nodeId: String, context: Context, metadata: MetaData, mode: InterpreterMode): Unit = {
    lazy val modeSuffix = if (mode == InterpreterMode.Traverse) "" else " for " + mode
    debug(List(metadata.id, nodeId), s"Node entered$modeSuffix. Context: $context")
  }

  override def deadEndEncountered(lastNodeId: String, context: Context, metadata: MetaData): Unit = {
    debug(List(metadata.id, lastNodeId, "deadEnd"), s"Dead end encountered. Context: $context")
  }

  override def expressionEvaluated(nodeId: String, expressionId: String, expr: String, context: Context, metadata: MetaData, result: Any): Unit = {
    debug(List(metadata.id, nodeId, "expression"), s"invoked expression: $expr with result $result. Context: $context")
  }

  override def serviceInvoked(nodeId: String, id: String, context: Context, metadata: MetaData, params: Map[String, Any], result: Try[Any]): Unit = {
    debug(List(metadata.id, nodeId, "service", id), s"Invocation ended-up with result: $result. Context: $context")
  }

  override def sinkInvoked(nodeId: String, id: String, context: Context, metadata: MetaData, param: Any) = {
    debug(List(metadata.id, nodeId, "sink", id), s"Sink invoked with param: $param. Context: $context")
  }

  override def exceptionThrown(exceptionInfo: EspExceptionInfo[_ <: Throwable]) = {
    //TODO:??
  }
}