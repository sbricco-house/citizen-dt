package it.unibo.service.authentication

import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.Storage
import it.unibo.service.authentication.api.{RestApiAuthentication, RestApiAuthenticationVerticle}
import it.unibo.service.authentication.app.MockUserStorage

import scala.collection.mutable
import scala.concurrent.Future

class AuthenticationVerticle extends ScalaVerticle {

  val userStorage: Storage[SystemUser, String] = MockUserStorage.generateDefault()

  override def startFuture(): Future[_] = {
    super.startFuture()

    val host = config.getString("api.rest.host", "localhost")
    val port = config.getInteger("api.rest.port", 8080)
    val symmetricKey = config.getString("jwt.key", "keyboard cat")

    val options = JWTAuthOptions().setPubSecKeys(mutable.Buffer(PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setPublicKey(symmetricKey)
        .setSymmetric(true)))

    val provider = JWTAuth.create(vertx, options)
    val auth = AuthenticationService(provider, userStorage)
    val verticle = new RestApiAuthenticationVerticle(auth, port, host) with RestApiAuthentication

    vertx.deployVerticleFuture(verticle)
  }
}
