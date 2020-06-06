package it.unibo.service.permission.mock

import io.vertx.core.json.JsonObject
import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.auth.AuthProvider
import io.vertx.scala.ext.auth.jwt.JWTAuth
import it.unibo.core.microservice.ServiceRuntime
import it.unibo.core.microservice.vertx.ServiceBootstrap
import it.unibo.core.parser.DataParserRegistry
import it.unibo.service.permission.{AuthorizationService, AuthorizationVerticle}
import it.unibo.core.microservice.vertx._

import scala.util.{Success, Try}

class MockAuthorizationBootstrap(provider : JWTAuth, vertx: Vertx, registry : DataParserRegistry[JsonObject], defaultPort : Int = 8081) extends ServiceBootstrap[JsonObject] {
  override def runtimeFromJson(config: JsonObject): Try[ServiceRuntime] = {
    val authorization : AuthorizationService = extractServiceFromConfig(config)
    println("AUTHORIZATION MOCK = " + authorization)
    val host = config.getString("api.rest.host", "localhost")
    val port = config.getInteger("api.rest.port", defaultPort)
    Success(new VertxRuntime(vertx, () => new AuthorizationVerticle(authorization, registry, port, host)))
  }

  def extractServiceFromConfig(jsonObject: JsonObject) : AuthorizationService = {
    val service = jsonObject.getAsString("type") match {
      case Some("role_based") => MockRoleBasedAuthorization.fromJson(jsonObject, provider, registry)
      case _ => None
    }
    service.getOrElse(MockAuthorization.acceptAll(registry))
  }
}
