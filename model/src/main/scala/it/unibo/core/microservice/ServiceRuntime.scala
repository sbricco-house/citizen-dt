package it.unibo.core.microservice

/**
 * a machine in that wrap a microservice with its core and all its interfaces (http, coap,...)
 */
trait ServiceRuntime {
  def start() : Unit
  def stop() : Unit
}

