package it.unibo.core.microservice.vertx

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.scala.core.Vertx

import scala.concurrent.ExecutionContext

abstract class RouterController {

  protected implicit var executionContext: ExecutionContext = _
  private var _vertx: Vertx = _
  protected def vertx: Vertx = _vertx

  def init(vertx: Vertx): Unit = {
    this._vertx = vertx
    this.executionContext = VertxExecutionContext(vertx.getOrCreateContext())
  }
}