package it.unibo.covid.demo

import io.vertx.lang.scala.json.Json
import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.VertxJWTProvider
import it.unibo.covid.data.Parsers
import it.unibo.core.microservice.vertx._
import it.unibo.service.permission.mock.MockRoleBasedAuthorization
object RealCaseDemo extends App {
  val default = jsonObjectFromFile("default-demo.json")

  val json = args.headOption.map(jsonObjectFromFile).getOrElse(default)
  val authorizationPart = json.getAsObject("authorization").getOrElse(default.getJsonObject("authorization"))
  val authenticationPart = json.getAsObject("authorization").getOrElse(default.getJsonObject("authentication"))
  val citizenPart = json.getAsObject("authorization").getOrElse(default.getJsonObject("citizen"))

  val registry = args.lift(1) match {
    case None => Parsers.configureRegistry()
    case Some(json) => Parsers.configureRegistryFromJson(Json.fromArrayString(json))
  }
  val key = authenticationPart.getString("jwt.key")
  val vertx = Vertx.vertx()
  println(MockRoleBasedAuthorization.fromJson(authorizationPart, authProvider, registry))
}
