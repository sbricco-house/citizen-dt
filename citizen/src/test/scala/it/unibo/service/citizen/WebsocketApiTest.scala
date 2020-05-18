package it.unibo.service.citizen
import java.util.concurrent.TimeUnit

import io.vertx.core.http.{UpgradeRejectedException, WebsocketRejectedException}
import io.vertx.scala.core.http.{HttpClient, WebSocket, WebSocketConnectOptions}
import io.vertx.scala.ext.web.client.WebClient
import it.unibo.service.citizen.HttpBootstrap.STATE_ENDPOINT
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.{Assertion, BeforeAndAfterAll, FlatSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import HttpBootstrap._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class WebsocketApiTest extends AsyncFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures with DataJsonMatcher {
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))
  var client : HttpClient = _
  "citizen api" should "can't upgrade to websocket if authorization header missing" in {
    client.webSocketFuture(STATE_ENDPOINT).transformWith {
      case Success(socket) => fail()
      case Failure(exception) => succeed
    }
  }

  "citizen api" should "enable to upgrade to websocket" in {
    client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER)).transformWith {
      case Success(_) => succeed
      case _ => fail()
    }
  }

  override def beforeAll(): Unit = {
    Await.result(HttpBootstrap.boot(), Duration(5000, TimeUnit.MILLISECONDS))
    client = HttpBootstrap.httpClient()
  }

  override def afterAll(): Unit = {
    client.close()
  }
}
