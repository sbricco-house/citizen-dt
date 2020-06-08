package it.unibo.core.microservice.vertx

import it.unibo.core.microservice.MicroserviceRuntime

import scala.util.Try

/**
 * a concept used to create a microservice runtime from a configuration
 */
trait MicroserviceBootstrap[CONFIG] {
  /**
   *
   * @param config
   * @return
   */
  def runtimeFromJson(config: CONFIG): Try[MicroserviceRuntime]
}
