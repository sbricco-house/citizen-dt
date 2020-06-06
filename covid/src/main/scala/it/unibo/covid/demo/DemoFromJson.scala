package it.unibo.covid.demo

import java.net.URI

import io.vertx.core.json.JsonArray
import io.vertx.lang.scala.json.{Json, JsonObject}
import it.unibo.core.data.{Data, InMemoryStorage, LeafCategory, Sensor, Storage}
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

  def jsonObjectFromFile(file : String) : JsonObject = Json.fromObjectString(Source.fromResource(file).mkString)
  def jsonArrayFromFile(file: String) : JsonArray = Json.fromArrayString(Source.fromFile(file).mkString)

  private val empty = Json.obj(
    "id" -> "citizen1",
    "coap_port" -> 5683,
    "http_port" -> 8082,
    "auth_client_uri" -> "http://localhost:8081"
  )
  private val categories = Json.fromArrayString(
    """
      |[
      |  {
      |    "name": "spo2",
      |    "ttl": -1,
      |    "type": "double",
      |    "groups": ["medicalData"]
      |  }
      |]
      |""".stripMargin)

  val json = args.headOption.map(jsonObjectFromFile).getOrElse(empty)
  val jsonRegistry = args.lastOption.map(jsonArrayFromFile).getOrElse(categories)

  val registry = Parsers.configureRegistryFromJson(jsonRegistry)
  val bootstrapper = new CitizenBootstrap(authorizationParser, authenticationParser, registry, storageParser)

  bootstrapper.runtimeFromJson(json) match {
    case Success(runtime) => runtime.start()
    case Failure(exception) => println(exception)
  }
}
