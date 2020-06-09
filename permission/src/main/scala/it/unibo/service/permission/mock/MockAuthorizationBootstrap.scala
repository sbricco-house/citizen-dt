package it.unibo.service.permission.mock

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.auth.jwt.JWTAuth
import it.unibo.core.microservice.MicroserviceRuntime
import it.unibo.core.microservice.vertx.{MicroserviceBootstrap, _}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.service.permission.{AuthorizationService, AuthorizationVerticle}

import scala.util.{Success, Try}

/**
 * create a authorization bootstrap from a Json configuration
 * @param provider JWTAuth provider used to decode JWT token. It must be the same provider used by the authentication service
 * @param vertx The vertx instance used to boo authorization
 * @param registry Used to encode / decode categories by authorization service
 * @param defaultPort the port used by default by the boostrapper
 */
class MockAuthorizationBootstrap(provider : JWTAuth, vertx: Vertx, registry : DataParserRegistry[JsonObject], defaultPort : Int = 8081) extends MicroserviceBootstrap[JsonObject] {
  override def runtimeFromJson(config: JsonObject): Try[MicroserviceRuntime] = {
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
