package it.unibo.core.microservice.vertx

import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.core.http.{HttpServer, ServerWebSocket}
import io.vertx.scala.ext.web.Router

import scala.concurrent.Future

abstract class BaseVerticle(port : Int = 8080, host : String = "0.0.0.0") extends ScalaVerticle {
  override def startFuture(): Future[Unit] = {
    createServer()
      .listenFuture(port, host)
      .map(_ => ())
  }

  def createServer() : HttpServer = vertx.createHttpServer()
}
