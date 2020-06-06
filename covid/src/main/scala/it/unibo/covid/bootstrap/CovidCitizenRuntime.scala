package it.unibo.covid.bootstrap

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import it.unibo.core.microservice.ServiceRuntime
import it.unibo.core.microservice.vertx.VertxRuntime
import it.unibo.core.parser.DataParserRegistry
import it.unibo.service.citizen.coap.CoapObservableApi
import it.unibo.service.citizen.websocket.WebSocketCitizenApi
import it.unibo.service.citizen.{CitizenDigitalTwin, CitizenVerticle, RestCitizenApi}

class HttpOnlyRuntime(httpPort : Int,
                      vertx: Vertx,
                      citizen : CitizenDigitalTwin,
                      parserRegistry: DataParserRegistry[JsonObject])
  extends VertxRuntime(vertx, () => new CitizenVerticle(citizen, parserRegistry, httpPort) with RestCitizenApi with WebSocketCitizenApi)


class HttpCoapRuntime(httpPort : Int,
                      coapPort : Int,
                      vertx: Vertx,
                      citizen : CitizenDigitalTwin,
                      parserRegistry: DataParserRegistry[JsonObject]) extends HttpOnlyRuntime(httpPort, vertx, citizen, parserRegistry) {
  val coap = CoapObservableApi(citizen, parserRegistry, coapPort)
  override def start(): Unit = {
    super.start()
    coap.start()
  }

  override def stop(): Unit = {
    super.stop()
    coap.destroy()
  }
}

