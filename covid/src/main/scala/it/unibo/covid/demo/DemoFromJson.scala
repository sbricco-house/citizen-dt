package it.unibo.covid.demo

import java.net.URI

import io.vertx.lang.scala.json.{Json, JsonObject}
import it.unibo.core.data.{Data, InMemoryStorage, Storage}
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ParserLike
import it.unibo.covid.bootstrap.CitizenBootstrap
import it.unibo.covid.data.Parsers
import it.unibo.service.authentication.AuthenticationService
import it.unibo.service.authentication.client.AuthenticationClient
import it.unibo.service.permission.{AuthorizationService, MockAuthorization}

import scala.io.Source
import scala.util.{Failure, Success}

object DemoFromJson extends App {
  private val authorizationParser = ParserLike.decodeOnly[JsonObject, AuthorizationService] {
    (json : JsonObject) => Some(MockAuthorization.acceptAll(Parsers.configureRegistry()))
  }
  private val authenticationParser = ParserLike.decodeOnly[JsonObject, AuthenticationService] {
    (json : JsonObject) => json.getAsString("auth_client_uri").map(new URI(_)).map(new AuthenticationClient(_))
  }
  private val storageParser = ParserLike.decodeOnly[JsonObject, Storage[Data,String]] {
    (json : JsonObject) => Some(InMemoryStorage[Data, String]())
  }

  val bootstrapper = new CitizenBootstrap(authorizationParser, authenticationParser, storageParser)

  def jsonObjectFromFile(file : String) : JsonObject = Json.fromObjectString(Source.fromResource(file).mkString)
  private val empty = Json.obj(
    "id" -> "gianluca",
    "coap_port" -> 5683,
    "auth_client_uri" -> "unkown"
  )

  val json = args.headOption.map(jsonObjectFromFile).getOrElse(empty)

  bootstrapper.runtimeFromJson(json) match {
    case Success(runtime) => runtime.start()
    case Failure(exception) => println(exception)
  }
}
