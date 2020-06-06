package it.unibo.covid.demo

import java.net.URI

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, InMemoryStorage, Storage}
import it.unibo.core.parser.ParserLike
import it.unibo.covid.demo.DemoFromJson.registry
import it.unibo.service.authentication.AuthenticationService
import it.unibo.service.authentication.client.AuthenticationClient
import it.unibo.service.permission.AuthorizationService
import it.unibo.service.permission.client.AuthorizationClient
import it.unibo.core.microservice.vertx._
object ClientParsers {
  val authorizationParser = ParserLike.decodeOnly[JsonObject, AuthorizationService] {
    (json : JsonObject) => json.getAsString("authorization_client_uri").map(new URI(_)).map(AuthorizationClient(_, registry))
  }
  val authenticationParser = ParserLike.decodeOnly[JsonObject, AuthenticationService] {
    (json : JsonObject) => json.getAsString("authentication_client_uri").map(new URI(_)).map(AuthenticationClient(_))
  }
}
