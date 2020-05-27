package it.unibo.core.microservice.coap

import java.util.concurrent.ScheduledExecutorService

import org.eclipse.californium.core.{CoapResource, CoapServer}

object CaopApi {
  def apply(port : Int, rootResource : CoapResource) : CoapServer = {
    val server = new CoapServer(port)
    server.add(rootResource)
    server
  }

  def withExecutor(port : Int, rootResource : CoapResource, executor : ScheduledExecutorService) : CoapServer = {
    val server = CaopApi(port, rootResource)
    server.setExecutors(executor, executor, false)
    server
  }
}
