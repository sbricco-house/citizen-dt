package it.unibo.service.authentication

import java.net.URI

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpRequest
import io.vertx.scala.core.{DeploymentOptions, Vertx}
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}
import io.vertx.scala.ext.web.client.{WebClient, WebClientOptions}
import it.unibo.core.authentication.{SystemUser, VertxJWTProvider}
import it.unibo.core.data.InMemoryStorage
import it.unibo.service.authentication.api.{RestApiAuthentication, RestApiAuthenticationVerticle}
import it.unibo.service.authentication.app.MockUserStorage
import it.unibo.service.authentication.client.AuthenticationClient

import scala.concurrent.duration._
import scala.collection.mutable
import scala.concurrent.Await
import scala.io.Source

object Users {
  object Citizen1 extends SystemUser {
    override def username: String = "citizen1"
    override def password: String = "citizen1"
    override def role: String = "citizen"
    override def email: String = "citizen1@email.com"
    override def identifier: String = "citizen1"
    def loginBodyJson : JsonObject = new JsonObject().put("email", email).put("password", password)
  }

  object WrongUser {
    def loginBodyJson: JsonObject = new JsonObject().put("email", "blblbl@gmail.com").put("password", "boh")
  }
}

object AuthBootstrap {

  var vertx: Vertx = _
  val config = new JsonObject(Source.fromResource("testconfig.json").mkString)
  val port: Integer = config.getInteger("http_port")

  val LOGIN_ENDPOINT = s"http://localhost:$port/login"
  val VERIFY_ENDPOINT = s"http://localhost:$port/verify?token=%s"
  val LOGOUT_ENDPOINT = s"http://localhost:$port/logout"
  val REFRESH_ENDPOINT = s"http://localhost:$port/refreshToken"

  def getAuthorizationHeader(token: String): (String, String) = "Authorization" -> s"Bearer $token"

  def boot(): Unit = {
    vertx = Vertx.vertx()

    val storage = MockUserStorage.generateDefault()
    val jwtProvider = VertxJWTProvider.fromSymmetric("blabla", vertx)
    val service = AuthenticationService(jwtProvider, storage)
    val deploy = vertx.deployVerticleFuture(new RestApiAuthenticationVerticle(service, port, "localhost") with RestApiAuthentication)
    Await.result(deploy, 10 seconds)
  }

  def httpClient: WebClient = {
    WebClient.create(vertx, WebClientOptions().setDefaultPort(port))
  }

  def client: AuthenticationClient = AuthenticationClient("localhost", port)

  def teardown(): Unit = {
    Await.result(vertx.closeFuture(), 10 seconds)
  }

  implicit class RichHttpRequest[T](request: HttpRequest[T]) {
    def putHeader(value: (String, String)): HttpRequest[T] = request.putHeader(value._1, value._2)
  }
}
