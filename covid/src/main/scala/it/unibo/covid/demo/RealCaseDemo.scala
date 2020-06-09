package it.unibo.covid.demo

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.VertxJWTProvider
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ParserLike
import it.unibo.covid.Personalnfo
import it.unibo.covid.bootstrap.CitizenBootstrap
import it.unibo.covid.data.Parsers
import it.unibo.covid.demo.ClientParsers._
import it.unibo.service.authentication.app.MockUserStorage
import it.unibo.service.authentication.bootstrap.AuthenticationBootstrap
import it.unibo.service.citizen.HistoryStorage
import it.unibo.service.permission.mock.MockAuthorizationBootstrap
import scala.util.{Failure, Success}

/**
 * A real case demo used to simulate a covid-like context.
 * This application starts:
 *  - n citizen digital twin service
 *  - mock role authorization
 *  - authentication
 *
 * the configuration is passed through a json configuration, that follow this pattern:
 *
 * {
 *  citizen : [ {#Citizen object schema} ],
 *  users : [ {#User object schema} ],
 *  authentication : { #Authentication object schema },
 *  authorization : { #Authorization object schema },
 * }
 *
 * #Citizen object schema
 * {
 *  "id" : #citizen id,
 *  "coap_port" : #default 5683,
 *  "http_port" : #default 8080
 *  "authentication_client_uri" : #URI authentication,
 *  "authorization_client_uri" : #URI authorization,
 *  "personal_info" : {
 *    "name": ..,
 *    "surname": ..,
 *    "birthdate": #PATTERN DD/MM/YYYY,
 *    "cf": ..
 *   }
 * }
 * #User object schema (user account used to log in the system)
 *
 * {
 *    "identifier" : ...,
 *    "username" : ...,
 *    "email" : ...,
 *    "password" : ...,
 *    "role" : ...
 * }
 *
 * #Authentication object schema
 * {
 *    "api.rest.host": ..,
 *    "api.rest.port": ..,
 *    "jwt.key": ".." #the secret shared among servers
 * }
 *
 * #Authorization object schema
 * {
 *    "api.rest.host": ..
 *    "api.rest.port": ..,
 *    "type" : "role_based", # only supported
 *    "read_map_permission" : [
 *      {
 *        "role" : ..
 *        "categories : [#array of string categories]
 *      }
 *    ]
 *    "write_map_permission" : [
 *      {
 *        "role" : ..
 *        "categories : [#array of string categories]
 *      }
 *    ]
 *    "categories" : [#array of string categories],
 *    "write_citizen_permission" : [#array of string categories]
 * }
 */
object RealCaseDemo extends App {
  private val storageUserParser = ParserLike.decodeOnly[JsonObject, HistoryStorage] {
    (json : JsonObject) =>
      val info = json.getAsObject("personal_info").map(info => Personalnfo.fromJson(info)).getOrElse(Seq())
      Some(info.foldRight(HistoryStorage.fromInMemory())((data, store) => { store.store(data.identifier, data); store }))
  }

  val default = jsonObjectFromFile("default-demo.json")

  val json = args.headOption.map(jsonObjectFromFile).getOrElse(default)
  val authorizationPart = json.getAsObject("authorization").getOrElse(default.getJsonObject("authorization"))
  val authenticationPart = json.getAsObject("authentication").getOrElse(default.getJsonObject("authentication"))
  val citizenArrayPart = json.getAsArray("citizen").getOrElse(default.getJsonArray("citizen"))

  val citizensPart = citizenArrayPart.getAsObjectSeq match {
    case Some(array) => array
    case None => throw new IllegalArgumentException("citizens part must be an array")
  }
  val registry = args.lift(1) match {
    case None => Parsers.configureRegistry()
    case Some(json) => Parsers.configureRegistryFromJson(Json.fromArrayString(json))
  }

  val storage = json.getAsArray("users").flatMap(MockUserStorage.userStorageParser.decode) match {
    case None => throw new IllegalArgumentException("users must be configured in file")
    case Some(some) => some
  }

  val authenticationRuntime = new AuthenticationBootstrap(storage).runtimeFromJson(authenticationPart)
  authenticationRuntime match {
    case Success(runtime) => runtime.start()
    case _ => throw new IllegalArgumentException("wrong configuration for authentication")
  }

  val citizenBootstrapper = new CitizenBootstrap(createAuthorizationParser(registry), authenticationParser, registry, storageUserParser)
  citizensPart.map(citizenBootstrapper.runtimeFromJson).foreach {
    case Success(runtime) => runtime.start()
    case Failure(exception) => throw exception
  }

  val key = authenticationPart.getString("jwt.key")
  val vertx = Vertx.vertx()
  val authProvider = VertxJWTProvider.fromSymmetric(key, vertx)

  val authorizationBootstrapper = new MockAuthorizationBootstrap(authProvider, vertx, registry)

  authorizationBootstrapper.runtimeFromJson(authorizationPart) match {
    case Success(runtime) => runtime.start()
    case Failure(exc) => throw exc
  }
}
