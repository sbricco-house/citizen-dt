package it.unibo.service.authentication

import it.unibo.core.microservice.{Fail, Response}
import it.unibo.core.utils.ServiceError.Unauthenticated
import it.unibo.service.authentication.client.AuthenticationClient
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class AuthenticationClientTest extends AnyFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(100, Millis))
  implicit val executionContext: ExecutionContext = ExecutionContext.global
  private var client: AuthenticationClient = _


  "A client" should " be able to login" in {
    whenReady(client.login(Users.Citizen1.email, Users.Citizen1.password).future) {
      case Response(content) => content.token.token should not be empty
      case Fail(error) => assert(fail(error.toString))
    }
  }

  "A client " can " verify its token" in {
    whenReady(client.login(Users.Citizen1.email, Users.Citizen1.password)
      .flatMap(authInfo => client.verifyToken(TokenIdentifier(authInfo.token.token))).future)
    {
      case Response(content) => content.email shouldBe Users.Citizen1.email
      case Fail(error) => assert(fail(error.toString))
    }
  }

  "A client" can " logout" in {
    whenReady(client.login(Users.Citizen1.email, Users.Citizen1.password)
      .flatMap(authInfo => client.logout(TokenIdentifier(authInfo.token.token))).future)
    {
      case Response(content) => content shouldBe true
      case Fail(error) => assert(fail(error.toString))
    }
  }

  "After logout, a client" should " not be able to verify its token" in {
    whenReady(client.login(Users.Citizen1.email, Users.Citizen1.password)
      .flatMap(authInfo => client.logout(TokenIdentifier(authInfo.token.token)).flatMap(_ => client.verifyToken(TokenIdentifier(authInfo.token.token))))
      .future) {
      case Fail(Unauthenticated(m)) => succeed
      case response => fail(response.toString)
    }
  }

  "Client logged " can "refresh his token" in {
    whenReady(client.login(Users.Citizen1.email, Users.Citizen1.password)
      .flatMap(authInfo => client.refresh(TokenIdentifier(authInfo.token.token))).future) {
      case Response(content) => assert(true)
      case Fail(error) => assert(fail(error.toString))
    }
  }

  "Client not logged " can  " not refresh his token" in {
    whenReady(client.login(Users.Citizen1.email, Users.Citizen1.password)
      .flatMap(authInfo => client.logout(TokenIdentifier(authInfo.token.token)).flatMap(_ => client.refresh(TokenIdentifier(authInfo.token.token))))
      .future)
    {
      case Fail(Unauthenticated(m)) => succeed
      case _ => assert(fail())
    }
  }

  override def beforeAll(): Unit = {
    AuthBootstrap.boot()
    client = AuthBootstrap.client
  }

  override def afterAll(): Unit = {
    Await.result(client.close(), 5 seconds)
    AuthBootstrap.teardown()
  }
}