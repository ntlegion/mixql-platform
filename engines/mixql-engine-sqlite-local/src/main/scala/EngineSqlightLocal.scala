package org.mixql.engine.sqlite.local

import org.mixql.core.context.{EngineContext, gtype}
import org.mixql.core.context.gtype.Type
import org.mixql.core.engine.Engine
import org.mixql.engine.local.logger.IEngineLogger

import scala.collection.mutable

class EngineSqlightLocal(dbPathParameter: Option[String] = None)
    extends Engine
    with IEngineLogger
    with java.lang.AutoCloseable:

  var context: SQLightJDBC = null

  override def name: String = "mixql-engine-sqlite-local"

  override def executeImpl(statement: String, ctx: EngineContext): gtype.Type = {
    logInfo(s"Received statement to execute: ${statement}")
    logDebug(s"Executing command ${statement}")

    initContextIfEmpty(ctx)

    val res = context.execute(statement)
    logInfo(s"Successfully executed command ${statement}")
    logDebug(s"Returning result of  executed command '${statement}': $res")
    res
  }

  private def initContextIfEmpty(ctx: EngineContext): Unit =
    if context == null then
      logDebug(s"Init SQlightJDBC context")
      context = SQLightJDBC(name, ctx, dbPathParameter)

  override def executeFuncImpl(name: String, ctx: EngineContext, kwargs: Map[String, Object], params: Type*): Type = {
    try
      logInfo(s"Started executing function $name")
      logDebug(s"Params provided for function $name : " + params.toString())
      logDebug(s"Executing function $name with params " + params.toString + "\n" +
      "And named params: " + kwargs.mkString(","))
      initContextIfEmpty(ctx)
      Thread.sleep(1000)
      logInfo(s"Successfully executed function $name with params " + params.toString)
      new gtype.Null()
    catch
      case e: Throwable =>
        throw new Exception(
          s"[ENGINE ${this.name}]: error while executing function $name: " +
            e.getMessage
        )
  }

  override def paramChangedImpl(name: String, ctx: EngineContext): Unit = {
    try {
      logDebug(s"Received notification that param $name was changed")
    } catch {
      case e: Throwable => throw new Exception(s"[ENGINE ${this.name}] error while setting parameter: " + e.getMessage)
    }
  }

  override def close(): Unit = if context != null then context.close()
