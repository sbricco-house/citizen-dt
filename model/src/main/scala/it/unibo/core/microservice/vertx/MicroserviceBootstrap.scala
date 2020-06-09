package it.unibo.core.microservice.vertx

import it.unibo.core.microservice.MicroserviceRuntime

import scala.util.Try

/**
 * a concept used to create a microservice runtime from a configuration
 */
trait MicroserviceBootstrap[CONFIG] {
  /**
   * create a service runtime from a configuration object
   * @param config
   * @return Success(runtime) if the configuration is valid, Failure(exc) otherwise
   */
  def runtimeFromJson(config: CONFIG): Try[MicroserviceRuntime]
}
