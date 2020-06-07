package it.unibo.covid.demo

import java.net.URI

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.{SystemUser, VertxJWTProvider}
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ParserLike
import it.unibo.covid.bootstrap.CitizenBootstrap
import it.unibo.covid.data.Parsers
import it.unibo.covid.demo.ClientParsers.{authenticationParser, authorizationParser}
import it.unibo.service.authentication.app.MockUserStorage
import it.unibo.service.authentication.bootstrap.AuthenticationBootstrap
import it.unibo.service.citizen.HistoryStorage
import it.unibo.service.permission.client.AuthorizationClient
import it.unibo.service.permission.mock.MockAuthorizationBootstrap

import scala.util.{Failure, Success}
object RealCaseDemo extends App {
  private val storageUserParser = ParserLike.decodeOnly[JsonObject, HistoryStorage] {
    (_ : JsonObject) => Some(HistoryStorage.fromInMemory())
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

  val citizenBootstrapper = new CitizenBootstrap(authorizationParser, authenticationParser, registry, storageUserParser)
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
