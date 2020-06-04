package it.unibo.covid.bootstrap

import io.vertx.scala.core.Vertx
import it.unibo.covid.data.Parsers
import it.unibo.service.citizen.coap.CoapObservableApi
import it.unibo.service.citizen.websocket.WebSocketCitizenApi
import it.unibo.service.citizen.{CitizenDigitalTwin, CitizenVerticle, RestCitizenApi}

sealed trait CovidCitizenRuntime {
  def start() : Unit
  def stop() : Unit
}
//TODO pass parser as an argument
class HttpOnlyRuntime(httpPort : Int, vertx: Vertx, citizen : CitizenDigitalTwin) extends CovidCitizenRuntime {
  val citizenVerticle = new CitizenVerticle(citizen, Parsers.configureRegistry(), httpPort) with RestCitizenApi with WebSocketCitizenApi
  override def start(): Unit = {
    vertx.deployVerticle(citizenVerticle)
  }
  override def stop(): Unit = {
    vertx.close()
  }
}

class HttpCoapRuntime(httpPort : Int, coapPort : Int, vertx: Vertx, citizen : CitizenDigitalTwin) extends HttpOnlyRuntime(httpPort, vertx, citizen) {
  val coap = CoapObservableApi(citizen, Parsers.configureRegistry(), coapPort)
  override def start(): Unit = {
    super.start()
    coap.start()
  }

  override def stop(): Unit = {
    super.stop()
    coap.destroy()
  }
}

