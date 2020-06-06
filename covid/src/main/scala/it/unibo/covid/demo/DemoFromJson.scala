package it.unibo.covid.demo

import java.net.URI

import io.vertx.core.json.JsonArray
import io.vertx.lang.scala.json.{Json, JsonObject}
import it.unibo.core.data.{Data, InMemoryStorage, Storage}
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ParserLike
import it.unibo.covid.bootstrap.CitizenBootstrap
import it.unibo.covid.data.Parsers
import it.unibo.service.authentication.AuthenticationService
import it.unibo.service.authentication.client.AuthenticationClient
import it.unibo.service.permission.AuthorizationService
import it.unibo.service.permission.client.AuthorizationClient

import scala.io.Source
import scala.util.{Failure, Success}

object DemoFromJson extends App {
  def jsonObjectFromFile(file : String) : JsonObject = Json.fromObjectString(Source.fromResource(file).mkString)
  def jsonArrayFromFile(file: String) : JsonArray = Json.fromArrayString(Source.fromResource(file).mkString)

  val json = args.headOption.orElse(Some("default-citizen.json")).map(jsonObjectFromFile).get
  val registry = args.lift(1) match {
    case None => Parsers.configureRegistry()
    case Some(file) => Parsers.configureRegistryFromJson(jsonArrayFromFile(file))
  }

  private val authorizationParser = ParserLike.decodeOnly[JsonObject, AuthorizationService] {
    (json : JsonObject) => json.getAsString("authorization_client_uri").map(new URI(_)).map(AuthorizationClient(_, registry))
  }
  private val authenticationParser = ParserLike.decodeOnly[JsonObject, AuthenticationService] {
    (json : JsonObject) => json.getAsString("authentication_client_uri").map(new URI(_)).map(AuthenticationClient(_))
  }
  private val storageParser = ParserLike.decodeOnly[JsonObject, Storage[Data,String]] {
    (json : JsonObject) => Some(InMemoryStorage[Data, String]())
  }
  val bootstrapper = new CitizenBootstrap(authorizationParser, authenticationParser, registry, storageParser)

  bootstrapper.runtimeFromJson(json) match {
    case Success(runtime) => runtime.start()
    case Failure(exception) => println(exception)
  }
}
