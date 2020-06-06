package it.unibo.core.microservice.vertx

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.ext.web.{Router, RoutingContext}
import io.vertx.scala.ext.web.handler.CorsHandler


object CorsSupport {
  def apply(): Handler[RoutingContext] = cors
  def enableTo(router: Router): Router = {
    router.route().handler(cors)
    router
  }

  private val allowedHeaders = scala.collection.mutable.Set(
    "Access-Control-Allow-Origin",
    "Authorization",
    "origin",
    "Content-Type",
    "accept",
    "x-requested-with")

  private val cors = CorsHandler.create(".*")
    .allowCredentials(true)
    .allowedHeaders(allowedHeaders)
    .allowedMethod(HttpMethod.GET)
    .allowedMethod(HttpMethod.POST)
    .allowedMethod(HttpMethod.DELETE)
    .allowedMethod(HttpMethod.PATCH)
    .allowedMethod(HttpMethod.OPTIONS)
    .allowedMethod(HttpMethod.PUT)
}
