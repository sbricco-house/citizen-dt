package it.unibo.core.microservice.vertx

import io.vertx.scala.core.http.HttpServer
import io.vertx.scala.ext.web.Router

/**
 * the abstract interface of component that is used to shape a rest like api (using http and vertx)
 */
trait RestApi extends BaseVerticle {
  /**
   * ABSTRACT METHOD
   * create the http router that handle the different routes
   * @return the router created
   */
  def createRouter : Router

  override def createServer(): HttpServer = super.createServer().requestHandler(createRouter)
}
