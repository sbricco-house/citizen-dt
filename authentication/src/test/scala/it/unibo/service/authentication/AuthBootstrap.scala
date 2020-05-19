package it.unibo.service.authentication

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpRequest
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}
import io.vertx.scala.ext.web.client.{WebClient, WebClientOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.InMemoryStorage

import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.Await

object Users {
  object Andrea extends SystemUser {
    override def username: String = "andrea"
    override def password: String = "b0b00310eb29a09c24770a50f2fa373fb4d76f9cbc56fdb5f7751123e9605fb2"
    override def role: String = "citizen"
    override def email: String = "petretiandrea@gmail.com"
    override def identifier: String = "lsdfnlsmfldnskdnek"
    def loginBodyJson : JsonObject = new JsonObject().put("email", email).put("password", "andrea96")
  }

  object WrongUser {
    def loginBodyJson: JsonObject = new JsonObject().put("email", "blblbl@gmail.com").put("password", "boh")
  }
}

object AuthBootstrap {
  val LOGIN_ENDPOINT = "http://localhost:8080/login"
  val VERIFY_ENDPOINT = "http://localhost:8080/verify?token=%s"
  val LOGOUT_ENDPOINT = "http://localhost:8080/logout"
  val REFRESH_ENDPOINT = "http://localhost:8080/refreshToken"
  def getAuthorizationHeader(token: String): (String, String) = "Authorization" -> s"Bearer $token"

  def boot(): Unit = {
    val userStorage = InMemoryStorage[SystemUser, String]()
    userStorage.store(Users.Andrea.email, Users.Andrea)

    val vertx = Vertx.vertx()
    val options = JWTAuthOptions()
      .setPubSecKeys(mutable.Buffer(PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setPublicKey("keyboard cat")
        .setSymmetric(true)))

    val provider = JWTAuth.create(vertx, options)
    val auth = new AuthenticationServiceBackend(provider, userStorage)
    val verticle = new AuthenticationVerticle(auth, 8080) with RestAuthenticationApi
    Await.result(vertx.deployVerticleFuture(verticle), 5 seconds)
  }

  def client: WebClient = {
    val vertx = Vertx.vertx()
    WebClient.create(vertx, WebClientOptions().setDefaultPort(8080))
  }

  implicit class RichHttpRequest[T](request: HttpRequest[T]) {
    def putHeader(value: (String, String)): HttpRequest[T] = request.putHeader(value._1, value._2)
  }
}
