package it.unibo.core.microservice.vertx

import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.core.http.{HttpServer, HttpServerOptions, ServerWebSocket}
import io.vertx.scala.ext.web.Router

import scala.concurrent.Future

/**
 * the base verticle used to create http server in wich host microservice logic
 * @param port The http port where the http server starts
 * @param host The host where the http server starts
 */
abstract class BaseVerticle(port : Int = 8080, host : String = "0.0.0.0") extends ScalaVerticle {

  private val options = HttpServerOptions().setReuseAddress(true).setReusePort(true)
  private var httpServer: HttpServer = _
  //TEMPLATE METHOD
  override def startFuture(): Future[Unit] = {
    super.startFuture()
    httpServer = createServer()
    httpServer.listenFuture(port, host)
      .map(_ => ())
  }

  /**
   * abstract method that create an http server
   * @return the http server created
   */
  def createServer() : HttpServer = vertx.createHttpServer(options)

  override def stopFuture(): Future[_] = {
    super.stopFuture()
    httpServer.closeFuture()
  }

}
