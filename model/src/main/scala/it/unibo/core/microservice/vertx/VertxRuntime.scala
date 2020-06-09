package it.unibo.core.microservice.vertx

import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.core.Vertx
import it.unibo.core.microservice.MicroserviceRuntime

/**
 * A runtime based on vertx framework
 * @param vertx The vertx instance
 * @param scalaVerticle The verticle that will be instantiate in vertx
 */
class VertxRuntime(vertx : Vertx, scalaVerticle: () => ScalaVerticle) extends MicroserviceRuntime {
  override def start(): Unit = vertx.deployVerticle(scalaVerticle())
  override def stop(): Unit = vertx.close()
}
