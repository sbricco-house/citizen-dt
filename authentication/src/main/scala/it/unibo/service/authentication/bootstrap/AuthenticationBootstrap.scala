package it.unibo.service.authentication.bootstrap

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.Storage
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ParserLike
import it.unibo.service.authentication.AuthenticationService
import it.unibo.service.authentication.api.{RestApiAuthentication, RestApiAuthenticationVerticle}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

trait AuthenticationRuntime {
  def start()
  def stop()
}

case class AuthenticationHttpRuntime(host: String, port: Int, vertx: Vertx, authService: AuthenticationService) extends AuthenticationRuntime {
  override def start(): Unit = {
    val verticle = new RestApiAuthenticationVerticle(authService, port, host) with RestApiAuthentication
    vertx.deployVerticleFuture(verticle)
  }
  override def stop(): Unit = {
    vertx.close()
  }
}

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
      json.getAsString("jwt.key").map {
          key => JWTAuthOptions().setPubSecKeys(mutable.Buffer(PubSecKeyOptions()
            .setAlgorithm("HS256")
            .setPublicKey(key)
            .setSymmetric(true)))
        }.map(AuthenticationConfig(host, port, _))
  }

  private case class AuthenticationConfig(host: String, port: Int, jwtAuthOptions: JWTAuthOptions)

  def runtimeFromJson(json: JsonObject): Try[AuthenticationRuntime] = {
    val vertx = Vertx.vertx()
    configParser.decode(json) match {
      case Some(AuthenticationConfig(host, port, jwtOptions)) =>
        val service = AuthenticationService(JWTAuth.create(vertx, jwtOptions), userStorage)
        Success(AuthenticationHttpRuntime(host, port, vertx, service))
      case None => Failure(new IllegalArgumentException("Invalid configuration file"))
    }
  }
}