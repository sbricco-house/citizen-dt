package it.unibo.service.authentication

import io.vertx.scala.ext.web.client.{HttpRequest, WebClient}
import it.unibo.service.authentication.AuthBootstrap._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

class AuthenticationTest extends AnyFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))
  private var client: WebClient = _

  "User with correct credential" should " login into system" in {
    whenReady(client.post(LOGIN_ENDPOINT).sendJsonObjectFuture(Users.Andrea.loginBodyJson)) {
      result =>
        result.statusCode() shouldBe 201
        result.bodyAsString() shouldBe defined
    }
  }

  "User with wrong credential" should " not login into system" in {
    whenReady(client.post(LOGIN_ENDPOINT).sendJsonObjectFuture(Users.WrongUser.loginBodyJson)) {
      result =>
        result.statusCode() shouldBe 401
    }
  }

  "User logged " should " verify the token with success" in {
    whenReady(client.post(LOGIN_ENDPOINT).sendJsonObjectFuture(Users.Andrea.loginBodyJson)) {
      result =>
        result.statusCode() shouldBe 201
        val token = result.bodyAsString().get
        whenReady(client.get(VERIFY_ENDPOINT.format(token)).sendFuture()) {
          result =>
            result.statusCode() shouldBe 200
            result.bodyAsJsonObject().get.getString("email") shouldBe Users.Andrea.email
        }
    }
  }

  "User " can " logout" in {
    whenReady(client.post(LOGIN_ENDPOINT).sendJsonObjectFuture(Users.Andrea.loginBodyJson)) {
      result => val token = result.bodyAsString().get
        whenReady(client.get(LOGOUT_ENDPOINT).putHeader(getAuthorizationHeader(token)).sendFuture()) {
          result => result.statusCode() shouldBe 204
        }
    }
  }

  "User logged out " can " not verify token" in {
    whenReady(client.post(LOGIN_ENDPOINT).sendJsonObjectFuture(Users.Andrea.loginBodyJson)) {
      result => val token = result.bodyAsString().get
        whenReady(client.get(LOGOUT_ENDPOINT).putHeader(getAuthorizationHeader(token)).sendFuture()) {
          result => result.statusCode() shouldBe 204
            whenReady(client.get(VERIFY_ENDPOINT.format(token)).sendFuture()) {
              result => result.statusCode() shouldBe 401
            }
        }
    }
  }

  "User logged " can "refresh his token" in {
    whenReady(client.post(LOGIN_ENDPOINT).sendJsonObjectFuture(Users.Andrea.loginBodyJson)) {
      result => val token = result.bodyAsString().get
        whenReady(client.post(REFRESH_ENDPOINT).putHeader(getAuthorizationHeader(token)).sendFuture()) {
          result =>
            result.statusCode() shouldBe 201
            result.bodyAsString() shouldBe defined
            result.bodyAsString().get should not be token
        }
    }
  }

  "User logged out" can  " not refresh his token" in {
    whenReady(client.post(LOGIN_ENDPOINT).sendJsonObjectFuture(Users.Andrea.loginBodyJson)) {
      result => val token = result.bodyAsString().get
        whenReady(client.get(LOGOUT_ENDPOINT).putHeader(getAuthorizationHeader(token)).sendFuture()) {
          result => result.statusCode() shouldBe 204
            whenReady(client.post(REFRESH_ENDPOINT).putHeader(getAuthorizationHeader(token)).sendFuture()) {
              result =>
                result.statusCode() shouldBe 401
            }
        }
    }
  }

  override def beforeAll(): Unit = {
    AuthBootstrap.boot()
    client = AuthBootstrap.httpClient
  }

  override def afterAll(): Unit = {
    AuthBootstrap.teardown()
  }

  implicit class RichHttpRequest[T](request: HttpRequest[T]) {
    def putHeader(value: (String, String)): HttpRequest[T] = request.putHeader(value._1, value._2)
  }
}