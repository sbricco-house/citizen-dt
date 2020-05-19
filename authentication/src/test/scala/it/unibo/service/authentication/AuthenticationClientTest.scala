package it.unibo.service.authentication

import java.net.URI

import it.unibo.core.microservice.{Fail, Response}
import it.unibo.core.protocol.ServiceError.{Unauthenticated, Unauthorized}
import it.unibo.service.authentication.AuthBootstrap.{LOGIN_ENDPOINT, LOGOUT_ENDPOINT, REFRESH_ENDPOINT, getAuthorizationHeader}
import it.unibo.service.authentication.client.AuthenticationClient
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.ExecutionContext
import scala.reflect.internal.util.FakePos

class AuthenticationClientTest extends AnyFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(100, Seconds), interval = Span(100, Millis))
  implicit val executionContext: ExecutionContext = ExecutionContext.global
  private val client = new AuthenticationClient(URI.create("http://localhost:8080"))


  "A client" should " be able to login" in {
    whenReady(client.login("petretiandrea@gmail.com", "andrea96").future) {
      case Response(content) => content.token should not be empty
      case Fail(error) => assert(fail(error.toString))
    }
  }

  "A client " can " verify its token" in {
    whenReady(client.login("petretiandrea@gmail.com", "andrea96")
      .flatMap(token => client.getAuthenticatedUser(token)).future)
    {
      case Response(content) => content.email shouldBe "petretiandrea@gmail.com"
      case Fail(error) => assert(fail(error.toString))
    }
  }

  "A client" can " logout" in {
    whenReady(client.login("petretiandrea@gmail.com", "andrea96")
      .flatMap(token => client.logout(token)).future)
    {
      case Response(content) => content shouldBe true
      case Fail(error) => assert(fail(error.toString))
    }
  }

  "After logout, a client" should " not be able to verify its token" in {
    whenReady(client.login("petretiandrea@gmail.com", "andrea96")
      .flatMap(token => client.logout(token).flatMap(_ => client.getAuthenticatedUser(token)))
      .future) {
      case Fail(Unauthenticated(m)) => succeed
      case response => fail(response.toString)
    }
  }

  "Client logged " can "refresh his token" in {
    whenReady(client.login("petretiandrea@gmail.com", "andrea96")
      .flatMap(token => client.refresh(token)).future) {
      case Response(content) => println(content); assert(true)
      case Fail(error) => assert(fail(error.toString))
    }
  }

  "Client not logged " can  " not refresh his token" in {
    whenReady(client.login("petretiandrea@gmail.com", "andrea96")
      .flatMap(token => client.logout(token).flatMap(_ => client.refresh(token)))
      .future)
    {
      case Fail(Unauthenticated(m)) => succeed
      case _ => assert(fail())
    }
  }

  override def beforeAll(): Unit = {
    AuthBootstrap.boot()
  }
}