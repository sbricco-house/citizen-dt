package it.unibo.service.authentication.bootstrap

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}
import it.unibo.core.authentication.{SystemUser, VertxJWTProvider}
import it.unibo.core.data.Storage
import it.unibo.core.microservice.ServiceRuntime
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ParserLike
import it.unibo.service.authentication.AuthenticationService
import it.unibo.service.authentication.api.{RestApiAuthentication, RestApiAuthenticationVerticle}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

case class AuthenticationHttpRuntime(val host: String, val port: Int, val vertx: Vertx, authService: AuthenticationService)
  extends VertxRuntime(vertx, () => { new RestApiAuthenticationVerticle(authService, port, host) with RestApiAuthentication })


/**
 * Bootstrap class for AuthenticationService using starting from a JSON config.
 * @example A complete example of JSON config file. All field are mandatory.
 *          {
 *            "api.rest.host" : "localhost",
 *            "api.rest.port" : 8123,
 *            "jwt.key": "blabla"
 *          }
 */
class AuthenticationBootstrap(userStorage: Storage[SystemUser, String]) {

  private val configParser = ParserLike.decodeOnly[JsonObject, AuthenticationConfig] {
    json =>
      val host = json.getString("api.rest.host", "localhost")
      val port = json.getInteger("api.rest.port", 8123)
      json.getAsString("jwt.key")
        .map(VertxJWTProvider.symmetricOptions)
        .map(AuthenticationConfig(host, port, _))
  }

  private case class AuthenticationConfig(host: String, port: Int, jwtAuthOptions: JWTAuthOptions)

  def runtimeFromJson(json: JsonObject): Try[ServiceRuntime] = {
    val vertx = Vertx.vertx()
    configParser.decode(json) match {
      case Some(AuthenticationConfig(host, port, jwtOptions)) =>
        val service = AuthenticationService(JWTAuth.create(vertx, jwtOptions), userStorage)
        Success(AuthenticationHttpRuntime(host, port, vertx, service))
      case None => Failure(new IllegalArgumentException("Invalid configuration file"))
    }
  }
}