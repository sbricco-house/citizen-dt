package it.unibo.core.microservice.vertx

import io.vertx.scala.core.http.HttpServer
import io.vertx.scala.ext.web.Router

trait RestApi extends BaseVerticle {

  def createRouter : Router

  override def createServer(): HttpServer = super.createServer().requestHandler(createRouter)
}
