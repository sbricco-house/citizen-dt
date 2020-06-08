package it.unibo.core.microservice.vertx
import io.vertx.scala.core.http.{HttpServer, ServerWebSocket}

/**
 * the abstract interface of component that is used to shape a websocket like api (using http and vertx)
 */
trait WebSocketApi extends BaseVerticle {
  /**
   * ABSTRACT METHOD
   *
   * the logic used to manage a web socket connections
   * @param serverWebSocket the websocket created from a client request.
   */
  def webSocketHandler(serverWebSocket : ServerWebSocket) : Unit

  override def createServer() : HttpServer = {
    super.createServer().webSocketHandler((websocket: io.vertx.scala.core.http.ServerWebSocket) => {
      webSocketHandler(websocket)
    })
  }

}
