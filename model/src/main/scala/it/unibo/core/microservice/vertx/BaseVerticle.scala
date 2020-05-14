package it.unibo.core.microservice.vertx

import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.core.http.ServerWebSocket
import io.vertx.scala.ext.web.Router

import scala.concurrent.Future

abstract class BaseVerticle(port : Int = 8080, host : String = "0.0.0.0") extends ScalaVerticle {
  override def startFuture(): Future[Unit] = {
    val router = createRouter()
    vertx
      .createHttpServer()
      .requestHandler(router.accept)
      .listenFuture(port, host) // <5>
      .map(_ => ()) // <6>
  }

  def createRouter() : Router
}
