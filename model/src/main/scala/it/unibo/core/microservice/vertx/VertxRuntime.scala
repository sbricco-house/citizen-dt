package it.unibo.core.microservice.vertx

import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.core.Vertx
import it.unibo.core.microservice.MicroserviceRuntime

class VertxRuntime(vertx : Vertx, scalaVerticle: () => ScalaVerticle) extends MicroserviceRuntime {
  override def start(): Unit = vertx.deployVerticle(scalaVerticle())
  override def stop(): Unit = vertx.close()
}
