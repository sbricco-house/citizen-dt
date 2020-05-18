package it.unibo.core.microservice.vertx
import io.vertx.scala.core.http.{HttpServer, ServerWebSocket}

trait WebSocketApi extends BaseVerticle {
  def webSocketHandler(serverWebSocket : ServerWebSocket) : Unit

  override def createServer() : HttpServer = {
    super.createServer().webSocketHandler((websocket: io.vertx.scala.core.http.ServerWebSocket) => {
      webSocketHandler(websocket)
    })
  }

}
