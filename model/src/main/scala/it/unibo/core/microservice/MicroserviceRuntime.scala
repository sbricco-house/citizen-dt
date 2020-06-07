package it.unibo.core.microservice

/**
 * a machine in that wrap a microservice with its core and all its interfaces (http, coap,...)
 */
trait MicroserviceRuntime {
  /**
   * start the microservice and all interfaces associated
   */
  def start() : Unit

  /**
   * stop the microservice and all interfaces associated
   */
  def stop() : Unit
}

