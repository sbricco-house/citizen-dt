package it.unibo.service.permission

import java.net.URI

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.{SystemUser, TokenIdentifier}
import it.unibo.core.data.LeafCategory
import it.unibo.core.microservice.Response
import it.unibo.core.parser.{DataParserRegistry, ValueParser, VertxJsonParser}
import it.unibo.service.permission.client.AuthorizationClient
import it.unibo.service.permission.mock.MockAuthorization
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.ExecutionContext
class AuthorizationClientTest extends AnyFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures {
  import AuthorizationBootstrapper._
  import PermissionScope._
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(100, Millis))
  implicit val executionContext: ExecutionContext = ExecutionContext.global

  "authorization client" should "verify category access" in {
    whenReady(authorizationClient.authorizeRead(userA, userA.token, categoryA).future) {
      case Response(`categoryA`) => succeed
      case other => println(other)
    }
    whenReady(authorizationClient.authorizeWrite(userA, userA.token, categoryA).future) {
      case Response(`categoryA`) => succeed
      case _ => fail()
    }
  }

  "authorization client" should "verify what user can access" in {
    whenReady(authorizationClient.authorizedReadCategories(userA, userA.token).future) {
      case Response(Seq(`categoryA`, `categoryB`, `categoryC`)) => succeed
      case _ => fail()
    }
    whenReady(authorizationClient.authorizedReadCategories(userB, userA.token).future) {
      case Response(Seq(`categoryB`)) => succeed
      case _ => fail()
    }
    whenReady(authorizationClient.authorizedReadCategories(userC, userA.token).future) {
      case Response(Seq(`categoryA`)) => succeed
      case _ => fail()
    }
  }

  override def afterAll(): Unit = vertx.close()
}

object AuthorizationBootstrapper {
  import PermissionScope._
  val mockAuthorization = MockAuthorization{
    Map(
      (userA.token -> userA.token) -> Seq(categoryA, categoryB, categoryC),
      (userB.token -> userA.token) -> Seq(categoryB),
      (userC.token -> userA.token) -> Seq(categoryA),
    )
  }

  private val verticle = new AuthorizationVerticle(mockAuthorization, categoryParser)
  val vertx = Vertx.vertx()
  vertx.deployVerticle(verticle)
  val authorizationClient = new AuthorizationClient(new URI("http://localhost:8080"), categoryParser)
}
