package it.unibo.service.authentication.app

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.{DeploymentOptions, Vertx}
import it.unibo.service.authentication.AuthenticationVerticle

import scala.io.Source

object Run extends App {
  // should be read directly from a file wooooooow
  val config = Source.fromResource("defaultconfig.json").mkString
  val deployConfig = DeploymentOptions().setConfig(new JsonObject(config))
  Vertx.vertx().deployVerticle(new AuthenticationVerticle(), deployConfig)
}
