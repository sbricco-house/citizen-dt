package it.unibo.core.microservice.vertx

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.microservice.ServiceRuntime

import scala.util.Try

/**
 * a concept used to create a runtime from a configuration
 */
trait ServiceBootstrap[CONFIG] {
  def runtimeFromJson(config: CONFIG): Try[ServiceRuntime]
}
