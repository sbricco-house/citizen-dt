package it.unibo.service.authentication.app

import io.vertx.lang.scala.json.Json
import it.unibo.service.authentication.bootstrap.AuthenticationBootstrap

import scala.io.Source
import scala.util.{Failure, Success}

object Run extends App {
  // should be read directly from a file wooooooow
  val config = Json.fromObjectString(Source.fromResource("defaultconfig.json").mkString)
  val storage = MockUserStorage.generateDefault()
  new AuthenticationBootstrap(storage).runtimeFromConfiguration(config) match {
    case Failure(exception) => println(exception)
    case Success(runtime) => runtime.start()
  }
}
