package it.unibo.core.authentication

import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}

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

  private def symmetricOptions(key: String): JWTAuthOptions = {
    val opts = PubSecKeyOptions().setAlgorithm("HS256")
      .setPublicKey(key)
      .setSymmetric(true)
    JWTAuthOptions().addPubSecKey(opts)
  }

  private def asymmetricOptions(privateKey: String, publicKey: String): JWTAuthOptions = {
    val opts = PubSecKeyOptions().setAlgorithm("RS256")
      .setSecretKey(privateKey)
      .setPublicKey(publicKey)
      .setSymmetric(false)

    JWTAuthOptions().addPubSecKey(opts)
  }

  private def publicKeyOptions(publicKey: String): JWTAuthOptions = {
    val opts = PubSecKeyOptions().setAlgorithm("RS256")
        .setPublicKey(publicKey)
        .setSymmetric(false)
    JWTAuthOptions().addPubSecKey(opts)
  }
}
