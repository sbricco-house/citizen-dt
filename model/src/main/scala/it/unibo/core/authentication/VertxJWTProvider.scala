package it.unibo.core.authentication

import io.vertx.lang.scala.json.Json
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}

import scala.concurrent.{ExecutionContext, Future}

//TODO add documentation
object VertxJWTProvider {

  def fromSymmetric(key: String, vertx: Vertx): JWTAuth = {
    JWTAuth.create(vertx, symmetricOptions(key))
  }

  def fromAsymmetric(privateKey: String, publicKey: String, vertx: Vertx): JWTAuth = {
    JWTAuth.create(vertx, asymmetricOptions(privateKey, publicKey))
  }

  def fromPublicKey(publicKey: String, vertx: Vertx): JWTAuth = {
    JWTAuth.create(vertx, publicKeyOptions(publicKey))
  }

  def symmetricOptions(key: String): JWTAuthOptions = {
    val opts = PubSecKeyOptions().setAlgorithm("HS256")
      .setPublicKey(key)
      .setSymmetric(true)
    JWTAuthOptions().addPubSecKey(opts)
  }

  def asymmetricOptions(privateKey: String, publicKey: String): JWTAuthOptions = {
    val opts = PubSecKeyOptions().setAlgorithm("RS256")
      .setSecretKey(privateKey)
      .setPublicKey(publicKey)
      .setSymmetric(false)

    JWTAuthOptions().addPubSecKey(opts)
  }

  def publicKeyOptions(publicKey: String): JWTAuthOptions = {
    val opts = PubSecKeyOptions().setAlgorithm("RS256")
        .setPublicKey(publicKey)
        .setSymmetric(false)
    JWTAuthOptions().addPubSecKey(opts)
  }

  implicit class RichAuthProvider(auth : JWTAuth) {
    def extractSystemUser(token : TokenIdentifier)(implicit context : ExecutionContext) : Future[SystemUser] = {
      auth.authenticateFuture(Json.obj("jwt" -> token.token))
        .map(_.principal())
        .map(AuthenticationParsers.SystemUserParser.decode)
        .flatMap {
          case Some(user) => Future.successful(user)
          case _ => Future.failed(new IllegalArgumentException("token not found"))
        }
    }
  }
}
