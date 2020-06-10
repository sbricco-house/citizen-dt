package it.unibo.core.microservice

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
  def runtimeFromConfiguration(config: CONFIG): Try[MicroserviceRuntime]
}
